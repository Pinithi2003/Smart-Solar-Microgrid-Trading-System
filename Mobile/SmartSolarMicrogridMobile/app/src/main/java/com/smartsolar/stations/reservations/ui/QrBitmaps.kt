package com.smartsolar.stations.reservations.ui

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** Draws the booking verification code as a QR image. Scanning it belongs to the operator app. */
object QrBitmaps {
    fun encode(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text.ifBlank { " " }, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.parseColor("#16211C") else Color.WHITE)
            }
        }
        return bitmap
    }
}
