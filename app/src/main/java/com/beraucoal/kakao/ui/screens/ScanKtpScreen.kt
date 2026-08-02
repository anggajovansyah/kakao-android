package com.beraucoal.kakao.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect as AndroidRect
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import com.beraucoal.kakao.data.OcrConfidence
import com.beraucoal.kakao.ocr.KtpBoundingBoxDetector
import com.beraucoal.kakao.ocr.KtpOcrAnalyzer
import com.beraucoal.kakao.ocr.WilayahCorrector
import com.beraucoal.kakao.ui.components.KakaoPrimaryButton
import com.beraucoal.kakao.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val KTP_ASPECT_RATIO = 85.6f / 53.98f
private const val ALIGNMENT_IOU_THRESHOLD = 0.55f
private const val CROP_WIDTH_FRACTION = 0.85f
private const val CROP_MARGIN_MULTIPLIER = 1.15f
private const val MIN_OCR_WIDTH_PX = 1200

/**
 * Tahap 1: Scan foto KTP dengan komposisi whitespace rapi & preview sudut membulat.
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
    val boxDetector = remember { KtpBoundingBoxDetector() }
    val wilayahCorrector = remember { WilayahCorrector(context.applicationContext) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { wilayahCorrector.ensureLoaded() }
        } catch (e: Exception) {
            // Preload gagal secara senyap
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KakaoColors.Background)
    ) {
        // ── Title & Description (Dengan Whitespace Nyaman) ──
        Column(
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = 24.dp
            )
        ) {
            Text(
                text = "Scan Foto KTP",
                style = MaterialTheme.typography.headlineMedium,
                color = KakaoColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Posisikan KTP fisik di dalam bingkai sudut, pastikan pencahayaan terang dan seluruh teks terbaca jelas.",
                style = MaterialTheme.typography.bodySmall,
                color = KakaoColors.TextSecondary,
                lineHeight = 20.sp
            )
        }

        // ── Camera Preview Container (Dengan Padding Kiri-Kanan Rapi & Rounded Corner) ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            CameraPreviewWithFrame(
                boxDetector = boxDetector,
                onImageCaptureReady = { imageCapture = it }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Bottom Action Area ──
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = KakaoColors.Primary,
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Mendeteksi & mengekstraksi data OCR KTP...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KakaoColors.TextPrimary
                )
            } else {
                KakaoPrimaryButton(
                    text = "PINDAI KTP SEKARANG",
                    onClick = {
                        val capture = imageCapture ?: return@KakaoPrimaryButton
                        errorMessage = null
                        isProcessing = true
                        capture.takePicture(
                            Executors.newSingleThreadExecutor(),
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
                                                errorMessage = "Hasil OCR belum yakin — silakan foto ulang " +
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
                                    isProcessing = false
                                    errorMessage = "Gagal mengambil foto: ${exception.message}"
                                }
                            }
                        )
                    }
                )
            }

            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KakaoColors.ErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = KakaoColors.Error)
                        Spacer(Modifier.width(10.dp))
                        Text(it, color = KakaoColors.Error, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ocrAnalyzer.close()
            boxDetector.close()
        }
    }
}

@Composable
private fun CameraPreviewWithFrame(
    boxDetector: KtpBoundingBoxDetector,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var detectedBox by remember { mutableStateOf<AndroidRect?>(null) }
    var detectedImageSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

                    val analysisExecutor = Executors.newSingleThreadExecutor()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                boxDetector.analyze(imageProxy) { result ->
                                    if (result != null) {
                                        detectedBox = result.boundingBox
                                        detectedImageSize = Pair(result.imageWidth, result.imageHeight)
                                    } else {
                                        detectedBox = null
                                    }
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                        onImageCaptureReady(imageCapture)
                    } catch (e: Exception) {
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        val frameWidth = canvasSize.width * 0.85f
        val frameHeight = frameWidth / KTP_ASPECT_RATIO
        val frameLeft = (canvasSize.width - frameWidth) / 2f
        val frameTop = (canvasSize.height - frameHeight) / 2f
        val guideRect = androidx.compose.ui.geometry.Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)

        val imgSize = detectedImageSize
        val box = detectedBox
        val mappedBoxRect: androidx.compose.ui.geometry.Rect? = if (
            imgSize != null && box != null && imgSize.first > 0 && imgSize.second > 0 && canvasSize.width > 0
        ) {
            val scale = minOf(canvasSize.width.toFloat() / imgSize.first, canvasSize.height.toFloat() / imgSize.second)
            val offsetX = (canvasSize.width - imgSize.first * scale) / 2f
            val offsetY = (canvasSize.height - imgSize.second * scale) / 2f
            androidx.compose.ui.geometry.Rect(
                left = offsetX + box.left * scale,
                top = offsetY + box.top * scale,
                right = offsetX + box.right * scale,
                bottom = offsetY + box.bottom * scale
            )
        } else null

        val isAligned = if (mappedBoxRect != null && frameWidth > 0) {
            val intersectLeft = maxOf(guideRect.left, mappedBoxRect.left)
            val intersectTop = maxOf(guideRect.top, mappedBoxRect.top)
            val intersectRight = minOf(guideRect.right, mappedBoxRect.right)
            val intersectBottom = minOf(guideRect.bottom, mappedBoxRect.bottom)
            val intersectArea = maxOf(0f, intersectRight - intersectLeft) * maxOf(0f, intersectBottom - intersectTop)
            val unionArea = guideRect.width * guideRect.height +
                mappedBoxRect.width * mappedBoxRect.height - intersectArea
            unionArea > 0f && (intersectArea / unionArea) >= ALIGNMENT_IOU_THRESHOLD
        } else false

        val frameColor = if (isAligned) KakaoColors.Primary else Color.White

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            if (frameWidth <= 0f) return@Canvas

            val scrimColor = Color.Black.copy(alpha = 0.60f)
            drawRect(color = scrimColor, topLeft = Offset(0f, 0f), size = Size(size.width, frameTop))
            drawRect(color = scrimColor, topLeft = Offset(0f, frameTop + frameHeight), size = Size(size.width, size.height - frameTop - frameHeight))
            drawRect(color = scrimColor, topLeft = Offset(0f, frameTop), size = Size(frameLeft, frameHeight))
            drawRect(color = scrimColor, topLeft = Offset(frameLeft + frameWidth, frameTop), size = Size(size.width - frameLeft - frameWidth, frameHeight))

            val cornerLen = frameWidth * 0.12f
            val strokeW = 6f
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(frameColor, Offset(x, y), Offset(x + dx, y), strokeWidth = strokeW)
                drawLine(frameColor, Offset(x, y), Offset(x, y + dy), strokeWidth = strokeW)
            }
            corner(frameLeft, frameTop, cornerLen, cornerLen)
            corner(frameLeft + frameWidth, frameTop, -cornerLen, cornerLen)
            corner(frameLeft, frameTop + frameHeight, cornerLen, -cornerLen)
            corner(frameLeft + frameWidth, frameTop + frameHeight, -cornerLen, -cornerLen)

            mappedBoxRect?.let { r ->
                drawRoundRect(
                    color = frameColor,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 4f)
                )
            }
        }

        val statusText = if (isAligned) "KTP terdeteksi, siap dipindai" else "Posisikan KTP di sini"
        val statusColor = if (isAligned) KakaoColors.PrimaryLight else Color.White
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 110.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PoppinsFontFamily,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}

private fun ImageProxy.toBitmapFixedRotation(): Bitmap {
    val bitmap = this.toBitmap()
    val rotationDegrees = this.imageInfo.rotationDegrees
    if (rotationDegrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

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
