package com.swein.pipexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.swein.pipexample.pipmanager.PIPManager
import java.lang.ref.WeakReference

class PIPExampleActivity : AppCompatActivity() {

    private val frameLayoutPlayerContainer: FrameLayout by lazy {
        findViewById(R.id.frameLayoutPlayerContainer)
    }

    private var pipExampleView: PIPExampleView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pip_example)

        Log.d("???", "PIPExampleActivity onCreate")

        initView()

        PIPManager.getContainer = {
            WeakReference(this)
        }
    }

    private fun initView() {

        Log.d("???", "receive pip")
        pipExampleView = PIPManager.getPIPViewGroup().get() as PIPExampleView

        // reset player size
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        pipExampleView?.layoutParams = layoutParams

        frameLayoutPlayerContainer.addView(pipExampleView)

    }

    override fun onDestroy() {
        super.onDestroy()
        frameLayoutPlayerContainer.removeAllViews()
        pipExampleView = null
        Log.d("???", "PipExampleActivity onDestroy")
    }
}