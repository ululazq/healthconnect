package com.healthconnect.sync.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.healthconnect.sync.R

class PermissionRationaleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rationale)

        findViewById<Button>(R.id.btnCloseRationale)?.setOnClickListener {
            finish()
        }
    }
}
