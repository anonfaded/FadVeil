package com.fadedhood.fadveil.tutorial

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class TutorialStep(
    @StringRes val textResId: Int,
    @DrawableRes val iconResId: Int
) 