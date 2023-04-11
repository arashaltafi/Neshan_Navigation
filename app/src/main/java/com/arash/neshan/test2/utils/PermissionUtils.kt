package com.arash.neshan.test2.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionUtils {

    interface PermissionListener {
        fun observe(permissions: Map<String, Boolean>)
    }

    fun register(
        activity: AppCompatActivity,
        listener: PermissionListener,
    ) = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        listener.observe(permissions)
    }

    fun register(
        parent: Fragment,
        listener: PermissionListener,
    ) = parent.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        listener.observe(permissions)
    }

    fun requestPermission(
        context: Context,
        resultContract: ActivityResultLauncher<Array<String>>,
        vararg permissions: String,
    ) {
        val isNotGranted = permissions.any { !isGranted(context, it) }
        if (isNotGranted) {
            request(resultContract, *permissions)
        }
    }

    private fun request(
        resultContract: ActivityResultLauncher<Array<String>>,
        vararg permissions: String
    ) {
        resultContract.launch(permissions.toList().toTypedArray())
    }

    fun isGranted(context: Context, vararg permissions: String): Boolean {
        return permissions.toList().any {
            ContextCompat.checkSelfPermission(
                context, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
