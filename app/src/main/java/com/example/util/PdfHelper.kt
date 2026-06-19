package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ReportDetail
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfHelper {
    fun generateAndSharePdf(context: Context, report: ReportDetail, isBengali: Boolean) {
        generateAndSharePdf(context, report, if (isBengali) "bn" else "en")
    }

    fun generateAndSharePdf(context: Context, report: ReportDetail, languageCode: String) {
        val pdfDocument = PdfDocument()
        
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 40f
        val contentWidth = (pageWidth - (2 * marginX)).toInt()
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val paint = Paint()
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        
        var currentY = 50f
        
        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }
        
        fun drawStaticLayoutText(text: String, x: Float, tp: TextPaint, isBold: Boolean = false, textSize: Float = 10f, colorHex: String = "#334155"): Int {
            tp.textSize = textSize
            tp.isFakeBoldText = isBold
            tp.color = Color.parseColor(colorHex)
            
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(text, 0, text.length, tp, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(text, tp, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0.0f, false)
            }
            
            val textHeight = staticLayout.height
            if (currentY + textHeight > pageHeight - 65f) {
                startNewPage()
            }
            
            canvas.save()
            canvas.translate(x, currentY)
            staticLayout.draw(canvas)
            canvas.restore()
            
            currentY += textHeight
            return textHeight
        }
        
        // Let's draw Header Layout
        paint.color = Color.parseColor("#005AC1") // MedicalPrimary
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 74f, paint)
        
        // Draw Header text
        val titleTextPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 21f
            isFakeBoldText = true
        }
        val appTitle = when (languageCode) {
            "bn" -> "মেডিনোভা"
            "hi" -> "मेडिनोवा"
            else -> "MediNova"
        }
        canvas.drawText(appTitle, marginX + 18f, currentY + 32f, titleTextPaint)
        
        titleTextPaint.textSize = 9.5f
        titleTextPaint.isFakeBoldText = false
        titleTextPaint.color = Color.parseColor("#93C5FD")
        val appSubtitle = when (languageCode) {
            "bn" -> "ডিজিটাল স্বাস্থ্য সহযোগী • AI HEALTH COMPANION"
            "hi" -> "डिजिटल स्वास्थ्य सहायक • AI स्वास्थ्य साथी"
            "es" -> "ASISTENTE DE SALUD DIGITAL • RESUMEN DE EDUCACIÓN AL PACIENTE"
            else -> "DIGITAL HEALTH CONTEXT • PATIENT EDUCATION SUMMARY"
        }
        canvas.drawText(appSubtitle, marginX + 18f, currentY + 52f, titleTextPaint)
        
        currentY += 92f
        
        // Subtitle Report Type
        val subtitlePaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F2937")
            textSize = 14f
            isFakeBoldText = true
        }
        val typeHeader = when (languageCode) {
            "bn" -> "রিপোর্ট বিশ্লেষণ: ${report.reportType}"
            "hi" -> "रिपोर्ट विश्लेषण: ${report.reportType}"
            "es" -> "Análisis del Informe: ${report.reportType}"
            else -> "Report Analysis: ${report.reportType}"
        }
        canvas.drawText(typeHeader, marginX, currentY, subtitlePaint)
        currentY += 18f
        
        // Date / Time of analysis
        subtitlePaint.color = Color.parseColor("#6B7280")
        subtitlePaint.textSize = 9f
        subtitlePaint.isFakeBoldText = false
        val formattedDate = SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale.getDefault()).format(Date())
        val dateLabel = when (languageCode) {
            "bn" -> "বিশ্লেষণের সময়: $formattedDate"
            "hi" -> "विश्लेषण का समय: $formattedDate"
            "es" -> "Fecha de Análisis: $formattedDate"
            else -> "Analysis Date: $formattedDate"
        }
        canvas.drawText(dateLabel, marginX, currentY, subtitlePaint)
        currentY += 24f
        
        // Thin horizontal separator rule
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, paint)
        currentY += 20f
        
        // Summary Section Header
        subtitlePaint.color = Color.parseColor("#005AC1")
        subtitlePaint.textSize = 12f
        subtitlePaint.isFakeBoldText = true
        val summaryHeader = when (languageCode) {
            "bn" -> "এআই ল্যাব বিশ্লেষণ (AI Summary Insight)"
            "hi" -> "एआई सारांश अंतर्दृष्टि (AI Summary Insight)"
            "es" -> "Perspectiva del Resumen de AI"
            else -> "AI Summary Insight"
        }
        canvas.drawText(summaryHeader, marginX, currentY, subtitlePaint)
        currentY += 12f
        
        // Draw Summary text
        val sText = when (languageCode) {
            "bn" -> report.summaryBen
            "hi" -> report.summaryHi.ifEmpty { report.summaryEng }
            "es" -> report.summaryEs.ifEmpty { report.summaryEng }
            else -> report.summaryEng
        }
        drawStaticLayoutText(sText, marginX, textPaint, isBold = false, textSize = 10f, colorHex = "#374151")
        currentY += 20f
        
        // Parameters Section Header
        subtitlePaint.color = Color.parseColor("#005AC1")
        subtitlePaint.textSize = 12f
        subtitlePaint.isFakeBoldText = true
        val paramsHeader = when (languageCode) {
            "bn" -> "ল্যাব সূচক বিশ্লেষণ (Detected Lab Parameters)"
            "hi" -> "पहचाने गए लैब पैरामीटर (Detected Lab Parameters)"
            "es" -> "Parámetros de Laboratorio Detectados"
            else -> "Detected Lab Parameters"
        }
        canvas.drawText(paramsHeader, marginX, currentY, subtitlePaint)
        currentY += 15f
        
        // Table Header
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 22f, paint)
        
        val colHeaderPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
            isFakeBoldText = true
        }
        val col1 = when (languageCode) {
            "bn" -> "পরীক্ষার নাম (Parameter)"
            "hi" -> "पैरामीटर (Parameter)"
            "es" -> "Parámetro"
            else -> "Parameter"
        }
        val col2 = when (languageCode) {
            "bn" -> "ফলাফল (Value)"
            "hi" -> "परिणाम (Value)"
            "es" -> "Valor del Resultado"
            else -> "Result Value"
        }
        val col3 = when (languageCode) {
            "bn" -> "স্বাভাবিক মাত্রা (Range)"
            "hi" -> "संदर्भ सीमा (Range)"
            "es" -> "Rango de Referencia"
            else -> "Ref. Range"
        }
        val col4 = when (languageCode) {
            "bn" -> "অবস্থা (Status)"
            "hi" -> "स्थिति (Status)"
            "es" -> "Estado"
            else -> "Status"
        }
        canvas.drawText(col1, marginX + 12f, currentY + 15f, colHeaderPaint)
        canvas.drawText(col2, marginX + 180f, currentY + 15f, colHeaderPaint)
        canvas.drawText(col3, marginX + 280f, currentY + 15f, colHeaderPaint)
        canvas.drawText(col4, marginX + 410f, currentY + 15f, colHeaderPaint)
        
        currentY += 32f
        
        // Print individual parameters
        for (param in report.detectedParameters) {
            // Check if we need a new page before drawing this parameter item
            if (currentY + 58f > pageHeight - 65f) {
                startNewPage()
                // Re-draw small section title on the new page
                subtitlePaint.color = Color.parseColor("#005AC1")
                subtitlePaint.textSize = 10f
                subtitlePaint.isFakeBoldText = true
                val listCont = when (languageCode) {
                    "bn" -> "ল্যাব সূচক বিশ্লেষণ (চলমান...)"
                    "hi" -> "पहचाने गए लैब पैरामीटर (जारी...)"
                    "es" -> "Parámetros de Laboratorio Detectados (Continuación...)"
                    else -> "Detected Lab Parameters (Continued...)"
                }
                canvas.drawText(listCont, marginX, currentY, subtitlePaint)
                currentY += 16f
            }
            
            // Draw param row card background
            paint.color = Color.parseColor("#F9FAFB")
            canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 44f, paint)
            
            paint.color = Color.parseColor("#E5E7EB")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 44f, paint)
            paint.style = Paint.Style.FILL // revert
            
            // 1. Parameter name
            val namePaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.parseColor("#111827")
                textSize = 9.5f
                isFakeBoldText = true
            }
            canvas.drawText(param.name, marginX + 12f, currentY + 16f, namePaint)
            
            // 2. Value
            val valPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1F2937")
                textSize = 9f
                isFakeBoldText = false
            }
            canvas.drawText("${param.value} ${param.unit}", marginX + 180f, currentY + 16f, valPaint)
            
            // 3. Range
            canvas.drawText(param.referenceRange, marginX + 280f, currentY + 16f, valPaint)
            
            // 4. Status
            val statusColor = when (param.status.uppercase(Locale.ROOT)) {
                "HIGH" -> Color.parseColor("#EF4444")
                "LOW" -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#10B981")
            }
            val statusStr = when (param.status.uppercase(Locale.ROOT)) {
                "HIGH" -> {
                    when (languageCode) {
                        "bn" -> "উচ্চ"
                        "hi" -> "उच्च"
                        "es" -> "ALTO"
                        else -> "HIGH"
                    }
                }
                "LOW" -> {
                    when (languageCode) {
                        "bn" -> "নিম্ন"
                        "hi" -> "निम्न"
                        "es" -> "BAJO"
                        else -> "LOW"
                    }
                }
                else -> {
                    when (languageCode) {
                        "bn" -> "স্বাভাবিক"
                        "hi" -> "सामान्य"
                        "es" -> "NORMAL"
                        else -> "NORMAL"
                    }
                }
            }
            val statusPaint = TextPaint().apply {
                isAntiAlias = true
                color = statusColor
                textSize = 9.5f
                isFakeBoldText = true
            }
            canvas.drawText(statusStr, marginX + 410f, currentY + 16f, statusPaint)
            
            // 5. Explanation printed as smaller wrapped text block inside the subrow
            val explStr = when (languageCode) {
                "bn" -> param.explanationBen
                "hi" -> param.explanationHi.ifEmpty { param.explanationEng }
                "es" -> param.explanationEs.ifEmpty { param.explanationEng }
                else -> param.explanationEng
            }
            
            val tempY = currentY
            currentY += 25f
            
            // Temporarily contract content width inside card margins
            val cardPadding = 12f
            textPaint.textSize = 8f
            textPaint.color = Color.parseColor("#4B5563")
            textPaint.isFakeBoldText = false
            
            val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(explStr, 0, explStr.length, textPaint, (contentWidth - 2 * cardPadding).toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.1f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(explStr, textPaint, (contentWidth - 2 * cardPadding).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.1f, 0.0f, false)
            }
            
            canvas.save()
            canvas.translate(marginX + cardPadding, currentY)
            staticLayout.draw(canvas)
            canvas.restore()
            
            currentY = tempY + 48f // Next item coordinates
        }
        
        // Draw Medical Disclaimer Banner
        if (currentY + 75f > pageHeight - 65f) {
            startNewPage()
        }
        
        currentY += 15f
        paint.color = Color.parseColor("#FEF2F2") // Light Red bg
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 68f, paint)
        
        paint.color = Color.parseColor("#FCA5A5") // outline border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawRect(marginX, currentY, pageWidth - marginX, currentY + 68f, paint)
        paint.style = Paint.Style.FILL // revert
        
        val discHeaderPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#991B1B")
            textSize = 9f
            isFakeBoldText = true
        }
        val disclaimerTitle = when (languageCode) {
            "bn" -> "⚠️ গুরুত্বপূর্ণ সতর্কীকরণ ও বৈধানিক পরামর্শ:"
            "hi" -> "⚠️ चिकित्सा अस्वीकरण और शैक्षिक सूचना:"
            "es" -> "⚠️ Descargo de Responsabilidad Médica y Aviso Educativo:"
            else -> "⚠️ Medical Disclaimer & Education Notice:"
        }
        canvas.drawText(disclaimerTitle, marginX + 12f, currentY + 16f, discHeaderPaint)
        
        val discBodyText = when (languageCode) {
            "bn" -> {
                "এই রিপোর্টটি একটি কৃত্রিম বুদ্ধিমত্তা (AI) দ্বারা সংকলিত সাধারণ শিক্ষামূলক ব্যাখ্যা মাত্র। এটি কোনো চিকিৎসকের রোগ নির্ণয় বা ব্যবস্থাপত্রের বিকল্প নয়। যেকোনো ধরনের ওষুধের সমন্বয় বা চিকিৎসার জন্য অবশ্যই একজন রেজিষ্টার্ড এমবিবিএস ডাক্তারের মতামত গ্রহণ করুন।"
            }
            "hi" -> {
                "यह रिपोर्ट केवल कृत्रिम बुद्धिमत्ता (AI) द्वारा संकलित सामान्य शैक्षिक व्याख्या है। यह किसी चिकित्सक के निदान या नुस्खे का विकल्प नहीं है। किसी भी प्रकार के उपचार के लिए कृपया पंजीकृत डॉक्टर से परामर्श करें।"
            }
            "es" -> {
                "Este informe educativo fue compilado por inteligencia artificial (AI) y tiene fines únicamente informativos y educativos. No reemplaza el diagnóstico clínico profesional o la consulta con un médico. Busque el consejo de un profesional de la salud."
            }
            else -> {
                "This automated patient educational report was compiled by AI. It is strictly for educational guidance and does not replace professional clinical diagnosis, treatment plans, or doctor consultations. Always seek clinical advice from a registered healthcare professional."
            }
        }
        
        val discBodyPaint = TextPaint().apply {
            isAntiAlias = true
            color = Color.parseColor("#7F1D1D")
            textSize = 7.5f
        }
        
        val discLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(discBodyText, 0, discBodyText.length, discBodyPaint, (contentWidth - 24f).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0.0f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(discBodyText, discBodyPaint, (contentWidth - 24f).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.15f, 0.0f, false)
        }
        
        canvas.save()
        canvas.translate(marginX + 12f, currentY + 24f)
        discLayout.draw(canvas)
        canvas.restore()
        
        // Conclude document creation
        pdfDocument.finishPage(page)
        
        // Write PDF file to safe cache directory to trigger sharing
        val pdfFileName = "MediNova_Report_${System.currentTimeMillis()}.pdf"
        val cacheFile = File(context.cacheDir, pdfFileName)
        
        try {
            val fos = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.flush()
            fos.close()
            
            // Share using safe FileProvider authorities
            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                val subjectText = when (languageCode) {
                    "bn" -> "মেডিনোভা মেডিকেল রিপোর্ট"
                    "hi" -> "मेडिनोवा लैब विश्लेषण रिपोर्ट"
                    "es" -> "Informe de Análisis de Laboratorio de MediNova"
                    else -> "MediNova Lab Analysis Report"
                }
                val bodyText = when (languageCode) {
                    "bn" -> "মেডিনোভা দ্বারা তৈরি ল্যাব রিপোর্ট বিশ্লেষণ পিডিএফ সংযুক্ত করা হলো।"
                    "hi" -> "कृपया मेडिनोवा द्वारा तैयार लैबोरेटरी रिपोर्ट विश्लेषण खोजें।"
                    "es" -> "Adjunto encontrará el informe de resumen educativo de laboratorio compilado a través de MediNova."
                    else -> "Find attached the Patient Lab Analysis Summary report compiled via MediNova."
                }
                putExtra(Intent.EXTRA_SUBJECT, subjectText)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val shareTitle = when (languageCode) {
                "bn" -> "পিডিএফ রিপোর্ট শেয়ার করুন"
                "hi" -> "पीडीएफ रिपोर्ट साझा करें"
                "es" -> "Compartir Informe PDF"
                else -> "Share PDF Report"
            }
            context.startActivity(Intent.createChooser(intent, shareTitle))
            
        } catch (e: IOException) {
            e.printStackTrace()
            val errorLabel = when (languageCode) {
                "bn" -> "পিডিএফ তৈরি করতে ত্রুটি হয়েছে!"
                "hi" -> "पीडीएफ बनाने में त्रुटि!"
                "es" -> "¡Error al crear el archivo PDF!"
                else -> "Error creating PDF file!"
            }
            Toast.makeText(context, errorLabel, Toast.LENGTH_LONG).show()
        }
    }
}
