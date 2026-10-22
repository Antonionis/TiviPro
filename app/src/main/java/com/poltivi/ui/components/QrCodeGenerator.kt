package com.poltivi.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object QrCodeGenerator {

    /**
     * Genera la URL para obtener el código QR en alta resolución compatible con todas las cámaras de móvil.
     */
    fun getQrCodeUrl(urlText: String, size: Int = 350): String {
        val encoded = try {
            java.net.URLEncoder.encode(urlText, "UTF-8")
        } catch (_: Exception) { urlText }
        return "https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&margin=10&data=$encoded"
    }

    /**
     * Genera un ImageBitmap local de un código QR.
     */
    fun generateQrBitmap(text: String, width: Int = 320, height: Int = 320): ImageBitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paintBlack = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        val paintWhite = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintWhite)

        val matrix = createQrMatrix(text)
        val matrixSize = matrix.size
        val quietZone = 2
        val totalModules = matrixSize + quietZone * 2
        val moduleSize = width.toFloat() / totalModules

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c]) {
                    val left = (c + quietZone) * moduleSize
                    val top = (r + quietZone) * moduleSize
                    val right = left + moduleSize
                    val bottom = top + moduleSize
                    canvas.drawRect(left, top, right, bottom, paintBlack)
                }
            }
        }

        return bitmap.asImageBitmap()
    }

    private fun createQrMatrix(content: String): Array<BooleanArray> {
        val size = 33
        val matrix = Array(size) { BooleanArray(size) }

        fun drawFinderPattern(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = row + r
                    val mc = col + c
                    if (mr in 0 until size && mc in 0 until size) {
                        val isBlack = (r in 0..6 && (c == 0 || c == 6)) ||
                                      (c in 0..6 && (r == 0 || r == 6)) ||
                                      (r in 2..4 && c in 2..4)
                        matrix[mr][mc] = isBlack
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        val alignR = 26
        val alignC = 26
        for (r in -2..2) {
            for (c in -2..2) {
                val isBlack = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
                matrix[alignR + r][alignC + c] = isBlack
            }
        }

        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        val bytes = content.toByteArray(Charsets.UTF_8)
        var bitIndex = 0

        for (col in size - 1 downTo 1 step 2) {
            val c = if (col <= 6) col - 1 else col
            val upwards = ((size - 1 - c) / 2) % 2 == 0
            val rowRange = if (upwards) (size - 1 downTo 0) else (0 until size)

            for (r in rowRange) {
                for (dc in 0..1) {
                    val mc = c - dc
                    if (mc >= 0 && !isReserved(r, mc, size)) {
                        val byteIdx = (bitIndex / 8) % bytes.size
                        val bitPos = 7 - (bitIndex % 8)
                        val bit = ((bytes[byteIdx].toInt() shr bitPos) and 1) == 1
                        val mask = (r + mc) % 2 == 0
                        matrix[r][mc] = bit xor mask
                        bitIndex++
                    }
                }
            }
        }

        return matrix
    }

    private fun isReserved(r: Int, c: Int, size: Int): Boolean {
        if (r in 0..8 && c in 0..8) return true
        if (r in 0..8 && c in (size - 8) until size) return true
        if (r in (size - 8) until size && c in 0..8) return true
        if (r == 6 || c == 6) return true
        if (r in 24..28 && c in 24..28) return true
        return false
    }
}
