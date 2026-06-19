package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfToBitmapHelper {
    fun renderPdfFirstPage(context: Context, pdfUri: Uri): Bitmap? {
        var fileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        var tempFile: File? = null
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(pdfUri) ?: return null
            tempFile = File(context.cacheDir, "temp_render_pdf.pdf")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor)
            
            if (renderer.pageCount > 0) {
                page = renderer.openPage(0)
                // Scale up the image quality so Gemini AI can perform perfect high-resolution OCR
                val scale = 2.0f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                page?.close()
                renderer?.close()
                fileDescriptor?.close()
                tempFile?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
}
