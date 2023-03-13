package com.arash.neshan.test2.component.view.snackbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.arash.neshan.test2.R
import com.google.android.material.snackbar.ContentViewCallback

class SnackBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ContentViewCallback {

    private val container: View
    private val snackBarIcon: ImageView
    private val snackBarText: TextView

    init {
        View.inflate(context, R.layout.view_snack_bar, this)
        clipToPadding = false
        this.container = findViewById(R.id.container)
        this.snackBarIcon = findViewById(R.id.snackBarIcon)
        this.snackBarText = findViewById(R.id.snackBarText)
    }

    override fun animateContentIn(delay: Int, duration: Int) {
//        val scaleX = ObjectAnimator.ofFloat(snackBarIcon, View.SCALE_X, 0f, 1f)
//        val scaleY = ObjectAnimator.ofFloat(snackBarIcon, View.SCALE_Y, 0f, 1f)
//        val animatorSet = AnimatorSet().apply {
//            interpolator = OvershootInterpolator()
//            setDuration(500)
//            playTogether(scaleX, scaleY)
//        }
//        animatorSet.start()
    }

    override fun animateContentOut(delay: Int, duration: Int) {
    }

    fun setSnackBarText(text: String) {
        this.snackBarText.text = text
    }

    fun setSnackBarType(snackBarType: SnackBarType) {
        when (snackBarType) {
            SnackBarType.NORMAL -> this.snackBarIcon.setImageResource(R.drawable.ic_circle_notification)
            SnackBarType.ERROR -> this.snackBarIcon.setImageResource(R.drawable.ic_warning)
        }
    }

    fun setSnackBarIconResource(@DrawableRes iconResource: Int) {
        this.snackBarIcon.setImageResource(iconResource)
    }

    fun onCloseClick(onClose: () -> Unit) {
        this.container.setOnClickListener {
            onClose.invoke()
        }
    }

}