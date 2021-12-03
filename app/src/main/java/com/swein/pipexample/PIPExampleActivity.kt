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

    private var pipExampleViewWeakReference: WeakReference<PIPExampleView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pip_example)

        Log.d("???", "PIPExampleActivity onCreate")

        initView()

        PIPManager.transmitContainerActivity = {
            WeakReference(this)
        }
    }

    private fun initView() {

        Log.d("???", "receive pip")
        PIPManager.transmitPIPViewGroup?.let { transmitPIPViewGroup ->

            pipExampleViewWeakReference = WeakReference(transmitPIPViewGroup().get() as PIPExampleView).apply {

                // reset player size
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.get()?.layoutParams = layoutParams

                frameLayoutPlayerContainer.removeAllViews()
                frameLayoutPlayerContainer.addView(this.get())

            }
        } ?: run {
            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        frameLayoutPlayerContainer.removeAllViews()
        pipExampleViewWeakReference?.clear()
        pipExampleViewWeakReference = null
        Log.d("???", "PipExampleActivity onDestroy")
    }
}