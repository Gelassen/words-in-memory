package io.github.gelassen.wordinmemory

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import io.github.gelassen.wordinmemory.R

class GroupChoiceView : LinearLayout {

    private var isOfferChosen = true
    private var enabledTextColor: Int = -1
    private var disabledTextColor: Int = -1

    constructor(context: Context?) : super(context) { }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init(context!!, attrs) }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context!!, attrs)
    }
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context!!, attrs)
    }

    override fun addView(child: View) {
        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        throw IllegalAccessException("Method is disabled. Component contains only two items which is added at launch")
    }

    override fun addView(child: View, width: Int, height: Int) {
        throw IllegalAccessException("Method is disabled. Component contains only two items which is added at launch")
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        super.addView(child, params)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    fun isOfferSelected() : Boolean {
        return isOfferChosen
    }

    fun onOfferClick() {
        isOfferChosen = true
        updateColors()
    }

    fun onDemandClick() {
        isOfferChosen = false
        updateColors()
    }

    fun updateColors() {
        findViewById<TextView>(R.id.offer_option).setTextColor(if (isOfferChosen) enabledTextColor else disabledTextColor)
        findViewById<TextView>(R.id.demand_option).setTextColor(if (isOfferChosen) disabledTextColor else enabledTextColor)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        enabledTextColor = ContextCompat.getColor(context, R.color.blue_light)
        disabledTextColor = ContextCompat.getColor(context, R.color.grey)

        addView(
            getView(text = resources.getString(R.string.offers_title), R.id.offer_option),
            getViewLayoutParams(
                marginStart = context.resources.getDimensionPixelOffset(R.dimen.base_margin),
                marginEnd = 0
            ))
        addView(
            getView(text = resources.getString(R.string.demands_title), R.id.demand_option),
            getViewLayoutParams(
                marginStart = 0,
                marginEnd = context.resources.getDimensionPixelOffset(R.dimen.base_margin)
            )
        )
        findViewById<TextView>(R.id.offer_option).setTextColor(if (isOfferChosen) enabledTextColor else disabledTextColor)
        findViewById<TextView>(R.id.demand_option).setTextColor(if (isOfferChosen) disabledTextColor else enabledTextColor)
    }

    private fun getView(text: String, id: Int) : TextView {
        val view = TextView(context)
        view.setId(id)
        view.text = text
        view.setPadding(context.resources.getDimensionPixelOffset(R.dimen.selectable_view_padding))
        view.setTextColor(context.resources.getColor(R.color.blue_light))
        view.isClickable = true
        view.isFocusable = true
        view.gravity = Gravity.CENTER
        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        view.setBackgroundResource(outValue.resourceId)
        return view
    }

    private fun getViewLayoutParams(marginStart: Int, marginEnd: Int) : LinearLayout.LayoutParams {
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        params.marginStart = marginStart
        params.marginEnd = marginEnd
        return params
    }

}