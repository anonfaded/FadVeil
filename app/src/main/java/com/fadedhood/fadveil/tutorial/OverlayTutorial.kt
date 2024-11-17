package com.fadedhood.fadveil.tutorial

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fadedhood.fadveil.R
import com.google.android.material.card.MaterialCardView

class OverlayTutorial(private val context: Context) {
    private var currentStep = 0
    private var tipView: View? = null
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_Fadveil)
    
    data class TutorialStep(
        val textResId: Int,
        val iconResId: Int
    )
    
    private val tutorialSteps = listOf(
        TutorialStep(R.string.resize_hint, R.drawable.ic_pinch),
        TutorialStep(R.string.mode_hint, R.drawable.ic_double_tap),
        TutorialStep(R.string.control_hint, R.drawable.ic_long_press)
    )
    
    fun showNextTip() {
        if (currentStep < tutorialSteps.size) {
            showTip(tutorialSteps[currentStep])
            currentStep++
        }
    }
    
    private fun showTip(step: TutorialStep) {
        try {
            // Remove previous tip if exists
            tipView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: IllegalArgumentException) {
                    // View might already be removed
                }
            }
            
            // Create new tip view
            val inflater = LayoutInflater.from(themedContext)
            val cardView = MaterialCardView(themedContext).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                elevation = context.resources.getDimension(R.dimen.card_elevation)
            }
            
            val contentView = inflater.inflate(R.layout.tutorial_tip, cardView, true)
            
            contentView.findViewById<ImageView>(R.id.tipIcon).setImageResource(step.iconResId)
            contentView.findViewById<TextView>(R.id.tipText).setText(step.textResId)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            
            tipView = cardView
            windowManager.addView(cardView, params)
            
            cardView.postDelayed({
                try {
                    windowManager.removeView(cardView)
                } catch (e: IllegalArgumentException) {
                    // View might already be removed
                }
            }, 3000)
        } catch (e: Exception) {
            // Handle any errors during tip display
            e.printStackTrace()
        }
    }
    
    fun destroy() {
        tipView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // View might already be removed
            }
        }
    }
} 