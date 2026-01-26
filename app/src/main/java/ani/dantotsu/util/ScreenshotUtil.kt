package ani.dantotsu.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * Utility to capture screenshot of a view for use with backdrop effects
 */
object ScreenshotUtil {
    
    /**
     * Captures a screenshot of the window using PixelCopy (reliable for hardware accelerated views)
     * @param window The window to capture
     * @param onCaptured Callback with the path to the saved screenshot, or null if failed
     */
    fun captureWindow(window: android.view.Window, context: Context, onCaptured: (String?) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(
                window.decorView.width,
                window.decorView.height,
                Bitmap.Config.ARGB_8888
            )
            
            val locationOfViewInWindow = IntArray(2)
            window.decorView.getLocationInWindow(locationOfViewInWindow)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    android.view.PixelCopy.request(
                        window,
                        android.graphics.Rect(0, 0, window.decorView.width, window.decorView.height),
                        bitmap,
                        { copyResult ->
                            if (copyResult == android.view.PixelCopy.SUCCESS) {
                                saveBitmap(bitmap, context, onCaptured)
                            } else {
                                // Fallback to drawing cache if PixelCopy fails
                                fallbackCapture(window.decorView, context, onCaptured)
                            }
                        },
                        android.os.Handler(android.os.Looper.getMainLooper())
                    )
                } catch (e: Exception) {
                    fallbackCapture(window.decorView, context, onCaptured)
                }
            } else {
                fallbackCapture(window.decorView, context, onCaptured)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onCaptured(null)
        }
    }

    private fun fallbackCapture(view: View, context: Context, onCaptured: (String?) -> Unit) {
        try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            saveBitmap(bitmap, context, onCaptured)
        } catch (e: Exception) {
            e.printStackTrace()
            onCaptured(null)
        }
    }

    private fun saveBitmap(bitmap: Bitmap, context: Context, onCaptured: (String?) -> Unit) {
        try {
            val file = File(context.cacheDir, "backdrop_screenshot.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()
            onCaptured(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onCaptured(null)
        }
    }
    
    /**
     * Legacy method - kept for reference but forwarded to fallback
     */
    fun captureAndSave(view: View, context: Context): String? {
        // Synchronous not supported with PixelCopy, this is just a wrapper now
        return null 
    }
    
    /**
     * Cleans up the temporary screenshot file
     */
    fun cleanup(context: Context) {
        try {
            val file = File(context.cacheDir, "backdrop_screenshot.png")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
