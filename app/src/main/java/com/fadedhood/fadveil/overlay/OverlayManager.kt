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
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import kotlin.random.Random

class OverlayManager(private val context: Context) {
    private val themedContext = ContextThemeWrapper(
        context, 
        com.google.android.material.R.style.Theme_Material3_DayNight
    )
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val overlayView = MaterialCardView(themedContext)
    private var controlPanel: View? = null
    
    private val overlayParams = WindowManager.LayoutParams(
        context.resources.getDimensionPixelSize(R.dimen.overlay_initial_width),
        context.resources.getDimensionPixelSize(R.dimen.overlay_initial_height),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
    }

    private var isPixelateMode = false
    private var opacity = 80
    private var pixelSize = 50
    private var updateHandler = Handler(Looper.getMainLooper())
    private var isUpdating = false
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var isResizing = false
    private var bitmapLock = Any()

    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val scaleGestureDetector = ScaleGestureDetector(
        themedContext,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                overlayParams.width = (overlayParams.width * scaleFactor).toInt()
                overlayParams.height = (overlayParams.height * scaleFactor).toInt()
                
                overlayParams.width = overlayParams.width.coerceAtLeast(100)
                overlayParams.height = overlayParams.height.coerceAtLeast(100)
                
                try {
                    windowManager.updateViewLayout(overlayView, overlayParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }
    )

    init {
        setupOverlay()
        setupTouchHandling()
    }

    private fun setupOverlay() {
        overlayView.apply {
            radius = context.resources.getDimension(R.dimen.overlay_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(Color.BLACK)
            alpha = opacity / 100f
            strokeWidth = 0
        }
        windowManager.addView(overlayView, overlayParams)
    }

    private fun setupTouchHandling() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                isPixelateMode = !isPixelateMode
                updateOverlayAppearance()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                showControlPanel()
            }
        })

        overlayView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    initialX = overlayParams.x.toFloat()
                    initialY = overlayParams.y.toFloat()
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && !scaleGestureDetector.isInProgress) {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        overlayParams.x = (initialX + deltaX).toInt()
                        overlayParams.y = (initialY + deltaY).toInt()
                        try {
                            windowManager.updateViewLayout(overlayView, overlayParams)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleMode() {
        isPixelateMode = !isPixelateMode
        updateOverlayAppearance()
    }

    private fun showControlPanel() {
        if (controlPanel == null) {
            val inflater = LayoutInflater.from(themedContext)
            val container = inflater.inflate(R.layout.overlay_control_panel, null) as ViewGroup

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // Setup opacity slider with correct range
            container.findViewById<Slider>(R.id.opacitySlider)?.apply {
                valueFrom = 10f  // Minimum 10% opacity
                valueTo = 100f   // Maximum 100% opacity
                value = opacity.toFloat()
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        setOpacity(value.toInt())
                    }
                }
            }

            // Setup pixelate slider
            container.findViewById<Slider>(R.id.pixelateSlider)?.apply {
                visibility = if (isPixelateMode) View.VISIBLE else View.GONE
                value = pixelSize.toFloat()
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        setPixelSize(value.toInt())
                    }
                }
            }

            // Setup mode toggle
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

            container.findViewById<View>(R.id.closeButton)?.setOnClickListener {
                hideControlPanel()
            }

            controlPanel = container
            windowManager.addView(container, params)
        }
    }

    private fun hideControlPanel() {
        controlPanel?.let {
            windowManager.removeView(it)
            controlPanel = null
        }
    }

    private fun updateOverlayAppearance() {
        if (isPixelateMode) {
            updatePixelateEffect()
        } else {
            synchronized(bitmapLock) {
                // Clean up any existing bitmaps
                currentBitmap?.recycle()
                currentBitmap = null
                nextBitmap?.recycle()
                nextBitmap = null
                overlayView.background = null
                overlayView.setCardBackgroundColor(Color.BLACK)
                overlayView.alpha = opacity / 100f
            }
        }
    }

    private fun setOpacity(value: Int) {
        opacity = value.coerceIn(10, 100)  // Ensure minimum 10% opacity
        overlayView.apply {
            setCardBackgroundColor(Color.BLACK)  // Ensure black background is maintained
            alpha = opacity / 100f  // Set opacity
        }
    }

    private fun setPixelSize(value: Int) {
        pixelSize = value
        if (isPixelateMode) {
            updatePixelateEffect()
        }
    }

    private fun updatePixelateEffect() {
        synchronized(bitmapLock) {
            if (isUpdating) return
            isUpdating = true

            updateHandler.removeCallbacksAndMessages(null)
            updateHandler.postDelayed({
                try {
                    // Create a new bitmap for the pixelation effect
                    val width = overlayView.width
                    val height = overlayView.height
                    if (width <= 0 || height <= 0) {
                        isUpdating = false
                        return@postDelayed
                    }

                    // Safely recycle old bitmaps
                    currentBitmap?.recycle()
                    currentBitmap = null
                    nextBitmap?.recycle()
                    nextBitmap = null

                    // Create new bitmap
                    nextBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(nextBitmap!!)
                    
                    // Scale down and up for pixelation effect
                    val scaledWidth = width / pixelSize
                    val scaledHeight = height / pixelSize
                    val scaled = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                    val scaleCanvas = Canvas(scaled)
                    
                    // Draw the scaled content
                    val matrix = Matrix()
                    matrix.setScale(1f / pixelSize, 1f / pixelSize)
                    scaleCanvas.drawBitmap(nextBitmap!!, matrix, null)
                    
                    // Scale back up
                    matrix.reset()
                    matrix.setScale(pixelSize.toFloat(), pixelSize.toFloat())
                    canvas.drawBitmap(scaled, matrix, null)
                    
                    scaled.recycle()
                    
                    // Update the view
                    overlayView.background = BitmapDrawable(context.resources, nextBitmap)
                    currentBitmap = nextBitmap
                    nextBitmap = null
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUpdating = false
                }
            }, 100)
        }
    }

    fun destroy() {
        synchronized(bitmapLock) {
            updateHandler.removeCallbacksAndMessages(null)
            currentBitmap?.recycle()
            currentBitmap = null
            nextBitmap?.recycle()
            nextBitmap = null
            hideControlPanel()
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onOverlaySizeChanged() {
        isResizing = true
        updatePixelateEffect()
        if (isPixelateMode) {
            updateHandler.postDelayed({
                isResizing = false
                updatePixelateEffect()
            }, 250) // Wait a bit before restarting effect
        }
    }
} 