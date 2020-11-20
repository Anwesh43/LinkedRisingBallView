package com.example.risingballlinerview

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.app.Activity
import android.content.Context

val colors : Array<Int> = arrayOf(
    "#F44336",
    "#3F51B5",
    "#4CAF50",
    "#FF9800",
    "#009688"
).map {
    Color.parseColor(it)
}.toTypedArray()
val parts : Int = 4
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f
val rFactor : Float = 9.4f
val lineSizeFactor : Float = 5.9f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")
val lines : Int = 4
val deg : Float = 360f
val rot : Float = 60f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawRisingBallLiner(scale : Float, w : Float, h : Float, paint : Paint) {
    val r : Float = Math.min(w, h) / rFactor
    val lSize : Float = Math.min(w, h) / lineSizeFactor
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val sf2 : Float = sf.divideScale(1, parts)
    val sf3 : Float = sf.divideScale(2, parts)
    val gap : Float = deg / parts
    save()
    translate(w / 2, h + r - (h / 2 + r) * sf1)
    rotate(rot * sf3)
    drawCircle(0f, 0f, r, paint)
    for (j in 0..(lines - 1)) {
        save()
        rotate(gap * j)
        drawLine(r, 0f, r + lSize * sf2, 0f, paint)
        restore()
    }
    restore()
}

fun Canvas.drawRBLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    drawRisingBallLiner(scale, w, h, paint)
}

class RisingBallLinerView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () ->  Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class RBLNode(var i : Int, val state : State = State()) {
        private var next : RBLNode? = null
        private var prev : RBLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = RBLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawRBLNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : RBLNode {
            var curr : RBLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }

        data class RisingBallLiner(var i : Int) {

            private var curr : RBLNode = RBLNode(0)
            private var dir : Int = 1

            fun draw(canvas : Canvas, paint : Paint) {
                curr.draw(canvas, paint)
            }

            fun update(cb : (Float) -> Unit) {
                curr.update {
                    curr = curr.getNext(dir) {
                        dir *= -1
                    }
                    cb(it)
                }
            }

            fun startUpdating(cb : () -> Unit) {
                curr.startUpdating(cb)
            }
        }
    }
    data class Renderer(var view : RisingBallLinerView) {

        private val animator : Animator = Animator(view)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rbl : RBLNode.RisingBallLiner = RBLNode.RisingBallLiner(0)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            rbl.draw(canvas, paint)
            animator.animate {
                rbl.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            rbl.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : RisingBallLinerView {
            val view : RisingBallLinerView = RisingBallLinerView(activity)
            activity.setContentView(view)
            return view
        }
    }
}