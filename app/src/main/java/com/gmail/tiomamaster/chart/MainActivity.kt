package com.gmail.tiomamaster.chart

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.5f
    }
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paths: List<Path>
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        drawChart(w, h / 2)
    }

    private fun drawChart(w: Int, h: Int) {
        val mappedX = x.run {
            val xMin = first()
            val xMax = last()
            val k = w / (xMax - xMin)
            map { fl -> Math.abs((xMax - fl) * k - w) }
        }

        val chatsMaxValues = y.map { it.max()!! }
        val maxOfAll = chatsMaxValues.max()!!
        val constraints = chatsMaxValues.map {
            if (it == maxOfAll) h.toFloat()
            else it * h / maxOfAll
        }

        paths = y.mapIndexed { index, ys ->
            val constraint = constraints[index]
            val yMin = ys.min()!!
            val yMax = ys.max()!!
            Path().apply {
                mappedX.forEachIndexed { index, fl ->
                    val k = constraint / (yMax - yMin)
                    val y = Math.abs((yMax - ys[index]) * k - constraint)
                    if (index == 0) {
                        moveTo(fl, y)
                    } else {
                        lineTo(fl, h - y)
                    }
                }
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)

        paths.forEachIndexed { index, path ->
            paint.apply {
                color = colors[index]
            }
            canvas.drawPath(path, paint)
        }
    }
}