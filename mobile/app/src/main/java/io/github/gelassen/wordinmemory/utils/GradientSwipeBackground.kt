package io.github.gelassen.wordinmemory.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class GradientSwipeBackground
    (start: Int, end: Int) : GradientDrawable(Orientation.LEFT_RIGHT, intArrayOf(start, end)) {

    init {
        cornerRadius = 16f
    }


}