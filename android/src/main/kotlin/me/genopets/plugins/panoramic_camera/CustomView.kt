package me.genopets.plugins.panoramic_camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dermandar.dmd_lib.CallbackInterfaceShooter
import com.dermandar.dmd_lib.DMD_Capture
import io.flutter.plugin.common.MethodChannel
import java.io.File

class CustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val activity: Activity,
    private val channel: MethodChannel
) : FrameLayout(context, attrs, defStyleAttr) {
    private val TAG = "Panoramic Camera Plugin"
    private var mEquiPath = ""
    private val REQUEST_PERMISSIONS = 1234
    private lateinit var mRelativeLayout: RelativeLayout
    private var viewGroup: ViewGroup? = null
    private lateinit var mDMDCapture: DMD_Capture
    private val folderName = "Panoramas"

    private var mIsShootingStarted = false

    private var mPanoramaPath: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var logoSize = 500
    private var outputHeight: Int = 500


    private val mCallbackInterface: CallbackInterfaceShooter = object : CallbackInterfaceShooter {
        override fun onCameraStopped() {
            mIsShootingStarted = false
            stopTimer()
            mainHandler.post { channel.invokeMethod("onCameraStopped", null) }
        }

        override fun onCameraStarted() {
            mainHandler.post { channel.invokeMethod("onCameraStarted", null) }
        }

        override fun onFinishClear() {
            mainHandler.post { channel.invokeMethod("onFinishClear", null) }
        }

        override fun onFinishRelease() {
            // mRelativeLayout.removeView(viewGroup);
            // viewGroup = null;
            // mDMDCapture.startCamera(activity, logoSize, logoSize)
            mainHandler.post { channel.invokeMethod("onFinishRelease", null) }
        }

        override fun onDirectionUpdated(direction: Float) {
            mainHandler.post { channel.invokeMethod("onDirectionUpdated", direction) }
        }

        override fun preparingToShoot() {
            mainHandler.post { channel.invokeMethod("preparingToShoot", null) }
        }

        override fun canceledPreparingToShoot() {
            mainHandler.post { channel.invokeMethod("canceledPreparingToShoot", null) }
        }

        override fun takingPhoto() {
            mainHandler.post { channel.invokeMethod("takingPhoto", null) }
        }
        override fun shotTakenPreviewReady(bitmapPreview: Bitmap?) {
            mainHandler.post { channel.invokeMethod("shotTakenPreviewReady", bitmapPreview?.toString()) }
        }
        
        override fun photoTaken() {
            mainHandler.post { channel.invokeMethod("photoTaken", null) }
        }

        override fun stitchingCompleted(info: HashMap<String?, Any?>?) {
            val time = System.currentTimeMillis()
            val imgName = "img_$time.jpg"
            
            // Use getExternalFilesDir for better compatibility with scoped storage
            val directory = context.getExternalFilesDir(null)
            val folder = File(directory, folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            mEquiPath = File(folder, imgName).absolutePath
            mDMDCapture.genEquiAt(mEquiPath, 800, 0, 0, false, false)
            mainHandler.post { channel.invokeMethod("stitchingCompleted", info.toString()) }
        }

        override fun shootingCompleted(finished: Boolean) {
            if(finished) {
                mDMDCapture.stopCamera();
            }
            mIsShootingStarted = false;
            mainHandler.post { channel.invokeMethod("shootingCompleted", finished) }
        }

        override fun deviceVerticalityChanged(isVertical: Int) {
            mainHandler.post { channel.invokeMethod("deviceVerticalityChanged", isVertical) }
        }

        override fun compassEvent(info: HashMap<String?, Any?>?) {
            mIsShootingStarted = false
            mainHandler.post { channel.invokeMethod("compassEvent", info) }
        }

        override fun onFinishGeneratingEqui() {
            mIsShootingStarted = false
            mDMDCapture.startCamera(activity, logoSize, logoSize)
            mainHandler.post { channel.invokeMethod  ("onFinishGeneratingEqui", mEquiPath) }
        }

        override fun onExposureChanged(mode: DMD_Capture.ExposureMode?) {}

        override fun onRotatorConnected() {}

        override fun onRotatorDisconnected() {}

        override fun onStartedRotating() {}

        override fun onFinishedRotating() {}
    }

    init {
        LayoutInflater.from(activity).inflate(R.layout.activity_shooter, this, true)

        
        mRelativeLayout = RelativeLayout(activity);
        this.addView(
            mRelativeLayout,
            RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        mRelativeLayout = findViewById(R.id.relativeLayout);

        checkAndRequestPermissions()
        initDmdCapture()
    }

     private fun checkAndRequestPermissions() {
         val permissions = arrayOf(
             Manifest.permission.CAMERA,
             Manifest.permission.ACCESS_FINE_LOCATION,
             Manifest.permission.READ_EXTERNAL_STORAGE,
             Manifest.permission.WRITE_EXTERNAL_STORAGE
         )

         val permissionsToRequest = permissions.filter {
             ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
         }

         if (permissionsToRequest.isNotEmpty()) {
             ActivityCompat.requestPermissions(
                 activity,
                 permissionsToRequest.toTypedArray(),
                 REQUEST_PERMISSIONS
             )
         }
     }
     
    fun initDmdCapture() {
        try {
            mDMDCapture = DMD_Capture()
            // Do not remove the following line, the library will necessarily try to find the name of the application if it cannot find it, it will crash. Since this is a package, it does not have an application name, so one is being set.
            activity.applicationInfo.labelRes = R.string.app_name

            val displayRotation = getDisplayRotation(activity)

            viewGroup = mDMDCapture.initShooter(activity, mCallbackInterface, displayRotation, false, false)

        } catch (e: java.lang.Exception){
            Log.e(TAG, e.printStackTrace().toString())
        }
    }

    private fun getDisplayRotation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = context.display
            val rotation = display?.rotation ?: Surface.ROTATION_0
            rotation
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val rotation = display.rotation
            rotation
        }
    }

    fun onResume() {
        // Ensure viewGroup is not already attached to another parent
        val currentParent = viewGroup?.parent as? ViewGroup
        currentParent?.removeView(viewGroup)

        // Add viewGroup to mRelativeLayout
        mRelativeLayout.addView(viewGroup)
        mDMDCapture.startCamera(activity, logoSize, logoSize)
    }

    fun startShooting() {
        startTimer()
        mPanoramaPath = Environment.getExternalStorageDirectory().toString() + "/Lib_Test/"
        mIsShootingStarted = mDMDCapture.startShooting(mPanoramaPath)
    }

    fun finishShooting() {
        mDMDCapture.finishShooting()
        mIsShootingStarted = false
    }

    fun onPause() {
        if (viewGroup?.parent != null) {
            mRelativeLayout.removeView(viewGroup)
        }
        if (mIsShootingStarted) {
            mDMDCapture.stopShooting()
            mIsShootingStarted = false
        }
        mDMDCapture.releaseShooter()
        mDMDCapture.stopCamera()
    }

    fun setOutputHeight(height: Int) {
        outputHeight = height
    }

    fun setShowLogo(showLogo: Boolean) {
        if(!showLogo) logoSize = 10
    }

    // All this logic is necessary to send percentage updates to the channel
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null

    private fun startTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                var map = mDMDCapture.getIndicators()
                mainHandler.post { channel.invokeMethod  ("onUpdateIndicators", map) }

                timerHandler?.postDelayed(this, 100)
            }
        }
        timerHandler?.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
        timerHandler = null
        timerRunnable = null
    }

}