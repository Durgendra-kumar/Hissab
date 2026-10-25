package com.project.hissab.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.project.hissab.domain.model.SpendingOverview

data class BarColors(val track: Int, val upi: Int, val cash: Int, val creditCard: Int)

internal object SpendingBarPainter {

    fun paint(widthPx: Int, heightPx: Int, o: SpendingOverview, colors: BarColors): Bitmap {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val total = o.upi + o.cash + o.creditCard
        val filled = when {
            total <= 0.0 -> 0.0
            o.budget.isSet -> o.budget.ratio.coerceAtMost(1.0)   // over budget = full bar
            else -> 1.0                                           // no budget: show the split only
        }

        // 1) the coloured segments, square-edged, on their own layer
        val layer = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layer)
        val paint = Paint()
        var x = 0f
        listOf(o.upi to colors.upi, o.cash to colors.cash, o.creditCard to colors.creditCard).forEach { (amount, color) ->
            if (amount > 0.0) {
                val segmentWidth = (w * filled * amount / total).toFloat()
                paint.color = color
                layerCanvas.drawRect(x, 0f, x + segmentWidth, h, paint)
                x += segmentWidth
            }
        }

        // 2) rounded empty track, then the layer drawn through the same rounded shape
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val rect = RectF(0f, 0f, w, h)
        val radius = h / 2
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.track })
        canvas.drawRoundRect(
            rect, radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(layer, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        )
        return out
    }
}