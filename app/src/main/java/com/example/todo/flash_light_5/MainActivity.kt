package com.example.todo.flash_light_5

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todo.flash_light_5.databinding.ActivityMainBinding
import yuku.ambilwarna.AmbilWarnaDialog
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private var screenFlash by Delegates.notNull<Boolean>()
    private var defaultColor = -1
    private lateinit var cameraId: String
    private lateinit var cameraManager: CameraManager
    private var click by Delegates.notNull<Boolean>()
    private var sosClick by Delegates.notNull<Boolean>()
    private val handler = Handler(Looper.getMainLooper())
    var seekHandler = Handler()
    private var currentStep = 0
    private var sosFlashing by Delegates.notNull<Boolean>()
    private var seekFlashing by Delegates.notNull<Boolean>()
    private var seekFlash by Delegates.notNull<Boolean>()
    private var intervalSeconds = arrayOf(0, 5, 10, 15, 20)
    private var sosPattern = arrayOf(
        longDelay, shortDelay, longDelay,
        shortDelay, longDelay, shortDelay,
        longDelay, longDelay, longDelay
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screenFlash = true
        click = false
        sosClick = false
        sosFlashing = false
        seekFlash = false
        seekFlashing = false
        checkSupportFlash()
        checkCameraManagerId()

        binding.circleSeek.setOnClickListener {
            checkSeekFlashing(seekFlash)
        }

        binding.torch.setOnClickListener {
            checkTorchFlashing(click)
        }

        binding.mobile.setOnClickListener {
            screenFlash = false
            checkMobileFlashing()
        }

        binding.main.setOnTouchListener { v, event ->
            if (!screenFlash) {
                screenFlash = true
                if (event.action == MotionEvent.ACTION_DOWN) {
                    closeFlashScreen()
                }
            }
            true
        }

        binding.changeColorText.setOnClickListener {
            showColorDialog()
        }

        binding.sos.setOnClickListener {
            checkSosFlashing(sosClick)
        }

    }

    private fun checkSosFlashing(sosClick: Boolean) {
        if (!sosClick) {
            this.sosClick = true
            binding.sosIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.yellow_sos))
            }
            startSos()
        } else {
            this.sosClick = false
            binding.sosIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.gray_sos))
            }
            stopSos()
        }
    }

    private fun stopSos() {
        try {
            stopCameraLight()
            this.sosFlashing = false
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSos() {
        try {
            startCameraLight()
            this.sosFlashing = true
            currentStep = 0
            sosFlash()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sosFlash() {
        if (currentStep < sosPattern.size) {
            val delay = sosPattern[currentStep]
            handler.postDelayed({
                toggleSosFlash()
                sosFlash()
            }, delay)
            currentStep++
        } else {
            currentStep = 0
            sosFlash()
        }
    }

    private fun toggleSosFlash() {
        if (sosFlashing) {
            stopSos()
        } else {
            startSos()
        }
    }

    private fun showColorDialog() {
        val dialog =
            AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {

                }

                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    defaultColor = color
                    binding.mobileScreen.apply {
                        Log.d("Color", color.toString())
                        setBackgroundColor(defaultColor)
                    }
                }
            })
        dialog.show()
    }

    private fun closeFlashScreen() {
        binding.mobileIcon.apply {
            setImageDrawable(resources.getDrawable(R.drawable.mobile_gray))
        }
        val flashAnimation = AlphaAnimation(1.0f, 0.0f)
        flashAnimation.duration = 300
        binding.mobileScreen.startAnimation(flashAnimation)
        binding.mobileScreen.visibility = View.INVISIBLE
    }

    private fun checkMobileFlashing() {
        binding.mobileIcon.apply {
            setImageDrawable(resources.getDrawable(R.drawable.mobile_yellow))
        }
        val flashAnimation = AlphaAnimation(0.0f, 1.0f)
        flashAnimation.duration = 300
        binding.mobileScreen.startAnimation(flashAnimation)
        binding.mobileScreen.visibility = View.VISIBLE
    }


    private fun checkTorchFlashing(click: Boolean) {
        if (!click) {
            binding.torchIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.flash_yellow))
            }
            this.click = true
            startCameraLight()
        } else {
            binding.torchIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.flash_gray))
            }
            this.click = false
            stopCameraLight()
        }
    }

    private fun checkSeekFlashing(seekFlash: Boolean) {
        if (!seekFlash) {
            binding.circleSeekIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.circle_seek_blue))
            }
            this.seekFlash = true
            binding.seekBar.visibility = View.VISIBLE
            startCameraLight()
            checkSeekBarFlash()
        } else {
            binding.circleSeekIcon.apply {
                setImageDrawable(resources.getDrawable(R.drawable.circle_seek_black))
            }
            stopCameraLight()
            this.seekFlash = false
            handler.removeCallbacksAndMessages(null)
            binding.seekBar.visibility = View.GONE
        }
    }

    private fun checkSeekBarFlash() {
        binding.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    startFlashing()
                }

            }
        )
    }

    private fun startFlashing() {
        val index = binding.seekBar.progress
        Log.d("Index 1", index.toString())
        val interval = intervalSeconds[index].toLong() * 20
        Log.d("Index 1 :", interval.toString())
        handler.removeCallbacksAndMessages(null)
        if (index != 0) {
            handler.post(object : Runnable {
                override fun run() {
                    toggleFlash()
                    handler.postDelayed(
                        this,
                        interval
                    )
                }
            })
        } else {
            startCameraLight()
        }
    }

    private fun toggleFlash() {
        seekFlashing = !seekFlashing
        if (seekFlashing) {
            startCameraLight()
        } else {
            stopCameraLight()
        }
    }

    private fun checkCameraManagerId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraId = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkSupportFlash() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Toast.makeText(this, "Your device does not support flash light", Toast.LENGTH_SHORT)
                .show()
            return
        }
    }

    private fun stopCameraLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCameraLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val shortDelay: Long = 50 // Milliseconds
        private const val longDelay: Long = 300 // Milliseconds
    }
}