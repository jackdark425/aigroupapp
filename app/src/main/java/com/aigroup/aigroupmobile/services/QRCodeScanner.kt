package com.aigroup.aigroupmobile.services

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

@Composable
fun rememberQRCodeScanner(): QRCodeScanner {
  val context = LocalContext.current
  return remember {
    QRCodeScanner(context)
  }
}

class QRCodeScanner @Inject constructor(private val context: Context) {
  private val options = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(
      Barcode.FORMAT_QR_CODE,
      Barcode.FORMAT_AZTEC
    )
    .build()

  suspend fun scanQRCode(imageUri: Uri): String? {
    return suspendCancellableCoroutine {
      val image: InputImage
      try {
        image = InputImage.fromFilePath(context, imageUri)

        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
          .addOnSuccessListener { barcodes ->
            if (barcodes.isNotEmpty()) {
              it.resume(barcodes.first().rawValue)
            } else {
              it.resume(null)
            }
          }
          .addOnFailureListener { e ->
            e.printStackTrace()
            it.cancel(e)
          }
      } catch (e: IOException) {
        e.printStackTrace()
        it.cancel(e)
      }
    }
  }
}