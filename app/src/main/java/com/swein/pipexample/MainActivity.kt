package com.swein.pipexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.swein.pipexample.pipmanager.PIPManager
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var pipExampleView: PIPExampleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.button).setOnClickListener {

            Intent(this, PIPExampleActivity::class.java).apply {
                startActivity(this)
            }

        }

        initPIPExampleView()

    }


    private fun initPIPExampleView() {

        pipExampleView = PIPExampleView(this,
            onPIPClicked = {
                PIPManager.requestOverlays(this) {
                    PIPManager.togglePIP(PIPManager.isPIP, this, PIPExampleActivity::class.java, pipExampleView,
                        beforeEnterPIP = {
                            PIPManager.isPIP = true
                            pipExampleView.resetActionListener(onActionMove = {
                                PIPManager.actionMove(
                                    it,
                                    pipExampleView,
                                    windowManager
                                )
                            }, onActionDown = {
                                PIPManager.actionDown(it)
                            })
                        },
                        afterExitPIP = {
                            PIPManager.isPIP = false
                        }
                    )
                }
            },
            onCloseClicked = {

                if (PIPManager.isPIP) {

                    PIPManager.removeFromWindowManager(
                        this,
                        pipExampleView
                    )
                }
                else {
                    PIPManager.getContainer().get()?.finish()
                }

                PIPManager.isPIP = false
            }
        )

        PIPManager.getPIPViewGroup = {
            PIPManager.removeFromWindowManager(this, pipExampleView)

            PIPManager.isPIP = false

            WeakReference(pipExampleView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PIPManager.removeFromWindowManager(this, pipExampleView)
        Log.d("???", "MainActivity onDestroy")
    }
}