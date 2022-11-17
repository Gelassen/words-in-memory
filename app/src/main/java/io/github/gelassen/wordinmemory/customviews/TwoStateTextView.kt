package io.github.gelassen.wordinmemory

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView


class TwoStateTextView : AppCompatTextView {

    private var isChosen = true
    private var isEnabledTextColor: Int = -1
    private var isDisabledTextColor: Int = -1

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
/*        // TODO replace on color state list when minSdk will be API 23 or higher
        val colorStateList = context.getColorStateList(R.color.selector_two_state_view)*/
        isEnabledTextColor = context.getColor(R.color.blue_light)
        isDisabledTextColor = context.getColor(R.color.grey)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onDraw(canvas: Canvas?) {
        setTextColor(
            if (isChosen) context.getColor(R.color.blue_light)
            else context.getColor(R.color.grey)
        )
        super.onDraw(canvas)
    }

    override fun callOnClick(): Boolean {
        isChosen = !isChosen
        invalidateOutline()
        return true
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val attributes: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.TwoStateTextView
        )
        isChosen = attributes.getBoolean(
            R.styleable.TwoStateTextView_selected,
            true
        )
        attributes.recycle()
    }
}