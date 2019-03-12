package com.gmail.tiomamaster.chart

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.gson.Gson
import java.nio.charset.Charset

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val columnStr = resources.openRawResource(
            resources.getIdentifier("column", "raw", packageName)
        ).readBytes().toString(Charset.defaultCharset())
        val gson = Gson()
        val chart = gson.fromJson<Chart>(columnStr, Chart::class.java)
        val v = ChartView(this)
        setContentView(v)
        v.chart = chart
    }
}

class ChartView(context: Context) : View(context) {

    var chart: Chart? = null
        set(value) {
            field = value
            x = (field?.columns?.get(0) as Iterable<*>).drop(1) as List<Float>
            field?.columns?.drop(1)?.run {
                val y = mutableListOf<List<Float>>()
                forEach {
                    y.add((it as Iterable<*>).drop(1) as List<Float>)
                }
                this@ChartView.y = y
            }
            colors = listOfNotNull(
                field?.colors?.y0,
                field?.colors?.y1,
                field?.colors?.y2,
                field?.colors?.y3
            ).map {
                Color.parseColor(it)
            }
        }

    private lateinit var x: List<Float>

    private lateinit var y: List<List<Float>>

    private lateinit var colors: List<Int>

    private val paint = Paint()
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    private lateinit var bigChartsPaths: List<Path>
    private lateinit var smallChartsPaths: List<Path>
    private lateinit var smallChartsRectangleRect: RectF

    private lateinit var mappedX: List<Float>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        mappedX = x.run {
            val xMin = first()
            val xMax = last()
            val k = w / (xMax - xMin)
            map { fl -> Math.abs((xMax - fl) * k - w) }
        }

        bigChartsPaths = drawChart(0, h / 2, mappedX)
        val smallChartsTomMargin = 25
        val smallChartsMaxY = (h / 2) + smallChartsTomMargin
        val smallChartsHeight = h / 10
        smallChartsPaths = drawChart(smallChartsMaxY, smallChartsHeight, mappedX)
        smallChartsRectangleRect = RectF(
            0f,
            smallChartsMaxY.toFloat(),
            w.toFloat(),
            smallChartsMaxY.toFloat() + smallChartsHeight
        )
        invalidate()
    }

    private fun drawChart(maxY: Int, chartHeight: Int, mappedX: List<Float>): List<Path> {
        val chatsMaxValues = y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).max()!! }
        val maxOfAll = chatsMaxValues.max()!!
        val scales = chatsMaxValues.map {
            if (it == maxOfAll) chartHeight.toFloat()
            else it * chartHeight / maxOfAll
        }

        return y.mapIndexed { index, ys ->
            (if (hasBounds) ys.subList(leftBound, rightBound)
            else ys).let {boundedYs ->
                val scale = scales[index]
                val yMin = boundedYs.min()!!
                val yMax = boundedYs.max()!!
                val k = scale / (yMax - yMin)
                Path().apply {
                    mappedX.forEachIndexed { i, fl ->
                        val y = Math.abs((yMax - boundedYs[i]) * k - scale)
                        if (i == 0) {
                            moveTo(fl, y)
                        } else {
                            lineTo(fl, chartHeight - y + maxY)
                        }
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)

        bigChartsPaths.forEachIndexed { index, path ->
            paint.apply {
                isAntiAlias = true
                isDither = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 2.5f
                color = colors[index]
            }
            canvas.drawPath(path, paint)
            canvas.drawPath(smallChartsPaths[index], paint)
        }
        canvas.drawRect(smallChartsRectangleRect, paint.apply {
            alpha = 25
            style = Paint.Style.FILL
        })
    }

    private var leftBound = -1
    private var rightBound = -1

    private val hasBounds get() = leftBound != -1 && rightBound != -1

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                println("$x $y")
                println(smallChartsRectangleRect.contains(x, y))

                leftBound = Math.min((mappedX.size * x / width).toInt(), mappedX.lastIndex - 10)
                rightBound = leftBound + 10

                val boundedMappedX = (if (hasBounds) this.x.subList(leftBound, rightBound) else this.x).run {
                    val xMin = first()
                    val xMax = last()
                    val k = width / (xMax - xMin)
                    map { fl -> Math.abs((xMax - fl) * k - width) }
                }

                bigChartsPaths = drawChart(0, height / 2, boundedMappedX)
                invalidate()

                this.y.forEachIndexed { index, list ->
                    println(index)
                    list.subList(leftBound, rightBound).forEach {
                        print(it)
                        print(" ")
                    }
                    println()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return false
    }
}