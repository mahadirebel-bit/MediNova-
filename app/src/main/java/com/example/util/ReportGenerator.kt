package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReportGenerator {

    fun shareBmiReport(
        context: Context,
        isBengali: Boolean,
        gender: String,
        heightFeet: String,
        heightInches: String,
        weightKg: String,
        bmi: Double,
        category: String,
        advice: String
    ) {
        try {
            val pdfDocument = PdfDocument()
            // Standard A4 document format: width 595, height 842
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            
            // Primary colored decorative header band
            paint.color = Color.parseColor("#1D4ED8") // Blue 700 Accent
            canvas.drawRect(0f, 0f, 595f, 130f, paint)

            // Title
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerTitle = if (isBengali) "ডিজিটাল স্বাস্থ্য ও বিএমআই রিপোর্ট" else "Digital Health & BMI Report"
            canvas.drawText(headerTitle, 40f, 60f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val headerSubtitle = if (isBengali) "মা ও শিশু স্বাস্থ্য সেবা - ডিজিটাল ক্যাবিনেট" else "Maternal & Child Health Services - Digital Cabinet"
            canvas.drawText(headerSubtitle, 40f, 85f, paint)

            paint.color = Color.parseColor("#93C5FD")
            paint.textSize = 10f
            val dateStr = if (isBengali) "রিপোর্ট জেনারেশন তারিখ: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                          else "Generated on: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
            canvas.drawText(dateStr, 40f, 110f, paint)

            paint.color = Color.BLACK
            var yPos = 180

            fun drawLine(title: String, value: String) {
                paint.color = Color.parseColor("#4B5563") // Slate 600
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(title, 40f, yPos.toFloat(), paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.BLACK
                canvas.drawText(value, 190f, yPos.toFloat(), paint)
                
                val dividerPaint = Paint().apply {
                    color = Color.parseColor("#E5E7EB")
                    strokeWidth = 1f
                }
                canvas.drawLine(40f, yPos.toFloat() + 10, 555f, yPos.toFloat() + 10, dividerPaint)
                yPos += 35
            }

            // Draw Section Title
            paint.color = Color.parseColor("#1D4ED8")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle1 = if (isBengali) "১. শারীরিক পরিমাপ সমূহের বিবরণ" else "1. Physical Measurement Details"
            canvas.drawText(secTitle1, 40f, yPos.toFloat(), paint)
            yPos += 25

            drawLine(
                if (isBengali) "লিঙ্গ (Gender):" else "Gender:",
                if (gender.lowercase() == "male") (if (isBengali) "পুরুষ (Male)" else "Male") else (if (isBengali) "নারী (Female)" else "Female")
            )
            drawLine(
                if (isBengali) "উচ্চতা (Height):" else "Height:",
                "$heightFeet f. $heightInches in."
            )
            drawLine(
                if (isBengali) "ওজন (Weight):" else "Weight:",
                "$weightKg kg"
            )

            yPos += 15
            paint.color = Color.parseColor("#1D4ED8")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle2 = if (isBengali) "২. পরিমাপ ভিত্তিক স্বয়ংক্রিয় ফলাফল" else "2. Automated Diagnostic Insights"
            canvas.drawText(secTitle2, 40f, yPos.toFloat(), paint)
            yPos += 25

            val df = java.text.DecimalFormat("#.#")
            drawLine(
                if (isBengali) "বিএমআই মান (BMI Value):" else "BMI Score:",
                df.format(bmi)
            )
            drawLine(
                if (isBengali) "শ্রেণীকরণ (Category):" else "Classification:",
                category
            )

            yPos += 15
            paint.color = Color.parseColor("#B45309") // Dark Amber
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val refGuideTitle = if (isBengali) "৩. ডিজিটাল পুষ্টি ও সতর্কীকরণ পরামর্শসূচী" else "3. Lifestyle Guidelines & Action Plan"
            canvas.drawText(refGuideTitle, 40f, yPos.toFloat(), paint)
            yPos += 25

            // Custom multi-line text wrapper for layout limit
            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val words = advice.split(" ")
            var line = java.lang.StringBuilder()
            val margin = 40f
            val widthLimit = 515f

            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val width = paint.measureText(testLine)
                if (width > widthLimit) {
                    canvas.drawText(line.toString(), margin, yPos.toFloat(), paint)
                    yPos += 20
                    line = java.lang.StringBuilder(word)
                } else {
                    line = java.lang.StringBuilder(testLine)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), margin, yPos.toFloat(), paint)
                yPos += 20
            }

            // Borders & Disclaimers
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            paint.textSize = 9.5f
            paint.color = Color.parseColor("#9CA3AF")
            val footerNote = if (isBengali) "বি.দ্র. এই সফটওয়্যারটি প্রারম্ভিক পরামর্শের জন্য। জরুরি প্রয়োজনে সর্বদা রেজিস্টার্ড চিকিৎসকের পরামর্শ নিন।"
                             else "Disclaimer: This digital copy is for primary calculation. Seek a professional physician for emergency care."
            canvas.drawText(footerNote, 40f, 800f, paint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "BMI_Health_Report.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isBengali) "রিপোর্ট শেয়ার করুন" else "Share Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePregnancyReport(
        context: Context,
        isBengali: Boolean,
        week: Int,
        trimester: String,
        edd: String,
        fruitComparison: String,
        hbValue: String,
        glucoseValue: String,
        bpValue: String
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()

            // Header design
            paint.color = Color.parseColor("#DB2777") // Pink/Rose Primary colors
            canvas.drawRect(0f, 0f, 595f, 130f, paint)

            paint.color = Color.WHITE
            paint.textSize = 21f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerTitle = if (isBengali) "ডিজিটাল গর্ভকালীন মাতৃত্বকালীন রিপোর্ট" else "Digital Maternal & Pregnancy Report"
            canvas.drawText(headerTitle, 40f, 60f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val headerSubtitle = if (isBengali) "মাতৃ মঙ্গল ও শিশু স্বাস্থ্য রক্ষা নেটওয়ার্ক" else "Maternal Care & Safe Newborn Network"
            canvas.drawText(headerSubtitle, 40f, 85f, paint)

            paint.color = Color.parseColor("#FBCFE8")
            paint.textSize = 10f
            val dateStr = if (isBengali) "রিপোর্ট প্রকাশের তারিখ: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                          else "Report Issued on: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
            canvas.drawText(dateStr, 40f, 110f, paint)

            var yPos = 180

            fun drawLine(title: String, value: String) {
                paint.color = Color.parseColor("#4B5563")
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(title, 40f, yPos.toFloat(), paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.BLACK
                canvas.drawText(value, 240f, yPos.toFloat(), paint)

                val dividerPaint = Paint().apply {
                    color = Color.parseColor("#F3F4F6")
                    strokeWidth = 1f
                }
                canvas.drawLine(40f, yPos.toFloat() + 10, 555f, yPos.toFloat() + 10, dividerPaint)
                yPos += 35
            }

            paint.color = Color.parseColor("#DB2777")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle1 = if (isBengali) "১. গর্ভকালীন অগ্রগতি ও সময়সীমা" else "1. Gestational Status & Estimates"
            canvas.drawText(secTitle1, 40f, yPos.toFloat(), paint)
            yPos += 25

            drawLine(
                if (isBengali) "গর্ভকালীন সপ্তাহ (Gestation Week):" else "Pregnancy Week Count:",
                if (isBengali) "$week তম সপ্তাহ" else "Week $week"
            )
            drawLine(
                if (isBengali) "ত্রৈমাসিক পর্যায় (Trimester Phase):" else "Trimester Phase:",
                trimester
            )
            drawLine(
                if (isBengali) "সম্ভাব্য প্রসবের তারিখ (EDD):" else "Expected Due Date (EDD):",
                edd
            )
            drawLine(
                if (isBengali) "ভ্রূণের আকার তুলনা (Baby Size Fruit Comparison):" else "Fetal Fruit Analogy Size:",
                fruitComparison
            )

            yPos += 15
            paint.color = Color.parseColor("#DB2777")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val secTitle2 = if (isBengali) "২. পরিমাপন ও ল্যাব টেস্টের পর্যবেক্ষণ" else "2. Physical Measurements & Lab Diagnostics"
            canvas.drawText(secTitle2, 40f, yPos.toFloat(), paint)
            yPos += 25

            drawLine(
                if (isBengali) "রক্তের হিমোগ্লোবিন (Hb. Level):" else "Blood Hemoglobin Level:",
                if (hbValue.isEmpty()) (if (isBengali) "পরিমাপ সংরক্ষিত নেই" else "Not Recorded") else "$hbValue g/dL"
            )
            drawLine(
                if (isBengali) "মুখের গ্লুকোজ স্তর (OGTT Blood Sugar):" else "OGTT Blood Sugar Reading:",
                if (glucoseValue.isEmpty()) (if (isBengali) "পরিমাপ সংরক্ষিত নেই" else "Not Recorded") else "$glucoseValue mmol/L"
            )
            drawLine(
                if (isBengali) "রক্তচাপ পরিমাপ (Blood Pressure):" else "Blood Pressure Status:",
                if (bpValue.isEmpty()) (if (isBengali) "পরিমাপ সংরক্ষিত নেই" else "Not Recorded") else "$bpValue mmHg"
            )

            yPos += 15
            paint.color = Color.parseColor("#1E3A8A")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val adviceTitle = if (isBengali) "৩. গর্ভবতী মায়েদের জন্য জীবনধারা ও পুষ্টি গাইড" else "3. Lifestyle & Maternal Nutrition Guidelines"
            canvas.drawText(adviceTitle, 40f, yPos.toFloat(), paint)
            yPos += 25

            paint.color = Color.BLACK
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val guidelines = if (isBengali) {
                listOf(
                    "• প্রচুর তরল, তাজা ফলমূল ও আয়রনসমৃদ্ধ পুষ্টিকর খাবার (যেমন পালংশাক ও লাল মাংস) নিয়মিত গ্রহণ করুন।",
                    "• দিন ও রাতের ভারসাম্য বিশ্রাম: রাতে কমপক্ষে ৮ ঘণ্টা ঘুম এবং দুপুরে অন্তত ২ ঘণ্টা ঘুমানো জরুরি।",
                    "• রক্তচাপ নিয়ন্ত্রণে রাখতে খাবারে বাড়তি প্রিজারভেটিভ বা বাড়তি কাঁচা লবণ খাওয়া সম্পূর্ণ পরিহার করুন।",
                    "• বিপদ চিহ্ন সমূহ: তীব্র ঝাপসা দৃষ্টি, চরম মাথাব্যথা ও বাচ্চার নড়াচড়া কম লক্ষ্য করলে দ্রুত হাসপাতালে যান।"
                )
            } else {
                listOf(
                    "• Stay hydrated, consume seasonal fresh fruits, and eat mineral-rich dishes (like spinach and iron).",
                    "• Highly crucial rest balance: sleep for 8 hours at night and take a 2-hour midday offline rest.",
                    "• Avoid dietary raw table salts completely to lower risks of gestational hypertension and preeclampsia.",
                    "• Danger Warnings: Seek clinical help if experiencing slurred speech, heavy swelling, or low fetal movement."
                )
            }

            for (line in guidelines) {
                canvas.drawText(line, 40f, yPos.toFloat(), paint)
                yPos += 22
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            paint.textSize = 9.5f
            paint.color = Color.parseColor("#9CA3AF")
            val footerNote = if (isBengali) "সতর্কবার্তা: এটি একটি অটো-জেনারেটেড মাতৃত্বকালীন অগ্রগতির প্রারম্ভিক রেকর্ড। জরুরি চিকিৎসায় চিকিৎসকের সঙ্গে থাকুন।"
                             else "Disclaimer: This is an automatically generated pregnancy progress sheet. Always coordinate with your clinical guides."
            canvas.drawText(footerNote, 40f, 800f, paint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Pregnancy_Maternal_Report.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isBengali) "মাতৃত্বকালীন রিপোর্ট শেয়ার করুন" else "Share Maternal Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePrescriptionReport(
        context: Context,
        isBengali: Boolean,
        prescription: com.example.data.PrescriptionDetail
    ) {
        sharePrescriptionReport(context, if (isBengali) "bn" else "en", prescription)
    }

    fun sharePrescriptionReport(
        context: Context,
        languageCode: String,
        prescription: com.example.data.PrescriptionDetail
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()

            // Header - Medical Blue-Green theme
            paint.color = Color.parseColor("#0F766E") // Teal 700 Accent
            canvas.drawRect(0f, 0f, 595f, 130f, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val headerTitle = when (languageCode) {
                "bn" -> "এআই ডিজিটাল প্রেসক্রিপশন রিপোর্ট"
                "hi" -> "एआई डिजिटल प्रिस्क्रिप्शन रिपोर्ट"
                "es" -> "Informe de Prescripción Digital IA"
                else -> "AI Digital Prescription Report"
            }
            canvas.drawText(headerTitle, 40f, 55f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val headerSubtitle = when (languageCode) {
                "bn" -> "MediNova প্রেসক্রিপশন রিডার সার্ভিস"
                "hi" -> "MediNova प्रिस्क्रिप्शन रीडर सेवा"
                "es" -> "MediNova Servicio de Lector de Prescripciones"
                else -> "MediNova Prescription Reader Service"
            }
            canvas.drawText(headerSubtitle, 40f, 80f, paint)

            paint.color = Color.parseColor("#99F6E4") // Teal 200
            paint.textSize = 10f
            val dateStr = when (languageCode) {
                "bn" -> "রিপোর্ট প্রকাশের তারিখ: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                "hi" -> "रिपोर्ट जारी करने की तिथि: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                "es" -> "Fecha de Emisión: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
                else -> "Report Issued on: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
            }
            canvas.drawText(dateStr, 40f, 105f, paint)

            var yPos = 170

            // Helper to draw clean sections
            fun drawSectionHeader(title: String) {
                paint.color = Color.parseColor("#0F766E")
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(title, 40f, yPos.toFloat(), paint)
                yPos += 20
            }

            // Doctor details
            val sec1Title = when (languageCode) {
                "bn" -> "১. চিকিৎসকের বিবরণ"
                "hi" -> "1. डॉक्टर का विवरण"
                "es" -> "1. Detalles del Médico"
                else -> "1. Doctor Details"
            }
            drawSectionHeader(sec1Title)
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val docNamePrefix = when (languageCode) {
                "bn" -> "চিকিৎসকের নাম: "
                "hi" -> "डॉक्टर का नाम: "
                "es" -> "Nombre del Médico: "
                else -> "Doctor Name: "
            }
            canvas.drawText("$docNamePrefix${prescription.doctorName}", 50f, yPos.toFloat(), paint)
            yPos += 18
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val specPrefix = when (languageCode) {
                "bn" -> "विशेषজ্ঞ: "
                "hi" -> "विशेषज्ञता: "
                "es" -> "Especialidad: "
                else -> "Specialty: "
            }
            canvas.drawText("$specPrefix${prescription.doctorSpecialty}", 50f, yPos.toFloat(), paint)
            yPos += 18
            if (prescription.date.isNotEmpty()) {
                val datePrefix = when (languageCode) {
                    "bn" -> "প্রেসক্রিপশনের তারিখ: "
                    "hi" -> "प्रिस्क्रिप्शन दिनांक: "
                    "es" -> "Fecha de Prescripción: "
                    else -> "Prescription Date: "
                }
                canvas.drawText("$datePrefix${prescription.date}", 50f, yPos.toFloat(), paint)
                yPos += 18
            }
            if (prescription.symptomsAndDiagnosis.isNotEmpty()) {
                val symPrefix = when (languageCode) {
                    "bn" -> "লক্ষণ/রোগ নির্ণয়: "
                    "hi" -> "लक्षण/रोग निदान: "
                    "es" -> "Síntomas/Diagnóstico: "
                    else -> "Symptoms/Diagnosis: "
                }
                canvas.drawText("$symPrefix${prescription.symptomsAndDiagnosis}", 50f, yPos.toFloat(), paint)
                yPos += 18
            }
            yPos += 15

            // Medicines section
            val sec2Title = when (languageCode) {
                "bn" -> "২. ঔষধ সেবনের তালিকা ও নির্দেশিকা"
                "hi" -> "2. दवाएं और खुराक दिशानिर्देश"
                "es" -> "2. Medicamentos y Pautas de Dosificación"
                else -> "2. Medicines & Dosage Guideline"
            }
            drawSectionHeader(sec2Title)
            paint.textSize = 11f
            for (med in prescription.medicines) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#0F766E")
                canvas.drawText("• ${med.name}", 50f, yPos.toFloat(), paint)
                yPos += 18
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.BLACK
                val medDetails = when (languageCode) {
                    "bn" -> "ডোজ: ${med.dosage} (${med.frequency}) | সময়: ${med.timing} | স্থায়িত্ব: ${med.duration}"
                    "hi" -> "खुराक: ${med.dosage} (${med.frequency}) | समय: ${med.timing} | अवधि: ${med.duration}"
                    "es" -> "Dosis: ${med.dosage} (${med.frequency}) | Horario: ${med.timing} | Duración: ${med.duration}"
                    else -> "Dosage: ${med.dosage} (${med.frequency}) | Timing: ${med.timing} | Duration: ${med.duration}"
                }
                canvas.drawText(medDetails, 65f, yPos.toFloat(), paint)
                yPos += 16

                val purposeVal = when (languageCode) {
                    "bn" -> med.purposeBen
                    "hi" -> med.purposeHi.ifEmpty { med.purposeEng }
                    "es" -> med.purposeEs.ifEmpty { med.purposeEng }
                    else -> med.purposeEng
                }
                val purposeLabel = when (languageCode) {
                    "bn" -> "উদ্দেশ্য: "
                    "hi" -> "उद्देश्य: "
                    "es" -> "Propósito: "
                    else -> "Purpose: "
                }
                val purpose = "$purposeLabel$purposeVal"
                if (purposeVal.isNotEmpty()) {
                    canvas.drawText(purpose, 65f, yPos.toFloat(), paint)
                    yPos += 18
                }
            }
            yPos += 15

            // Recommendations section
            if (prescription.recommendations.isNotEmpty()) {
                val sec3Title = when (languageCode) {
                    "bn" -> "৩. প্রস্তাবিত স্বাস্থ্য পরীক্ষা সমূহ"
                    "hi" -> "3. अनुशंसित नैदानिक परीक्षण"
                    "es" -> "3. Pruebas de Diagnóstico Recomendadas"
                    else -> "3. Recommended Diagnostic Tests"
                }
                drawSectionHeader(sec3Title)
                paint.color = Color.BLACK
                for (rec in prescription.recommendations) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("• ${rec.title}", 50f, yPos.toFloat(), paint)
                    yPos += 16
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val recDesc = when (languageCode) {
                        "bn" -> rec.descBen
                        "hi" -> rec.descHi.ifEmpty { rec.descEng }
                        "es" -> rec.descEs.ifEmpty { rec.descEng }
                        else -> rec.descEng
                    }
                    canvas.drawText(recDesc, 65f, yPos.toFloat(), paint)
                    yPos += 18
                }
                yPos += 15
            }

            // General Advice
            val sec4Title = when (languageCode) {
                "bn" -> "৪. সাধারণ ও জীবনধারা বিষয়ক পরামর্শ"
                "hi" -> "4. सामान्य और जीवनशैली सलाह"
                "es" -> "4. Consejos Generales de Estilo de Vida"
                else -> "4. General Lifestyle Advice"
            }
            drawSectionHeader(sec4Title)
            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val adviceText = when (languageCode) {
                "bn" -> prescription.adviceBen
                "hi" -> prescription.adviceHi.ifEmpty { prescription.adviceEng }
                "es" -> prescription.adviceEs.ifEmpty { prescription.adviceEng }
                else -> prescription.adviceEng
            }
            if (adviceText.isNotEmpty()) {
                val words = adviceText.split(" ")
                var line = java.lang.StringBuilder()
                val margin = 50f
                val widthLimit = 500f

                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    val width = paint.measureText(testLine)
                    if (width > widthLimit) {
                        canvas.drawText(line.toString(), margin, yPos.toFloat(), paint)
                        yPos += 18
                        line = java.lang.StringBuilder(word)
                    } else {
                        line = java.lang.StringBuilder(testLine)
                    }
                }
                if (line.isNotEmpty()) {
                    canvas.drawText(line.toString(), margin, yPos.toFloat(), paint)
                    yPos += 18
                }
            }

            if (prescription.followUpDate.isNotEmpty()) {
                yPos += 10
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val followUpLabel = when (languageCode) {
                    "bn" -> "ফলো-আপের তারিখ: "
                    "hi" -> "अनुवर्ती तिथि: "
                    "es" -> "Fecha de Seguimiento: "
                    else -> "Follow-up Date: "
                }
                canvas.drawText("$followUpLabel${prescription.followUpDate}", 50f, yPos.toFloat(), paint)
                yPos += 18
            }

            // Border
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            // Footer
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#9CA3AF")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val footerNote = when (languageCode) {
                "bn" -> "সতর্কবার্তা: এই প্রেসক্রিপশন রিপোর্টটি ল্যাব টেস্ট সনাক্তকরণ মডেল দ্বারা তৈরি এবং শিক্ষামূলক উদ্দেশ্যে। ডাক্তারী সেবার জন্য ডাক্তারের মূল নির্দেশনা মেনে চলুন।"
                "hi" -> "अस्वीकरण: यह रिपोर्ट केवल शैक्षिक/सूचनात्मक सहायक उद्देश्यों के लिए MediNova OCR द्वारा पार्स की गई है। हमेशा अपने डॉक्टर से परामर्श लें।"
                "es" -> "Descargo de responsabilidad: Este informe educativo fue analizado por MediNova OCR con fines informativos. Siempre consulte con su médico."
                else -> "Disclaimer: This report is parsed by MediNova OCR for educational/informational helper purposes only. Always check with your doctor."
            }
            canvas.drawText(footerNote, 40f, 800f, paint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Prescription_Scan_Report.pdf")
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareTitle = when (languageCode) {
                "bn" -> "প্রেসক্রিপশন রিপোর্ট শেয়ার করুন"
                "hi" -> "प्रिस्क्रिप्शन रिपोर्ट साझा करें"
                "es" -> "Compartir Informe de Prescripción"
                else -> "Share Prescription Report"
            }
            context.startActivity(Intent.createChooser(shareIntent, shareTitle))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
