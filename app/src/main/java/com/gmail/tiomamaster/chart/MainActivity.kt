package com.gmail.tiomamaster.chart

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import kotlin.math.max

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

    private val formatter = SimpleDateFormat("MMM dd")
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@ChartView.strokeWidth
        textSize = 10f.convertDpToPx()
    }
    private val strokeWidth = 2.5f
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private val bitmapPaint = Paint(Paint.DITHER_FLAG)

    private lateinit var bigChartsPaths: List<Path>
    private lateinit var smallChartsPaths: List<Path>

    private val boundsRectWidth = 8f.convertDpToPx()
    private lateinit var leftBoundRect: RectF
    private lateinit var beforeLeftBoundRect: RectF
    private lateinit var rightBoundRect: RectF
    private lateinit var afterRightBoundRect: RectF
    private lateinit var betweenBoundsStrokeRect: RectF

    private lateinit var mappedX: List<Float>

    private var bigChartsHeight: Int = 0

    private lateinit var xLabelsIndexes: List<Int>
    private var maxCountOfXLabels = 0

    private fun Float.convertDpToPx()  =
        this * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT

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

        bigChartsHeight = h / 2
        bigChartsPaths = calcChartPaths(0, bigChartsHeight, mappedX)
        val smallChartsTopMargin = 32f.convertDpToPx()
        val smallChartsMaxY = bigChartsHeight + smallChartsTopMargin
        val smallChartsHeight = h / 10
        smallChartsPaths = calcChartPaths(smallChartsMaxY.toInt(), smallChartsHeight, mappedX)
        leftBoundRect = RectF(
            0f,
            smallChartsMaxY - strokeWidth,
            boundsRectWidth,
            smallChartsMaxY + smallChartsHeight + strokeWidth
        )
        beforeLeftBoundRect = RectF(
            0f,
            smallChartsMaxY - strokeWidth,
            leftBoundRect.left,
            smallChartsMaxY + smallChartsHeight + strokeWidth
        )
        rightBoundRect = RectF(
            w.toFloat() - boundsRectWidth,
            smallChartsMaxY - strokeWidth,
            w.toFloat(),
            smallChartsMaxY + smallChartsHeight + strokeWidth
        )
        afterRightBoundRect = RectF(
            rightBoundRect.right,
            smallChartsMaxY - strokeWidth,
            w.toFloat(),
            smallChartsMaxY + smallChartsHeight + strokeWidth
        )
        betweenBoundsStrokeRect = RectF(
            boundsRectWidth,
            smallChartsMaxY,
            w.toFloat() - boundsRectWidth,
            smallChartsMaxY + smallChartsHeight
        )

        val sampleText = "Aug 17"
        val textRect = Rect()
        paint.getTextBounds(sampleText, 0, sampleText.lastIndex, textRect)
//        textRect.apply {
//            left += 4f.convertDpToPx().toInt()
//            right += 4f.convertDpToPx().toInt()
//        }
        maxCountOfXLabels = width / (textRect.width() + 36f.convertDpToPx()).toInt()
        val first = textRect.centerX() * x.size / w
        xLabelsIndexes = List(maxCountOfXLabels) {
            when (it) {
                0 -> {
                    (textRect.centerX() * x.lastIndex / w).toInt()
                }
                else -> {
                    (((w / maxCountOfXLabels) * it + textRect.centerX()) * x.lastIndex / w).toInt()
//                    (x.size / maxCountOfXLabels) * it
                }
            }
        }

        postInvalidate()
    }

    private fun calcChartPaths(maxY: Int, chartHeight: Int, mappedX: List<Float>): List<Path> {
        val chatsMaxYValues =
            y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).max()!! }
        val chatsMinYValues =
            y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).min()!! }
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

        paint.apply {
            textAlign = Paint.Align.LEFT
            color = Color.GRAY
        }

        xLabelsIndexes.forEachIndexed { i, p ->
            val text = formatter.format((if (hasBounds) x.subList(leftBound, rightBound) else x)[p])
            val xCoordinate = (if (hasBounds) boundedMappedX!! else mappedX)[p]
            canvas.drawText(text, xCoordinate, bigChartsHeight.toFloat() + 12f.convertDpToPx(), paint)
        }

        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat(),
            width.toFloat(),
            bigChartsHeight.toFloat(),
            paint.apply {
                style = Paint.Style.STROKE
                color = Color.GRAY
                strokeWidth = 0.5f
            }
        )
        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat() - bigChartsHeight / 6,
            width.toFloat(),
            bigChartsHeight.toFloat() - bigChartsHeight / 6,
            paint
        )
        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat() - 2 * (bigChartsHeight / 6),
            width.toFloat(),
            bigChartsHeight.toFloat() - 2 * (bigChartsHeight / 6),
            paint
        )
        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat() - 3 * (bigChartsHeight / 6),
            width.toFloat(),
            bigChartsHeight.toFloat() - 3 * (bigChartsHeight / 6),
            paint
        )
        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat() - 4 * (bigChartsHeight / 6),
            width.toFloat(),
            bigChartsHeight.toFloat() - 4 * (bigChartsHeight / 6),
            paint
        )
        canvas.drawLine(
            0f,
            bigChartsHeight.toFloat() - 5 * (bigChartsHeight / 6),
            width.toFloat(),
            bigChartsHeight.toFloat() - 5 * (bigChartsHeight / 6),
            paint
        )

        bigChartsPaths.forEachIndexed { index, path ->
            paint.apply {
                strokeWidth = this@ChartView.strokeWidth
                color = colors[index]
            }
            canvas.drawPath(path, paint)
            canvas.drawPath(smallChartsPaths[index], paint)
        }

        canvas.drawRect(leftBoundRect, paint.apply {
            color = colors[3]
            alpha = 100
            style = Paint.Style.FILL
            strokeWidth = this@ChartView.strokeWidth
        })
        canvas.drawRect(rightBoundRect, paint)
        canvas.drawRect(beforeLeftBoundRect, paint.apply {
            alpha = 50
            style = Paint.Style.FILL
        })
        canvas.drawRect(afterRightBoundRect, paint)
        canvas.drawLine(
            leftBoundRect.right,
            leftBoundRect.top + strokeWidth / 2,
            rightBoundRect.left,
            rightBoundRect.top + strokeWidth / 2,
            paint.apply {
                alpha = 100
                style = Paint.Style.FILL_AND_STROKE
            }
        )
        canvas.drawLine(
            leftBoundRect.right,
            leftBoundRect.bottom - strokeWidth / 2,
            rightBoundRect.left,
            rightBoundRect.bottom - strokeWidth / 2,
            paint
        )
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

    private var boundedMappedX: List<Float>? = null

    private fun recalculateAndPostInvalidate() {
        leftBound = mappedX.size * leftBoundRect.left.toInt() / width
        rightBound = mappedX.size * rightBoundRect.right.toInt() / width

        boundedMappedX = (if (hasBounds) this.x.subList(leftBound, rightBound) else this.x).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax - xMin)
            map { fl -> width - (xMax - fl) * k }
        }
        bigChartsPaths = calcChartPaths(0, height / 2, boundedMappedX!!)
        postInvalidate()
    }
}