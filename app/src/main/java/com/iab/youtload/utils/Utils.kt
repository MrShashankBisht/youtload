package com.iab.youtload

import android.content.res.Resources

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Float
    get() = (this * Resources.getSystem().displayMetrics.density)