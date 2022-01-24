package com.gmail.tiomamaster.chart

import android.content.Context
import android.graphics.*
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat

class ChartView(context: Context) : View(context) {

    var chart: Chart? = null
        set(value) {
            field = value
            x = (field?.columns?.get(0) as Iterable<*>).drop(1) as List<Float>
            rightBound = x.size
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
    private val strokeWidth = 2.5f
    private val textStrokeWidth = 0f
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@ChartView.textStrokeWidth
        textSize = 10f.convertDpToPx()
    }
    private val linePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        color = Color.GRAY
        strokeWidth = 0.5f
    }
    private val textPaint = Paint(paint).apply {
        color = Color.GRAY
    }
    private val chartPaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@ChartView.strokeWidth
    }
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

    private lateinit var xLabels: MutableList<XLabel>
    private var maxCountOfXLabels = 0

    private lateinit var textRect: Rect

    private var leftBound = 0
    private var rightBound = -1

    private val hasBounds get() = leftBound != -1 && rightBound != -1

    private var leftBoundClicked = false
    private var rightBoundClicked = false
    private var betweenBoundsClicked = false
        set(value) {
            field = value
            betweenBoundsStrokeRectWidth =
                betweenBoundsStrokeRect.right - betweenBoundsStrokeRect.left
        }
    private var betweenBoundsStrokeRectWidth = 0f
    private var prevX = 0f

    private var boundedMappedX: List<Float>? = null

    private var maxYOfAll = 0f

    private fun Float.convertDpToPx() =
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
        textRect = Rect()
        paint.getTextBounds(sampleText, 0, sampleText.lastIndex, textRect)
        maxCountOfXLabels = width / (textRect.width() * 2)
        val textBounds = List(maxCountOfXLabels) {
            if (it == 0) w / maxCountOfXLabels.toFloat()
            else w / maxCountOfXLabels * (it + 1).toFloat()
        }
        xLabels = textBounds.map {
            val lBound = it - w / maxCountOfXLabels
            val rBound = it
            val center = (rBound - lBound) / 2 + lBound
            val c = center - (textRect.width() / 2)
            val i = center * x.lastIndex / w
            XLabel(c, i.toInt(), lBound, rBound)
        }.toMutableList()

        postInvalidate()
    }

    private fun calcChartPaths(maxY: Int, chartHeight: Int, mappedX: List<Float>): List<Path> {
        val chatsMaxYValues =
            y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).maxOrNull()!! }
        val chatsMinYValues =
            y.map { (if (hasBounds) it.subList(leftBound, rightBound) else it).minOrNull()!! }
        maxYOfAll = chatsMaxYValues.maxOrNull()!!
        val minYOfAll = chatsMinYValues.minOrNull()!!
        val kY = chartHeight / (maxYOfAll - minYOfAll)

        return y.mapIndexed { _, ys ->
            (if (hasBounds) ys.subList(leftBound, rightBound) else ys).let { boundedYs ->
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

        xLabels.forEach { label ->
            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
            if (label.index > x.lastIndex) return@forEach
            val text = formatter.format(x[label.index])
            canvas.drawText(
                text,
                label.coordinate,
                bigChartsHeight.toFloat() + 16f.convertDpToPx(),
                textPaint
            )
        }

        for (i in 0..5) {
            canvas.drawLine(
                0f,
                bigChartsHeight.toFloat() - i * bigChartsHeight / 6,
                width.toFloat(),
                bigChartsHeight.toFloat() - i * bigChartsHeight / 6,
                linePaint
            )

            if (i == 0) continue
            val text = maxYOfAll / 6 * i
            canvas.drawText(
                text.toInt().toString(),
                8f.convertDpToPx(),
                bigChartsHeight.toFloat() - i * (bigChartsHeight / 6) - 4f.convertDpToPx(),
                textPaint
            )
        }

        bigChartsPaths.forEachIndexed { index, path ->
            chartPaint.apply {
                color = colors[index]
            }
            canvas.drawPath(path, chartPaint)
            canvas.drawPath(smallChartsPaths[index], chartPaint)
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
                val deltaX = x - prevX
                prevX = x
                when {
                    leftBoundClicked -> {
                        val left = (x - boundsRectWidth / 2).coerceAtLeast(0f)
                            .coerceAtMost(rightBoundRect.left - boundsRectWidth)
                        val right = left + boundsRectWidth
                        leftBoundRect.left = left
                        leftBoundRect.right = right

                        beforeLeftBoundRect.right = leftBoundRect.left
                        betweenBoundsStrokeRect.left = leftBoundRect.right
                    }
                    rightBoundClicked -> {
                        val left = (x - boundsRectWidth / 2).coerceAtMost(width - boundsRectWidth)
                            .coerceAtLeast(leftBoundRect.right)
                        val right = left + boundsRectWidth
                        rightBoundRect.left = left
                        rightBoundRect.right = right

                        afterRightBoundRect.left = rightBoundRect.right
                        betweenBoundsStrokeRect.right = rightBoundRect.left
                    }
                    betweenBoundsClicked && (deltaX > 0 && rightBoundRect.right < width
                        || deltaX < 0 && leftBoundRect.left > 0) -> {
                        val betweenLeft = (betweenBoundsStrokeRect.left + deltaX)
                            .coerceAtLeast(boundsRectWidth)
                        val betweenRight = (betweenBoundsStrokeRect.right + deltaX)
                            .coerceAtMost(width - boundsRectWidth)
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
                    }
                }
                recalculateAndPostInvalidate()
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
        val newLeft = mappedX.size * leftBoundRect.left.toInt() / width
        leftBound = newLeft
        val newRight = mappedX.size * rightBoundRect.right.toInt() / width
        rightBound = newRight

        boundedMappedX = (if (hasBounds) this.x.subList(leftBound, rightBound) else this.x).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax - xMin)
            map { fl -> width - (xMax - fl) * k }
        }
        bigChartsPaths = calcChartPaths(0, bigChartsHeight, boundedMappedX!!)

        xLabels.forEach {
            it.index = it.coordinate.toInt() * boundedMappedX!!.lastIndex / width
        }
        postInvalidate()
    }

    private data class XLabel(
        var coordinate: Float,
        var index: Int,
        var leftBound: Float = 0f,
        var rightBound: Float = 0f,
        var alpha: Int = -1
    )
}
