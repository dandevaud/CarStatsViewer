package com.ixam97.carStatsViewer.ui.plot.graphics

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.ixam97.carStatsViewer.CarStatsViewer

class PlotPaint(
    val Plot: Paint,
    val PlotGap: Paint,
    val PlotBackground: Paint,

    val PlotSecondary: Paint,
    val PlotGapSecondary: Paint,
    val PlotBackgroundSecondary: Paint,

    val Color: Int,
    val TransparentColor: Int,

    val HighlightLabel: Paint,
    val HighlightLabelLine: Paint,

    val MinMax: Paint
) {
    companion object {
        private val paintCache : HashMap<Int, HashMap<Float, PlotPaint>> = HashMap()

        fun byColor(color : Int, textSize: Float, minMaxAlpha: Int  = 64): PlotPaint {

            val cached = paintCache[color]?.get(textSize)
            if (cached != null) return cached

            val basePaint = basePaint(textSize)

            val plotPaint = Paint(basePaint)
            plotPaint.color = color
            plotPaint.strokeWidth = 3f

            val plotGapPaint = Paint(plotPaint)
            plotGapPaint.color = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color))
            plotGapPaint.pathEffect = DashPathEffect(floatArrayOf(2f, 10f), 0f)

            val plotBackgroundPaint = Paint(plotPaint)
            plotBackgroundPaint.color = Color.argb(160, Color.red(color), Color.green(color), Color.blue(color))
            plotBackgroundPaint.style = Paint.Style.FILL

            val minMaxPaint = Paint(plotBackgroundPaint)
            minMaxPaint.color = Color.argb(minMaxAlpha, Color.red(color), Color.green(color), Color.blue(color))
            minMaxPaint.strokeWidth = 3f
            minMaxPaint.style = Paint.Style.FILL_AND_STROKE

            val highlightLabelPaint = Paint(basePaint)
            highlightLabelPaint.color = color
            highlightLabelPaint.style = Paint.Style.FILL

            val highlightLabelLinePaint = Paint(plotPaint)
            highlightLabelLinePaint.strokeWidth = 3f
            highlightLabelLinePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 10f), 0f)

            val paint = PlotPaint(
                plotPaint,
                plotGapPaint,
                plotBackgroundPaint,
                plotPaint, // use same as primary for now
                plotGapPaint,
                plotBackgroundPaint,
                color, // use same as primary for now
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
                highlightLabelPaint,
                highlightLabelLinePaint,
                minMaxPaint
            )

            if (paintCache[color] == null) paintCache[color] = HashMap()

            paintCache[color]?.set(textSize, paint)

            return paint
        }

        fun basePaint(textSize: Float): Paint {
            // defines paint and canvas
            val basePaint = Paint()
            basePaint.isAntiAlias = true
            basePaint.strokeWidth = 1f
            basePaint.style = Paint.Style.STROKE
            basePaint.strokeJoin = Paint.Join.ROUND
            basePaint.strokeCap = Paint.Cap.ROUND
            basePaint.textSize = textSize

            CarStatsViewer.typefaceRegular?.let {
                basePaint.typeface = it
            }

            return basePaint
        }
    }
}