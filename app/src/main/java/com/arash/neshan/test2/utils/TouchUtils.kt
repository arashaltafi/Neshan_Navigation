package com.arash.neshan.test2.utils

import android.view.MotionEvent
import android.view.View

internal object TouchUtils {
    fun isTouchOutsideInitialPosition(event: MotionEvent, view: View, isRtl: Boolean): Boolean {
        return if (isRtl) event.x < view.x else event.x > view.x + view.width
    }
}