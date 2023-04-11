package com.arash.neshan.test2.ui

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.arash.neshan.test2.databinding.ActivityHomeBinding
import com.arash.neshan.test2.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityHomeBinding.inflate(layoutInflater)
    }

    private val registerNotificationResult = PermissionUtils.register(this,
        object : PermissionUtils.PermissionListener {
            override fun observe(permissions: Map<String, Boolean>) {
            }
        })

    private val registerLocationResult = PermissionUtils.register(this,
        object : PermissionUtils.PermissionListener {
            override fun observe(permissions: Map<String, Boolean>) {
                requestPermissionNotification()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        requestPermissionLocation()
    }

    private fun requestPermissionNotification() {
        if (!PermissionUtils.isGranted(this, Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionUtils.requestPermission(
                    this, registerNotificationResult,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun requestPermissionLocation() {
        if (PermissionUtils.isGranted(this, Manifest.permission.ACCESS_FINE_LOCATION).not()) {
            PermissionUtils.requestPermission(
                this, registerLocationResult, Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
}