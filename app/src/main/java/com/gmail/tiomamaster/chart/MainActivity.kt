package com.gmail.tiomamaster.chart

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.nio.charset.Charset

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
    private val strokeWidth = 2.5f
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    private lateinit var bigChartsPaths: List<Path>
    private lateinit var smallChartsPaths: List<Path>

    private val boundsRectWidth = 25f
    private lateinit var leftBoundRect: RectF
    private lateinit var beforeLeftBoundRect: RectF
    private lateinit var rightBoundRect: RectF
    private lateinit var afterRightBoundRect: RectF
    private lateinit var betweenBoundsStrokeRect: RectF

    private lateinit var mappedX: List<Float>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        mappedX = x.run {
            val xMin = first()
            val xMax = last()
            val k = w / (xMax - xMin)
            map { fl -> w - (xMax - fl) * k }
        }

        bigChartsPaths = calcChartPaths(0, h / 2, mappedX)
        val smallChartsTomMargin = 25
        val smallChartsMaxY = (h / 2) + smallChartsTomMargin
        val smallChartsHeight = h / 10
        smallChartsPaths = calcChartPaths(smallChartsMaxY, smallChartsHeight, mappedX)
//        smallChartsRect = RectF(
//            0f,
//            smallChartsMaxY.toFloat(),
//            w.toFloat(),
//            smallChartsMaxY.toFloat() + smallChartsHeight
//        )
        leftBoundRect = RectF(
            0f,
            smallChartsMaxY.toFloat() - strokeWidth,
            boundsRectWidth,
            smallChartsMaxY.toFloat() + smallChartsHeight + strokeWidth
        )
        beforeLeftBoundRect = RectF(
            0f,
            smallChartsMaxY.toFloat() - strokeWidth,
            leftBoundRect.left,
            smallChartsMaxY.toFloat() + smallChartsHeight + strokeWidth
        )
        rightBoundRect = RectF(
            w.toFloat() - boundsRectWidth,
            smallChartsMaxY.toFloat() - strokeWidth,
            w.toFloat(),
            smallChartsMaxY.toFloat() + smallChartsHeight + strokeWidth
        )
        afterRightBoundRect = RectF(
            rightBoundRect.right,
            smallChartsMaxY.toFloat() - strokeWidth,
            w.toFloat(),
            smallChartsMaxY.toFloat() + smallChartsHeight + strokeWidth
        )
        betweenBoundsStrokeRect = RectF(
            boundsRectWidth,
            smallChartsMaxY.toFloat(),
            w.toFloat() - boundsRectWidth,
            smallChartsMaxY.toFloat() + smallChartsHeight
        )
        postInvalidate()
    }

    private fun calcChartPaths(maxY: Int, chartHeight: Int, mappedX: List<Float>): List<Path> {
        val chatsMaxYValues = y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).max()!! }
        val chatsMinYValues = y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).min()!! }
        val maxYOfAll = chatsMaxYValues.max()!!
        val minYOfAll = chatsMinYValues.min()!!
        val kY = chartHeight / (maxYOfAll - minYOfAll)

        return y.mapIndexed { _, ys ->
            (if (hasBounds) ys.subList(leftBound, rightBound)
            else ys).let { boundedYs ->
                Path().apply {
                    mappedX.forEachIndexed { i, x ->
                        val y = chartHeight - (maxYOfAll - boundedYs[i]) * kY
                        if (i == 0) {
                            moveTo(x, chartHeight - y + maxY)
                        } else {
                            lineTo(x, chartHeight - y + maxY)
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
                strokeWidth = this@ChartView.strokeWidth
                color = colors[index]
            }
            canvas.drawPath(path, paint)
            canvas.drawPath(smallChartsPaths[index], paint)
        }

        canvas.drawRect(leftBoundRect, paint.apply {
            alpha = 100
            style = Paint.Style.FILL
        })
        canvas.drawRect(beforeLeftBoundRect, paint.apply {
            alpha = 50
            style = Paint.Style.FILL
        })
        canvas.drawRect(rightBoundRect, paint.apply {
            alpha = 100
            style = Paint.Style.FILL
        })
        canvas.drawRect(afterRightBoundRect, paint.apply {
            alpha = 50
            style = Paint.Style.FILL
        })
        canvas.drawRect(betweenBoundsStrokeRect, paint.apply {
            alpha = 100
            style = Paint.Style.STROKE
        })
    }

    private var leftBound = -1
    private var rightBound = -1

    private val hasBounds get() = leftBound != -1 && rightBound != -1

    private var leftBoundClicked = false
    private var rightBoundClicked = false
    private var betweenBoundsClicked = false
        set(value) {
            field = value
            betweenBoundsStrokeRectWidth = betweenBoundsStrokeRect.right - betweenBoundsStrokeRect.left
        }
    private var betweenBoundsStrokeRectWidth = 0f
    private var prevX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                leftBoundClicked = leftBoundRect.contains(x, y)
                rightBoundClicked = rightBoundRect.contains(x, y)
                betweenBoundsClicked = betweenBoundsStrokeRect.contains(x, y)
                prevX = x
            }
            MotionEvent.ACTION_MOVE -> {
                if (leftBoundClicked) {
                    val left = Math.min(Math.max(x - boundsRectWidth / 2, 0f), rightBoundRect.left - boundsRectWidth)
                    val right = left + boundsRectWidth
                    leftBoundRect.left = left
                    leftBoundRect.right = right

                    beforeLeftBoundRect.right = leftBoundRect.left
                    betweenBoundsStrokeRect.left = leftBoundRect.right

                    recalculateAndPostInvalidate()
                }
                if (rightBoundClicked) {
                    val left = Math.max(Math.min(x - boundsRectWidth / 2, width - boundsRectWidth), leftBoundRect.right)
                    val right = left + boundsRectWidth
                    rightBoundRect.left = left
                    rightBoundRect.right = right

                    afterRightBoundRect.left = rightBoundRect.right
                    betweenBoundsStrokeRect.right = rightBoundRect.left

                    recalculateAndPostInvalidate()
                }
                if (betweenBoundsClicked) {
                    val d = x - prevX
                    prevX = x
                    when {
                        d > 0 && rightBoundRect.right < width || d < 0 && leftBoundRect.left > 0 -> {
                            val betweenLeft = Math.max(betweenBoundsStrokeRect.left + d, boundsRectWidth)
                            val betweenRight = Math.min(betweenBoundsStrokeRect.right + d, width - boundsRectWidth)
                            if (betweenRight - betweenLeft < betweenBoundsStrokeRectWidth) return false
                            betweenBoundsStrokeRect.left = betweenLeft
                            betweenBoundsStrokeRect.right = betweenRight

                            val leftBoundLeft = betweenBoundsStrokeRect.left - boundsRectWidth
                            val leftBoundRight = leftBoundLeft + boundsRectWidth
                            leftBoundRect.left = leftBoundLeft
                            leftBoundRect.right = leftBoundRight

                            val rightBoundLeft = betweenBoundsStrokeRect.right
                            val rightBoundRight = rightBoundLeft + boundsRectWidth
                            rightBoundRect.left = rightBoundLeft
                            rightBoundRect.right = rightBoundRight

                            beforeLeftBoundRect.right = leftBoundRect.left
                            afterRightBoundRect.left = rightBoundRect.right

                            recalculateAndPostInvalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                leftBoundClicked = false
                rightBoundClicked = false
                betweenBoundsClicked = false
            }
        }
        return true
    }

    private fun recalculateAndPostInvalidate() {
        leftBound = mappedX.size * leftBoundRect.left.toInt() / width
        rightBound = mappedX.size * rightBoundRect.right.toInt() / width

        val boundedMappedX = (if (hasBounds) this.x.subList(leftBound, rightBound) else this.x).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax - xMin)
            map { fl -> width - (xMax - fl) * k }
        }
        bigChartsPaths = calcChartPaths(0, height / 2, boundedMappedX)
        postInvalidate()
    }
}