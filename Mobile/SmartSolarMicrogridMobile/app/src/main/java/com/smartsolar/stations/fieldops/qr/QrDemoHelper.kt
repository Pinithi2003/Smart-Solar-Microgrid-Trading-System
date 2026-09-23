package com.smartsolar.stations.fieldops.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QR generation helper (demo). Never encodes passwords/PII — only TXN ids.
 * Scanning + verify/finalize belong to Member 4 (POST /api/qr/verify, /api/qr/finalize).
 */
object QrDemoHelper {
    fun makeBitmap(content: String, size: Int = 512): Bitmap {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bmp.setPixel(x, y, if (bits.get(x, y)) Color.BLACK else Color.WHITE)
        return bmp
    }
}
