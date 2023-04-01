package com.arash.neshan.test2.utils.swipe

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import com.arash.neshan.test2.R
import com.arash.neshan.test2.databinding.LayoutSwipeButtonBinding

class SwipeButton : RelativeLayout {
    private var initialX = 0f
    var isActive = false
        private set
    private var onStateChangeListener: OnStateChangeListener? = null
    private var onActiveListener: OnActiveListener? = null
    private var collapsedWidth = 0
    private var collapsedHeight = 0
    private var trailEnabled = false
    private var hasActivationState = false

    private var binding: LayoutSwipeButtonBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = LayoutSwipeButtonBinding.inflate(inflater, this, true)
    }

    constructor(context: Context) : super(context) {
        init(context, null, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, -1, -1)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr, -1)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    fun setTextTime(text: String?) {
        binding.tvTime.text = text
    }

    fun setTextDate(text: String?) {
        binding.tvDate.text = text
    }

    fun setTextDay(text: String?) {
        binding.tvDay.text = text
    }

    fun setOnStateChangeListener(onStateChangeListener: OnStateChangeListener?) {
        this.onStateChangeListener = onStateChangeListener
    }

    fun setOnActiveListener(onActiveListener: OnActiveListener?) {
        this.onActiveListener = onActiveListener
    }

    fun setInnerTextTimePadding(left: Int, top: Int, right: Int, bottom: Int) {
        binding.tvTime.setPadding(left, top, right, bottom)
    }

    fun setInnerTextDayPadding(left: Int, top: Int, right: Int, bottom: Int) {
        binding.tvDay.setPadding(left, top, right, bottom)
    }

    fun setInnerTextDatePadding(left: Int, top: Int, right: Int, bottom: Int) {
        binding.tvDate.setPadding(left, top, right, bottom)
    }

    fun setHasActivationState(hasActivationState: Boolean) {
        this.hasActivationState = hasActivationState
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        hasActivationState = true
        val layoutParamsView = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParamsView.addRule(CENTER_IN_PARENT, TRUE)
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(CENTER_IN_PARENT, TRUE)
        val swipeButton = ImageView(context)
        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
            val typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.SwipeButton,
                defStyleAttr, defStyleRes
            )
            collapsedWidth = typedArray.getDimension(
                R.styleable.SwipeButton_button_image_width,
                ViewGroup.LayoutParams.WRAP_CONTENT.toFloat()
            ).toInt()
            collapsedHeight = typedArray.getDimension(
                R.styleable.SwipeButton_button_image_height,
                ViewGroup.LayoutParams.WRAP_CONTENT.toFloat()
            ).toInt()
            trailEnabled = typedArray.getBoolean(
                R.styleable.SwipeButton_button_trail_enabled,
                false
            )
            binding.tvSliding.text = typedArray.getText(R.styleable.SwipeButton_text_sliding)
            binding.tvSlidingSwipe.text =
                typedArray.getText(R.styleable.SwipeButton_text_sliding_swipe)
            binding.ivSwipe.setImageDrawable(typedArray.getDrawable(R.styleable.SwipeButton_drawable_sliding_swipe))

            if (typedArray.getBoolean(R.styleable.SwipeButton_is_rtl, true)) {
                binding.flSliding.layoutDirection = LAYOUT_DIRECTION_RTL
            } else {
                binding.flSliding.layoutDirection = LAYOUT_DIRECTION_LTR
            }

            binding.llSwipe.background =
                typedArray.getDrawable(R.styleable.SwipeButton_background_swipe_color)
            binding.flSliding.background =
                typedArray.getDrawable(R.styleable.SwipeButton_background_color)
            binding.tvTime.text = typedArray.getText(R.styleable.SwipeButton_inner_time)
            binding.tvDate.text = typedArray.getText(R.styleable.SwipeButton_inner_date)
            binding.tvDay.text = typedArray.getText(R.styleable.SwipeButton_inner_day)
            val initialState = typedArray.getInt(R.styleable.SwipeButton_initial_state, DISABLED)
            if (initialState == ENABLED) {
                val layoutParamsButton = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParamsButton.addRule(ALIGN_PARENT_LEFT, TRUE)
                layoutParamsButton.addRule(CENTER_VERTICAL, TRUE)
                addView(swipeButton, layoutParamsButton)
                isActive = true
            } else {
                val layoutParamsButton = LayoutParams(collapsedWidth, collapsedHeight)
                layoutParamsButton.addRule(ALIGN_PARENT_LEFT, TRUE)
                layoutParamsButton.addRule(CENTER_VERTICAL, TRUE)
                addView(swipeButton, layoutParamsButton)
                isActive = false
            }
            val buttonLeftPadding = typedArray.getDimension(
                R.styleable.SwipeButton_button_left_padding, 0f
            )
            val buttonTopPadding = typedArray.getDimension(
                R.styleable.SwipeButton_button_top_padding, 0f
            )
            val buttonRightPadding = typedArray.getDimension(
                R.styleable.SwipeButton_button_right_padding, 0f
            )
            val buttonBottomPadding = typedArray.getDimension(
                R.styleable.SwipeButton_button_bottom_padding, 0f
            )
            swipeButton.setPadding(
                buttonLeftPadding.toInt(),
                buttonTopPadding.toInt(),
                buttonRightPadding.toInt(),
                buttonBottomPadding.toInt()
            )
            hasActivationState =
                typedArray.getBoolean(R.styleable.SwipeButton_has_activate_state, true)
            typedArray.recycle()
        }
        setOnTouchListener(buttonTouchListener)

    }

    private val buttonTouchListener: OnTouchListener
        get() = object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        return !TouchUtils.isTouchOutsideInitialPosition(
                            event,
                            binding.llSwipe,
                            binding.flSliding.layoutDirection == LAYOUT_DIRECTION_RTL
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (initialX == 0f) {
                            initialX = binding.llSwipe.x
                        }
                        if (event.x > binding.llSwipe.width / 2 &&
                            event.x + binding.llSwipe.width / 2 < width
                        ) {
                            if (binding.flSliding.layoutDirection == LAYOUT_DIRECTION_RTL) {
                                binding.llSwipe.x = event.x - binding.llSwipe.width / 2
                                binding.view.alpha =
                                    0 + 1.3f * (binding.llSwipe.x - binding.llSwipe.width) / width
                                binding.tvTime.alpha =
                                    0 + 1.3f * (binding.llSwipe.x - binding.llSwipe.width) / width
                                binding.tvDay.alpha =
                                    0 + 1.3f * (binding.llSwipe.x - binding.llSwipe.width) / width
                                binding.tvDate.alpha =
                                    0 + 1.3f * (binding.llSwipe.x - binding.llSwipe.width) / width


                            } else if (binding.flSliding.layoutDirection == LAYOUT_DIRECTION_LTR) {
                                binding.llSwipe.x = event.x - binding.llSwipe.width / 2
                                binding.view.alpha =
                                    1 - 1.3f * (binding.llSwipe.x + binding.llSwipe.width) / width
                                binding.tvTime.alpha =
                                    1 - 1.3f * (binding.llSwipe.x + binding.llSwipe.width) / width
                                binding.tvDay.alpha =
                                    1 - 1.3f * (binding.llSwipe.x + binding.llSwipe.width) / width
                                binding.tvDate.alpha =
                                    1 - 1.3f * (binding.llSwipe.x + binding.llSwipe.width) / width
                            }

                        }
                        if (event.x + binding.llSwipe.width / 2 > width &&
                            binding.llSwipe.x + binding.llSwipe.width / 2 < width
                        ) {
                            binding.llSwipe.x = (width - binding.llSwipe.width).toFloat()
                        }
                        if (event.x < binding.llSwipe.width / 2) {
                            binding.llSwipe.x = 0f
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isActive) {
                            collapseButton()
                        } else {
                            if (binding.flSliding.layoutDirection == LAYOUT_DIRECTION_RTL) {
                                if (binding.llSwipe.x < width * 0.1) {
                                    if (hasActivationState) {
                                        expandButton()
                                    } else if (onActiveListener != null) {
                                        onActiveListener!!.onActive()
                                        moveButtonBack()
                                    }
                                } else {
                                    moveButtonBack()
                                }
                            } else {
                                if (binding.llSwipe.x + binding.llSwipe.width > width * 0.9) {
                                    if (hasActivationState) {
                                        expandButton()
                                    } else
                                        if (onActiveListener != null) {
                                            onActiveListener!!.onActive()
                                            moveButtonBack()
                                        }
                                } else {
                                    moveButtonBack()
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }
        }

    @SuppressLint("ObjectAnimatorBinding")
    private fun moveButtonBack() {
        val positionAnimator = ValueAnimator.ofFloat(
            binding.llSwipe.x,
            if (binding.flSliding.layoutDirection == LAYOUT_DIRECTION_RTL) ((binding.flSliding.x + binding.flSliding.width) - binding.llSwipe.width) else 1f
        )
        positionAnimator.interpolator = AccelerateDecelerateInterpolator()
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            binding.llSwipe.x = x
        }
        positionAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
            }

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
            }
        })

        val objectAnimatorView = ObjectAnimator.ofFloat(
            binding.view, "alpha", 1f
        )
        val objectAnimatorTime = ObjectAnimator.ofFloat(
            binding.tvTime, "alpha", 1f
        )
        val objectAnimatorDay = ObjectAnimator.ofFloat(
            binding.tvDay, "alpha", 1f
        )
        val objectAnimatorDate = ObjectAnimator.ofFloat(
            binding.tvDate, "alpha", 1f
        )
        positionAnimator.duration = 200
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(objectAnimatorView, positionAnimator)
        animatorSet.playTogether(objectAnimatorTime, positionAnimator)
        animatorSet.playTogether(objectAnimatorDate, positionAnimator)
        animatorSet.playTogether(objectAnimatorDay, positionAnimator)
        animatorSet.start()
    }

    private fun expandButton() {
        val positionAnimator = ValueAnimator.ofFloat(
            binding.llSwipe.x,
            if (binding.flSliding.layoutDirection == LAYOUT_DIRECTION_RTL) 5f else 0f
        )
        positionAnimator.addUpdateListener {
            val x = positionAnimator.animatedValue as Float
            binding.llSwipe.x = x
        }
        val widthAnimator = ValueAnimator.ofInt(
            binding.llSwipe.width,
            width
        )
        widthAnimator.addUpdateListener {
            val params = binding.llSwipe.layoutParams
            params.width = (widthAnimator.animatedValue as Int)
            binding.llSwipe.layoutParams = params
        }
        val animatorSet = AnimatorSet()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                isActive = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                isActive = true
                if (onStateChangeListener != null) {
                    onStateChangeListener!!.onStateChange(isActive)
                }
                if (onActiveListener != null) {
                    onActiveListener!!.onActive()
                }
                binding.root.postDelayed({
                    collapseButton()
                }, 500)
            }
        })
        animatorSet.playTogether(positionAnimator, widthAnimator)
        animatorSet.duration = 0
        animatorSet.start()
        moveButtonBack()
    }

    private fun active() {
        isActive = true
        binding.root.postDelayed({
            isActive = true
            if (onStateChangeListener != null) {
                onStateChangeListener!!.onStateChange(isActive)
            }
            if (onActiveListener != null) {
                onActiveListener!!.onActive()
            }
            binding.root.postDelayed({
                collapseButton()
            }, 500)
        }, 500)
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun collapseButton() {
        val finalWidth: Int = if (collapsedWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
            binding.llSwipe.height
        } else {
            collapsedWidth
        }
        val widthAnimator = ValueAnimator.ofInt(binding.llSwipe.width, finalWidth)
        widthAnimator.addUpdateListener {
            val params = binding.llSwipe.layoutParams
            params.width = (widthAnimator.animatedValue as Int)
            binding.llSwipe.layoutParams = params
        }
        widthAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                isActive = false
                if (onStateChangeListener != null) {
                    onStateChangeListener!!.onStateChange(isActive)
                }
            }
        })
        val objectAnimatorView = ObjectAnimator.ofFloat(
            binding.view, "alpha", 1f
        )
        val objectAnimatorTime = ObjectAnimator.ofFloat(
            binding.tvTime, "alpha", 1f
        )
        val objectAnimatorDate = ObjectAnimator.ofFloat(
            binding.tvDate, "alpha", 1f
        )
        val objectAnimatorDay = ObjectAnimator.ofFloat(
            binding.tvDay, "alpha", 1f
        )
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(objectAnimatorView, widthAnimator)
        animatorSet.playTogether(objectAnimatorTime, widthAnimator)
        animatorSet.playTogether(objectAnimatorDate, widthAnimator)
        animatorSet.playTogether(objectAnimatorDay, widthAnimator)
        animatorSet.start()
    }

    companion object {
        private const val ENABLED = 0
        private const val DISABLED = 1
    }
}
