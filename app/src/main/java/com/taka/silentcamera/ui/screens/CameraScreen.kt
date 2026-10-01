package com.taka.silentcamera.ui.screens
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.taka.silentcamera.ui.util.saveImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCaptureRequested by remember { mutableStateOf(false) }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }}

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            if (isCaptureRequested) {
                try {
                    saveImage(context, imageProxy)

                    coroutineScope.launch {
                        snackBarHostState.showSnackbar("画像を保存しました")
                    }
                } catch (e: Exception) {
                    errorMessage = "画像の保存に失敗しました: ${e.localizedMessage}"
                } finally {
                    isCaptureRequested = false
                }
            }

            imageProxy.close()
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()

            cameraInstance = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            errorMessage = "カメラの起動に失敗しました: ${exc.localizedMessage}"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val camera = cameraInstance ?: return@detectTransformGestures
                        val zoomState = camera.cameraInfo.zoomState.value ?: return@detectTransformGestures

                        val newZoomRatio = (zoomState.zoomRatio * zoomChange).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

                        camera.cameraControl.setZoomRatio(newZoomRatio)
                    }
                }
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = {
                    isCaptureRequested = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .offset(y = (-40).dp)
                    .size(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.4f)
                ),
                border = BorderStroke(4.dp, Color.White)
            ) {
                Text("撮影", color = Color.White)
            }
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("エラーが発生しました") },
                text = { Text(errorMessage ?: "カメラの初期化に失敗しました") },
                confirmButton = {
                    Button(onClick = {
                        (context as? android.app.Activity)?.finish()
                    }) {
                        Text("アプリを終了")
                    }
                }
            )
        }
    }
}