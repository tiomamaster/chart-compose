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

    //    private lateinit var xLabelsIndexes: MutableList<Int>
    private lateinit var xLabels: MutableList<XLabel>
    private var maxCountOfXLabels = 0

    private fun Float.convertDpToPx() =
        this * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT

    private lateinit var textRect: Rect

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
        val maxYOfAll = chatsMaxYValues.maxOrNull()!!
        val minYOfAll = chatsMinYValues.minOrNull()!!
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
            strokeWidth = textStrokeWidth
            color = Color.GRAY
        }

        xLabels.forEach { l ->
            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
            if (l.index > x.lastIndex) return@forEach
            val text = formatter.format(x[l.index])
            paint.apply { alpha = l.alpha }
            canvas.drawText(
                text,
                l.coordinate,
                bigChartsHeight.toFloat() + 12f.convertDpToPx(),
                paint
            )
        }
//        xLabelsFadeIn.forEach { l ->
//            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
//            if (l.index > x.lastIndex) return@forEach
//            val text = formatter.format(x[l.index])
////            val xCoordinate = (if (hasBounds) boundedMappedX!! else mappedX)[p]
//            canvas.drawText(
//                text,
//                l.coordinate,
//                bigChartsHeight.toFloat() + 12f.convertDpToPx(),
//                paint
//            )
//        }
//        xLabelsFadeIn.forEach { l ->
//            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
//            if (l.index > x.lastIndex) return@forEach
//            val text = formatter.format(x[l.index])
//            paint.apply { alpha = l.alpha }
//            canvas.drawText(
//                text,
//                l.coordinate,
//                bigChartsHeight.toFloat() + 12f.convertDpToPx(),
//                paint
//            )
//        }
//        xLabelsFadeOut.forEach { l ->
//            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
//            if (l.index > x.lastIndex) return@forEach
//            val text = formatter.format(x[l.index])
//            paint.apply { alpha = l.alpha }
//            canvas.drawText(
//                text,
//                l.coordinate,
//                bigChartsHeight.toFloat() + 12f.convertDpToPx(),
//                paint
//            )
//        }

//        xLabels1.forEach { p ->
//            val x = (if (hasBounds) x.subList(leftBound, rightBound) else x)
//            if (p.index > x.lastIndex) return@forEach
//            val text = formatter.format(x[p.index])
////            val xCoordinate = (if (hasBounds) boundedMappedX!! else mappedX)[p]
//            canvas.drawText(
//                text,
//                p.coordinate,
//                bigChartsHeight.toFloat() + 12f.convertDpToPx(),
//                paint
//            )
//        }

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
                val d = x - prevX
                prevX = x
                if (leftBoundClicked) {
                    val left = Math.min(
                        Math.max(x - boundsRectWidth / 2, 0f),
                        rightBoundRect.left - boundsRectWidth
                    )
                    val right = left + boundsRectWidth
                    leftBoundRect.left = left
                    leftBoundRect.right = right

                    beforeLeftBoundRect.right = leftBoundRect.left
                    betweenBoundsStrokeRect.left = leftBoundRect.right

                    recalculateAndPostInvalidate(d)
                }
                if (rightBoundClicked) {
                    val left = Math.max(
                        Math.min(x - boundsRectWidth / 2, width - boundsRectWidth),
                        leftBoundRect.right
                    )
                    val right = left + boundsRectWidth
                    rightBoundRect.left = left
                    rightBoundRect.right = right

                    afterRightBoundRect.left = rightBoundRect.right
                    betweenBoundsStrokeRect.right = rightBoundRect.left

                    recalculateAndPostInvalidate(d)
                }
                if (betweenBoundsClicked) {
                    when {
                        d > 0 && rightBoundRect.right < width || d < 0 && leftBoundRect.left > 0 -> {
                            val betweenLeft =
                                Math.max(betweenBoundsStrokeRect.left + d, boundsRectWidth)
                            val betweenRight =
                                Math.min(betweenBoundsStrokeRect.right + d, width - boundsRectWidth)
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

                            recalculateAndPostInvalidate(d)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                leftBoundClicked = false
                rightBoundClicked = false
                betweenBoundsClicked = false
                index = -1
            }
        }
        return true
    }

    private var boundedMappedX: List<Float>? = null

//    private var realD: Float = 0f

    private var index = -1

    private var xLabels1: MutableList<XLabel> = mutableListOf()

    private var xLabelsFadeIn: MutableList<XLabel> = mutableListOf()
    private var xLabelsFadeOut: MutableList<XLabel> = mutableListOf()

    private fun recalculateAndPostInvalidate(dd: Float) {
        val newLeft = mappedX.size * leftBoundRect.left.toInt() / width
        var d = leftBound - newLeft
        leftBound = newLeft
        val newRight = mappedX.size * rightBoundRect.right.toInt() / width
        if (rightBoundClicked) {
            d = rightBound - newRight
        }
        rightBound = newRight

        boundedMappedX = (if (hasBounds) this.x.subList(leftBound, rightBound) else this.x).run {
            val xMin = first()
            val xMax = last()
            val k = width / (xMax - xMin)
            map { fl -> width - (xMax - fl) * k }
        }
        bigChartsPaths = calcChartPaths(0, bigChartsHeight, boundedMappedX!!)

        if (betweenBoundsClicked) {
            if (index == -1) index = 0
            index += d
            if (index < 0) index = rightBound - leftBound - 1
            if (index > rightBound - leftBound - 1) index = 0
            val coordinate = boundedMappedX!![index]

//            xLabels.clear()
//            var i = 0
//            while (true) {
//                if (xLabels.isEmpty()) {
//                    xLabels.add(coordinate to index)
//                    i++
//                }
//                else {
//                    val prev = xLabels[i++ - 1]
//                    var newCoordinate = prev.first + (textRect.width() + 32f.convertDpToPx())
//                    if (newCoordinate > width - textRect.centerX()) {
//                        newCoordinate -= width /*- textRect.centerX() */
//                    }
//                    if (/*Math.abs(xLabels.last().first - newCoordinate) < textRect.centerX() || */xLabels.size == maxCountOfXLabels) break
//                    xLabels.add(newCoordinate to ((newCoordinate + textRect.centerX()) * (rightBound - leftBound - 1) / width).toInt())
//                }
//            }

            for (i in xLabels.indices) {
                if (i == 0) {
                    xLabels[i] = XLabel(coordinate, index)
                    continue
                }
                val prev = xLabels[i - 1]
                var newCoordinate = prev.coordinate + (textRect.width() + 32f.convertDpToPx())
                if (newCoordinate > width - textRect.centerX()) {
                    newCoordinate -= width
                }
                xLabels[i] = XLabel(
                    newCoordinate,
                    ((newCoordinate + textRect.centerX()) * (rightBound - leftBound - 1) / width).toInt()
                )
            }
        }
        if (rightBoundClicked) {
            paint.apply {
                style = Paint.Style.FILL
                strokeWidth = textStrokeWidth
            }

//            val min = xLabels.minBy { it.coordinate }

            val xIndToRemove = mutableListOf<Int>()
            val labelsToAdd = mutableListOf<XLabel>()
            for (i in 0 until xLabels.size) {
                if (i == 0) {
//                    xLabels[i] = min!!
                    continue
                }
                val label = xLabels[i]
                if (label.index < rightBound - leftBound - 1) {
                    val text = formatter.format(x.subList(leftBound, rightBound)[label.index])
                    val textWidth = paint.measureText(text)
                    val c = boundedMappedX!![label.index] - textWidth / 2
                    val center = (label.rightBound - label.leftBound) / 2 + label.leftBound
                    val k = 255 / ((label.rightBound - label.leftBound) / 2)
                    val alpha = 255 - Math.abs(center - boundedMappedX!![label.index]) * k
                    if (alpha < 2) {
                        val newIndex = (label.leftBound - textRect.width() / 2 + textRect.centerX()) * (rightBound - leftBound - 1) / width
                        labelsToAdd.add(label.copy(coordinate = label.leftBound - textRect.width() / 2, index = newIndex.toInt(), alpha = 1))
                        xIndToRemove.add(i)
                    }
                    if (alpha > 0) {
                        xLabels[i] =
                            label.copy(coordinate = c, index = label.index, alpha = alpha.toInt())
                        println("$alpha")
                    }

//                    when {
//                        c + textWidth > label.rightBound - textWidth / 2 -> {
////                            val newIndex = (label.leftBound + textRect.centerX()) * (rightBound - leftBound - 1) / width
////                            xLabelsFadeIn.add(label.copy(coordinate = label.leftBound, index = newIndex.toInt(), alpha = 5))
////                            xLabelsFadeOut.add(label.copy(coordinate = c, index = label.index, alpha = 250))
////                            xIndToRemove.add(i)
//                        }
//                        c < label.leftBound + textWidth / 2 -> {
////                            val newIndex = (label.rightBound - textRect.width() + textRect.centerX()) * (rightBound - leftBound - 1) / width
////                            xLabelsFadeOut.add(label.copy(coordinate = label.rightBound - textRect.width(), index = newIndex.toInt(), alpha = 250))
////                            xIndToRemove.add(i)
//                        }
//                        else -> xLabels[i] = label.copy(coordinate = c, index = label.index)
//                    }
                }
            }
            xLabels.addAll(labelsToAdd)
            xIndToRemove.sortedDescending().forEach {
                xLabels.removeAt(it)
            }

//            val xIndToRemove1 = mutableListOf<Int>()
//            for (i in 0 until xLabelsFadeIn.size) {
//                val label = xLabelsFadeIn[i]
//                if (label.index < rightBound - leftBound - 1) {
//                    val text = formatter.format(x.subList(leftBound, rightBound)[label.index])
//                    val textWidth = paint.measureText(text)
//                    val c = boundedMappedX!![label.index] - textWidth / 2
//                    when {
//                        c + textWidth / 2 > (label.rightBound - label.leftBound) / 2 + label.leftBound -> {
//                            xLabelsFadeOut.add(label.copy(coordinate = c, index = label.index, alpha = 250))
//                            xIndToRemove1.add(i)
//                        }
//                        else -> {
//                            val newAlpha = if (label.alpha <= 250) label.alpha + 5 else 255
//                            xLabelsFadeIn[i] = label.copy(coordinate = c, index = label.index, alpha = newAlpha)
//                        }
//                    }
//                }
//            }
//            xIndToRemove1.sortedDescending().forEach {
//                xLabelsFadeIn.removeAt(it)
//            }
//
//            val xIndToRemove2 = mutableListOf<Int>()
//            for (i in 0 until xLabelsFadeOut.size) {
//                val label = xLabelsFadeOut[i]
//                if (label.index < rightBound - leftBound - 1) {
//                    val text = formatter.format(x.subList(leftBound, rightBound)[label.index])
//                    val textWidth = paint.measureText(text)
//                    val c = boundedMappedX!![label.index] - textWidth / 2
//                    when {
//                        c + textWidth > label.rightBound  -> {
//                            val newIndex = (label.leftBound + textRect.centerX()) * (rightBound - leftBound - 1) / width
//                            xLabelsFadeIn.add(label.copy(coordinate = label.leftBound, index = newIndex.toInt(), alpha = 5))
//                            xIndToRemove2.add(i)
//                        }
//                        else -> {
//                            val newAlpha = if (label.alpha >= 5) label.alpha - 5 else 0
//                            xLabelsFadeOut[i] = label.copy(coordinate = c, index = label.index, alpha = newAlpha)
//                        }
//                    }
//                }
//            }
//            xIndToRemove2.sortedDescending().forEach {
//                xLabelsFadeOut.removeAt(it)
//            }

        }
        if (leftBoundClicked) {
        }
        /*else {
            var prev = 0f
            xLabels = MutableList(maxCountOfXLabels) {
                val coordinate = if (it == 0) 16f.convertDpToPx()
                else prev + textRect.width() + 32f.convertDpToPx()
                prev = coordinate
                val index = (coordinate + textRect.centerX()) * (rightBound - leftBound - 1) / width
                coordinate to index.toInt()
            }
        }*/
        postInvalidate()
    }
}

data class XLabel(
    var coordinate: Float,
    var index: Int,
    var leftBound: Float = 0f,
    var rightBound: Float = 0f,
    var alpha: Int = -1
)