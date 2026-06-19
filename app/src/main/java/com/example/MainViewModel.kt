package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MedicalDatabase
import com.example.data.ReportDetail
import com.example.data.ReportParser
import com.example.data.MedicineReminder
import com.example.data.ReportRepository
import com.example.data.SavedReport
import com.example.data.api.GeminiClient
import com.example.util.PdfToBitmapHelper
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class MainViewModel(private val repository: ReportRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadingStep = MutableStateFlow("")
    val loadingStep: StateFlow<String> = _loadingStep

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentReport = MutableStateFlow<ReportDetail?>(null)
    val currentReport: StateFlow<ReportDetail?> = _currentReport

    private val _currentPrescription = MutableStateFlow<com.example.data.PrescriptionDetail?>(null)
    val currentPrescription: StateFlow<com.example.data.PrescriptionDetail?> = _currentPrescription

    private val _isPrescriptionMode = MutableStateFlow(false)
    val isPrescriptionMode: StateFlow<Boolean> = _isPrescriptionMode

    private val _isBengali = MutableStateFlow(false)
    val isBengali: StateFlow<Boolean> = _isBengali

    private val _isHindi = MutableStateFlow(false)
    val isHindi: StateFlow<Boolean> = _isHindi

    private val _isSpanish = MutableStateFlow(false)
    val isSpanish: StateFlow<Boolean> = _isSpanish

    private val _isArabic = MutableStateFlow(false)
    val isArabic: StateFlow<Boolean> = _isArabic

    private val _isFrench = MutableStateFlow(false)
    val isFrench: StateFlow<Boolean> = _isFrench

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode

    // Expose reports from repository in Room with initial emptyState Flow
    val reportsHistory: StateFlow<List<SavedReport>> = repository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val medicineReminders: StateFlow<List<MedicineReminder>> = repository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(name: String, dosage: String, frequency: String, time: String, instructions: String) {
        viewModelScope.launch {
            val reminder = MedicineReminder(
                medicineName = name,
                dosage = dosage,
                frequency = frequency,
                time = time,
                instructions = instructions,
                isActive = true
            )
            repository.insertReminder(reminder)
        }
    }

    fun toggleReminderActive(reminder: MedicineReminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            repository.clearAllReminders()
        }
    }

    private var ttsHelper: TextToSpeechHelper? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    // Custom Daily Tips states
    private val _customHealthTip = MutableStateFlow<com.example.util.HealthTip?>(null)
    val customHealthTip: StateFlow<com.example.util.HealthTip?> = _customHealthTip

    private val _customTipLoading = MutableStateFlow(false)
    val customTipLoading: StateFlow<Boolean> = _customTipLoading

    fun generateCustomHealthTip() {
        viewModelScope.launch {
            _customTipLoading.value = true
            try {
                val code = _languageCode.value
                val languageName = when (code) {
                    "bn" -> "plain, polite Bengali"
                    "hi" -> "plain, polite Hindi"
                    "es" -> "plain, polite Spanish"
                    "ar" -> "plain, polite Arabic"
                    "fr" -> "plain, polite French"
                    else -> "clear, professional English"
                }
                val formatSuffix = when (code) {
                    "bn" -> "in Bengali"
                    "hi" -> "in Hindi"
                    "es" -> "in Spanish"
                    "ar" -> "in Arabic"
                    "fr" -> "in French"
                    else -> "in English"
                }
                
                val prompt = """
                    You are "MedExplain AI", an expert medical tip provider.
                    Please generate 1 unique, highly educational wellness health tip and 1 caution/warning in $languageName.
                    The tip should be beneficial for daily wellness, nutrition, or physical activity, randomizing between different fields (e.g. hydration, heart safety, diabetes alert, eye fitness, liver care, CBC parameters).
                    Keep the answers short, professional and easy to understand for laypeople.
                    Strictly return ONLY a JSON block in this format:
                    {
                      "title": "Short title $formatSuffix",
                      "category": "Friendly category $formatSuffix (e.g., হার্টের যত্ন, रोग निवारण, Cuidado del corazón, or Heart Care, Immunity)",
                      "icon": "One single matching emoji",
                      "tip": "Short 2-3 sentence tip $formatSuffix",
                      "warning": "Short 1-2 sentence caution/warning $formatSuffix"
                    }
                """.trimIndent()

                val resultJson = GeminiClient.generateText(prompt)
                if (resultJson != "ERROR_KEY_MISSING" && !resultJson.startsWith("ERROR_API_FAILED") && !resultJson.startsWith("ERROR_EXCEPTION")) {
                    val cleaned = cleanJsonString(resultJson)
                    val json = org.json.JSONObject(cleaned)
                    val defaultTitle = when (code) {
                        "bn" -> "সুস্থতা টিপস"
                        "hi" -> "स्वास्थ्य युक्तियाँ"
                        "es" -> "Consejos de salud"
                        "ar" -> "نصائح صحية"
                        "fr" -> "Conseils de santé"
                        else -> "Wellness Tip"
                    }
                    val defaultCategory = when (code) {
                        "bn" -> "আজকের স্বাস্থ্য"
                        "hi" -> "आज का स्वास्थ्य"
                        "es" -> "Salud de hoy"
                        "ar" -> "صحة اليوم"
                        "fr" -> "Santé d'aujourd'hui"
                        else -> "Today's Health"
                    }
                    
                    val title = json.optString("title", defaultTitle)
                    val category = json.optString("category", defaultCategory)
                    val icon = json.optString("icon", "✨")
                    val tip = json.optString("tip", "")
                    val warning = json.optString("warning", "")
                    
                    if (tip.isNotEmpty()) {
                        _customHealthTip.value = com.example.util.HealthTip(
                            dayIndex = 0,
                            title = title,
                            category = category,
                            icon = icon,
                            tip = tip,
                            warning = warning
                        )
                    }
                } else {
                    Log.e("ViewModel", "Failed to generate dynamic health tip: $resultJson")
                    _customHealthTip.value = null
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error in generating custom health tip", e)
                _customHealthTip.value = null
            } finally {
                _customTipLoading.value = false
            }
        }
    }

    fun resetCustomHealthTip() {
        _customHealthTip.value = null
    }

    fun initializeTts(context: Context) {
        if (ttsHelper == null) {
            ttsHelper = TextToSpeechHelper(context) { success ->
                Log.d("ViewModel", "TTS initialized: $success")
            }
        }
    }

    fun toggleLanguage() {
        val next = when (_languageCode.value) {
            "en" -> "bn"
            "bn" -> "hi"
            "hi" -> "es"
            "es" -> "ar"
            "ar" -> "fr"
            else -> "en"
        }
        setLanguageCode(next)
    }

    fun setLanguageBengali(bengali: Boolean) {
        setLanguageCode(if (bengali) "bn" else "en")
    }

    fun setLanguageCode(code: String) {
        _languageCode.value = code
        _isBengali.value = (code == "bn")
        _isHindi.value = (code == "hi")
        _isSpanish.value = (code == "es")
        _isArabic.value = (code == "ar")
        _isFrench.value = (code == "fr")
        stopSpeaking()
        generateCustomHealthTip()
    }

    fun speakReport() {
        val report = _currentReport.value ?: return
        val currentLang = _languageCode.value
        val textToSpeak = when (currentLang) {
            "bn" -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationBen}"
                }
                "${report.summaryBen}\n\n$paramsText"
            }
            "hi" -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationHi.ifEmpty { it.explanationEng }}"
                }
                "${report.summaryHi.ifEmpty { report.summaryEng }}\n\n$paramsText"
            }
            "es" -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationEs.ifEmpty { it.explanationEng }}"
                }
                "${report.summaryEs.ifEmpty { report.summaryEng }}\n\n$paramsText"
            }
            "ar" -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationAr.ifEmpty { it.explanationEng }}"
                }
                "${report.summaryAr.ifEmpty { report.summaryEng }}\n\n$paramsText"
            }
            "fr" -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationFr.ifEmpty { it.explanationEng }}"
                }
                "${report.summaryFr.ifEmpty { report.summaryEng }}\n\n$paramsText"
            }
            else -> {
                val paramsText = report.detectedParameters.joinToString("\n") { 
                    "${it.name}: ${it.explanationEng}"
                }
                "${report.summaryEng}\n\n$paramsText"
            }
        }

        _isSpeaking.value = true
        ttsHelper?.speak(textToSpeak, currentLang)
    }

    fun setPrescriptionMode(enabled: Boolean) {
        _isPrescriptionMode.value = enabled
    }

    fun speakPrescription() {
        val rx = _currentPrescription.value ?: return
        val currentLang = _languageCode.value
        val textToSpeak = when (currentLang) {
            "bn" -> {
                val medsText = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}। উদ্দেশ্য: ${it.purposeBen}"
                }
                "ডাক্তার ${rx.doctorName}। বিশেষজ্ঞ: ${rx.doctorSpecialty}। প্রেসক্রিপশনের তারিখ: ${rx.date}। সেবন নির্দেশিকা:\n$medsText\n\nপরামর্শ: ${rx.adviceBen}"
            }
            "hi" -> {
                val medsHi = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}. उद्देश्य: ${it.purposeHi.ifEmpty { it.purposeEng }}"
                }
                "डॉक्टर ${rx.doctorName}. विशेषज्ञ: ${rx.doctorSpecialty}. तारीख: ${rx.date}. दवा विवरण:\n$medsHi\n\nसलाह: ${rx.adviceHi.ifEmpty { rx.adviceEng }}"
            }
            "es" -> {
                val medsEs = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}. Propósito: ${it.purposeEs.ifEmpty { it.purposeEng }}"
                }
                "Médico ${rx.doctorName}. Especialidad: ${rx.doctorSpecialty}. Fecha: ${rx.date}. Instrucciones de medicamentos:\n$medsEs\n\nConsejo: ${rx.adviceEs.ifEmpty { rx.adviceEng }}"
            }
            "ar" -> {
                val medsAr = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}. الغرض: ${it.purposeAr.ifEmpty { it.purposeEng }}"
                }
                "الطبيب ${rx.doctorName}. التخصص: ${rx.doctorSpecialty}. التاريخ: ${rx.date}. إرشادات الأدوية:\n$medsAr\n\nنصيحة: ${rx.adviceAr.ifEmpty { rx.adviceEng }}"
            }
            "fr" -> {
                val medsFr = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}. Objectif: ${it.purposeFr.ifEmpty { it.purposeEng }}"
                }
                "Médecin ${rx.doctorName}. Spécialité: ${rx.doctorSpecialty}. Date: ${rx.date}. Instructions de traitement:\n$medsFr\n\nConseils: ${rx.adviceFr.ifEmpty { rx.adviceEng }}"
            }
            else -> {
                val medsText = rx.medicines.joinToString("\n") { 
                    "${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}, ${it.duration}. Purpose: ${it.purposeEng}"
                }
                "Doctor ${rx.doctorName}. Specialty: ${rx.doctorSpecialty}. Date: ${rx.date}. Guideline Instructions:\n$medsText\n\nAdvice: ${rx.adviceEng}"
            }
        }

        _isSpeaking.value = true
        ttsHelper?.speak(textToSpeak, currentLang)
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        ttsHelper?.stop()
    }

    fun clearCurrentReport() {
        _currentReport.value = null
        _currentPrescription.value = null
        _error.value = null
        stopSpeaking()
    }

    fun clearCurrentPrescription() {
        _currentPrescription.value = null
        _error.value = null
        stopSpeaking()
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun loadSavedReport(savedReport: SavedReport) {
        if (savedReport.reportType == "Prescription") {
            val parsedRx = com.example.data.PrescriptionParser.parseJson(savedReport.rawJson)
            if (parsedRx != null) {
                _currentPrescription.value = parsedRx
                _error.value = null
                stopSpeaking()
            } else {
                _error.value = "Failed to open selected prescription."
            }
        } else {
            val parsed = ReportParser.parseJson(savedReport.rawJson)
            if (parsed != null) {
                _currentReport.value = parsed
                _error.value = null
                stopSpeaking()
            } else {
                _error.value = "Failed to open selected report."
            }
        }
    }

    fun analyzeReportBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            stopSpeaking()

            try {
                if (_isPrescriptionMode.value) {
                    _loadingStep.value = "AI Prescription Scan..."
                    val systemPrompt = """
You are "MedExplain AI", an expert medical helper. Your job is to read and analyze doctors' hand-written or printed prescriptions.
Extract the doctor's name, specialization, prescribed medicines, diagnostic tests/recommendations, and general guidelines or lifestyle advice.
Translate instructions and descriptions into English, polite Bengali (বাংলা), polite Hindi (हिंदी), polite Spanish (Español), polite Arabic (العربية), and polite French (Français) so the patient can easily understand.

Your response must strictly be a JSON object containing:
{
  "doctorName": "Doctor's Name",
  "doctorSpecialty": "e.g. Cardiologist / Medicine Specialist",
  "date": "Date of prescription if visible",
  "medicines": [
    {
      "name": "Medicine Name (e.g. Napa Extend)",
      "dosage": "e.g. 1+0+1 or 5ml",
      "frequency": "e.g. Twice daily",
      "timing": "e.g. After meal",
      "duration": "e.g. 7 Days",
      "purposeEng": "For fever and severe head pain",
      "purposeBen": "জ্বর এবং তীব্র মাথাব্যথা কমানোর জন্য",
      "purposeHi": "बुखार और गंभीर सिरदर्द को कम करने के लिए",
      "purposeEs": "Para reducir la fiebre y el dolor de cabeza agudo",
      "purposeAr": "لتخفيف الحمى وآلام الرأس الشديدة",
      "purposeFr": "Pour réduire la fièvre et les maux de tête intenses"
    }
  ],
  "recommendations": [
    {
      "title": "e.g. CBC Test / রক্ত পরীক্ষা / রক্ত जांच / Prueba de CBC / فحص الدم / Examen CBC",
      "descEng": "To check blood cells and infection level",
      "descBen": "রক্তের কোষ ও ইনফেকশন পরীক্ষা করতে",
      "descHi": "رक्त कोशिकाओं और संक्रमण के स्तर की जांच के लिए",
      "descEs": "Para verificar las células sanguíneas y el nivel de infección",
      "descAr": "لفحص خلايا الدم ومستوى الالتهاب",
      "descFr": "Pour vérifier les cellules sanguines et le niveau d'infection"
    }
  "symptomsAndDiagnosis": "Doctor's symptoms notes or diagnostics if written",
  "followUpDate": "e.g. Next month or After 2 weeks",
  "adviceEng": "English lifestyle guidance, rest recommendations, etc.",
  "adviceBen": "বাংলায় জীবনধারা, বিশ্রাম এবং অন্যান্য পরামর্শ",
  "adviceHi": "जीवनशैली मार्गदर्शन, आराम की सिफारिशें आदि हिंदी में",
  "adviceEs": "Guía de estilo de vida, recomendaciones de descanso, etc. en español",
  "adviceAr": "إرشادات طبية حول نمط الحياة والراحة باللغة العربية",
  "adviceFr": "Conseils d'hygiène de vie, repos recommandé, etc. en français"
}
                    """.trimIndent()

                    val result = GeminiClient.analyzeReport(bitmap, systemPrompt)
                    if (result == "ERROR_KEY_MISSING") {
                        _error.value = "Gemini API Key is missing. Please set your GEMINI_API_KEY inside the Secrets panel of AI Studio."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_API_FAILED")) {
                        _error.value = "AI analysis failed. Please check your internet connection or API credits."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_EXCEPTION")) {
                        _error.value = "Connection error. Please try again."
                        _isLoading.value = false
                        return@launch
                    }

                    val cleanedResult = cleanJsonString(result)
                    val parsedRx = com.example.data.PrescriptionParser.parseJson(cleanedResult)
                    if (parsedRx != null) {
                        _currentPrescription.value = parsedRx
                        val title = "Prescription: ${parsedRx.doctorName}"
                        val entity = SavedReport(
                            title = title,
                            reportType = "Prescription",
                            rawJson = cleanedResult
                        )
                        repository.insert(entity)
                    } else {
                        Log.e("ViewModel", "Failed to parse rx: $result")
                        _error.value = "Failed to understand prescription. Please ensure the image is clear and written clearly."
                    }
                } else {
                    _loadingStep.value = "AI OCR & Explanation..."
                    val systemPrompt = """
You are "MedExplain AI", an expert educational medical report companion.
You identify the medical report type and explain lab test parameters in plain, friendly language for a regular layperson.

You MUST satisfy these mandatory limitations:
- NEVER diagnose diseases. Do NOT tell users they have diabetes, kidney disease, etc.
- ALWAYS use speculative phrases like "may indicate", "can be associated with", "higher than normal", "lower than normal", "consult a doctor for proper diagnosis".
- You must explain in English, Bengali, Hindi, Spanish, Arabic, and French.
- Highlight high, low, or normal ranges correctly.

Determine the report status ("HIGH", "LOW", "NORMAL") for each detected parameter.
Provide simple patient-friendly explanation in ALL six languages.

Output ONLY a JSON block containing:
{
  "reportType": "CBC | Blood Sugar | Kidney Function | Liver Function | Lipid Profile | Thyroid",
  "detectedParameters": [
    {
      "name": "Hemoglobin",
      "value": "11.2",
      "unit": "g/dL",
      "referenceRange": "12.0 - 15.5",
      "status": "LOW",
      "explanationEng": "Hemoglobin is slightly lower than the normal range. Hemoglobin helps carry oxygen in the blood. Low levels may be associated with weakness or anemia-related conditions.",
      "explanationBen": "হিমোগ্লোবিন স্বাভাবিক সীমার চেয়ে কিছুটা কম। হিমোগ্লোবিন রক্তে অক্সিজেন বহনে সাহায্য করে। কম মাত্রা দুর্বলতা বা রক্তাল্পতার সাথে জড়িত থাকতে পারে।",
      "explanationHi": "हीमोग्लोबिन सामान्य सीमा से थोड़ा कम है। हीमोग्लोबिन रक्त में ऑक्सीजन ले जाने में मदद करता है। कम स्तर कमजोरी या एनीमिया से संबंधित स्थितियों से जुड़ा हो सकता है।",
      "explanationEs": "La hemoglobina es ligeramente inferior al rango normal. La hemoglobina ayuda a transportar oxígeno en la sangre. Los niveles bajos pueden estar asociados con debilidad o condiciones relacionadas con la anemia.",
      "explanationAr": "الهيموجلوبين أقل بقليل من المعدل الطبيعي. يساعد الهيموجلوبين في نقل الأكسجين في الدم. قد ترتبط المستويات المنخفضة بالضعف أو الأنيميا.",
      "explanationFr": "L'hémoglobine est légèrement inférieure à la normale. L'hémoglobine aide à transporter l'oxygène dans le sang. Un taux bas peut être associé à de la fatigue."
    }
  ],
  "summaryEng": "A short, simple explanation summary in English, ending with: Please consult a doctor for proper diagnosis.",
  "summaryBen": " বাংলায় একটি সহজ সংক্ষিপ্ত সংক্ষিপ্ত বিবরণ, যা শেষ হবে: সঠিক রোগ নির্ণয়ের জন্য দয়া করে একজন ডাক্তারের সাথে পরামর্শ করুন।",
  "summaryHi": "हिंदी में एक संक्षिप्त, सरल व्याख्या सारांश, जो इस पर समाप्त होगा: कृपया उचित निदान के लिए डॉक्टर से परामर्श करें।",
  "summaryEs": "Un resumen explicativo breve y sencillo en español, que termine con: Por favor, consulte a un médico para un diagnóstico adecuado.",
  "summaryAr": "ملخص بسيط باللغة العربية ينتهي بـ: يرجى استشارة الطبيب للحصول على التشخيص الصحيح.",
  "summaryFr": "Un résumé simple en français se terminant par : Veuillez consulter un médecin pour un diagnostic approprié."
}
                    """.trimIndent()

                    val result = GeminiClient.analyzeReport(bitmap, systemPrompt)
                    if (result == "ERROR_KEY_MISSING") {
                        _error.value = "Gemini API Key is missing. Please set your GEMINI_API_KEY inside the Secrets panel of AI Studio."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_API_FAILED")) {
                        _error.value = "AI analysis failed. Please check your internet connection or API credits."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_EXCEPTION")) {
                        _error.value = "Connection error. Please try again."
                        _isLoading.value = false
                        return@launch
                    }

                    val cleanedResult = cleanJsonString(result)
                    val parsedDetail = ReportParser.parseJson(cleanedResult)
                    if (parsedDetail != null) {
                        _currentReport.value = parsedDetail
                        val reportTitle = "${parsedDetail.reportType} Lab Report"
                        val entity = SavedReport(
                            title = reportTitle,
                            reportType = parsedDetail.reportType,
                            rawJson = cleanedResult
                        )
                        repository.insert(entity)
                    } else {
                        Log.e("ViewModel", "Failed to parse: $result")
                        _error.value = "Failed to understand lab report. Please ensure the image is clear and contains a supported test (CBC, Sugar, Thyroid, Kidney, Liver, or Lipids)."
                    }
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
                _loadingStep.value = ""
            }
        }
    }

    fun analyzeReportFile(context: Context, uri: Uri, isPdf: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            stopSpeaking()

            try {
                _loadingStep.value = "Processing document..."
                val bitmap = if (isPdf) {
                    PdfToBitmapHelper.renderPdfFirstPage(context, uri)
                } else {
                    getBitmapFromUri(context, uri)
                }

                if (bitmap == null) {
                    _error.value = "Failed to load report image or PDF page."
                    _isLoading.value = false
                    return@launch
                }

                if (_isPrescriptionMode.value) {
                    _loadingStep.value = "Extracting prescription text..."
                    val systemPrompt = """
You are "MedExplain AI", an expert medical helper. Your job is to read and analyze doctors' hand-written or printed prescriptions.
Extract the doctor's name, specialization, prescribed medicines, diagnostic tests/recommendations, and general guidelines or lifestyle advice.
Translate instructions and descriptions into English, polite Bengali (বাংলা), polite Hindi (हिंदी), polite Spanish (Español), polite Arabic (العربية), and polite French (Français) so the patient can easily understand.

Your response must strictly be a JSON object containing:
{
  "doctorName": "Doctor's Name",
  "doctorSpecialty": "e.g. Cardiologist / Medicine Specialist",
  "date": "Date of prescription if visible",
  "medicines": [
    {
      "name": "Medicine Name (e.g. Napa Extend)",
      "dosage": "e.g. 1+0+1 or 5ml",
      "frequency": "e.g. Twice daily",
      "timing": "e.g. After meal",
      "duration": "e.g. 7 Days",
      "purposeEng": "For fever and severe head pain",
      "purposeBen": "জ্বর এবং তীব্র মাথাব্যহা কমানোর জন্য",
      "purposeHi": "बुखार और गंभीर सिरदर्द को कम करने के लिए",
      "purposeEs": "Para reducir la fiebre y el dolor de cabeza agudo",
      "purposeAr": "لتخفيف الحمى وآلام الرأس الشديدة",
      "purposeFr": "Pour réduire la fièvre et les maux de tête intenses"
    }
  ],
  "recommendations": [
    {
      "title": "e.g. CBC Test / রক্ত পরীক্ষা / रक्त जांच / Prueba de CBC / فحص الدم / Examen CBC",
      "descEng": "To check blood cells and infection level",
      "descBen": "রক্তের কোষ ও ইনফেকশন পরীক্ষা করতে",
      "descHi": "रक्त कोशिकाओं और संक्रमण के स्तर की जांच के लिए",
      "descEs": "Para verificar las células sanguíneas y el nivel de infección",
      "descAr": "لفحص خلايا الدم ومستوى الالتهاب",
      "descFr": "Pour vérifier les cellules sanguines et le niveau d'infection"
    }
  ],
  "symptomsAndDiagnosis": "Doctor's symptoms notes or diagnostics if written",
  "followUpDate": "e.g. Next month or After 2 weeks",
  "adviceEng": "English lifestyle guidance, rest recommendations, etc.",
  "adviceBen": "বাংলায় জীবনধারা, বিশ্রাম এবং অন্যান্য পরামর্শ",
  "adviceHi": "जीवनशैली मार्गदर्शन, आराम की सिफारिशें आदि हिंदी में",
  "adviceEs": "Guía de estilo de vida, recomendaciones de descanso, etc. en español",
  "adviceAr": "إرشادات طبية حول نمط الحياة والراحة باللغة العربية",
  "adviceFr": "Conseils d'hygiène de vie, repos recommandé, etc. en français"
}
                    """.trimIndent()

                    _loadingStep.value = "AI Prescription Scan..."
                    val result = GeminiClient.analyzeReport(bitmap, systemPrompt)
                    if (result == "ERROR_KEY_MISSING") {
                        _error.value = "Gemini API Key is missing. Please set your GEMINI_API_KEY inside the Secrets panel of AI Studio."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_API_FAILED")) {
                        _error.value = "AI analysis failed. Please check your internet connection or API credits."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_EXCEPTION")) {
                        _error.value = "Connection error. Please try again."
                        _isLoading.value = false
                        return@launch
                    }

                    val cleanedResult = cleanJsonString(result)
                    val parsedRx = com.example.data.PrescriptionParser.parseJson(cleanedResult)
                    if (parsedRx != null) {
                        _currentPrescription.value = parsedRx
                        val title = "Prescription: ${parsedRx.doctorName}"
                        val entity = SavedReport(
                            title = title,
                            reportType = "Prescription",
                            rawJson = cleanedResult
                        )
                        repository.insert(entity)
                    } else {
                        Log.e("ViewModel", "Failed to parse rx: $result")
                        _error.value = "Failed to understand prescription. Please ensure the image is clear and written clearly."
                    }
                } else {
                    _loadingStep.value = "Extracting lab report text..."
                    val systemPrompt = """
You are "MedExplain AI", an expert educational medical report companion.
You identify the medical report type and explain lab test parameters in plain, friendly language for a regular layperson.

You MUST satisfy these mandatory limitations:
- NEVER diagnose diseases. Do NOT tell users they have diabetes, kidney disease, etc.
- ALWAYS use speculative phrases like "may indicate", "can be associated with", "higher than normal", "lower than normal", "consult a doctor for proper diagnosis".
- You must explain in English, Bengali, Hindi, Spanish, Arabic, and French.
- Highlight high, low, or normal ranges correctly.

Determine the report status ("HIGH", "LOW", "NORMAL") for each detected parameter.
Provide simple patient-friendly explanation in ALL six languages.

Output ONLY a JSON block containing:
{
  "reportType": "CBC | Blood Sugar | Kidney Function | Liver Function | Lipid Profile | Thyroid",
  "detectedParameters": [
    {
      "name": "Hemoglobin",
      "value": "11.2",
      "unit": "g/dL",
      "referenceRange": "12.0 - 15.5",
      "status": "LOW",
      "explanationEng": "Hemoglobin is slightly lower than the normal range. Hemoglobin helps carry oxygen in the blood. Low levels may be associated with weakness or anemia-related conditions.",
      "explanationBen": "হিমোগ্লোবিন স্বাভাবিক সীমার চেয়ে কিছুটা কম। হিমোগ্লোবিন রক্তে অক্সিজেন বহনে সাহায্য করে। কম মাত্রা দুর্বলতা বা রক্তাল্পতার সাথে জড়িত থাকতে পারে।",
      "explanationHi": "हीमोग्लोबिन सामान्य सीमा से थोड़ा कम है। हीमोग्लोबिन रक्त में ऑक्सीजन ले जाने में मदद करता है। कम स्तर कमजोरी या एनीमिया से संबंधित स्थितियों से जुड़ा हो सकता है।",
      "explanationEs": "La hemoglobina es ligeramente inferior al rango normal. La hemoglobina ayuda a transportar oxígeno en la sangre. Los niveles bajos pueden estar asociados con debilidad o condiciones relacionadas con la anemia.",
      "explanationAr": "الهيموجلوبين أقل بقليل من المعدل الطبيعي. يساعد الهيموجلوبين في نقل الأكسجين في الدم. قد ترتبط المستويات المنخفضة بالضعف أو الأنيميا.",
      "explanationFr": "L'hémoglobine est légèrement inférieure à la normale. L'hémoglobine aide à transporter l'oxygène dans le sang. Un taux bas peut être associé à de la fatigue."
    }
  ],
  "summaryEng": "A short, simple explanation summary in English, ending with: Please consult a doctor for proper diagnosis.",
  "summaryBen": " বাংলায় একটি সহজ সংক্ষিপ্ত সংক্ষিপ্ত বিবরণ, যা শেষ হবে: সঠিক রোগ নির্ণয়ের জন্য দয়া করে একজন ডাক্তারের সাথে পরামর্শ করুন।",
  "summaryHi": "हिंदी में एक संक्षिप्त, सरल व्याख्या सारांश, जो इस पर समाप्त होगा: कृपया उचित निदान के लिए डॉक्टर से परामर्श करें।",
  "summaryEs": "Un resumen explicativo breve y sencillo en español, que termine con: Por favor, consulte a un médico para un diagnóstico adecuado.",
  "summaryAr": "ملخص بسيط باللغة العربية ينتهي بـ: يرجى استشارة الطبيب للحصول على التشخيص الصحيح.",
  "summaryFr": "Un résumé simple en français se terminant par : Veuillez consulter un médecin pour un diagnostic approprié."
}
                    """.trimIndent()

                    _loadingStep.value = "AI OCR & Explanation..."
                    val result = GeminiClient.analyzeReport(bitmap, systemPrompt)
                    if (result == "ERROR_KEY_MISSING") {
                        _error.value = "Gemini API Key is missing. Please set your GEMINI_API_KEY inside the Secrets panel of AI Studio."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_API_FAILED")) {
                        _error.value = "AI analysis failed. Please check your internet connection or API credits."
                        _isLoading.value = false
                        return@launch
                    } else if (result.startsWith("ERROR_EXCEPTION")) {
                        _error.value = "Connection error. Please try again."
                        _isLoading.value = false
                        return@launch
                    }

                    val cleanedResult = cleanJsonString(result)
                    val parsedDetail = ReportParser.parseJson(cleanedResult)
                    if (parsedDetail != null) {
                        _currentReport.value = parsedDetail
                        val reportTitle = "${parsedDetail.reportType} Lab Report"
                        val entity = SavedReport(
                            title = reportTitle,
                            reportType = parsedDetail.reportType,
                            rawJson = cleanedResult
                        )
                        repository.insert(entity)
                    } else {
                        Log.e("ViewModel", "Failed to parse: $result")
                        _error.value = "Failed to understand lab report. Please ensure the image is clear and contains a supported test (CBC, Sugar, Thyroid, Kidney, Liver, or Lipids)."
                    }
                }
            } catch (e: Exception) {
                _error.value = "An unexpected error occurred: ${e.message}"
            } finally {
                _isLoading.value = false
                _loadingStep.value = ""
            }
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var scale = 1
            val maxDimension = 1500
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val largest = maxOf(options.outHeight, options.outWidth)
                scale = (largest / maxDimension.toDouble()).let { Math.ceil(it).toInt() }
            }

            val scaleOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val scaledStream = contentResolver.openInputStream(uri)
            val resultBitmap = BitmapFactory.decodeStream(scaledStream, null, scaleOptions)
            scaledStream?.close()
            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanJsonString(input: String): String {
        var cleaned = input.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper?.shutdown()
    }
}

class MainViewModelFactory(private val repository: ReportRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
