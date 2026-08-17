package com.beraucoal.kakao.ui.screens

import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.beraucoal.kakao.data.BagianPohon
import com.beraucoal.kakao.ui.theme.KakaoColors
import kotlinx.coroutines.launch
import java.io.File

/**
 * Layar kamera yang memungkinkan pengambilan beberapa foto sekaligus
 * untuk satu bagian pohon. Pengguna bisa terus memotret tanpa
 * harus kembali ke layar laporan setiap kali. Tekan "Selesai"
 * atau tombol back saat sudah cukup.
 */
@Composable
fun ReportCameraScreen(
    bagian: BagianPohon,
    existingPhotoCount: Int = 0,
    maxPhotos: Int = 5,
    onPhotosCaptured: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val capturedPhotos = remember { mutableStateListOf<String>() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val totalPhotos = existingPhotoCount + capturedPhotos.size
    val canTakeMore = totalPhotos < maxPhotos

    // Fungsi untuk menyelesaikan dan mengirim semua foto yang diambil
    val finishAndSend: () -> Unit = {
        onPhotosCaptured(capturedPhotos.toList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (capturedPhotos.isNotEmpty()) {
                        // Kirim foto yang sudah diambil saat back
                        finishAndSend()
                    } else {
                        onCancel()
                    }
                },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Foto ${bagian.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (canTakeMore) "Ambil foto, tekan Selesai jika sudah cukup"
                           else "Batas foto tercapai ($maxPhotos/$maxPhotos)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Counter badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (capturedPhotos.isNotEmpty()) KakaoColors.Primary else Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "$totalPhotos/$maxPhotos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Camera Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            SimpleCameraPreview { capture ->
                imageCapture = capture
            }

            // Flash feedback saat foto berhasil
            // (animasi sederhana bisa ditambahkan nanti)
        }

        // Thumbnail strip — foto yang sudah diambil di sesi ini
        if (capturedPhotos.isNotEmpty()) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capturedPhotos) { photoPath ->
                    val bitmap = remember(photoPath) {
                        BitmapFactory.decodeFile(photoPath)?.asImageBitmap()
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(2.dp, KakaoColors.Primary, RoundedCornerShape(10.dp))
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Foto ${bagian.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Bar — tombol shutter + tombol selesai
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = if (capturedPhotos.isEmpty()) 16.dp else 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer kiri untuk centering
            Spacer(Modifier.width(72.dp))

            // Shutter button
            if (canTakeMore) {
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        isProcessing = true
                        errorMessage = null

                        val reportDir = File(
                            context.getExternalFilesDir(null),
                            "KakaoReports/${bagian.name}"
                        ).also { it.mkdirs() }

                        val photoFile = File(
                            reportDir,
                            "REPORT_${bagian.name}_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    isProcessing = false
                                    capturedPhotos.add(photoFile.absolutePath)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(capturedPhotos.size - 1)
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isProcessing = false
                                    errorMessage = "Gagal memotret: ${exception.message}"
                                }
                            }
                        )
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = KakaoColors.Primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                                .background(Color.LightGray, CircleShape)
                        )
                    }
                }
            } else {
                // Batas tercapai — tampilkan placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Penuh", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Tombol Selesai — hanya muncul jika ada foto yang diambil
            if (capturedPhotos.isNotEmpty()) {
                Button(
                    onClick = finishAndSend,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KakaoColors.Primary),
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selesai",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(56.dp))
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SimpleCameraPreview(
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onImageCaptureReady(imageCapture)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    // Log error
                }
            }, executor)

            previewView
        }
    )
}
