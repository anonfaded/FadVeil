package com.fadedhood.fadveil.utils

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object FontUtils {
    private var ubuntuRegular: Typeface? = null

    fun getUbuntuFont(context: Context): Typeface {
        if (ubuntuRegular == null) {
            ubuntuRegular = try {
                Typeface.createFromAsset(context.assets, "ubuntu_regular.ttf")
            } catch (e: Exception) {
                e.printStackTrace()
                Typeface.DEFAULT
            }
        }
        return ubuntuRegular!!
    }

    fun applyFontToActivity(activity: AppCompatActivity) {
        activity.window?.decorView?.let { 
            if (it is ViewGroup) applyFontRecursively(it, activity) 
        }
    }

    fun applyFontToFragment(fragment: Fragment, view: View) {
        if (view is ViewGroup) applyFontRecursively(view, fragment.requireContext())
    }

    private fun applyFontRecursively(viewGroup: ViewGroup, context: Context) {
        val childCount = viewGroup.childCount
        for (i in 0 until childCount) {
            when (val child = viewGroup.getChildAt(i)) {
                is TextView -> child.typeface = getUbuntuFont(context)
                is ViewGroup -> {
                    if (child is BottomNavigationView) {
                        // Special handling for BottomNavigationView
                        child.getChildAt(0)?.let { 
                            if (it is ViewGroup) applyFontRecursively(it, context)
                        }
                    }
                    applyFontRecursively(child, context)
                }
            }
        }
    }
} 