package com.swein.pipexample.pipmanager

import android.app.Activity
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.swein.exoplayerkotlin.framework.display.DisplayUtility
import com.swein.pipexample.PIPExampleActivity
import com.swein.pipexample.PIPExampleView
import java.lang.ref.WeakReference


class PIPManager(
    private val appCompatActivity: AppCompatActivity
) {

    companion object {

        var isPIP = false

        private val layoutParams = WindowManager.LayoutParams()
        private var lastX = 0f
        private var lastY = 0f

        // init this in main activity
        lateinit var getPIPViewGroup: () -> WeakReference<ViewGroup>

        // init this in pip container activity
        lateinit var getContainer: () -> WeakReference<Activity>

        private fun requestOverlays(activity: Activity, afterSuccess: () -> Unit) {

            if (!Settings.canDrawOverlays(activity)) {
                if (!Settings.canDrawOverlays(activity)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.packageName)
                    )
                    activity.startActivityForResult(intent, 999)
                }
            }
            else {
                afterSuccess()
            }
        }

        private fun togglePIP(windowManager: WindowManager, viewGroup: ViewGroup, willEnterPIP: () -> Unit) {

            Log.d("???", "isPIP????? $isPIP")

            if (isPIP) {

                exitPIPMode(windowManager, PIPExampleActivity::class.java, viewGroup) {
                    isPIP = false
                }
            }
            else {

                getContainer.let {
                    it().get()?.finish()
                }

                enterPIPMode(windowManager, viewGroup, false, 250f, 250 * 0.5625f, willEnterPIP)
            }

            Log.d("???", "isPIP now ????? $isPIP")
        }

        private fun removeFromWindowManager(windowManager: WindowManager, viewGroup: ViewGroup) {

            if (viewGroup.windowToken != null) {
                windowManager.removeViewImmediate(viewGroup)
            }
        }

        private fun enterPIPMode(windowManager: WindowManager, viewGroup: ViewGroup, backToDesktop: Boolean, widthInDp: Float, heightInDp: Float, willEnterPIP: () -> Unit) {

            willEnterPIP()

            lastX = 0f
            lastY = 0f

            if (backToDesktop) {
                Intent().apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    viewGroup.context.startActivity(this)
                }
            }

            Thread {

                layoutParams.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }

                layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                layoutParams.gravity = Gravity.CENTER
                layoutParams.x = 0
                layoutParams.y = 0

                layoutParams.width = DisplayUtility.dipToPx(viewGroup.context, widthInDp)
                layoutParams.height = DisplayUtility.dipToPx(viewGroup.context, heightInDp)
                layoutParams.format = PixelFormat.TRANSPARENT

                Handler(Looper.getMainLooper()).post {
                    viewGroup.parent.let {
                        if (it is ViewGroup) {
                            it.removeAllViews()
                        }
                    }

                    windowManager.addView(viewGroup, layoutParams)
                }

            }.start()

        }

        private fun exitPIPMode(windowManager: WindowManager, cls: Class<*>?, viewGroup: ViewGroup, exitPIP: (() -> Unit)? = null) {

            Log.d("???", "exitPIPMode")
            try {
                removeFromWindowManager(windowManager, viewGroup)
            }
            catch (e: Exception) {
                Log.d("???", "${e.message}")
            }

            // reset player size
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            viewGroup.layoutParams = layoutParams

            Intent(viewGroup.context, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                viewGroup.context.startActivity(this)
                Log.d("???", "startActivity from main")
            }

            exitPIP?.let {
                it()
            }
        }

        private fun actionDown(motionEvent: MotionEvent) {
            lastX = motionEvent.rawX
            lastY = motionEvent.rawY
        }

        private fun actionMove(motionEvent: MotionEvent, pipExampleView: PIPExampleView, windowManager: WindowManager) {

            val nowX: Float = motionEvent.rawX
            val nowY: Float = motionEvent.rawY

            val tranX = nowX - lastX
            val tranY = nowY - lastY

            layoutParams.x += tranX.toInt()
            layoutParams.y += tranY.toInt()

            windowManager.updateViewLayout(pipExampleView, layoutParams)

            lastX = nowX
            lastY = nowY
        }

    }

    var isForeground = true

    private lateinit var pipExampleView: PIPExampleView

    private var lifecycleObserver = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)

            isForeground = true
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)

            isForeground = false
        }
    }

    fun create() {

        appCompatActivity.lifecycle.addObserver(lifecycleObserver)

        pipExampleView = PIPExampleView(appCompatActivity,
            onPIPClicked = {
                requestOverlays(appCompatActivity) {
                    togglePIP(appCompatActivity.windowManager, pipExampleView) {
                        isPIP = true
                        pipExampleView.resetActionListener(onActionMove = {
                            actionMove(it, pipExampleView, appCompatActivity.windowManager)
                        }, onActionDown = {
                            actionDown(it)
                        })
                    }
                }
            },
            onCloseClicked = {

                if (isPIP) {

                    if (isForeground) {
                        // should stop playing
                        removeFromWindowManager(appCompatActivity.windowManager, pipExampleView)
                    }
                    else {
                        appCompatActivity.finish()
                    }
                }
                else {
                    getContainer().get()?.finish()
                }

                isPIP = false
            }
        )

        getPIPViewGroup = {
            removeFromWindowManager(appCompatActivity.windowManager, pipExampleView)

            isPIP = false

            WeakReference(pipExampleView)
        }
    }

    fun destroy() {
        appCompatActivity.lifecycle.removeObserver(lifecycleObserver)
        removeFromWindowManager(appCompatActivity.windowManager, pipExampleView)
    }

    protected fun finalize() {
        Log.d("???", "PIPExampleViewManager finalize")
    }
}