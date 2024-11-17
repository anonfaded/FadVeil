package com.fadedhood.fadveil.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.fadedhood.fadveil.R
import com.google.android.material.card.MaterialCardView
import android.view.ContextThemeWrapper
import com.fadedhood.fadveil.tutorial.OverlayTutorial
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import android.view.Gravity
import android.graphics.drawable.GradientDrawable

class OverlayManager(private val context: Context) {
    private var windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_Fadveil)
    val overlayView: MaterialCardView
    private var controlPanel: View? = null
    
    private var isPixelateMode = false
    private var opacity = 80
    private var pixelSize = 50
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var updateHandler = Handler(Looper.getMainLooper())
    private var isUpdating = false
    
    private var params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        300, // Initial width
        300, // Initial height
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    private val tutorial = OverlayTutorial(context)
    
    init {
        overlayView = MaterialCardView(themedContext).apply {
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.black))
            alpha = opacity.toFloat() / 100
            radius = 0f // Square corners
        }
        
        windowManager.addView(overlayView, params)
        
        // Show first tip after a short delay
        overlayView.postDelayed({
            tutorial.showNextTip()
        }, 1000)
    }
    
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            toggleMode()
            return true
        }
        
        override fun onLongPress(e: MotionEvent) {
            showControlPanel()
        }
    })
    
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            handleResize(detector.scaleFactor)
            return true
        }
    })
    
    fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle multi-touch first
        if (event.pointerCount > 1) {
            scaleGestureDetector.onTouchEvent(event)
            return true
        }
        
        // Then check for gestures
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        
        // Finally handle movement
        return handleMove(event)
    }
    
    private fun handleMove(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x.toFloat()
                initialY = params.y.toFloat()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                params.y = (initialY + (event.rawY - initialTouchY)).toInt()
                windowManager.updateViewLayout(overlayView, params)
            }
        }
        return true
    }
    
    private fun handleResize(scaleFactor: Float) {
        params.width = (params.width * scaleFactor).toInt()
        params.height = (params.height * scaleFactor).toInt()
        
        // Ensure minimum size
        params.width = params.width.coerceAtLeast(100)
        params.height = params.height.coerceAtLeast(100)
        
        windowManager.updateViewLayout(overlayView, params)
        
        // Show next tip after certain actions
        tutorial.showNextTip()
    }
    
    private fun toggleMode() {
        isPixelateMode = !isPixelateMode
        updateOverlayAppearance()
    }
    
    private fun showControlPanel() {
        if (controlPanel == null) {
            // Use themed context for inflation
            val inflater = LayoutInflater.from(themedContext)
            
            // Create the container with themed context
            val container = MaterialCardView(themedContext).apply {
                radius = context.resources.getDimension(R.dimen.card_corner_radius)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                elevation = context.resources.getDimension(R.dimen.card_elevation)
            }
            
            // Inflate the content into the container
            inflater.inflate(R.layout.overlay_control_panel_content, container, true)
            
            val controlParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            
            controlPanel = container
            windowManager.addView(container, controlParams)
            
            // Set up controls
            container.findViewById<MaterialButtonToggleGroup>(R.id.modeToggleGroup)?.apply {
                check(if (isPixelateMode) R.id.pixelateMode else R.id.blackMode)
                addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (isChecked) {
                        isPixelateMode = (checkedId == R.id.pixelateMode)
                        container.findViewById<Slider>(R.id.pixelateSlider)?.visibility =
                            if (isPixelateMode) View.VISIBLE else View.GONE
                        updateOverlayAppearance()
                    }
                }
            }
            
            container.findViewById<Slider>(R.id.opacitySlider)?.apply {
                value = opacity.toFloat()
                valueFrom = 0f    // 0% (fully transparent)
                valueTo = 100f    // 100% (fully opaque)
                stepSize = 1f
                setLabelFormatter { value -> "${value.toInt()}%" }
                addOnChangeListener { _, value, _ ->
                    setOpacity(value.toInt())
                }
            }
            
            container.findViewById<Slider>(R.id.pixelateSlider)?.apply {
                visibility = if (isPixelateMode) View.VISIBLE else View.GONE
                value = pixelSize.toFloat()
                valueFrom = 0f    // Minimal pixelation
                valueTo = 100f    // Maximum pixelation
                stepSize = 1f
                setLabelFormatter { value -> "${value.toInt()}%" }
                addOnChangeListener { _, value, _ ->
                    setPixelSize(value.toInt())
                }
            }
        }
    }
    
    private fun updateOverlayAppearance() {
        if (isPixelateMode) {
            startPixelateEffect()
        } else {
            stopPixelateEffect()
            applyBlackEffect()
        }
    }

    private fun startPixelateEffect() {
        if (!isUpdating) {
            isUpdating = true
            updatePixelateEffect()
        }
    }

    private fun stopPixelateEffect() {
        isUpdating = false
        updateHandler.removeCallbacksAndMessages(null)
    }

    private fun updatePixelateEffect() {
        if (!isUpdating) return

        // Create a simple pixelation effect using a checkerboard pattern
        val alphaValue = ((opacity * 255) / 100).toInt()
        val pattern = (System.currentTimeMillis() / 100) % 2 == 0L
        
        // Create two slightly different shades based on pixelSize
        val shade1 = ((pixelSize * 255) / 100).toInt()
        val shade2 = (shade1 * 0.8).toInt()

        val color = if (pattern) {
            android.graphics.Color.argb(alphaValue, shade1, shade1, shade1)
        } else {
            android.graphics.Color.argb(alphaValue, shade2, shade2, shade2)
        }

        overlayView.setCardBackgroundColor(color)
        
        updateHandler.postDelayed({ updatePixelateEffect() }, 50)
    }

    private fun applyBlackEffect() {
        // When opacity is 100%, force full black with max alpha
        val color = if (opacity >= 100) {
            android.graphics.Color.BLACK
        } else {
            val alphaValue = ((opacity * 255) / 100).toInt()
            android.graphics.Color.argb(alphaValue, 0, 0, 0)
        }
        
        overlayView.apply {
            setCardBackgroundColor(color)
            cardElevation = 0f  // Remove any elevation that might affect opacity
            alpha = 1f  // Ensure view alpha is at maximum
        }
    }

    private fun setOpacity(value: Int) {
        opacity = value.coerceIn(0, 100)  // Ensure value stays within bounds
        updateOverlayAppearance()
    }

    private fun setPixelSize(value: Int) {
        pixelSize = value
        if (isPixelateMode) {
            updateOverlayAppearance()
        }
    }

    fun destroy() {
        stopPixelateEffect()
        windowManager.removeView(overlayView)
        controlPanel?.let { windowManager.removeView(it) }
        tutorial.destroy()
    }
} 