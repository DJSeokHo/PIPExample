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
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.swein.exoplayerkotlin.framework.display.DisplayUtility
import java.lang.ref.WeakReference


object PIPManager {

    var isPIP = false

    private val layoutParams = WindowManager.LayoutParams()
    private var lastX = 0f
    private var lastY = 0f

    // init this in main activity
    var transmitPIPViewGroup: (() -> WeakReference<ViewGroup>)? = null

    // init this in pip container activity
    var transmitContainerActivity: (() -> WeakReference<Activity>)? = null

    fun requestOverlaysPermission(activity: Activity, afterSuccess: () -> Unit) {

        if (!Settings.canDrawOverlays(activity)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.packageName)
            )
            activity.startActivityForResult(intent, 999)
        }
        else {
            afterSuccess()
        }
    }

    fun togglePIP(isPIP: Boolean, activity: Activity, exitToTarget: Class<*>?, viewGroup: ViewGroup,
                  beforeEnterPIP: () -> Unit, afterExitPIP:() -> Unit
    ) {

        Log.d("???", "isPIP????? $isPIP")

        if (isPIP) {
            exitPIPMode(activity, exitToTarget, viewGroup, afterExitPIP)
        }
        else {
            enterPIPMode(activity, viewGroup, false, 250f, 250 * 0.5625f, beforeEnterPIP)
            transmitContainerActivity?.let {
                it().get()?.finish()
            }
        }

        Log.d("???", "isPIP now ????? $isPIP")
    }

    fun removeFromWindowManager(activity: Activity, viewGroup: ViewGroup) {

        if (viewGroup.windowToken != null) {

            try {
                activity.windowManager.removeViewImmediate(viewGroup)
            }
            catch (e: Exception) {
                Log.d("???", "${e.message}")
            }
        }
    }

    private fun enterPIPMode(activity: Activity, viewGroup: ViewGroup, backToDesktop: Boolean, widthInDp: Float, heightInDp: Float, beforeAction: () -> Unit) {

        beforeAction()

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

                activity.windowManager.addView(viewGroup, layoutParams)
            }

        }.start()

    }

    private fun exitPIPMode(activity: Activity, exitToTarget: Class<*>?, viewGroup: ViewGroup, afterAction: () -> Unit) {

        try {
            removeFromWindowManager(activity, viewGroup)
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

        Intent(viewGroup.context, exitToTarget).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            viewGroup.context.startActivity(this)
        }

        afterAction()
    }

    fun actionDown(motionEvent: MotionEvent) {
        lastX = motionEvent.rawX
        lastY = motionEvent.rawY
    }

    fun actionMove(motionEvent: MotionEvent, viewGroup: ViewGroup, windowManager: WindowManager) {

        val nowX: Float = motionEvent.rawX
        val nowY: Float = motionEvent.rawY

        val tranX = nowX - lastX
        val tranY = nowY - lastY

        layoutParams.x += tranX.toInt()
        layoutParams.y += tranY.toInt()

        windowManager.updateViewLayout(viewGroup, layoutParams)

        lastX = nowX
        lastY = nowY
    }
}