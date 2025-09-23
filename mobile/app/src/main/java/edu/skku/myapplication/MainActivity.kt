package edu.skku.myapplication

import android.content.pm.PackageManager
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import edu.skku.myapplication.databinding.ActivityMainBinding
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 카메라 권한 확인 및 요청
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        // TFLite 모델 로드 및 ObjectDetector 초기화
        Log.d(TAG, "startCamera() 호출됨")
//        setupObjectDetector()

        // 카메라 실행을 위한 Provider 요청
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            Log.d(TAG, "CameraProvider 리스너 실행됨")
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 카메라 미리보기(Preview) 설정
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // 실시간 이미지 분석(ImageAnalysis) 설정
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    // 추론을 수행할 Executor 설정
                    cameraExecutor = Executors.newSingleThreadExecutor()
//                    it.setAnalyzer(cameraExecutor) { imageProxy ->
//                        // 1. Bitmap을 TensorImage로 변환합니다.
//                        val tensorImage = TensorImage.fromBitmap(imageProxy.toBitmap())
//
//                        // 2. 이미지 회전 정보를 ImageProcessingOptions 객체에 담습니다.
//                        val imageProcessingOptions = ImageProcessingOptions.builder()
//                            .setOrientation(getOrientationFromRotation(imageProxy.imageInfo.rotationDegrees))
//                            .build()
//
//                        // 3. 올바른 인자(TensorImage, ImageProcessingOptions)로 detect 함수를 호출합니다.
//                        val results = objectDetector.detect(tensorImage, imageProcessingOptions)
//
//                        // UI 스레드에서 OverlayView 업데이트
//                        runOnUiThread {
//                            binding.overlayView.setResults(results, tensorImage.width, tensorImage.height)
//                        }
//                        imageProxy.close()
//                    }
                }

            // 후면 카메라 선택
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 기존에 바인딩된 카메라가 있다면 해제
                cameraProvider.unbindAll()
                // Preview와 ImageAnalysis를 카메라에 바인딩
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
                Log.d(TAG, "카메라 바인딩 성공!")
            } catch(exc: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupObjectDetector() {
//        val options = ObjectDetector.ObjectDetectorOptions.builder()
//            .setMaxResults(5) // 최대 5개 객체 탐지
//            .setScoreThreshold(0.5f) // 신뢰도 50% 이상만
//            .build()
//        try {
//            // assets 폴더의 tflite 모델 파일로 ObjectDetector 생성
//            objectDetector = ObjectDetector.createFromFileAndOptions(
//                this,
//                "best-int8.tflite", // 모델 파일 이름
//                options
//            )
//        } catch(e: Exception) {
//            Log.e(TAG, "TFLite 모델 초기화 실패.", e)
//        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            0 -> ImageProcessingOptions.Orientation.TOP_LEFT
            90 -> ImageProcessingOptions.Orientation.RIGHT_TOP
            180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            else -> ImageProcessingOptions.Orientation.LEFT_BOTTOM
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "카메라 권한을 허용해야 앱을 사용할 수 있습니다.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "TrashDetector"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}