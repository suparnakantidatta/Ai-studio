package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

object QrCodeHelper {

  fun generateUpiQrBitmap(
    upiId: String,
    payeeName: String,
    amount: Double? = null,
    note: String? = null,
    size: Int = 512
  ): Bitmap? {
    if (upiId.isBlank()) return null
    return try {
      val uriBuilder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId.trim())
        .appendQueryParameter("pn", payeeName.ifBlank { "Pixel Pathsala" })
        .appendQueryParameter("cu", "INR")

      if (amount != null && amount > 0.0) {
        uriBuilder.appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
      }
      if (!note.isNullOrBlank()) {
        uriBuilder.appendQueryParameter("tn", note)
      }

      val upiString = uriBuilder.build().toString()

      val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8"
      )

      val bitMatrix = QRCodeWriter().encode(upiString, BarcodeFormat.QR_CODE, size, size, hints)
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      for (x in 0 until size) {
        for (y in 0 until size) {
          bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
      }
      bmp
    } catch (_: Exception) {
      null
    }
  }
}
