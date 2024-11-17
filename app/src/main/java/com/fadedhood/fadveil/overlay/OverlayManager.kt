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
    private var pixelSize = 20
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

    private var currentDrawable: BitmapDrawable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDestroyed = false

    private val colors = arrayOf(
        Color.BLACK,
        Color.DKGRAY,
        Color.rgb(20, 20, 20),  // Very dark gray
        Color.rgb(40, 40, 40)   // Dark gray
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

            // Setup opacity slider
            container.findViewById<Slider>(R.id.opacitySlider)?.apply {
                valueFrom = 10f
                valueTo = 100f
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
                valueFrom = 1f    // Smallest pixels (largest number of them)
                valueTo = 50f     // Largest pixels (smallest number of them)
                value = pixelSize.toFloat()
                stepSize = 1f     // Ensure whole number steps
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        pixelSize = value.toInt()
                        if (isPixelateMode) {
                            updatePixelateEffect()
                        }
                    }
                }
            }

            // Setup mode toggle
            container.findViewById<MaterialButtonToggleGroup>(R.id.modeToggleGroup)?.apply {
                check(if (isPixelateMode) R.id.pixelateMode else R.id.blackMode)
                addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (isChecked) {
                        val newPixelateMode = (checkedId == R.id.pixelateMode)
                        if (isPixelateMode != newPixelateMode) {
                            isPixelateMode = newPixelateMode
                            container.findViewById<Slider>(R.id.pixelateSlider)?.visibility =
                                if (isPixelateMode) View.VISIBLE else View.GONE
                            updateOverlayAppearance()
                        }
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
        synchronized(bitmapLock) {
            if (isPixelateMode) {
                overlayView.setCardBackgroundColor(Color.TRANSPARENT)
                updatePixelateEffect()
            } else {
                // Clean up pixelation resources
                mainHandler.removeCallbacksAndMessages(null)
                currentDrawable?.bitmap?.recycle()
                currentDrawable = null
                
                // Reset to solid black
                overlayView.background = null
                overlayView.setCardBackgroundColor(Color.BLACK)
                overlayView.alpha = opacity / 100f
            }
        }
    }

    private fun setOpacity(value: Int) {
        opacity = value.coerceIn(10, 100)
        overlayView.alpha = opacity / 100f  // Always apply opacity to the view directly
    }

    private fun updatePixelateEffect() {
        synchronized(bitmapLock) {
            if (isUpdating || isDestroyed) return
            isUpdating = true

            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.post {
                try {
                    val width = overlayView.width
                    val height = overlayView.height
                    if (width <= 0 || height <= 0) {
                        isUpdating = false
                        return@post
                    }

                    // Create new bitmap
                    val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(newBitmap)
                    val paint = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.FILL
                    }

                    // Fill background with black first
                    canvas.drawColor(Color.BLACK)

                    // Calculate number of pixels (ensure complete coverage)
                    val adjustedPixelSize = 51 - pixelSize.coerceIn(1, 50)  // Invert the scale
                    val numPixelsX = kotlin.math.ceil(width / adjustedPixelSize.toFloat()).toInt()
                    val numPixelsY = kotlin.math.ceil(height / adjustedPixelSize.toFloat()).toInt()
                    val pixelW = width.toFloat() / numPixelsX
                    val pixelH = height.toFloat() / numPixelsY
                    val random = Random(System.currentTimeMillis())

                    // Fill entire canvas with pixels
                    for (y in 0 until numPixelsY) {
                        for (x in 0 until numPixelsX) {
                            paint.color = colors[random.nextInt(colors.size)]
                            canvas.drawRect(
                                x * pixelW,
                                y * pixelH,
                                (x + 1) * pixelW,
                                (y + 1) * pixelH,
                                paint
                            )
                        }
                    }

                    // Create new drawable and clean up old one
                    val oldDrawable = currentDrawable
                    currentDrawable = BitmapDrawable(context.resources, newBitmap)

                    if (!isDestroyed) {
                        overlayView.background = currentDrawable
                        overlayView.alpha = opacity / 100f  // Apply opacity to the whole view
                        
                        // Clean up old drawable after setting new one
                        oldDrawable?.bitmap?.recycle()
                    } else {
                        newBitmap.recycle()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUpdating = false
                    if (isPixelateMode && !isDestroyed) {
                        mainHandler.postDelayed({ updatePixelateEffect() }, 100)
                    }
                }
            }
        }
    }

    fun destroy() {
        synchronized(bitmapLock) {
            isDestroyed = true
            mainHandler.removeCallbacksAndMessages(null)
            currentDrawable?.bitmap?.recycle()
            currentDrawable = null
        }
        hideControlPanel()
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            e.printStackTrace()
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