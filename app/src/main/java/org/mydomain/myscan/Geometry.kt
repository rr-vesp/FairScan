package org.mydomain.myscan

import kotlin.math.atan2

data class Point(val x: Int, val y: Int)

data class Line(val from: Point, val to: Point)

data class Quad(
    val topLeft: Point,
    val topRight: Point,
    val bottomRight: Point,
    val bottomLeft: Point
) {
    fun edges(): List<Line> {
        return listOf(
            Line(topLeft, topRight),
            Line(topRight, bottomRight),
            Line(bottomRight, bottomLeft),
            Line(bottomLeft, topLeft))
    }
}

fun createQuad(vertices: List<Point>?): Quad? {
    if (vertices == null || vertices.size != 4) return null

    // Centroid of the points
    val cx = vertices.map { it.x }.average()
    val cy = vertices.map { it.y }.average()

    // Sort by angle from centroid (clockwise)
    val sorted = vertices.sortedWith(compareBy<Point> {
        atan2((it.y - cy).toDouble(), (it.x - cx).toDouble())
    })

    return Quad(sorted[0], sorted[1], sorted[2], sorted[3])
}

fun Quad.scaledTo(fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): Quad {
    val scaleX = toWidth.toFloat() / fromWidth
    val scaleY = toHeight.toFloat() / fromHeight
    return Quad(
        topLeft = topLeft.scaled(scaleX, scaleY),
        topRight = topRight.scaled(scaleX, scaleY),
        bottomRight = bottomRight.scaled(scaleX, scaleY),
        bottomLeft = bottomLeft.scaled(scaleX, scaleY)
    )
}

fun Point.scaled(scaleX: Float, scaleY: Float): Point {
    return Point((x * scaleX).toInt(), (y * scaleY).toInt())
}
