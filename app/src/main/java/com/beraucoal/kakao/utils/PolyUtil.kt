package com.beraucoal.kakao.utils

import com.google.android.gms.maps.model.LatLng

object PolyUtil {
    fun containsLocation(point: LatLng, polygon: List<LatLng>, geodesic: Boolean): Boolean {
        val size = polygon.size
        if (size < 3) {
            return false
        }
        var intersections = 0
        for (i in 0 until size) {
            val vertex1 = polygon[i]
            val vertex2 = polygon[(i + 1) % size]
            if (rayIntersectsSegment(point, vertex1, vertex2)) {
                intersections++
            }
        }
        return (intersections % 2) == 1
    }

    private fun rayIntersectsSegment(point: LatLng, a: LatLng, b: LatLng): Boolean {
        var aY = a.latitude
        var bY = b.latitude
        var aX = a.longitude
        var bX = b.longitude
        var pY = point.latitude
        var pX = point.longitude

        if (aY > bY) {
            val tY = aY; aY = bY; bY = tY
            val tX = aX; aX = bX; bX = tX
        }

        if (pY == aY || pY == bY) {
            pY += 0.00000001
        }

        if (pY > bY || pY < aY || pX >= Math.max(aX, bX)) {
            return false
        }

        if (pX < Math.min(aX, bX)) {
            return true
        }

        val red = if (aX != bX) (bY - aY) / (bX - aX) else Double.MAX_VALUE
        val blue = if (aX != pX) (pY - aY) / (pX - aX) else Double.MAX_VALUE
        return blue >= red
    }

    private const val EARTH_RADIUS = 6371009.0

    fun computeArea(path: List<LatLng>): Double {
        return Math.abs(computeSignedArea(path))
    }

    private fun computeSignedArea(path: List<LatLng>): Double {
        val size = path.size
        if (size < 3) {
            return 0.0
        }
        var total = 0.0
        val prev = path[size - 1]
        var prevTanLat = Math.tan((Math.PI / 2 - Math.toRadians(prev.latitude)) / 2)
        var prevLng = Math.toRadians(prev.longitude)
        for (point in path) {
            val tanLat = Math.tan((Math.PI / 2 - Math.toRadians(point.latitude)) / 2)
            val lng = Math.toRadians(point.longitude)
            total += polarTriangleArea(tanLat, lng, prevTanLat, prevLng)
            prevTanLat = tanLat
            prevLng = lng
        }
        return total * (EARTH_RADIUS * EARTH_RADIUS)
    }

    private fun polarTriangleArea(tan1: Double, lng1: Double, tan2: Double, lng2: Double): Double {
        val deltaLng = lng1 - lng2
        val t = tan1 * tan2
        return 2 * Math.atan2(t * Math.sin(deltaLng), 1 + t * Math.cos(deltaLng))
    }
}
