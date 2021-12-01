package com.swein.pipexample

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout

@SuppressLint("ViewConstructor")
class PIPExampleView(
    context: Context,
    private val onPIPClicked: () -> Unit,
    private val onCloseClicked: () -> Unit
): FrameLayout(context) {

    private var button: Button
    private var buttonExit: Button
    private var frameLayout: FrameLayout

    init {

        inflate(context, R.layout.view_pip_example, this)

        button = findViewById(R.id.button)
        buttonExit = findViewById(R.id.buttonExit)
        frameLayout = findViewById(R.id.frameLayout)

        button.text = "test"
        buttonExit.text = "exit"

        setListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {

        button.setOnClickListener {
            onPIPClicked()
        }

        buttonExit.setOnClickListener {
            onCloseClicked()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun resetActionListener(
        onActionDown: ((MotionEvent) -> Unit)? = null,
        onActionMove: ((MotionEvent) -> Unit)? = null
    ) {

        frameLayout.setOnTouchListener(null)

        if (onActionDown != null && onActionMove != null) {

            var currentTime: Long = 0

            frameLayout.setOnTouchListener { _, event ->

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onActionDown(event)
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        onActionMove(event)
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (System.currentTimeMillis() - currentTime > 500) {
                            currentTime = 0
                            return@setOnTouchListener true
                        }
                    }
                }

                false
            }
        }
    }

    protected fun finalize() {
        Log.d("???", "PIPExampleView finalize")
    }
}