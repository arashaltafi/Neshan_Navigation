package org.neshan.component.view.snackbar

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import org.neshan.R

class SnackBar(parent: ViewGroup, content: SnackBarView) :
    BaseTransientBottomBar<SnackBar>(parent, content, content) {

    init {
        val sbView = getView()
        sbView.setBackgroundColor(
            ContextCompat.getColor(view.context, android.R.color.transparent)
        )
        sbView.setPadding(0, 0, 0, 0)
        when (val params = sbView.layoutParams) {
            is CoordinatorLayout.LayoutParams -> {
                params.gravity = Gravity.TOP
                params.topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_30)
                params.leftMargin = 0
                params.rightMargin = 0
                sbView.layoutParams = params
            }
            else -> {
                params as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                params.topMargin = context.resources.getDimensionPixelSize(R.dimen.margin_30)
                params.leftMargin = 0
                params.rightMargin = 0
                sbView.layoutParams = params
            }
        }
        animationMode = ANIMATION_MODE_FADE
    }

    companion object {
        fun make(
            anchorView: View,
            @StringRes text: Int,
            type: SnackBarType? = SnackBarType.NORMAL,
            @DrawableRes icon: Int? = null
        ): SnackBar {
            return make(anchorView, anchorView.context.resources.getString(text), type, icon)
        }

        fun make(
            anchorView: View,
            text: String,
            type: SnackBarType? = SnackBarType.NORMAL,
            @DrawableRes icon: Int? = null
        ): SnackBar {
            // First we find a suitable parent for our custom view
            val parent = findSuitableParent(anchorView) ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            // We inflate our custom view
            val snackBarView = LayoutInflater.from(anchorView.context).inflate(
                R.layout.layout_snack_bar, parent, false
            ) as SnackBarView
            val snackBar = SnackBar(parent, snackBarView)

            snackBarView.setSnackBarText(text)
            type?.let { snackBarView.setSnackBarType(it) }
            icon?.let { snackBarView.setSnackBarIconResource(it) }
            snackBarView.onCloseClick { snackBar.dismiss() }

            return snackBar
        }

        private fun findSuitableParent(targetView: View): ViewGroup? {
            var view: View? = targetView
            var fallback: ViewGroup? = null
            do {
                if (view is CoordinatorLayout) {
                    // We've found a CoordinatorLayout, use it
                    return view
                } else if (view is FrameLayout) {
                    if (view.id == android.R.id.content) {
                        // If we've hit the decor content view, then we didn't find a CoL in the
                        // hierarchy, so use it.
                        return view
                    } else {
                        // It's not the content view but we'll use it as our fallback
                        fallback = view
                    }
                }

                if (view != null) {
                    // Else, we will loop and crawl up the view hierarchy and try to find a parent
                    val parent = view.parent
                    view = if (parent is View) parent else null
                }
            } while (view != null)

            // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
            return fallback
        }
    }

}
