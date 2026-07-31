package com.beraucoal.kakao.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import com.beraucoal.kakao.R
import com.beraucoal.kakao.data.OcrConfidence
import com.beraucoal.kakao.ocr.KtpOcrAnalyzer
import com.beraucoal.kakao.ocr.WilayahCorrector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Rasio KTP Indonesia (kartu ID-1 / CR80): 85.6mm x 53.98mm
private const val KTP_ASPECT_RATIO = 85.6f / 53.98f

// Fraksi lebar guide frame terhadap lebar foto (sama seperti overlay visual),
// ditambah sedikit margin toleransi supaya tidak kepotong kalau posisinya
// tidak 100% pas.
private const val CROP_WIDTH_FRACTION = 0.85f
private const val CROP_MARGIN_MULTIPLIER = 1.15f

// Lebar minimum hasil crop sebelum dikirim ke OCR -- di bawah ini teks kecil
// di KTP (terutama NIK dan alamat) jadi kurang tajam untuk ML Kit baca.
private const val MIN_OCR_WIDTH_PX = 1200

/**
 * Tahap 1: Scan foto KTP.
 *
 * Cuma SATU kotak panduan statis (gaya sudut siku/corner bracket, rasio KTP,
 * posisi tetap di tengah layar). Sebelumnya ada kotak KEDUA yang mengikuti
 * deteksi objek real-time dari ML Kit Object Detection, tapi itu model
 * generik (bukan khusus kartu ID) jadi hasilnya kadang akurat kadang nyasar
 * ke benda lain di frame -- membingungkan dan bikin dua kotak yang saling
 * tidak sinkron. Dihapus supaya lebih fokus dan konsisten: pengguna cukup
 * pas-kan KTP ke satu kotak yang posisinya selalu pasti (tidak bergantung
 * hasil deteksi AI yang tidak selalu bisa diandalkan).
 */
@Composable
fun ScanKtpScreen(
    onKtpProcessed: (com.beraucoal.kakao.data.KtpData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val ocrAnalyzer = remember { KtpOcrAnalyzer() }
    val wilayahCorrector = remember { WilayahCorrector(context.applicationContext) }
    // Satu executor dipakai ulang untuk semua capture -- dibuat sekali, ditutup
    // saat layar ini keluar dari komposisi (lihat DisposableEffect di bawah).
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    // Muat dataset wilayah (~500KB gzip) di background begitu layar dibuka --
    // biasanya sudah selesai sebelum pengguna selesai posisikan KTP & tekan PINDAI.
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { wilayahCorrector.ensureLoaded() }
        } catch (e: Exception) {
            // Preload gagal -- tidak apa, correct() nanti akan skip koreksi
            // dengan aman kalau data belum ter-load.
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 16.dp, 20.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.itsb_logo),
                contentDescription = "Logo ITSB",
                modifier = Modifier.height(22.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.berau_logo),
                contentDescription = "Logo Berau",
                modifier = Modifier.height(22.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Scan foto KTP",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 8.dp)
        )
        Text(
            "Posisikan KTP di dalam kotak, pastikan tidak silau dan seluruh sisi kartu terlihat.",
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CameraPreviewWithFrame(
                onImageCaptureReady = { imageCapture = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Membaca data KTP...")
            } else {
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        errorMessage = null
                        isProcessing = true
                        capture.takePicture(
                            captureExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmapFixedRotation()
                                    image.close()
                                    val ocrInputBitmap = cropToGuideFrame(bitmap)
                                    scope.launch {
                                        try {
                                            val ktpDataRaw = ocrAnalyzer.recognize(ocrInputBitmap)
                                            val correction = try {
                                                withContext(Dispatchers.IO) {
                                                    wilayahCorrector.ensureLoaded()
                                                    wilayahCorrector.correct(
                                                        rawOcrText = ktpDataRaw.rawOcrText,
                                                        kecamatanRaw = ktpDataRaw.kecamatan,
                                                        kelurahanRaw = ktpDataRaw.kelurahanDesa
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                // Koreksi wilayah gagal (mis. asset bermasalah) --
                                                // tetap lanjut pakai hasil OCR asli, jangan sampai
                                                // menggagalkan seluruh proses scan.
                                                WilayahCorrector.CorrectionResult(
                                                    ktpDataRaw.kecamatan, ktpDataRaw.kelurahanDesa, false, false
                                                )
                                            }
                                            val ktpData = ktpDataRaw.copy(
                                                kecamatan = correction.kecamatan,
                                                kelurahanDesa = correction.kelurahanDesa
                                            )
                                            isProcessing = false
                                            if (ktpData.ocrConfidence == OcrConfidence.LOW) {
                                                errorMessage = "Hasil OCR belum yakin -- silakan foto ulang " +
                                                    "dengan pencahayaan lebih baik, atau lanjut untuk koreksi manual."
                                            }
                                            onKtpProcessed(ktpData)
                                        } catch (e: Exception) {
                                            isProcessing = false
                                            errorMessage = "Gagal membaca KTP: ${e.message}"
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // onError dipanggil di thread captureExecutor (background), sama
                                    // seperti onCaptureSuccess -- state Compose harus diubah di main
                                    // thread, jadi dibungkus scope.launch juga.
                                    scope.launch {
                                        isProcessing = false
                                        errorMessage = "Gagal mengambil foto: ${exception.message}"
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PINDAI")
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ocrAnalyzer.close()
            captureExecutor.shutdown()
        }
    }
}

@Composable
private fun CameraPreviewWithFrame(
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        onImageCaptureReady(imageCapture)
                    } catch (e: Exception) {
                        // Kamera gagal di-bind (mis. permission belum diberikan) --
                        // tombol capture tetap disabled karena imageCapture null.
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Guide frame statis -- SATU-SATUNYA kotak yang ditampilkan, posisinya
        // tetap (tidak bergantung deteksi AI apa pun) sehingga selalu akurat
        // sebagai acuan visual, meskipun tidak "pintar" mengikuti posisi KTP.
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        val frameWidth = canvasSize.width * 0.85f
        val frameHeight = frameWidth / KTP_ASPECT_RATIO
        val frameLeft = (canvasSize.width - frameWidth) / 2f
        val frameTop = (canvasSize.height - frameHeight) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            if (frameWidth <= 0f) return@Canvas

            // Area di luar guide frame digelapkan
            val scrimColor = Color.Black.copy(alpha = 0.55f)
            drawRect(color = scrimColor, topLeft = Offset(0f, 0f), size = Size(size.width, frameTop))
            drawRect(color = scrimColor, topLeft = Offset(0f, frameTop + frameHeight), size = Size(size.width, size.height - frameTop - frameHeight))
            drawRect(color = scrimColor, topLeft = Offset(0f, frameTop), size = Size(frameLeft, frameHeight))
            drawRect(color = scrimColor, topLeft = Offset(frameLeft + frameWidth, frameTop), size = Size(size.width - frameLeft - frameWidth, frameHeight))

            // Guide frame bergaya sudut siku (corner bracket)
            val cornerLen = frameWidth * 0.12f
            val strokeW = 5f
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(Color.White, Offset(x, y), Offset(x + dx, y), strokeWidth = strokeW)
                drawLine(Color.White, Offset(x, y), Offset(x, y + dy), strokeWidth = strokeW)
            }
            corner(frameLeft, frameTop, cornerLen, cornerLen)                                     // kiri-atas
            corner(frameLeft + frameWidth, frameTop, -cornerLen, cornerLen)                       // kanan-atas
            corner(frameLeft, frameTop + frameHeight, cornerLen, -cornerLen)                      // kiri-bawah
            corner(frameLeft + frameWidth, frameTop + frameHeight, -cornerLen, -cornerLen)         // kanan-bawah
        }

        Text(
            "Scan KTP di sini",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 90.dp)
        )
    }
}

/** Konversi ImageProxy ke Bitmap sambil menyesuaikan rotasi sensor kamera. */
private fun ImageProxy.toBitmapFixedRotation(): Bitmap {
    val bitmap = this.toBitmap()
    val rotationDegrees = this.imageInfo.rotationDegrees
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Crop foto hasil capture ke area yang kira-kira sama dengan guide frame yang
 * ditampilkan di layar (posisi tengah, rasio KTP, plus sedikit margin toleransi),
 * lalu upscale kalau hasilnya terlalu kecil. Mengasumsikan bitmap sudah dalam
 * orientasi tegak (portrait) seperti yang dilihat pengguna di preview -- sama
 * seperti asumsi guide frame overlay.
 *
 * Tujuannya: kurangi noise dari background di sekitar KTP dan naikkan resolusi
 * efektif teks kartu supaya ML Kit lebih akurat baca karakter kecil (NIK, alamat).
 */
private fun cropToGuideFrame(bitmap: Bitmap): Bitmap {
    val cropWidth = (bitmap.width * CROP_WIDTH_FRACTION * CROP_MARGIN_MULTIPLIER)
        .toInt().coerceIn(1, bitmap.width)
    val cropHeight = (cropWidth / KTP_ASPECT_RATIO).toInt().coerceIn(1, bitmap.height)
    val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
    val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
    val safeWidth = minOf(cropWidth, bitmap.width - left)
    val safeHeight = minOf(cropHeight, bitmap.height - top)

    val cropped = Bitmap.createBitmap(bitmap, left, top, safeWidth, safeHeight)

    return if (cropped.width < MIN_OCR_WIDTH_PX) {
        val scale = MIN_OCR_WIDTH_PX.toFloat() / cropped.width
        Bitmap.createScaledBitmap(cropped, MIN_OCR_WIDTH_PX, (cropped.height * scale).toInt(), true)
    } else {
        cropped
    }
}
