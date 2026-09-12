package com.example.spotted

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withRotation
import com.google.android.material.R as MaterialR
import kotlin.math.max
import kotlin.math.min

/**
 * Weekly-goal ring for the gallery screen.
 *
 * Every colour is pulled from the theme rather than written into the
 * class, so the ring follows the cream and the charcoal palettes
 * instead of drawing dark ink on a dark background in night mode.
 */
class SightingsRingView(
    context: Context,
    attrs: AttributeSet
) : View(context, attrs) {

    // ---- state ----

    private var goal: Int =
        SightingStore.DEFAULT_GOAL

    private var count: Int = 0

    private var nightCount: Int = 0

    private var caption: String =
        "this week"

    private var sweepFraction: Float = 0f

    private var nightFraction: Float = 0f

    private var animator: ValueAnimator? = null

    private var ringColor: Int =
        themeColor(MaterialR.attr.colorPrimary)

    /**
     * The bright point of the sweep gradient. Blending the ring colour
     * towards white keeps the highlight in the same hue family whatever
     * accent the theme hands us.
     */
    private var ringHighlight: Int =
        ColorUtils.blendARGB(
            ringColor,
            Color.WHITE,
            0.38f
        )

    // ---- paints ----

    private val trackPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = themeColor(
                MaterialR.attr.colorSurfaceVariant
            )
        }

    private val progressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    private val nightPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = themeColor(
                MaterialR.attr.colorSecondary
            )
        }

    private val tickPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = ColorUtils.setAlphaComponent(
                themeColor(
                    MaterialR.attr.colorOutline
                ),
                140
            )
        }

    private val countPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
            color = themeColor(
                MaterialR.attr.colorOnSurface
            )
        }

    private val captionPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            color = themeColor(
                MaterialR.attr.colorOnSurfaceVariant
            )
        }

    private val oval = RectF()

    private val innerOval = RectF()

    private val gradientMatrix = Matrix()

    init {

        context.theme
            .obtainStyledAttributes(
                attrs,
                R.styleable.SightingsRingView,
                0,
                0
            )
            .apply {

                try {

                    goal = getInteger(
                        R.styleable.SightingsRingView_goal,
                        SightingStore.DEFAULT_GOAL
                    )

                    ringColor = getColor(
                        R.styleable.SightingsRingView_ringColor,
                        ringColor
                    )

                    caption = getString(
                        R.styleable.SightingsRingView_ringCaption
                    ) ?: caption

                } finally {
                    recycle()
                }
            }

        ringHighlight =
            ColorUtils.blendARGB(
                ringColor,
                Color.WHITE,
                0.38f
            )
    }

    /**
     * Reads a single colour off the current theme.
     */
    private fun themeColor(
        attr: Int
    ): Int {

        val typed =
            context.theme.obtainStyledAttributes(
                intArrayOf(attr)
            )

        try {

            return typed.getColor(
                0,
                Color.GRAY
            )

        } finally {
            typed.recycle()
        }
    }

    /**
     * Updates the data displayed by the ring.
     */
    fun setData(
        count: Int,
        nightCount: Int,
        goal: Int,
        animate: Boolean = true
    ) {

        this.count = max(0, count)

        this.nightCount =
            max(
                0,
                min(
                    nightCount,
                    this.count
                )
            )

        this.goal = max(1, goal)

        val targetSweep =
            min(
                1f,
                this.count.toFloat() / this.goal
            )

        val targetNight =
            if (this.count == 0) {
                0f
            } else {
                this.nightCount.toFloat() /
                        this.count
            }

        animator?.cancel()

        if (!animate) {

            sweepFraction = targetSweep
            nightFraction = targetNight

            invalidate()

            return
        }

        val fromSweep = sweepFraction
        val fromNight = nightFraction

        animator =
            ValueAnimator.ofFloat(
                0f,
                1f
            ).apply {

                duration = 750

                interpolator =
                    DecelerateInterpolator()

                addUpdateListener {

                    val t =
                        it.animatedValue as Float

                    sweepFraction =
                        fromSweep +
                                (targetSweep - fromSweep) * t

                    nightFraction =
                        fromNight +
                                (targetNight - fromNight) * t

                    invalidate()
                }

                start()
            }
    }

    /**
     * Always make the view square.
     */
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {

        val w =
            MeasureSpec.getSize(
                widthMeasureSpec
            )

        val h =
            MeasureSpec.getSize(
                heightMeasureSpec
            )

        val size =
            if (w == 0 || h == 0) {
                max(w, h)
            } else {
                min(w, h)
            }

        setMeasuredDimension(
            size,
            size
        )
    }

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(canvas)

        val size =
            min(
                width,
                height
            ).toFloat()

        if (size <= 0f) {
            return
        }

        val stroke =
            size / 10f

        val tickZone =
            size / 14f

        val cx =
            width / 2f

        val cy =
            height / 2f

        val radius =
            size / 2f -
                    stroke / 2f -
                    tickZone

        trackPaint.strokeWidth =
            stroke

        progressPaint.strokeWidth =
            stroke

        tickPaint.strokeWidth =
            size / 90f

        nightPaint.strokeWidth =
            stroke * 0.28f

        oval.set(
            cx - radius,
            cy - radius,
            cx + radius,
            cy + radius
        )

        // 1 — background track

        canvas.drawArc(
            oval,
            0f,
            360f,
            false,
            trackPaint
        )

        // 2 — progress arc with sweep gradient

        val shader =
            SweepGradient(
                cx,
                cy,
                intArrayOf(
                    ringColor,
                    ringHighlight,
                    ringColor
                ),
                floatArrayOf(
                    0f,
                    0.55f,
                    1f
                )
            )

        gradientMatrix.setRotate(
            -90f,
            cx,
            cy
        )

        shader.setLocalMatrix(
            gradientMatrix
        )

        progressPaint.shader =
            shader

        if (sweepFraction > 0f) {

            canvas.drawArc(
                oval,
                -90f,
                360f * sweepFraction,
                false,
                progressPaint
            )
        }

        // 3 — night arc

        if (nightFraction > 0f) {

            val innerRadius =
                radius -
                        stroke * 0.95f

            innerOval.set(
                cx - innerRadius,
                cy - innerRadius,
                cx + innerRadius,
                cy + innerRadius
            )

            canvas.drawArc(
                innerOval,
                -90f,
                360f * nightFraction,
                false,
                nightPaint
            )
        }

        // 4 — goal tick marks

        val tickInner =
            radius +
                    stroke / 2f +
                    size / 60f

        val tickOuter =
            tickInner +
                    size / 30f

        for (i in 0 until goal) {

            val angle =
                i * 360f / goal

            canvas.withRotation(
                angle,
                cx,
                cy
            ) {

                drawLine(
                    cx,
                    cy - tickInner,
                    cx,
                    cy - tickOuter,
                    tickPaint
                )
            }
        }

        // 5 — count

        countPaint.textSize =
            size / 3.1f

        val baseline =
            cy -
                    (
                            countPaint.descent() +
                                    countPaint.ascent()
                            ) / 2f -
                    size / 20f

        canvas.drawText(
            count.toString(),
            cx,
            baseline,
            countPaint
        )

        // 6 — caption, shrunk if it would run out under the ring

        captionPaint.textSize =
            size / 12f

        val hole =
            (radius - stroke) * 1.8f

        val captionWidth =
            captionPaint.measureText(caption)

        if (captionWidth > hole) {

            captionPaint.textSize *=
                hole / captionWidth
        }

        canvas.drawText(
            caption,
            cx,
            baseline + size / 6.5f,
            captionPaint
        )
    }

    override fun onDetachedFromWindow() {

        animator?.cancel()

        super.onDetachedFromWindow()
    }
}
