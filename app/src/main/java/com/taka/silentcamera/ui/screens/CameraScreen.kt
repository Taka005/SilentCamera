package com.taka.silentcamera.ui.screens
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()){ innerPadding ->
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val previewView = remember { PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }}

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val qualitySelector = QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
            val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
            val videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (exc: Exception) {
                errorMessage = "カメラの起動に失敗しました: ${exc.localizedMessage}"
            }
        }

        Box(modifier = Modifier.padding(innerPadding)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = {
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .size(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f)
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