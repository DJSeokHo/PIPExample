package com.swein.pipexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.swein.pipexample.pipmanager.PIPManager

class MainActivity : AppCompatActivity() {

    private lateinit var pipManager: PIPManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pipManager = PIPManager(this).apply {
            create()
        }

        findViewById<Button>(R.id.button).setOnClickListener {

            Intent(this, PIPExampleActivity::class.java).apply {
                startActivity(this)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pipManager.destroy()
        Log.d("???", "MainActivity onDestroy")
    }
}