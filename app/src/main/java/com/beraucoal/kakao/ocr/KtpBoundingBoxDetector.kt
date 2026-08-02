package com.beraucoal.kakao.ocr

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * Deteksi objek real-time (bukan khusus KTP -- ML Kit Object Detection tidak
 * punya model khusus kartu ID) yang dipakai sebagai pendekatan "bounding box
 * bergerak" seperti scanner OCR pada umumnya: kotak mengikuti benda paling
 * menonjol di frame, dan kita anggap itu KTP karena diarahkan ke dalam guide frame.
 *
 * STREAM_MODE + enableClassification(false) supaya ringan untuk jalan tiap frame.
 *
 * CATATAN AKURASI: karena bukan model khusus kartu ID, deteksi bisa saja
 * menangkap benda lain (tangan, meja, dll.) kalau KTP belum masuk frame.
 * Kombinasikan dengan guide frame statis supaya pengguna tetap diarahkan.
 */
class KtpBoundingBoxDetector {

    private val detector: ObjectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .build()
    )

    /**
     * Hasil deteksi satu frame.
     * @param boundingBox posisi objek dalam koordinat gambar SETELAH rotasi
     *        (mengikuti orientasi tegak seperti yang dilihat pengguna).
     * @param imageWidth/imageHeight ukuran gambar setelah rotasi -- dipakai
     *        pemanggil untuk memetakan boundingBox ke ukuran layar preview.
     */
    data class DetectionResult(
        val boundingBox: Rect,
        val imageWidth: Int,
        val imageHeight: Int
    )

    fun analyze(imageProxy: ImageProxy, onResult: (DetectionResult?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(null)
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Lebar/tinggi setelah rotasi -- tertukar kalau rotasi 90/270 derajat
        val rotated = rotationDegrees == 90 || rotationDegrees == 270
        val imageWidth = if (rotated) imageProxy.height else imageProxy.width
        val imageHeight = if (rotated) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val largest = detectedObjects.maxByOrNull {
                    it.boundingBox.width() * it.boundingBox.height()
                }
                if (largest != null) {
                    onResult(DetectionResult(largest.boundingBox, imageWidth, imageHeight))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        detector.close()
    }
}
