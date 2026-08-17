package com.beraucoal.kakao.data

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Parser KML ringan yang membaca file dari assets/ secara luring.
 *
 * File KML Sentinel-2 (S2C_MP_ACQ) berisi rencana akuisisi citra satelit.
 * Parser ini mengekstrak poligon orbit dan metadata-nya, lalu memfilter
 * hanya yang melintasi area Berau (sekitar lat 0°–4°, lng 115°–120°).
 *
 * Digunakan sebagai overlay layer di atas Google Maps untuk menunjukkan
 * kapan citra satelit terbaru akan tersedia untuk area kebun kakao.
 */
object KmlParser {

    private const val TAG = "KmlParser"

    // Bounding box area Berau & sekitarnya (dengan margin lebar)
    private const val BERAU_LAT_MIN = -2.0
    private const val BERAU_LAT_MAX = 6.0
    private const val BERAU_LNG_MIN = 114.0
    private const val BERAU_LNG_MAX = 120.0

    /**
     * Data satu style KML (warna garis & isi poligon).
     */
    data class KmlStyle(
        val id: String,
        val lineColor: Long = 0xFFFFFFFFL,
        val lineWidth: Float = 2f,
        val fillColor: Long = 0x40FFFFFFL
    )

    /**
     * Data satu placemark KML yang sudah di-parse.
     */
    data class KmlPlacemark(
        val name: String,
        val styleId: String,
        val polygon: List<LatLng>,
        val timeStart: String? = null,
        val timeEnd: String? = null,
        val mode: String? = null,
        val orbitAbsolute: String? = null,
        val orbitRelative: String? = null,
        val scenes: String? = null,
        val observationDuration: String? = null
    )

    /**
     * Hasil parsing KML secara keseluruhan.
     */
    data class KmlData(
        val documentName: String,
        val styles: Map<String, KmlStyle>,
        val placemarks: List<KmlPlacemark>,
        val berauPlacemarks: List<KmlPlacemark>
    ) {
        val totalPlacemarks: Int get() = placemarks.size
        val berauCount: Int get() = berauPlacemarks.size
    }

    /**
     * Parse file KML dari assets.
     * @param context Android context untuk akses AssetManager
     * @param assetPath path relatif dalam assets/ (misal "kml/sentinel2_schedule.kml")
     * @return KmlData berisi semua placemark dan yang difilter untuk area Berau
     */
    fun parseFromAssets(context: Context, assetPath: String = "kml/sentinel2_schedule.kml"): KmlData {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                parse(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca KML dari assets: $assetPath", e)
            KmlData(
                documentName = "Error",
                styles = emptyMap(),
                placemarks = emptyList(),
                berauPlacemarks = emptyList()
            )
        }
    }

    /**
     * Parse KML dari InputStream menggunakan XmlPullParser.
     */
    fun parse(inputStream: InputStream): KmlData {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "iso-8859-1")

        var documentName = ""
        val styles = mutableMapOf<String, KmlStyle>()
        val placemarks = mutableListOf<KmlPlacemark>()

        // State tracking
        var inStyle = false
        var inPlacemark = false
        var inLineStyle = false
        var inPolyStyle = false
        var inTimeSpan = false
        var inExtendedData = false
        var inOuterBoundary = false
        var inLinearRing = false
        var currentDataName = ""

        // Current style building
        var styleId = ""
        var lineColor = 0xFFFFFFFFL
        var lineWidth = 2f
        var fillColor = 0x40FFFFFFL

        // Current placemark building
        var pmName = ""
        var pmStyleUrl = ""
        var pmCoordinates = ""
        var pmTimeStart = ""
        var pmTimeEnd = ""
        var pmMode = ""
        var pmOrbitAbs = ""
        var pmOrbitRel = ""
        var pmScenes = ""
        var pmDuration = ""

        var eventType = parser.eventType
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""

                    when (currentTag) {
                        "Style" -> {
                            inStyle = true
                            styleId = parser.getAttributeValue(null, "id") ?: ""
                            lineColor = 0xFFFFFFFFL
                            lineWidth = 2f
                            fillColor = 0x40FFFFFFL
                        }
                        "LineStyle" -> if (inStyle) inLineStyle = true
                        "PolyStyle" -> if (inStyle) inPolyStyle = true
                        "Placemark" -> {
                            inPlacemark = true
                            pmName = ""
                            pmStyleUrl = ""
                            pmCoordinates = ""
                            pmTimeStart = ""
                            pmTimeEnd = ""
                            pmMode = ""
                            pmOrbitAbs = ""
                            pmOrbitRel = ""
                            pmScenes = ""
                            pmDuration = ""
                        }
                        "TimeSpan" -> if (inPlacemark) inTimeSpan = true
                        "ExtendedData" -> if (inPlacemark) inExtendedData = true
                        "Data" -> if (inExtendedData) {
                            currentDataName = parser.getAttributeValue(null, "name") ?: ""
                        }
                        "outerBoundaryIs" -> if (inPlacemark) inOuterBoundary = true
                        "LinearRing" -> if (inOuterBoundary) inLinearRing = true
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isEmpty()) {
                        eventType = parser.next()
                        continue
                    }

                    when {
                        // Document name
                        currentTag == "name" && !inPlacemark && !inStyle -> {
                            documentName = text
                        }

                        // Style parsing
                        inStyle && inLineStyle && currentTag == "color" -> {
                            lineColor = parseKmlColor(text)
                        }
                        inStyle && inLineStyle && currentTag == "width" -> {
                            lineWidth = text.toFloatOrNull() ?: 2f
                        }
                        inStyle && inPolyStyle && currentTag == "color" -> {
                            fillColor = parseKmlColor(text)
                        }

                        // Placemark parsing
                        inPlacemark && currentTag == "name" && !inExtendedData -> {
                            pmName = text
                        }
                        inPlacemark && currentTag == "styleUrl" -> {
                            pmStyleUrl = text.removePrefix("#")
                        }
                        inPlacemark && inTimeSpan && currentTag == "begin" -> {
                            pmTimeStart = text
                        }
                        inPlacemark && inTimeSpan && currentTag == "end" -> {
                            pmTimeEnd = text
                        }
                        inPlacemark && inExtendedData && currentTag == "value" -> {
                            when (currentDataName) {
                                "Mode" -> pmMode = text
                                "OrbitAbsolute" -> pmOrbitAbs = text
                                "OrbitRelative" -> pmOrbitRel = text
                                "Scenes" -> pmScenes = text
                                "ObservationDuration" -> pmDuration = text
                            }
                        }
                        inPlacemark && inLinearRing && currentTag == "coordinates" -> {
                            pmCoordinates = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val endTag = parser.name ?: ""
                    when (endTag) {
                        "Style" -> {
                            if (styleId.isNotEmpty()) {
                                styles[styleId] = KmlStyle(
                                    id = styleId,
                                    lineColor = lineColor,
                                    lineWidth = lineWidth,
                                    fillColor = fillColor
                                )
                            }
                            inStyle = false
                        }
                        "LineStyle" -> inLineStyle = false
                        "PolyStyle" -> inPolyStyle = false
                        "Placemark" -> {
                            if (pmCoordinates.isNotEmpty()) {
                                val polygon = parseCoordinates(pmCoordinates)
                                if (polygon.isNotEmpty()) {
                                    placemarks.add(
                                        KmlPlacemark(
                                            name = pmName,
                                            styleId = pmStyleUrl,
                                            polygon = polygon,
                                            timeStart = pmTimeStart.ifEmpty { null },
                                            timeEnd = pmTimeEnd.ifEmpty { null },
                                            mode = pmMode.ifEmpty { null },
                                            orbitAbsolute = pmOrbitAbs.ifEmpty { null },
                                            orbitRelative = pmOrbitRel.ifEmpty { null },
                                            scenes = pmScenes.ifEmpty { null },
                                            observationDuration = pmDuration.ifEmpty { null }
                                        )
                                    )
                                }
                            }
                            inPlacemark = false
                        }
                        "TimeSpan" -> inTimeSpan = false
                        "ExtendedData" -> {
                            inExtendedData = false
                            currentDataName = ""
                        }
                        "outerBoundaryIs" -> inOuterBoundary = false
                        "LinearRing" -> inLinearRing = false
                    }
                    // Reset currentTag setelah end tag
                    if (endTag == currentTag) currentTag = ""
                }
            }
            eventType = parser.next()
        }

        // Filter placemarks yang melintasi area Berau
        val berauPlacemarks = placemarks.filter { pm ->
            pm.polygon.any { point ->
                point.latitude in BERAU_LAT_MIN..BERAU_LAT_MAX &&
                point.longitude in BERAU_LNG_MIN..BERAU_LNG_MAX
            }
        }

        Log.i(TAG, "KML parsed: ${placemarks.size} placemarks total, ${berauPlacemarks.size} near Berau")

        return KmlData(
            documentName = documentName,
            styles = styles,
            placemarks = placemarks,
            berauPlacemarks = berauPlacemarks
        )
    }

    /**
     * Parse string koordinat KML menjadi List<LatLng>.
     * Format KML: "lng,lat,alt lng,lat,alt ..."
     */
    private fun parseCoordinates(coordString: String): List<LatLng> {
        return coordString.trim()
            .split(Regex("\\s+"))
            .mapNotNull { coord ->
                val parts = coord.split(",")
                if (parts.size >= 2) {
                    val lng = parts[0].toDoubleOrNull()
                    val lat = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } else null
            }
    }

    /**
     * Parse warna KML (format AABBGGRR) ke Long ARGB.
     * KML pakai format aneh: alpha-blue-green-red, bukan alpha-red-green-blue.
     */
    private fun parseKmlColor(kmlColor: String): Long {
        return try {
            val hex = kmlColor.removePrefix("#").uppercase()
            if (hex.length == 8) {
                val a = hex.substring(0, 2).toLong(16)
                val b = hex.substring(2, 4).toLong(16)
                val g = hex.substring(4, 6).toLong(16)
                val r = hex.substring(6, 8).toLong(16)
                (a shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                0xFFFFFFFFL
            }
        } catch (e: Exception) {
            0xFFFFFFFFL
        }
    }
}
