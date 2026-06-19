package com.example

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MedicalDatabase
import com.example.data.ParameterDetail
import com.example.data.ReportDetail
import com.example.data.ReportRepository
import com.example.data.SavedReport
import com.example.util.DailyHealthTips
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init Room Database and Repository
        val database = MedicalDatabase.getDatabase(applicationContext)
        val repository = ReportRepository(database.reportDao(), database.reminderDao())

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = MainViewModelFactory(repository)
                )

                // Initialize Speech Engine
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.initializeTts(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedExplainApp(viewModel)
                }
            }
        }
    }
}

// Supported reports templates metadata for reference cards
data class ReportTemplate(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val commonTests: List<String>,
    val descriptionBn: String,
    val commonTestsBn: List<String>
)

val REPORT_TEMPLATES = listOf(
    ReportTemplate(
        name = "CBC (Complete Blood Count)",
        description = "Analyzes red blood cells, white blood cells, and platelets. Checks general health and anemias.",
        icon = Icons.Default.Favorite,
        commonTests = listOf("Hemoglobin (Hb)", "RBC count", "WBC count", "Platelets"),
        descriptionBn = "লোহিত রক্তকণিকা, শ্বেত রক্তকণিকা এবং অণুচক্রিকা বিশ্লেষণ করে। সাধারণ স্বাস্থ্য এবং রক্তাল্পতা পরীক্ষা করে।",
        commonTestsBn = listOf("হিমোগ্লোবিন (Hb)", "লোহিত রক্তকণিকা", "শ্বেত রক্তকণিকা", "প্লেটলেট বা অণুচক্রিকা")
    ),
    ReportTemplate(
        name = "Blood Sugar / Diabetes",
        description = "Measures glucose levels in your blood to evaluate risk or management of diabetes.",
        icon = Icons.Default.DateRange,
        commonTests = listOf("Fasting Blood Sugar", "Post-Prandial Sugar", "HbA1c"),
        descriptionBn = "ডায়াবেটিসের ঝুঁকি বা ব্যবস্থাপনা মূল্যায়নের জন্য রক্তে গ্লুকোজের মাত্রা পরিমাপ করে।",
        commonTestsBn = listOf("খালি পেটে রক্তের শর্করা", "খাবারের পরের শর্করা", "এইচবিএ১সি (HbA1c)")
    ),
    ReportTemplate(
        name = "Thyroid Test",
        description = "Evaluates thyroid gland function. Checks for hyperthyroidism or hypothyroidism conditions.",
        icon = Icons.Default.Info,
        commonTests = listOf("TSH", "Free T3", "Free T4"),
        descriptionBn = "থাইরয়েড গ্রন্থির কার্যকারিতা মূল্যায়ন করে। হাইপারথাইরয়েডিজম বা হাইপোথাইরয়েডিজম পরীক্ষা করে।",
        commonTestsBn = listOf("টিএসএইচ (TSH)", "ফ্রি টি৩ (Free T3)", "ফ্রি টি৪ (Free T4)")
    ),
    ReportTemplate(
        name = "Kidney Function Test",
        description = "Determines how well your kidneys are filtering wastes. Crucial for metabolic monitoring.",
        icon = Icons.Default.Build,
        commonTests = listOf("Urea", "Creatinine", "eGFR", "Uric Acid"),
        descriptionBn = "আপনার কিডনি কতটা ভালোভাবে রক্ত থেকে বর্জ্য ফিল্টার করছে তা নির্ধারণ করে।",
        commonTestsBn = listOf("ইউরিয়া", "ক্রিয়েটিনিন", "ইজিএফআর (eGFR)", "ইউরিক অ্যাসিড")
    ),
    ReportTemplate(
        name = "Liver Function Test",
        description = "Measures levels of proteins, liver enzymes, and bilirubin in your blood.",
        icon = Icons.Default.Star,
        commonTests = listOf("SGPT / ALT", "SGOT / AST", "Bilirubin", "Alkaline Phosphatase"),
        descriptionBn = "রক্তে প্রোটিন, লিভার এনজাইম এবং বিলিরুবিনের মাত্রা পরিমাপ করে।",
        commonTestsBn = listOf("এসজিপিট (SGPT / ALT)", "এসজিওটি (SGOT / AST)", "বিলিরুবিন", "অ্যালকালাইন ফসফেটেজ")
    ),
    ReportTemplate(
        name = "Lipid Profile",
        description = "Checks cholesterol and triglycerides levels to evaluate cardiovascular/heart health.",
        icon = Icons.Default.ThumbUp,
        commonTests = listOf("Total Cholesterol", "HDL (Good)", "LDL (Bad)", "Triglycerides"),
        descriptionBn = "হার্টের স্বাস্থ্যের ঝুঁকি মূল্যায়নের জন্য কোলেস্টেরল এবং ট্রাইগ্লিসেরাইডের মাত্রা পরীক্ষা করে।",
        commonTestsBn = listOf("মোট কোলেস্টেরল", "এইচডিএল (ভালো কোলেস্টেরল)", "এলডিএল (খারাপ কোলেস্টেরল)", "ট্রাইগ্লিসেরাইড")
    )
)

@Composable
fun HighDensityHeader(
    viewModel: MainViewModel,
    showActions: Boolean = true,
    onHistoryClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val isBengali by viewModel.isBengali.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFFEFF6FF),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MedicalPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
            Column {
                Text(
                    text = "MediNova",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = LightTextPrimary,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (isBengali) "মেডিনোভা • আপনার স্বাস্থ্য সহযোগী" else "MediNova • HEALTH COMPANION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedicalPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }
        if (showActions) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language Pill Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    val languages = listOf(
                        "en" to "EN",
                        "bn" to "বাং",
                        "hi" to "हिं",
                        "es" to "ES",
                        "ar" to "عربي",
                        "fr" to "FR"
                    )
                    val languageCode by viewModel.languageCode.collectAsState()
                    languages.forEach { (code, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (languageCode == code) MedicalPrimary else Color.Transparent)
                                .clickable { viewModel.setLanguageCode(code) }
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (languageCode == code) Color.White else LightTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MedicalTertiary)
                        .clickable { onHistoryClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕒", fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MedicalTertiary)
                        .clickable { onProfileClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedExplainApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Daily Health, 2 = Pregnancy, 3 = BMI, 4 = History

    val currentReportDetail by viewModel.currentReport.collectAsState()
    val currentPrescription by viewModel.currentPrescription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingStep by viewModel.loadingStep.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    // File pickers and camera launchers
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeReportFile(context, uri, isPdf = true)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.analyzeReportFile(context, uri, isPdf = false)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.analyzeReportBitmap(bitmap)
        }
    }

    var selectedTemplateForHelp by remember { mutableStateOf<ReportTemplate?>(null) }

    Scaffold(
        topBar = {
            if (currentReportDetail == null && currentPrescription == null) {
                HighDensityHeader(
                    viewModel = viewModel,
                    showActions = true,
                    onHistoryClick = { currentTab = 4 },
                    onProfileClick = {
                        Toast.makeText(context, if (isBengali) "মেডিনোভা সম্পূর্ণ নিরাপদ ও লোকালি চলছে" else "MediNova is running locally and securely", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        bottomBar = {
            if (currentReportDetail == null && currentPrescription == null) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .drawBehind {
                            drawLine(
                                color = Color(0xFFEFF6FF),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        },
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = if (isBengali) "স্ক্যান হোম" else "Scan Home") },
                        label = { Text(if (isBengali) "স্ক্যান হোম" else "Scan Home", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MedicalPrimary,
                            indicatorColor = MedicalPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = if (isBengali) "দৈনিক স্বাস্থ্য" else "Daily Health") },
                        label = { Text(if (isBengali) "দৈনিক স্বাস্থ্য" else "Daily Health", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MedicalPrimary,
                            indicatorColor = MedicalPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tips_tab")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Star, contentDescription = if (isBengali) "গর্ভকালীন" else "Pregnancy") },
                        label = { Text(if (isBengali) "গর্ভকালীন" else "Pregnancy", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MedicalPrimary,
                            indicatorColor = MedicalPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_pregnancy_tab")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Place, contentDescription = if (isBengali) "ডিরেক্টরি" else "Directory") },
                        label = { Text(if (isBengali) "ডিরেক্টরি" else "Directory", fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MedicalPrimary,
                            indicatorColor = MedicalPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_directory_tab")
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.List, contentDescription = if (isBengali) "হিস্ট্রি" else "History") },
                        label = { Text(if (isBengali) "হিস্ট্রি" else "History", fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = MedicalPrimary,
                            indicatorColor = MedicalPrimary,
                            unselectedIconColor = LightTextSecondary,
                            unselectedTextColor = LightTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_history_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentReportDetail != null) {
                // If a report is loaded, show the detailed results interface fullscreen!
                ReportDetailScreen(
                    report = currentReportDetail!!,
                    viewModel = viewModel,
                    onBack = { viewModel.clearCurrentReport() }
                )
            } else if (currentPrescription != null) {
                // If a prescription is loaded, show the detailed prescription interface fullscreen!
                PrescriptionDetailScreen(
                    prescription = currentPrescription!!,
                    viewModel = viewModel,
                    onBack = { viewModel.clearCurrentPrescription() }
                )
            } else {
                // Main Switcher between different tabs
                when (currentTab) {
                    0 -> HomeScanScreen(
                        viewModel = viewModel,
                        onPdfClick = { pdfLauncher.launch("application/pdf") },
                        onImageClick = { imageLauncher.launch("image/*") },
                        onCameraClick = { cameraLauncher.launch() },
                        onTemplateClick = { selectedTemplateForHelp = it },
                        onTabChange = { currentTab = it }
                    )
                    1 -> DailyHealthScreen(viewModel)
                    2 -> PregnancyCareScreen(viewModel)
                    3 -> DirectoryScreen(viewModel)
                    4 -> HistoryScreen(viewModel)
                }
            }

            // AI OCR Processing overlay
            if (isLoading) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "MediNova Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loadingStep,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Our model is reading the text structures and explaining medical ranges. Please retain stability.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Error snackbar popup
            errorMsg?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearCurrentReport() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Info Dialog for Supported Test Parameters templates
            selectedTemplateForHelp?.let { template ->
                Dialog(onDismissRequest = { selectedTemplateForHelp = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        template.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Overview (English)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "বিবরণ (বাংলা)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = template.descriptionBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Common Parameters / সাধারণ পরীক্ষাসমূহ:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(template.commonTests.zip(template.commonTestsBn)) { (eng, ben) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "• $eng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = "• $ben", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { selectedTemplateForHelp = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Got It / বুঝেছি")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScanScreen(
    viewModel: MainViewModel,
    onPdfClick: () -> Unit,
    onImageClick: () -> Unit,
    onCameraClick: () -> Unit,
    onTemplateClick: (ReportTemplate) -> Unit,
    onTabChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val isPrescriptionMode by viewModel.isPrescriptionMode.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. Warm Greeting & Personalized Welcoming Banner
        item {
            val calendar = remember { java.util.Calendar.getInstance() }
            val hour = remember { calendar.get(java.util.Calendar.HOUR_OF_DAY) }
            val greeting = remember(hour, isBengali) {
                if (isBengali) {
                    when {
                        hour < 12 -> "শুভ সকাল ☀️"
                        hour < 16 -> "শুভ দুপুর 🌤️"
                        hour < 18 -> "শুভ বিকাল 🌅"
                        else -> "শুভ সন্ধ্যা 🌙"
                    }
                } else {
                    when {
                        hour < 12 -> "Good Morning ☀️"
                        hour < 16 -> "Good Afternoon 🌤️"
                        hour < 18 -> "Good Evening 🌅"
                        else -> "Good Evening 🌙"
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBengali) "আজ আপনাকে কীভাবে সাহায্য করতে পারি?" else "How can we assist you today?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LightTextPrimary,
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MedicalPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🩺", fontSize = 24.sp)
                    }
                }
            }
        }

        // 2. Unified AI Medical Scan Hub Card (No Scrolling required to capture/switch mode)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MedicalPrimary.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Header with pulsing status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MedicalPrimary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "এআই স্মার্ট স্ক্যানার" else "AI Medical Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = LightTextPrimary
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isBengali) "সক্রিয়" else "Live AI",
                                color = MedicalPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selection Segment Control (Double Pill layout)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(false, true).forEach { isRx ->
                            val selected = isPrescriptionMode == isRx
                            val toggleLabel = if (isRx) {
                                if (isBengali) "প্রেসক্রিপশন রিডার 📄" else "Prescription Reader 📄"
                            } else {
                                if (isBengali) "ল্যাব টেস্ট ডিক্রিপ্টার 🧪" else "Lab Decrypter 🧪"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MedicalPrimary else Color.Transparent)
                                    .clickable { viewModel.setPrescriptionMode(isRx) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = toggleLabel,
                                    color = if (selected) Color.White else Color(0xFF475569),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtitle description based on mode
                    val modeExplanation = if (isPrescriptionMode) {
                        if (isBengali) "ডাক্তারি প্রেসক্রিপশন স্ক্যান বা আপলোড করুন। এআই স্বয়ংক্রিয়ভাবে ঔষধের নাম, ডোজ, খাওয়ার নিয়ম ও নির্দেশনা বাংলায় বুঝিয়ে দেবে।" 
                        else "Scan or upload prescriptions. AI extracts medicine names, schedules, dosing frequency, and wellness guidelines dynamically."
                    } else {
                        if (isBengali) "ডাক্তারি ল্যাব রিপোর্টের ছবি বা পিডিএফ দিন। কঠিন সব মেডিকেল প্যারামিটার ও পরিমাপের সহজ অর্থ এআই বুঝিয়ে দেবে।" 
                        else "Upload blood test, diabetes panel, or hormone reports. AI simplifies complex parameters and flags high/low ranges instantly."
                    }
                    
                    Text(
                        text = modeExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightTextSecondary,
                        modifier = Modifier.align(Alignment.Start),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // PRIMARY camera action: full width, large accent color
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clickable { onCameraClick() }
                            .testTag("action_scan_camera"),
                        colors = CardDefaults.cardColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📷", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPrescriptionMode) {
                                    if (isBengali) "প্রেসক্রিপশন ক্যামেরা স্ক্যান" else "Snap Prescription Photo"
                                } else {
                                    if (isBengali) "ল্যাব রিপোর্ট ক্যামেরা স্ক্যান" else "Snap Lab Report Photo"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary upload buttons row (Image, PDF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Upload Image
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { onImageClick() }
                                .testTag("action_upload_image"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MedicalPrimary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🖼️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "ছবি আপলোড" else "Upload Image",
                                    color = MedicalPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Upload PDF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { onPdfClick() }
                                .testTag("action_upload_pdf"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MedicalPrimary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📄", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "পিডিএফ আপলোড" else "Upload PDF",
                                    color = MedicalPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Department Core Services Grid (Navigates to proper bottom tabs instantly)
        item {
            Text(
                text = if (isBengali) "অন্যান্য স্বাস্থ্য সেবা সমূহ" else "Explore Core Health Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 10.dp, start = 4.dp),
                color = LightTextPrimary
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val features = listOf(
                    Triple("💧", if (isBengali) "দৈনিক স্বাস্থ্য ডায়েরি" else "Daily Wellness Diary", if (isBengali) "পানি পান ও লক্ষণ ট্র্যাকিং" else "Water tracker, tips & log health symptoms"),
                    Triple("🤰", if (isBengali) "গর্ভকালীন এআই সহযোগী" else "Pregnancy Companion", if (isBengali) "সাপ্তাহিক গাইডলাইন ও কিক কাউন্টার" else "Weekly progress & kick tracking"),
                    Triple("⚖️", if (isBengali) "বিএমআই ও ওজন ড্যাশবোর্ড" else "BMI & Weight Dashboard", if (isBengali) "ওজন নিয়ন্ত্রণ ও স্বাস্থ্যকর মানদণ্ড" else "Log physical stats & receive AI guides"),
                    Triple("🕒", if (isBengali) "আমার বিশ্লেষণ ইতিহাস" else "Saved Reports History", if (isBengali) "আগের সমস্ত এআই স্ক্যান ও ডেসক্রিপশন" else "Access older digital records & reports")
                )

                val colors = listOf(
                    Color(0xFF3B82F6), // Vibrant Blue
                    Color(0xFFEC4899), // Soft Pink
                    Color(0xFF10B981), // Emerald Green
                    Color(0xFF64748B)  // Slate Grey
                )

                // 2x2 grid rows
                for (i in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (j in 0 until 2) {
                            val index = i * 2 + j
                            val feat = features[index]
                            val accentColor = colors[index]
                            val tabIndex = index + 1 // tab 1 = daily wellness, 2 = pregnancy, 3 = bmi, 4 = history

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onTabChange(tabIndex) }
                                    .testTag("home_service_card_$index"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(accentColor.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(feat.first, fontSize = 20.sp)
                                        }
                                        Text("➡️", fontSize = 12.sp, color = LightTextSecondary.copy(alpha = 0.5f))
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = feat.second,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LightTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = feat.third,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LightTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Daily Health Tips Interactive Promotion
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val defaultTip = remember { com.example.util.DailyHealthTips.getTipForToday() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onTabChange(1) } // Switch to daily wellness tab for tips
                    .testTag("daily_health_tips_promo_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)), // soft pastel green
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF047857).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(defaultTip.icon, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBengali) "আজকের বিশেষ স্বাস্থ্য পরামর্শ" else "Today's Medical Tip & Wellness Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = defaultTip.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("💚", fontSize = 16.sp)
                }
            }
        }

        // 5. Supported Lab Reference Ranges listing (renders only when lab mode is selected)
        if (!isPrescriptionMode) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val refTitle = if (isBengali) "সমর্থিত ল্যাব পরীক্ষা ও সাধারণ সীমা" else "Supported Lab Ranges Reference"
                Text(
                    text = refTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    color = LightTextPrimary
                )
                val refSubtitle = if (isBengali) "পরীক্ষার সাধারণ সীমা ও বিবরণী বিস্তারিত দেখতে যেকোনো টেস্টে ক্লিক করুন:" else "Click any category below to inspect typical normal reference values:"
                Text(
                    text = refSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LightTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, start = 4.dp)
                )
            }

            items(REPORT_TEMPLATES) { template ->
                Card(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(vertical = 4.dp)
                         .clickable { onTemplateClick(template) },
                     colors = CardDefaults.cardColors(containerColor = Color.White),
                     shape = RoundedCornerShape(16.dp),
                     border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MedicalPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                template.icon,
                                contentDescription = null,
                                tint = MedicalPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightTextPrimary
                            )
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = LightTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "View Details",
                            tint = LightTextSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 6. Medical Safety Disclaimer / Footnote notice (low profile empathy item)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), // quiet light red background
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFEE2E2))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                    val disclaimerText = if (isBengali) {
                        "সতর্কতা: এই এআই অ্যাপ্লিকেশনটি শুধুমাত্র শিক্ষামূলক উদ্দেশ্যে তৈরি। এটি কোনো রোগ বা প্রেসক্রিপশনের প্রকৃত চিকিৎসা পরামর্শ, নির্ণয় বা প্রতিস্থাপন করে না।"
                    } else {
                        "Disclaimer: This AI system is for educational support purposes only. It does not replace professional medical diagnosis, prescription advice, or clinician oversight."
                    }
                    Text(
                        text = disclaimerText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF991B1B), // dark red text
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DirectoryScreen(viewModel: MainViewModel) {
    val isBengali by viewModel.isBengali.collectAsState()
    var selectedTabState by remember { mutableStateOf(0) } // 0 = Hospitals, 1 = Doctors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Very light cool slate background
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Upper Header card with beautiful gradient visual
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MedicalPrimary),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗺️", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBengali) "মেডিকেল ডিরেক্টরি" else "Medical Directory",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isBengali) "নিকটস্থ বিশ্বমানের সেবা কেন্দ্র ও বিশেষজ্ঞ গাইড" else "Trusted health companions & accredited facilities",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Segmented tab switch row matching Material 3 specifications but highly customized
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hospitals Tab Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        color = if (selectedTabState == 0) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedTabState = 0 }
                    .testTag("directory_tab_hospitals"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏥", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBengali) "হাসপাতাল" else "Hospitals",
                        color = if (selectedTabState == 0) MedicalPrimary else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Doctors Tab Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        color = if (selectedTabState == 1) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedTabState = 1 }
                    .testTag("directory_tab_doctors"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👨‍⚕️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBengali) "বিশেষজ্ঞ ডাক্তার" else "Doctors",
                        color = if (selectedTabState == 1) MedicalPrimary else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Render content based on selected tab
        if (selectedTabState == 0) {
            HospitalDirectorySection(isBengali = isBengali)
        } else {
            DoctorDirectorySection(isBengali = isBengali)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

data class TopDoctor(
    val countryCode: String,
    val nameEn: String,
    val nameBn: String,
    val specialtyEn: String,
    val specialtyBn: String,
    val degreeEn: String,
    val degreeBn: String,
    val hospitalEn: String,
    val hospitalBn: String,
    val rating: Double,
    val feeEn: String,
    val feeBn: String,
    val hoursEn: String,
    val hoursBn: String,
    val phoneEn: String,
    val phoneBn: String,
    val phoneRaw: String,
    val experienceEn: String,
    val experienceBn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDirectorySection(isBengali: Boolean) {
    var selectedCountry by remember { mutableStateOf("BD") }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val countries = listOf(
        Triple("BD", "🇧🇩 বাংলাদেশ", "🇧🇩 Bangladesh"),
        Triple("IN", "🇮🇳 ভারত", "🇮🇳 India"),
        Triple("SG", "🇸🇬 সিঙ্গাপুর", "🇸🇬 Singapore"),
        Triple("SA", "🇸🇦 সৌদি আরব", "🇸🇦 Saudi Arabia"),
        Triple("UK", "🇬🇧 যুক্তরাজ্য", "🇬🇧 United Kingdom")
    )
    
    val doctorsList = remember {
        listOf(
            TopDoctor(
                countryCode = "BD",
                nameEn = "Prof. Dr. M. Mafizur Rahman",
                nameBn = "অধ্যাপক ড. এম. মফিজুর রহমান",
                specialtyEn = "Cardiac Surgery & Cardiology",
                specialtyBn = "কার্ডিয়াক সার্জারি ও হৃদরোগ বিশেষজ্ঞ",
                degreeEn = "MBBS, FCPS (Surgery), MS (Cardio-thoracic), FICS",
                degreeBn = "এমবিবিএস, এফসিপিএস (সার্জারি), এমএস (কার্ডিও-থোরাসিক), এফআইসিএস",
                hospitalEn = "Evercare Hospital Dhaka",
                hospitalBn = "এভারকেয়ার হাসপাতাল ঢাকা",
                rating = 4.9,
                feeEn = "BDT 1,500",
                feeBn = "১৫০০ টাকা",
                hoursEn = "Sat - Thu (5:00 PM - 9:00 PM)",
                hoursBn = "শনি - বৃহস্পতি (বিকেল ৫টা - রাত ৯টা)",
                phoneEn = "10678 or +8809666710678",
                phoneBn = "১০৬৭৮ অথবা +৮৮০৯৬৬৬৭১০৬৭৮",
                phoneRaw = "10678",
                experienceEn = "25+ Years Experience",
                experienceBn = "২৫+ বছরের অধিক অভিজ্ঞতা"
            ),
            TopDoctor(
                countryCode = "BD",
                nameEn = "Dr. Shahnaz Quadery",
                nameBn = "ডাঃ শাহনাজ কাদেরী",
                specialtyEn = "Gynecology & Obstetrics",
                specialtyBn = "স্ত্রীরোগ ও প্রসূতি বিশেষজ্ঞ",
                degreeEn = "MBBS, FCPS, DGO",
                degreeBn = "এমবিবিএস, এফসিপিএস, ডিজিও",
                hospitalEn = "Square Hospitals Limited",
                hospitalBn = "স্কয়ার হাসপাতাল লিমিটেড",
                rating = 4.8,
                feeEn = "BDT 1,200",
                feeBn = "১২০০ টাকা",
                hoursEn = "Sat, Mon, Wed (4:00 PM - 8:00 PM)",
                hoursBn = "শনি, সোম, বুধ (বিকেল ৪টা - রাত ৮টা)",
                phoneEn = "10616 or +8802222241555",
                phoneBn = "১০৬১৬ অথবা +৮৮০২২২২২৪১৫৫৫",
                phoneRaw = "10616",
                experienceEn = "18+ Years Experience",
                experienceBn = "১৮+ বছরের অধিক অভিজ্ঞতা"
            ),
            TopDoctor(
                countryCode = "BD",
                nameEn = "Prof. Dr. Pran Gopal Datta",
                nameBn = "অধ্যাপক ডাঃ প্রাণ গোপাল দত্ত",
                specialtyEn = "ENT Specialist & Head Surgeon",
                specialtyBn = "কান, নাক ও গলা বিশেষজ্ঞ",
                degreeEn = "MBBS, PhD, FRCS",
                degreeBn = "এমবিবিএস, পিএইচডি, এফআরসিএস",
                hospitalEn = "Green Life Medical College & Hospital",
                hospitalBn = "গ্রীন লাইফ হাসপাতাল",
                rating = 4.9,
                feeEn = "BDT 2,000",
                feeBn = "২০০০ টাকা",
                hoursEn = "Sun, Tue, Thu (3:00 PM - 7:00 PM)",
                hoursBn = "রবি, মঙ্গল, বৃহস্পতি (দুপুর ৩টা - রাত ৭টা)",
                phoneEn = "+88029612345",
                phoneBn = "+৮৮০২৯৬১২৩৪৫",
                phoneRaw = "+88029612345",
                experienceEn = "30+ Years Experience",
                experienceBn = "৩০+ বছরের অধিক অভিজ্ঞতা"
            ),
            TopDoctor(
                countryCode = "IN",
                nameEn = "Dr. Devi Prasad Shetty",
                nameBn = "ডাঃ দেবী প্রসাদ শেঠি",
                specialtyEn = "Cardiac Surgery (Heart Bypass & Valve)",
                specialtyBn = "কার্ডিয়াক সার্জারি বিশেষজ্ঞ",
                degreeEn = "MS, FRCS (England), World-renowned Heart Surgeon",
                degreeBn = "এমএস, এফআরসিএস (ইংল্যান্ড), বিশ্বখ্যাত হৃদরোগ সার্জন",
                hospitalEn = "Narayana Health, Bangalore",
                hospitalBn = "নারায়ণ হেলথ, ব্যাঙ্গালোর",
                rating = 5.0,
                feeEn = "INR 2,500",
                feeBn = "২৫০০ রুপি",
                hoursEn = "Mon - Fri (10:00 AM - 4:00 PM)",
                hoursBn = "সোম - শুক্র (সকাল ১০টা - বিকেল ৪টা)",
                phoneEn = "+91-80-7122-2222",
                phoneBn = "+৯১-৮০-৭১২২-২২২২",
                phoneRaw = "+918071222222",
                experienceEn = "35+ Years Worldwide",
                experienceBn = "৩৫+ বছরের বিশ্বব্যাপী অভিজ্ঞতা"
            ),
            TopDoctor(
                countryCode = "IN",
                nameEn = "Dr. Sandeep Vaishya",
                nameBn = "ড. সন্দীপ বৈশ্য",
                specialtyEn = "Neuro-Surgery & Spine Specialist",
                specialtyBn = "নিউরো-সার্জারি ও স্পাইন বিশেষজ্ঞ",
                degreeEn = "MBBS, MS, MCh (Neurosurgery)",
                degreeBn = "এমবিবিএস, এমএস, এমসিএইচ (নিউরোসার্জারি)",
                hospitalEn = "Fortis Memorial Research Institute, Gurugram",
                hospitalBn = "ফোর্টিস মেমোরিয়াল রিসার্চ ইনস্টিটিউট, গুরুগ্রাম",
                rating = 4.9,
                feeEn = "INR 2,000",
                feeBn = "২০০০ রুপি",
                hoursEn = "Mon - Sat (11:00 AM - 5:00 PM)",
                hoursBn = "সোম - শনি (সকাল ১১টা - বিকেল ৫টা)",
                phoneEn = "+91-11-4277-6222",
                phoneBn = "+৯১-১১-৪২৭থ-৬২২২",
                phoneRaw = "+911142776222",
                experienceEn = "22+ Years Professional Experience",
                experienceBn = "২২+ বছরের পেশাগত অভিজ্ঞতা"
            ),
            TopDoctor(
                countryCode = "IN",
                nameEn = "Dr. Suresh H. Advani",
                nameBn = "ডাঃ সুরেশ এইচ আদভানি",
                specialtyEn = "Medical Oncology & Cancer Therapy",
                specialtyBn = "ক্যান্সার ও অনকোলজি বিশেষজ্ঞ",
                degreeEn = "MBBS, MD, FICP, Padma Bhushan Awardee",
                degreeBn = "এমবিবিএস, এমডি, এফআইসিপি, পদ্মভূষণ পদকপ্রাপ্ত",
                hospitalEn = "Jaslok Hospital, Mumbai",
                hospitalBn = "জাসলক হাসপাতাল, মুম্বাই",
                rating = 4.9,
                feeEn = "INR 3,000",
                feeBn = "৩০০০ রুপি",
                hoursEn = "Tue - Fri (1:00 PM - 5:00 PM)",
                hoursBn = "মঙ্গল - শুক্র (দুপুর ১টা - বিকেল ৫টা)",
                phoneEn = "+91-22-6657-3333",
                phoneBn = "+৯১-২২-৬৬৫৭-৩৩৩৩",
                phoneRaw = "+912266573333",
                experienceEn = "40+ Years Oncology Pioneer",
                experienceBn = "৪০+ বছরের ক্যান্সার গবেষণা ও চিকিৎসা"
            ),
            TopDoctor(
                countryCode = "SG",
                nameEn = "Dr. Yeo Khung Keong",
                nameBn = "ডক্টটর ইয়ো খুং কিয়ং",
                specialtyEn = "Interventional Cardiology & Coronary Care",
                specialtyBn = "হার্ট ও ইন্টারভেনশনাল ক্রনিক কেয়ার",
                degreeEn = "MBBS, FRCP (Edinburgh), FAMS (Cardiology)",
                degreeBn = "এমবিবিএস, এফসিপিএস (এডিনবরা), এফএএমএস (কার্ডিও)",
                hospitalEn = "Singapore General Hospital (SGH)",
                hospitalBn = "সিঙ্গাপুর জেনারেল হাসপাতাল - SGH",
                rating = 4.8,
                feeEn = "SGD 180",
                feeBn = "১৮০ সিঙ্গাপুরি ডলার",
                hoursEn = "Mon, Wed, Fri (9:00 AM - 1:00 PM)",
                hoursBn = "সোম, বুধ, শুক্র (সকাল ৯টা - দুপুর ১টা)",
                phoneEn = "+65-6222-3322",
                phoneBn = "+৬৫-৬২২২-৩৩২২",
                phoneRaw = "+6562223322",
                experienceEn = "20+ Years Excellence",
                experienceBn = "২০+ বছরের চিকিৎসা উৎকর্ষ"
            ),
            TopDoctor(
                countryCode = "SG",
                nameEn = "Dr. Tan Seng Hoe",
                nameBn = "ডাঃ তান সেং হো",
                specialtyEn = "Gastroenterology, Liver & Diagnostic Endoscopy",
                specialtyBn = "লিভার ও সংক্রামক পাকস্থলী বিশেষজ্ঞ",
                degreeEn = "MBBS, MRCP (UK), FAMS",
                degreeBn = "এমবিবিএস, এমআরসিপি (ইউকে), এফএএমএস",
                hospitalEn = "Mount Elizabeth Hospital (Orchard)",
                hospitalBn = "মাউন্ট এলিজাবেথ হাসপাতাল (অর্চার্ড)",
                rating = 4.7,
                feeEn = "SGD 220",
                feeBn = "২২০ সিঙ্গাপুরি ডলার",
                hoursEn = "Tue, Thu (10:00 AM - 3:00 PM)",
                hoursBn = "মঙ্গল, বৃহস্পতি (সকাল ১০টা - বিকেল ৩টা)",
                phoneEn = "+65-6738-1111",
                phoneBn = "+৬৫-৬৭৩৮-১১১১",
                phoneRaw = "+6567381111",
                experienceEn = "15+ Years Clinical Expert",
                experienceBn = "১৫+ বছরের ক্লিনিক্যাল বিশেষজ্ঞ"
            ),
            TopDoctor(
                countryCode = "SA",
                nameEn = "Dr. Abdullah Al Rabeeah",
                nameBn = "ডক্টটর আবদুল্লাহ আল রাবিয়াহ",
                specialtyEn = "Pediatric Surgery & Conjoined Twins Reconstruction",
                specialtyBn = "সম্পূর্ণ পেডিয়াট্রিক ও জোড়া শিশু পৃথকীকরণ সার্জন",
                degreeEn = "FRCS (Canada), Pioneer Pediatric Specialist",
                degreeBn = "এফআরসিএস (কানাডা), বিশ্বখ্যাত পেডিয়াট্রিক সার্জন",
                hospitalEn = "King Faisal Specialist Hospital",
                hospitalBn = "কিং ফয়সাল স্পেশালিস্ট হাসপাতাল, রিয়াদ",
                rating = 5.0,
                feeEn = "SAR 400",
                feeBn = "৪০৪ سعودی রিয়াল",
                hoursEn = "Sun, Tue (9:00 AM - 12:00 PM)",
                hoursBn = "রবি, মঙ্গল (সকাল ৯টা - দুপুর ১২টা)",
                phoneEn = "+966-11-464-7272",
                phoneBn = "+৯৬৬-১১-৪৬৪-৭২৭২",
                phoneRaw = "+966114647272",
                experienceEn = "30+ Years Global Legend",
                experienceBn = "৩০+ বছরের চিকিৎসা কীর্তি"
            ),
            TopDoctor(
                countryCode = "UK",
                nameEn = "Dr. Richard Howard",
                nameBn = "ডাঃ রিচার্ড হাওয়ার্ড",
                specialtyEn = "Pediatric Oncology & Bone Marrow Therapy",
                specialtyBn = "শিশু অনকোলজি ও রেয়ার ব্লাড ডিজিজ",
                degreeEn = "MBChB, MRCPCH, PhD (Pediatric Cancers)",
                degreeBn = "এমবিসিএইচবি, এমআরসিপিসিএইচ, পিএইচডি (পেডিয়াট্রিক ক্যান্সার)",
                hospitalEn = "Great Ormond Street Hospital (GOSH)",
                hospitalBn = "গ্রেট অরমন্ড স্ট্রিট হাসপাতাল - GOSH",
                rating = 4.9,
                feeEn = "GBP 150",
                feeBn = "১৫০ ইউকে পাউন্ড",
                hoursEn = "Mon - Thu (9:00 AM - 3:00 PM)",
                hoursBn = "সোম - বৃহস্পতি (সকাল ৯টা - বিকেল ৩টা)",
                phoneEn = "+44-20-7405-9200",
                phoneBn = "+৪৪-২০-৭৪০৫-৯২০০",
                phoneRaw = "+442074059200",
                experienceEn = "20+ Years Research Leader",
                experienceBn = "২০+ বছরের শিশু ক্যান্সার গবেষণা"
            )
        )
    }
    
    val filteredDoctors = remember(selectedCountry, searchQuery) {
        doctorsList.filter { doctor ->
            val matchesCountry = doctor.countryCode == selectedCountry
            val matchesSearch = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                doctor.nameEn.lowercase().contains(q) ||
                doctor.nameBn.lowercase().contains(q) ||
                doctor.specialtyEn.lowercase().contains(q) ||
                doctor.specialtyBn.lowercase().contains(q) ||
                doctor.hospitalEn.lowercase().contains(q) ||
                doctor.hospitalBn.lowercase().contains(q) ||
                doctor.degreeEn.lowercase().contains(q) ||
                doctor.degreeBn.lowercase().contains(q)
            }
            matchesCountry && matchesSearch
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search & Instruction
        Text(
            text = if (isBengali) "📍 দেশের ও আন্তর্জাতিক খ্যাতনামা চিকিৎসক গাইড" else "📍 Top Accredited Specialist Doctors",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = LightTextPrimary,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        
        Text(
            text = if (isBengali) "জটিল বা বিশেষ চিকিৎসাসেবা নেওয়ার জন্য স্বনামধন্য ও সনদপ্রাপ্ত বিশেষজ্ঞ ডাক্তারদের প্রোফাইল:" 
                   else "Verified profiles of board-certified clinical experts and consulting specialists:",
            style = MaterialTheme.typography.bodySmall,
            color = LightTextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        // Country Selector Row (Horizontal Chip Scroll)
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            countries.forEach { (code, labelBn, labelEn) ->
                val label = if (isBengali) labelBn else labelEn
                val isSelected = selectedCountry == code
                
                Card(
                    modifier = Modifier
                        .clickable { selectedCountry = code }
                        .testTag("doctor_country_tab_$code"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MedicalPrimary else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) MedicalPrimary else Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // Search Input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isBengali) "ডাক্তারের নাম বা রোগ বিভাগ দিয়ে খুঁজুন..." else "Search by doctor or clinical department...",
                    fontSize = 12.sp,
                    color = LightTextSecondary.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("doctor_search_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Text("🔍", modifier = Modifier.padding(start = 8.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("❌", fontSize = 10.sp)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MedicalPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )
        
        // Dynamic Listings
        if (filteredDoctors.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBengali) "কোনো বিশেষজ্ঞ ডাক্তার পাওয়া যায়নি!" else "No matching doctors found!",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBengali) "অনুগ্রহ করে রোগের ধরন বা ডাক্তারের নাম পুনরায় ট্রাই করুন।" else "Try altering specialty labels or surgeon names.",
                        color = LightTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredDoctors.forEach { doctor ->
                    val docName = if (isBengali) doctor.nameBn else doctor.nameEn
                    val docSpec = if (isBengali) doctor.specialtyBn else doctor.specialtyEn
                    val docDegree = if (isBengali) doctor.degreeBn else doctor.degreeEn
                    val docHosp = if (isBengali) doctor.hospitalBn else doctor.hospitalEn
                    val docExp = if (isBengali) doctor.experienceBn else doctor.experienceEn
                    val docFee = if (isBengali) doctor.feeBn else doctor.feeEn
                    val docHours = if (isBengali) doctor.hoursBn else doctor.hoursEn
                    val docPhoneStr = if (isBengali) doctor.phoneBn else doctor.phoneEn
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doctor_item_${doctor.countryCode}_${doctor.nameEn.replace(" ", "_").lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            // Header Row: Doctor Name & Rating
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MedicalPrimary.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🩺", fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = docName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = LightTextPrimary
                                        )
                                        Text(
                                            text = docDegree,
                                            fontSize = 10.sp,
                                            color = LightTextSecondary,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("⭐", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = doctor.rating.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Department Tag
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(MedicalPrimary.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = docSpec,
                                        color = MedicalPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = docExp,
                                        color = Color(0xFF475569),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Affiliation / Chamber info
                            Row(verticalAlignment = Alignment.Top) {
                                Text("🏥", fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = docHosp,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Visiting Hours
                            Row(verticalAlignment = Alignment.Top) {
                                Text("⏰", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "কন্সাল্টেশন সময়: $docHours" else "Visiting Hours: $docHours",
                                    fontSize = 11.sp,
                                    color = LightTextSecondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Consultation Fees
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💵", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "সাক্ষাৎ ফি: $docFee" else "Consultation Fee: $docFee",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Call Book appointment & Copy address actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + doctor.phoneRaw))
                                        try {
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (isBengali) "সিরিয়াল বুকিং কল ব্যর্থ হয়েছে" else "Unable to launch serial booking dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📞", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBengali) "সিরিয়াল বুকিং" else "Book Consulting",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(doctor.nameEn + " - " + doctor.hospitalEn))
                                        Toast.makeText(context, if (isBengali) "ডাক্তার ও চেম্বারের নাম কপি করা হয়েছে" else "Doctor information copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    border = BorderStroke(1.dp, MedicalPrimary.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📋", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBengali) "তথ্য কপি করুন" else "Copy Info",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MedicalPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TopHospital(
    val countryCode: String,
    val nameEn: String,
    val nameBn: String,
    val rating: Double,
    val typeEn: String,
    val typeBn: String,
    val locationEn: String,
    val locationBn: String,
    val hotlineEn: String,
    val hotlineBn: String,
    val hotlineRaw: String,
    val specialEn: String,
    val specialBn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDirectorySection(isBengali: Boolean) {
    var selectedCountry by remember { mutableStateOf("BD") }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val countries = listOf(
        Triple("BD", "🇧🇩 বাংলাদেশ", "🇧🇩 Bangladesh"),
        Triple("IN", "🇮🇳 ভারত", "🇮🇳 India"),
        Triple("SG", "🇸🇬 সিঙ্গাপুর", "🇸🇬 Singapore"),
        Triple("SA", "🇸🇦 সৌদি আরব", "🇸🇦 Saudi Arabia"),
        Triple("UK", "🇬🇧 যুক্তরাজ্য", "🇬🇧 United Kingdom")
    )
    
    val hospitalsList = remember {
        listOf(
            TopHospital(
                countryCode = "BD",
                nameEn = "Evercare Hospital Dhaka",
                nameBn = "এভারকেয়ার হাসপাতাল ঢাকা",
                rating = 4.8,
                typeEn = "JCI Accredited Multi-specialty",
                typeBn = "জেসিআই স্বীকৃত মাল্টি-স্পেশালিটি",
                locationEn = "Plot 81, Block E, Bashundhara R/A, Dhaka 1229",
                locationBn = "প্লট ৮১, ব্লগ ই, বসুন্ধরা আ/এ, ঢাকা ১২২৯",
                hotlineEn = "10678 or +8809666710678",
                hotlineBn = "১০৬৭৮ অথবা +৮৮০৯৬৬৬৭১০৬৭৮",
                hotlineRaw = "10678",
                specialEn = "Cardiology, Oncology, Orthopedics, Bone Marrow Transplant",
                specialBn = "কার্ডিওলজি, অনকোলজি, অর্থোপেডিক্স, বোন ম্যারো ট্রান্সপ্ল্যান্ট"
            ),
            TopHospital(
                countryCode = "BD",
                nameEn = "Square Hospitals Limited",
                nameBn = "স্কয়ার হাসপাতাল লিমিটেড",
                rating = 4.7,
                typeEn = "Tertiary Care & Emergency Hub",
                typeBn = "টারশিয়ারি কেয়ার ও ইমার্জেন্সি হাব",
                locationEn = "18/F, Bir Uttam Qazi Nuruzzaman Sarak, Panthapath, Dhaka 1205",
                locationBn = "১৮/এফ, বীর উত্তম কাজী নুরুজ্জামান সড়ক, পান্থপথ, ঢাকা ১২০৫",
                hotlineEn = "10616 or +8802222241555",
                hotlineBn = "১০৬১৬ অথবা +৮৮০২২২২২৪১৫৫৫",
                hotlineRaw = "10616",
                specialEn = "Critical Care, Cardiac Surgery, Neonatal ICU, Neurology",
                specialBn = "ক্রিটিক্যাল কেয়ার, কার্ডিয়াক সার্জারি, নিওনেটাল আইসিইউ, নিউরোলজি"
            ),
            TopHospital(
                countryCode = "BD",
                nameEn = "United Hospital Limited",
                nameBn = "ইউনাইটেড হাসপাতাল লিমিটেড",
                rating = 4.6,
                typeEn = "Super-Specialty Cardiac Care",
                typeBn = "সুপার-স্পেশালিটি কার্ডিয়াক সেন্টার",
                locationEn = "Plot 15, Road 71, Gulshan 2, Dhaka 1212",
                locationBn = "প্লট ১৫, রোড ৭১, গুলশান ২, ঢাকা ১২১২",
                hotlineEn = "10666 or +8809666710666",
                hotlineBn = "১০৬৬৬ অথবা +৮৮০৯৬৬৬৭১০৬৬৬",
                hotlineRaw = "10666",
                specialEn = "Interventional Cardiology, Renal Care, Neuro-Surgery, Radiology",
                specialBn = "ইন্টারভেনশনাল কার্ডিওলজি, রেনাল কেয়ার, নিউরো-সার্জারি, রেডিওলজি"
            ),
            TopHospital(
                countryCode = "BD",
                nameEn = "Labaid Specialized Hospital",
                nameBn = "ল্যাবএইড স্পেশালাইজড হাসপাতাল",
                rating = 4.5,
                typeEn = "Cardiology & Gastro Center",
                typeBn = "কার্ডিওলজি ও গ্যাস্ট্রো সেন্টার",
                locationEn = "House 6, Road 4, Dhanmondi, Dhaka 1205",
                locationBn = "বাড়ি ৬, রোড ৪, ধানমন্ডি, ঢাকা ১২০৫",
                hotlineEn = "10606 or +8802223361001",
                hotlineBn = "১০৬০৬ অথবা +৮৮০২২২৩৩৬১০০১",
                hotlineRaw = "10606",
                specialEn = "Cardiovascular Disease, Kidney Disease, Liver and Gastro-enterology",
                specialBn = "কার্ডিওভাসকুলার ডিজিজ, কিডনি রোগ, লিভার ও গ্যাস্ট্রো-এন্টারোলজি"
            ),
            TopHospital(
                countryCode = "IN",
                nameEn = "Apollo Hospitals Greams Road, Chennai",
                nameBn = "অ্যাপোলো হসপিটালস গ্রীমস রোড, চেন্নাই",
                rating = 4.9,
                typeEn = "Asia's Leading Transplant & Cardiac Hub",
                typeBn = "এশিয়ার শীর্ষস্থানীয় ট্রান্সপ্ল্যান্ট ও হার্ট হাব",
                locationEn = "21, Greams Lane, off Greams Road, Chennai, Tamil Nadu 600006",
                locationBn = "২১, গ্রীমস লেন, গ্রীমস রোড সংলগ্ন, চেন্নাই, তামিলনাড়ু ৬০০০০৬",
                hotlineEn = "1860-500-1066 or +91-44-2829-0200",
                hotlineBn = "১৮৬০-৫০০-১০৬৬ অথবা +৯১-৪৪-২৮২৯-০২০০",
                hotlineRaw = "18605001066",
                specialEn = "Organ Transplant, Complex Cardiac Bypass, Robotic Knee Replacement, Oncology",
                specialBn = "অঙ্গ প্রতিস্থাপন, জটিল কার্ডিয়াক বাইপাস, রোবোটিক হাঁটু প্রতিস্থাপন, ক্যান্সার চিকিৎসা"
            ),
            TopHospital(
                countryCode = "IN",
                nameEn = "Medanta - The Medicity, Gurugram",
                nameBn = "মেদান্ত - দ্য মেডিসিটি, গুরুগ্রাম",
                rating = 4.8,
                typeEn = "Super-Specialty Comprehensive Research Hospital",
                typeBn = "সুপার-স্পেশালিটি সমন্বিত গবেষণা হাসপাতাল",
                locationEn = "CH Baktawar Singh Road, Sector 38, Gurugram, Delhi-NCR, Haryana 122001",
                locationBn = "সিএইচ বখতাওয়ার সিং রোড, সেক্টর ৩৮, গুরুগ্রাম, দিল্লি-এনসিআর, হরিয়ানা ১২২০০১",
                hotlineEn = "+91-124-4141414",
                hotlineBn = "+৯১-১২৪-৪১৪১৪১৪",
                hotlineRaw = "+911244141414",
                specialEn = "Liver & Kidney Transplant, Advanced Neurology, Liver Surgery, Heart Valves",
                specialBn = "লিভার ও কিডনি প্রতিস্থাপন, উন্নত নিউরোলজি, লিভার সার্জারি, হার্ট ভালভ"
            ),
            TopHospital(
                countryCode = "IN",
                nameEn = "Fortis Memorial Research Institute, Gurugram",
                nameBn = "ফোর্টিস মেমোরিয়াল রিসার্চ ইনস্টিটিউট, গুরুগ্রাম",
                rating = 4.7,
                typeEn = "Next-Gen Smart Tertiary Care",
                typeBn = "নেক্সট-জেন স্মার্ট টারশিয়ারি কেয়ার",
                locationEn = "Sector 44, opposite HUDA City Centre, Gurugram, Haryana 122002",
                locationBn = "সেক্টর ৪৪, হুডা সিটি সেন্টারের বিপরীতে, গুরুগ্রাম, হরিয়ানা ১২২০০২",
                hotlineEn = "+91-11-4277-6222",
                hotlineBn = "+৯১-১১-৪২৭থ-৬২২২",
                hotlineRaw = "+911142776222",
                specialEn = "Robotic Oncology with Cyber-Knife, Neuromodulation, Joint Replacements",
                specialBn = "সাইবার-নাইফ দ্বারা রোবোটিক অনকোলজি, নিউরোমডুলেশন, জয়েন্ট প্রতিস্থাপন"
            ),
            TopHospital(
                countryCode = "SG",
                nameEn = "Singapore General Hospital (SGH)",
                nameBn = "সিঙ্গাপুর জেনারেল হাসপাতাল (SGH)",
                rating = 4.9,
                typeEn = "World-Class Academic Research Medical Center",
                typeBn = "বিশ্বমানের একাডেমিক গবেষণা চিকিৎসা কেন্দ্র",
                locationEn = "Outram Rd, Singapore 169608",
                locationBn = "আউট্রাম রোড, সিঙ্গাপুর ১৬৯৬০৮",
                hotlineEn = "+65-6222-3322",
                hotlineBn = "+৬৫-৬২২২-৩৩২২",
                hotlineRaw = "+6562223322",
                specialEn = "Internal Medicine, Critical Burns, Immunology, Advanced Vascular Surgery",
                specialBn = "ইন্টারনাল মেডিসিন, আশঙ্কাজনক দগ্ধ রোগী সেবা, ইমিউনোলজি, উন্নত ভাস্কুলার সার্জারি"
            ),
            TopHospital(
                countryCode = "SG",
                nameEn = "Mount Elizabeth Hospital (Orchard)",
                nameBn = "মাউন্ট এলিজাবেথ হাসপাতাল (অর্চার্ড)",
                rating = 4.8,
                typeEn = "Elite Private Tertiary Medical Hub",
                typeBn = "এলিট বেসরকারি টারশিয়ারি চিকিৎসা হাব",
                locationEn = "3 Mount Elizabeth, Singapore 228510",
                locationBn = "৩ মাউন্ট এলিজাবেথ, সিঙ্গাপুর ২২৮৫১০",
                hotlineEn = "+65-6738-1111",
                hotlineBn = "+৬৫-৬৭৩৮-১১১১",
                hotlineRaw = "+6567381111",
                specialEn = "Advanced Nuclear Cardiology, Complex Eye Surgery, Neurological Tumor Excision",
                specialBn = "উন্নত নিউক্লিয়ার কার্ডিওলজি, জটিল চোখের অস্ত্রোপচার, নিউরোলজিক্যাল টিউমার অপসারণ"
            ),
            TopHospital(
                countryCode = "SA",
                nameEn = "King Faisal Specialist Hospital & Research Centre",
                nameBn = "কিং ফয়সাল স্পেশালিস্ট হাসপাতাল ও রিসার্চ সেন্টার",
                rating = 4.9,
                typeEn = "Premier Oncology & Genetics Hub",
                typeBn = "প্রধান অনকোলজি ও জেনেটিক্স হাব",
                locationEn = "Al Zahrawi Street, Al Maather, Riyadh 12713, Saudi Arabia",
                locationBn = "আল জাহরাভি স্ট্রিট, আল মাথায়েব, রিয়াদ ১২৭১৩, সৌদি আরব",
                hotlineEn = "199019 or +966-11-464-7272",
                hotlineBn = "১৯৯০১৯ অথবা +৯কে৬-১১-৪৬৪-৭২৭২",
                hotlineRaw = "+966114647272",
                specialEn = "Oncology, Genetics, Pediatrics, Organ Transplantation, Precision Medicine",
                specialBn = "অনকোলজি, জেনেটিক্স, পেডিয়াট্রিক্স, অঙ্গ প্রতিস্থাপন, প্রিসিশন মেডিসিন"
            ),
            TopHospital(
                countryCode = "SA",
                nameEn = "Dr. Sulaiman Al Habib Hospital, Riyadh",
                nameBn = "ড. সুলাইমান আল হাবিব হাসপাতাল, রিয়াদ",
                rating = 4.8,
                typeEn = "Pioneering Cloud-Integrated Smart Hospital",
                typeBn = "অগ্রগামী ক্লাউড-সংযুক্ত স্মার্ট হাসপাতাল",
                locationEn = "King Fahd Road, Olaya, Riyadh 12214, Saudi Arabia",
                locationBn = "কিং ফাহাদ রোড, ওলায়া, রিয়াদ ১২২১৪, সৌদি আরব",
                hotlineEn = "+966-11-490-9999",
                hotlineBn = "+৯৬৬-১১-৪৯০-৯৯৯৯",
                hotlineRaw = "+966114909999",
                specialEn = "High-tech ICU, Advanced Maternity and Women's Health, Laser Eye Correction",
                specialBn = "হাই-টেক আইসিইউ, উন্নত মাতৃত্ব ও নারী স্বাস্থ্য, লেজার কন্টাক্ট লেন্স লেসিক"
            ),
            TopHospital(
                countryCode = "UK",
                nameEn = "Great Ormond Street Hospital (GOSH), London",
                nameBn = "গ্রেট অরমন্ড স্ট্রিট হাসপাতাল (GOSH), লন্ডন",
                rating = 5.0,
                typeEn = "Global Pioneer in Complex Child Care",
                typeBn = "জটিল শিশু চিকিৎসায় বিশ্বখ্যাত অগ্রগামী কেন্দ্র",
                locationEn = "Great Ormond St, London WC1N 3JH, United Kingdom",
                locationBn = "গ্রেট অরমন্ড স্ট্রিট, লন্ডন ডব্লিউসি১এন ৩জেএইচ, যুক্তরাজ্য",
                hotlineEn = "+44-20-7405-9200",
                hotlineBn = "+৪৪-২০-৭৪০৫-৯২০০",
                hotlineRaw = "+442074059200",
                specialEn = "Pediatric Rare Diseases, Gene and Cell Therapy, Children's Heart Surgery",
                specialBn = "পেডিয়াট্রিক বিরল রোগ, জিন ও সেল থেরাপি, শিশুদের হার্ট সার্জারি"
            ),
            TopHospital(
                countryCode = "UK",
                nameEn = "King's College Hospital, London",
                nameBn = "কিংস কলেজ হাসপাতাল, লন্ডন",
                rating = 4.7,
                typeEn = "Elite Academic Major Trauma Hub",
                typeBn = "এলিট একাডেমিক মেজর ট্রমা হাব",
                locationEn = "Denmark Hill, London SE5 9RS, United Kingdom",
                locationBn = "ডেনমার্ক হিল, লন্ডন এসই৫ ৯আরএস, যুক্তরাজ্য",
                hotlineEn = "+44-20-3299-9000",
                hotlineBn = "+৪৪-২০-৩২৯৯-৯০০০",
                hotlineRaw = "+442032999000",
                specialEn = "Hepatology (Liver Disease), Comprehensive Stroke Recovery, Blood Cancers",
                specialBn = "হেপাটোলজি (লিভার রোগ), স্ট্রোক রিকভারি, ব্লাড ক্যান্সার চিকিৎসা"
            )
        )
    }
    
    val filteredHospitals = remember(selectedCountry, searchQuery) {
        hospitalsList.filter { hospital ->
            val matchesCountry = hospital.countryCode == selectedCountry
            val matchesSearch = if (searchQuery.trim().isEmpty()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                hospital.nameEn.lowercase().contains(q) ||
                hospital.nameBn.lowercase().contains(q) ||
                hospital.specialEn.lowercase().contains(q) ||
                hospital.specialBn.lowercase().contains(q) ||
                hospital.locationEn.lowercase().contains(q) ||
                hospital.locationBn.lowercase().contains(q) ||
                hospital.typeEn.lowercase().contains(q) ||
                hospital.typeBn.lowercase().contains(q)
            }
            matchesCountry && matchesSearch
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Text(
            text = if (isBengali) "📍 সেরা হাসপাতাল ও চিকিৎসা কেন্দ্র গাইড" else "📍 Top Accredited Hospitals Directory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = LightTextPrimary,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        
        Text(
            text = if (isBengali) "মেডিকেল প্রয়োজন অথবা জরুরি যেকোনো চিকিৎসার জন্য বাংলাদেশ ও বিদেশের স্বনামধন্য হাসপাতালের তালিকা:" 
                   else "Verified listing of world-renowned healthcare institutions for critical or general consultations:",
            style = MaterialTheme.typography.bodySmall,
            color = LightTextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        // Country Selector Row (Horizontal Chip Scroll)
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            countries.forEach { (code, labelBn, labelEn) ->
                val label = if (isBengali) labelBn else labelEn
                val isSelected = selectedCountry == code
                
                Card(
                    modifier = Modifier
                        .clickable { selectedCountry = code }
                        .testTag("hospital_country_tab_$code"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MedicalPrimary else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) MedicalPrimary else Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // Search Input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isBengali) "হাসপাতাল বা রোগের বিশেষত্ব দিয়ে খুঁজুন..." else "Search by hospital or specialty...",
                    fontSize = 12.sp,
                    color = LightTextSecondary.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("hospital_search_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Text("🔍", modifier = Modifier.padding(start = 8.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("❌", fontSize = 10.sp)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MedicalPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )
        
        // Dynamic Listings
        if (filteredHospitals.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isBengali) "কোনো হাসপাতাল পাওয়া যায়নি!" else "No matching hospitals found!",
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBengali) "অনুগ্রহ করে অন্য শব্দ বা নাম দিয়ে ট্রাই করুন।" else "Try adjusting your query descriptors.",
                        color = LightTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredHospitals.forEach { hospital ->
                    val hospitalName = if (isBengali) hospital.nameBn else hospital.nameEn
                    val hospitalType = if (isBengali) hospital.typeBn else hospital.typeEn
                    val hospitalLoc = if (isBengali) hospital.locationBn else hospital.locationEn
                    val hospitalHotline = if (isBengali) hospital.hotlineBn else hospital.hotlineEn
                    val hospitalSpecial = if (isBengali) hospital.specialBn else hospital.specialEn
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hospital_item_${hospital.countryCode}_${hospital.nameEn.replace(" ", "_").lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            // Header Row: Hospital Name & Rating
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = hospitalName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = LightTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("⭐", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = hospital.rating.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                            
                            // Badge indicating standard
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(MedicalPrimary.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = hospitalType,
                                    color = MedicalPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Specialties
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp)
                                        .background(Color(0xFFEFF6FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🩺", fontSize = 9.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isBengali) "প্রধান চিকিৎসাসেবা সমূহ:" else "Principal Medical Specialties:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightTextPrimary
                                    )
                                    Text(
                                        text = hospitalSpecial,
                                        fontSize = 11.sp,
                                        color = LightTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Location info
                            Row(verticalAlignment = Alignment.Top) {
                                Text("📍", fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = hospitalLoc,
                                    fontSize = 11.sp,
                                    color = LightTextSecondary,
                                    lineHeight = 14.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Hotline info
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📞", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "হটলাইন: $hospitalHotline" else "Hotline Center: $hospitalHotline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Call & Map actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hospital.hotlineRaw))
                                        try {
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, if (isBengali) "ডায়ালার রিকোয়েস্ট ব্যর্থ হয়েছে" else "Unable to launch call dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📞", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBengali) "কল করুন" else "Call Center",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(hospital.locationEn)))
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hospital.locationEn))
                                            Toast.makeText(context, if (isBengali) "ঠিকানা ক্লিপবোর্ডে কপি করা হয়েছে" else "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    border = BorderStroke(1.dp, MedicalPrimary.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🗺️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBengali) "ম্যাপে দেখুন" else "View on Map",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MedicalPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val historyList by viewModel.reportsHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Report Analysis History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (historyList.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllReports() }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No analyzed reports yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Once you upload or scan medical reports, your personal history will appear here for instant offline access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
                    val dateFormatted = remember(item.dateMillis) {
                        try {
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            sdf.format(Date(item.dateMillis))
                        } catch (e: Exception) {
                            "Unknown Date"
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.loadSavedReport(item) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BoxBorder(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val emoji = if (item.reportType == "Prescription") "📄" else "🧪"
                                    Text(
                                        text = emoji,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightTextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.deleteReport(item.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Report",
                                    tint = Color.Red.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailScreen(
    report: ReportDetail,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBengali by viewModel.isBengali.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // App bar top
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .drawBehind {
                    drawLine(
                        color = Color(0xFFEFF6FF),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.stopSpeaking()
                    onBack()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .testTag("detail_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightTextPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${report.reportType} Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightTextPrimary
                )
                Text(
                    text = if (isBengali) "ক্লিনিক্যালি এআই দ্বারা বিশ্লেষিত" else "Patient educational report summary",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightTextSecondary
                )
            }
        }

        // Quick Controls Row (Language Switcher, Play TTS, Stop TTS)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFEFF6FF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Language toggle tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp)
                ) {
                    val languages = listOf(
                        "en" to "English",
                        "bn" to "বাংলা",
                        "hi" to "हिंदी",
                        "es" to "Español",
                        "ar" to "العربية",
                        "fr" to "Français"
                    )
                    val languageCode by viewModel.languageCode.collectAsState()
                    languages.forEach { (code, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (languageCode == code) MedicalPrimary else Color.Transparent)
                                .clickable { viewModel.setLanguageCode(code) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("lang_${code}_tab")
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (languageCode == code) Color.White else LightTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Voice & Sharing controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isBengali) "টকিং ডক্টর:" else "Voice Guide:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = LightTextSecondary
                    )
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakReport()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isSpeaking) Color(0xFFFEE2E2) else MedicalTertiary,
                            contentColor = if (isSpeaking) Color(0xFFEF4444) else MedicalPrimary
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("speech_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Speak Explanation",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // PDF export and sharing button
                    IconButton(
                        onClick = {
                            com.example.util.PdfHelper.generateAndSharePdf(context, report, languageCode)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("pdf_export_share_button")
                    ) {
                        Text("📄", fontSize = 16.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Identified Report Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)) // green-500
                                )
                                Text(
                                    text = if (isBengali) "রিপোর্ট স্বয়ংক্রিয় সনাক্তকরণ" else "REPORT IDENTIFIED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B) // Slate 500
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFFEFF6FF)) // blue-50
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8) // blue-700
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val reportTitle = when (report.reportType.uppercase()) {
                            "CBC", "COMPLETE BLOOD COUNT" -> "Complete Blood Count (CBC)"
                            "BLOOD SUGAR", "DIABETES", "SUGAR" -> "Blood Sugar / Diabetes Test"
                            "KIDNEY", "KIDNEY FUNCTION" -> "Kidney Function Test (KFT)"
                            "LIVER", "LIVER FUNCTION" -> "Liver Function Test (LFT)"
                            "LIPID", "LIPID PROFILE" -> "Lipid Profile (Cholesterol)"
                            "THYROID" -> "Thyroid Panel Test"
                            else -> report.reportType
                        }
                        
                        val reportSubtitleBn = when (report.reportType.uppercase()) {
                            "CBC", "COMPLETE BLOOD COUNT" -> "সম্পূর্ণ রক্ত গণনা পরীক্ষার রিপোর্ট"
                            "BLOOD SUGAR", "DIABETES", "SUGAR" -> "রক্তের শর্করা বা ডায়াবেটিস পরীক্ষা"
                            "KIDNEY", "KIDNEY FUNCTION" -> "কিডনি কার্যকারিতা বিশ্লেষণ রিপোর্ট"
                            "LIVER", "LIVER FUNCTION" -> "যকৃত বা লিভার কার্যকারিতা রিপোর্ট"
                            "LIPID", "LIPID PROFILE" -> "লিপিড প্রোফাইল (রক্তের চর্বি পরীক্ষা)"
                            "THYROID" -> "থাইরয়েড হরমোন পরিমাপ রিপোর্ট"
                            else -> "মেডিকেল ল্যাব পরীক্ষার ফলাফল বিশ্লেষণ"
                        }

                        Text(
                            text = reportTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LightTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reportSubtitleBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569), // Slate 600
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Overall summary card in Deep High Density Blue
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MedicalPrimary), // Solid #005AC1
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isBengali) "এআই ল্যাব বিশ্লেষণ" else "AI SUMMARY INSIGHT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBengali) report.summaryBen else report.summaryEng,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Results details title
            item {
                Text(
                    text = if (isBengali) "ল্যাব সূচক বিশ্লেষণ" else "Detected Lab Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Specific values details
            items(report.detectedParameters) { param ->
                ParameterRowCard(param = param, isBengali = isBengali, languageCode = languageCode)
            }

            // Advice Banner (Disclaimer)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), // Light red container
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "গুরুত্বপূর্ণ সতর্কীকরণ" else "Medical Disclaimer Reminder",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "This application is built solely for general educational reference. It does not possess medical authority, diagnose health issues, or provide treatment options. Always seek direct consult from a registered medical professional or medical practitioner with your original test documents for diagnosis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B).copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterRowCard(param: ParameterDetail, isBengali: Boolean, languageCode: String) {
    var expanded by remember { mutableStateOf(false) }

    // Color-code values based on status
    val statusColor = when (param.status.uppercase()) {
        "HIGH" -> StatusHigh
        "LOW" -> StatusLow
        else -> StatusNormal
    }

    val statusBgColor = when (param.status.uppercase()) {
        "HIGH" -> Color(0xFFFEF2F2) // Red 50
        "LOW" -> Color(0xFFFFFBEB)  // Amber 50
        else -> Color(0xFFECFDF5)   // Emerald 50
    }

    val statusTextColor = when (param.status.uppercase()) {
        "HIGH" -> Color(0xFFDC2626) // Red 600
        "LOW" -> Color(0xFFD97706)  // Amber 600
        else -> Color(0xFF059669)   // Emerald 600
    }

    val statusText = when (param.status.uppercase()) {
        "HIGH" -> if (isBengali) "উচ্চ" else "HIGH"
        "LOW" -> if (isBengali) "নিম্ন" else "LOW"
        else -> if (isBengali) "স্বাভাবিক" else "NORMAL"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left-border status stripe (equivalent to border-l-4)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = param.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B) // slate-800
                    )

                    // Rounded status value badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${param.value} ${param.unit} ${if (param.status.uppercase() == "LOW") "L" else if (param.status.uppercase() == "HIGH") "H" else ""}",
                            color = statusTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                
                // Bangla equivalent name & Ref range row
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val paramBangla = when (param.name.uppercase()) {
                        "HEMOGLOBIN", "HB" -> "হিমোগ্লোবিন"
                        "RBC COUNT", "RBC" -> "লোহিত রক্তকণিকা"
                        "WBC COUNT", "WBC" -> "শ্বেত রক্তকণিকা"
                        "PLATELETS", "PLATELET COUNT" -> "অণুচক্রিকা"
                        "FASTING BLOOD SUGAR", "FASTING GLUCOSE", "FBS" -> "খালি পেটে সুগার"
                        "POST-PRANDIAL SUGAR", "POST PRANDIAL GLUCOSE", "PPBS" -> "খাবারের পরে সুগার"
                        "HBA1C" -> "এইচবিএ১সি (তিন মাসের সুগার)"
                        "TSH" -> "টিএসএইচ হরমোন"
                        "FREE T3" -> "ফ্রি টি৩"
                        "FREE T4" -> "ফ্রি টি৪"
                        "CREATININE" -> "ক্রিয়েটিনিন (কিডনির টেস্ট)"
                        "UREA" -> "ইউরিয়া"
                        "EGFR" -> "ইজিএফআর (কিডনি ছাঁকন হার)"
                        "TOTAL CHOLESTEROL" -> "মোট কোলেস্টেরল"
                        "HDL" -> "এইচডিএল (ভালো চর্বি)"
                        "LDL" -> "এলডিএল (খারাপ চর্বি)"
                        "TRIGLYCERIDES" -> "ট্রাইগ্লিসেরাইড"
                        "SGPT", "ALT" -> "এসজিপিট (লিভার এনজাইম)"
                        "SGOT", "AST" -> "এসজিওটি (লিভার এনজাইম)"
                        "BILIRUBIN" -> "বিলিরুবিন (জন্ডিস পরীক্ষা)"
                        else -> "ল্যাব প্যারামিটার"
                    }

                    Text(
                        text = paramBangla,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Ref Range: ${param.referenceRange}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 11.sp
                    )
                }

                // Expandable explanation details block
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        // Explanation panel styled with Slate 50 background, rounded corners & Slate 100 border
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // Slate-50 background
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEFF6FF)) // Slate-100/blue-50 equivalent
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val selectedExplanation = when (languageCode) {
                                    "bn" -> param.explanationBen.ifEmpty { param.explanationEng }
                                    "hi" -> param.explanationHi.ifEmpty { param.explanationEng }
                                    "es" -> param.explanationEs.ifEmpty { param.explanationEng }
                                    "ar" -> param.explanationAr.ifEmpty { param.explanationEng }
                                    "fr" -> param.explanationFr.ifEmpty { param.explanationEng }
                                    else -> param.explanationEng
                                }
                                val secondaryExplanation = if (languageCode == "bn") {
                                    param.explanationEng
                                } else {
                                    param.explanationBen
                                }

                                // Highlighted status prefix
                                val explanationPrimary = if (param.status.uppercase() == "LOW" || param.status.uppercase() == "HIGH") {
                                    "${if (param.status.uppercase() == "LOW") "Lower" else "Higher"} than normal: "
                                } else {
                                    "Normal: "
                                }
                                
                                val statusTextPrefixColor = when (param.status.uppercase()) {
                                    "HIGH" -> Color(0xFFB91C1C) // dark red
                                    "LOW" -> Color(0xFFB45309)  // dark amber
                                    else -> Color(0xFF047857)   // dark green
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = explanationPrimary,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTextPrefixColor,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = selectedExplanation,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155), // slate-700
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = secondaryExplanation,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B), // Slate-500 equivalent
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Toggle visibility hints
                if (!expanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = if (isBengali) "বিস্তারিত দেখতে ট্যাপ করুন ↓" else "Tap to view explanation ↓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// Custom border builder to avoid compose-version dependencies of modifier.border in older models
@Composable
fun BoxBorder(width: androidx.compose.ui.unit.Dp, color: Color) = BorderStroke(width, color)

@Composable
fun DailyHealthScreen(viewModel: MainViewModel) {
    val isBengali by viewModel.isBengali.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val reminders by viewModel.medicineReminders.collectAsState()
    
    var currentSubTab by remember { mutableStateOf(0) } // 0 = Tips, 1 = Reminders
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Day Index based on Day of Month
    val todayDayIndex = remember {
        val cal = java.util.Calendar.getInstance()
        val dom = cal.get(java.util.Calendar.DAY_OF_MONTH)
        if (dom in 1..31) dom else 1
    }
    
    var selectedDayIndex by remember { mutableStateOf(todayDayIndex) }
    
    val currentDayTip = remember(selectedDayIndex) {
        com.example.util.DailyHealthTips.tipsList.firstOrNull { it.dayIndex == selectedDayIndex } 
            ?: com.example.util.DailyHealthTips.tipsList.first()
    }
    
    val customTip by viewModel.customHealthTip.collectAsState()
    val customTipLoading by viewModel.customTipLoading.collectAsState()
    
    // If there is an AI custom tip generated, show it. Otherwise show the navigated daily tip!
    val activeTip = customTip ?: currentDayTip
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        // Premium Sub-Tab Segment Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(0, 1).forEach { tabIndex ->
                val selected = currentSubTab == tabIndex
                val label = if (tabIndex == 0) {
                    when (languageCode) {
                        "bn" -> "স্বাস্থ্য পরামর্শ 🌿"
                        "hi" -> "स्वास्थ्य सलाह 🌿"
                        "es" -> "Consejos salud 🌿"
                        else -> "Health Tips 🌿"
                    }
                } else {
                    when (languageCode) {
                        "bn" -> "ঔষধ রিমাইন্ডার 💊"
                        "hi" -> "दवा रिमाइंडर 💊"
                        "es" -> "Plan de medicina 💊"
                        else -> "Pill Reminders 💊"
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MedicalPrimary else Color.Transparent)
                        .clickable { currentSubTab = tabIndex }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else Color(0xFF475569),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (currentSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        // Upper Title / Info
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MedicalSecondary.copy(alpha = 0.06f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBengali) "দৈনিক সুস্থতা নির্দেশিকা 🌿" else "Daily Health Advisory 🌿",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedicalPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBengali) "প্রতিদিনের সুষম জীবনযাত্রার সহজ বৈজ্ঞানিক পরামর্শ" else "Simple and scientific guidelines for healthy daily living",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Single Interactive Tip Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("daily_health_tips_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MedicalPrimary.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Area: Day counter & Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MedicalPrimary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activeTip.icon, fontSize = 24.sp)
                            }
                            Column {
                                Text(
                                    text = if (isBengali) "আজকের স্বাস্থ্য পরামর্শ" else "Today's Health Advisory",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = activeTip.getLocalizedCategory(isBengali),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalPrimary
                                )
                            }
                        }
                        
                        // Badge: Day X vs AI
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (customTip != null) StatusNormal.copy(alpha = 0.1f) else MedicalPrimary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (customTip != null) {
                                    if (isBengali) "AI জেনারেটেড" else "AI Generated"
                                } else {
                                    if (isBengali) "দিন $selectedDayIndex" else "Day $selectedDayIndex"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (customTip != null) StatusNormal else MedicalPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Tip Content Area
                    Text(
                        text = activeTip.getLocalizedTitle(isBengali),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeTip.getLocalizedTip(isBengali),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightTextSecondary,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Caution Warning Box (High Contrast)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFFBEB)) // Light Amber bg
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚠️", fontSize = 18.sp, modifier = Modifier.padding(top = 1.dp))
                            Column {
                                Text(
                                    text = if (isBengali) "সতর্কতা ও ক্লিনিক্যাল লক্ষণ:" else "Warning & Clinical Signals:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309) // Amber Dark Text
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeTip.getLocalizedWarning(isBengali),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    // Hide Day Stepper when showing AI tip
                    if (customTip == null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Stepper row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Prev Day button
                            TextButton(
                                onClick = {
                                    if (selectedDayIndex > 1) {
                                        selectedDayIndex--
                                    } else {
                                        selectedDayIndex = 31
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⬅️", fontSize = 14.sp)
                                    Text(
                                        text = if (isBengali) "আগের পরামর্শ" else "Previous Tip",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MedicalPrimary
                                    )
                                }
                            }
                            
                            // Day Indicator
                            Text(
                                text = if (isBengali) "৩১ দিনের মধ্যে $selectedDayIndex" else "$selectedDayIndex of 31",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = LightTextPrimary
                            )
                            
                            // Next Day button
                            TextButton(
                                onClick = {
                                    if (selectedDayIndex < 31) {
                                        selectedDayIndex++
                                    } else {
                                        selectedDayIndex = 1
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isBengali) "পরের পরামর্শ" else "Next Tip",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MedicalPrimary
                                    )
                                    Text("➡️", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Dynamic Health Tip / AI actions
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MedicalPrimary.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (customTipLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MedicalTertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MedicalPrimary
                                )
                                Text(
                                    text = if (isBengali) "নতুন স্বাস্থ্য পরামর্শ তৈরি হচ্ছে..." else "Generating custom AI health advice...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalPrimary
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.generateCustomHealthTip() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_generate_dynamic_tip"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MedicalPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("✨", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBengali) "নতুন AI টিপস" else "Generate AI Tip",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (customTip != null || selectedDayIndex != todayDayIndex) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resetCustomHealthTip()
                                        selectedDayIndex = todayDayIndex
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("btn_reset_default_tip"),
                                    border = BorderStroke(1.5.dp, MedicalPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalPrimary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("↩️", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBengali) "আজকের টিপে ফিরুন" else "Reset to Today",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} else {
    LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Medicine Reminder Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💊", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (languageCode) {
                                        "bn" -> "মেডিকেশন রিমাইন্ডার"
                                        "hi" -> "दवा रिमाइंडर ट्रैकर"
                                        "es" -> "Recordatorio de Medicinas"
                                        else -> "Medication Reminders"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = when (languageCode) {
                                        "bn" -> "আপনার ঔষুধ অন-টাইম খেতে এবং ডোজ ট্র্যাক করতে রিমাইন্ডার সেট করুন"
                                        "hi" -> "समय पर अपनी दवाएं लेने और खुराक का ध्यान रखने के लिए रिमाइंडर जोड़ें"
                                        "es" -> "Programe sus pastillas y reciba recordatorios para no saltarse ninguna toma"
                                        else -> "Keep your health on track. Schedule, view and toggle your pill reminder checklists"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }
                }

                // Add reminder trigger button
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_show_add_reminder"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("➕", fontSize = 16.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (languageCode) {
                                    "bn" -> "নতুন রিমাইন্ডার যোগ করুন"
                                    "hi" -> "नया रिमाइंडर जोड़ें"
                                    "es" -> "Añadir Recordatorio"
                                    else -> "Add Custom Reminder"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Reminders list
                if (reminders.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("🔔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (languageCode) {
                                    "bn" -> "কোনো ঔষধের রিমাইন্ডার শিডিউল নেই"
                                    "hi" -> "कोई दवा अनुसूची निर्धारित नहीं है"
                                    "es" -> "No hay recordatorios programados"
                                    else -> "No Reminders Scheduled"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (languageCode) {
                                    "bn" -> "আপনার প্রতিদিনের ভিটামিন, ট্যাবলেট বা কার্যতালিকা যুক্ত করতে উপরের বাটনে চাপুন।"
                                    "hi" -> "अपने दैनिक पूरक या गोलियों को जोड़ने के लिए ऊपर दिए गए बटन का उपयोग करें।"
                                    "es" -> "Programe sus tomas diarias usando el botón superior para mantenerse saludable."
                                    else -> "Add your daily pills, syrups, or supplements above to ensure you never miss a dose."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LightTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(reminders.size) { index ->
                        val remind = reminders[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("reminder_item_${remind.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (remind.isActive) MedicalPrimary.copy(alpha = 0.15f) else Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (remind.isActive) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (remind.isActive) "💊" else "💤",
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = remind.medicineName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remind.isActive) LightTextPrimary else LightTextSecondary,
                                        textDecoration = if (remind.isActive) null else androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(remind.dosage, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MedicalPrimary)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(remind.time, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        }
                                    }
                                    if (remind.instructions.isNotEmpty() || remind.frequency.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${remind.frequency} • ${remind.instructions}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LightTextSecondary
                                        )
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = remind.isActive,
                                        onCheckedChange = { viewModel.toggleReminderActive(remind) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFFDC2626),
                                            uncheckedThumbColor = Color(0xFF94A3B8),
                                            uncheckedTrackColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteReminder(remind.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var inputName by remember { mutableStateOf("") }
        var inputDosage by remember { mutableStateOf("1 Tablet") }
        var inputFrequency by remember { mutableStateOf("Once Daily") }
        var inputTime by remember { mutableStateOf("08:00 AM") }
        var inputInstructions by remember { mutableStateOf("After Meals") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = when (languageCode) {
                        "bn" -> "ঔষধের রিমাইন্ডার শিডিউল তৈরি"
                        "hi" -> "दवा रिमाइंडर जोड़ें"
                        "es" -> "Crear Recordatorio"
                        else -> "Set Medication Schedule"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = {
                            Text(when (languageCode) {
                                "bn" -> "ঔষধের নাম"
                                "hi" -> "दवा का नाम"
                                "es" -> "Nombre del medicamento"
                                else -> "Medicine Name"
                            })
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_name")
                    )

                    OutlinedTextField(
                        value = inputDosage,
                        onValueChange = { inputDosage = it },
                        label = {
                            Text(when (languageCode) {
                                "bn" -> "ডোজ / মাত্রা (উদা. 1 Tablet)"
                                "hi" -> "खुराक (उदा: 1 गोली)"
                                "es" -> "Dosis (ej: 1 Tableta)"
                                else -> "Dosage (e.g., 1 Tablet, 5ml)"
                            })
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_dosage")
                    )

                    OutlinedTextField(
                        value = inputTime,
                        onValueChange = { inputTime = it },
                        label = {
                            Text(when (languageCode) {
                                "bn" -> "সেবনের সময় (উদা. 08:30 AM)"
                                "hi" -> "सेवन का समय (उदा: 08:30 AM)"
                                "es" -> "Hora de recordatorio (ej: 08:30 AM)"
                                else -> "Reminder Time (e.g., 08:30 AM)"
                            })
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_time")
                    )

                    OutlinedTextField(
                        value = inputFrequency,
                        onValueChange = { inputFrequency = it },
                        label = {
                            Text(when (languageCode) {
                                "bn" -> "সেবনের সংখ্যা (উদা. Daily)"
                                "hi" -> "आवृत्ति (उदा: Daily)"
                                "es" -> "Frecuencia (ej: Diario)"
                                else -> "Frequency (e.g., Once Daily)"
                            })
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_freq")
                    )

                    OutlinedTextField(
                        value = inputInstructions,
                        onValueChange = { inputInstructions = it },
                        label = {
                            Text(when (languageCode) {
                                "bn" -> "বিশেষ নির্দেশনা (উদা. খাবারের পর)"
                                "hi" -> "विशेष निर्देश (उदा: भोजन के बाद)"
                                "es" -> "Instrucciones (ej: Después de comer)"
                                else -> "Special instructions (e.g., After Meal)"
                            })
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_instructions")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            viewModel.addReminder(
                                name = inputName,
                                dosage = inputDosage,
                                frequency = inputFrequency,
                                time = inputTime,
                                instructions = inputInstructions
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(
                        text = when (languageCode) {
                            "bn" -> "যুক্ত করুন"
                            "hi" -> "डेटा सेव करें"
                            "es" -> "Añadir"
                            else -> "Schedule"
                        }
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false }
                ) {
                    Text(
                        text = when (languageCode) {
                            "bn" -> "বাতিল"
                            "hi" -> "रद्द करें"
                            "es" -> "Cancelar"
                            else -> "Cancel"
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun BmiCalculatorScreen(viewModel: MainViewModel) {
    val isBengali by viewModel.isBengali.collectAsState()
    var gender by remember { mutableStateOf("male") } // male/female
    var weightInput by remember { mutableStateOf("65") } // in kg
    var feetInput by remember { mutableStateOf("5") } // feet
    var inchesInput by remember { mutableStateOf("6") } // inches

    var calculatedBmi by remember { mutableStateOf<Double?>(null) }

    fun calculate() {
        val weight = weightInput.toDoubleOrNull() ?: 0.0
        val feet = feetInput.toDoubleOrNull() ?: 0.0
        val inches = inchesInput.toDoubleOrNull() ?: 0.0

        if (weight > 0.0 && (feet > 0.0 || inches > 0.0)) {
            val heightMeters = (feet * 0.3048) + (inches * 0.0254)
            if (heightMeters > 0.0) {
                val bmi = weight / (heightMeters * heightMeters)
                calculatedBmi = bmi
            }
        }
    }

    val bmiInfo = remember(calculatedBmi, isBengali) {
        val bmi = calculatedBmi ?: return@remember null
        val (category, advice) = when {
            bmi < 18.5 -> Pair(
                if (isBengali) "কম ওজন (Underweight)" else "Underweight",
                if (isBengali) "আপনার দৈনন্দিন পুষ্টিকর খাবার (যেমন প্রোটিন, ডিম, দুধ, বাদাম, এবং সুষম মেদ) বেশি খাওয়া উচিত। পেশি বৃদ্ধির জন্য হালকা কসরত বা হালকা ব্যায়াম করুন। ডাক্তারের পরামর্শে স্বাস্থ্যকর চার্ট অনুসরণ করতে পারেন।" else "You should consume more nutrient-dense foods (such as proteins, eggs, dairy, nuts, and healthy fats). Practice light physical activity to build muscle mass. Seek a personalized dietary plan from a lifestyle guide."
            )
            bmi >= 18.5 && bmi < 25.0 -> Pair(
                if (isBengali) "আদর্শ ও স্বাস্থ্যকর ওজন (Healthy Weight)" else "Healthy Weight",
                if (isBengali) "চমৎকার! আপনার দেহের ওজন একদম নিখুঁত সীমার মধ্যে রয়েছে। এই স্বাস্থ্যকর ওজনের ধারা বজায় রাখতে সুষম খাদ্যাভ্যাস সচল রাখুন এবং প্রতিদিন ২০-৩০ মিনিট অন্তত জোরে হাঁটার অভ্যাস বজায় রাখুন।" else "Excellent! Your body weight is in the ideal range. Keep up this healthy status by maintaining a balanced diet and brisk walking for 20-30 minutes daily."
            )
            bmi >= 25.0 && bmi < 30.0 -> Pair(
                if (isBengali) "অতিরিক্ত ওজন (Overweight)" else "Overweight",
                if (isBengali) "আপনার খাবারে ফাস্টফুড, চিনি, অতিরিক্ত মাখন এবং শর্করার পরিমাণ কমিয়ে আঁশযুক্ত সবুজ শাকসবজি ও ফলমূল বেশি খেতে হবে। দৈনিক অন্তত ৪০-৫০ মিনিট নিয়মিত ঘাম ঝরানো কসরত করুন।" else "You should reduce fast food, simple sugars, butter, and refined carbohydrates. Incorporate fiber-rich greens and seasonal fruits. Practice regular exercise for 40-50 minutes daily to lose weight."
            )
            else -> Pair(
                if (isBengali) "স্থূল বা অতি-ওজন (Obese)" else "Obese",
                if (isBengali) "উচ্চ ওজনের কারণে টাইপ-২ ডায়াবেটিস, ফ্যাটি লিভার, স্ট্রোক ও হৃদরোগের ঝুঁকি চরমভাবে বৃদ্ধি পায়। অবিলম্বে মিষ্টি ও ভাজাপোড়া ত্যাগ করুন এবং একজন পুষ্টিবিদ বা চিকিৎসকের পরামর্শ নিয়ে কঠোর ওজনের লক্ষ্য নির্ধারণ কসরত করুন।" else "Elevated weight increases risks for cardiovascular diseases, fatty liver, type-2 diabetes and strokes. Avoid sugary foods and deep fried dishes. Work with a physician for a target plan."
            )
        }
        Pair(category, advice)
    }

    val bmiColor = when {
        calculatedBmi == null -> MedicalPrimary
        calculatedBmi!! < 18.5 -> StatusLow
        calculatedBmi!! < 25.0 -> StatusNormal
        calculatedBmi!! < 30.0 -> StatusLow
        else -> StatusHigh
    }

    val bmiCategory = bmiInfo?.first ?: ""
    val bmiAdvice = bmiInfo?.second ?: ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("bmi_calculator_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                        text = if (isBengali) "শারীরিক পরিমাপ টাইপ করুন" else "Enter Physical Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Gender Selection
                    Text(
                        text = if (isBengali) "লিঙ্গ নির্বাচন করুন:" else "Choose Gender:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { gender = "male" },
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gender == "male") MedicalPrimary else Color(0xFFF1F5F9),
                                contentColor = if (gender == "male") Color.White else LightTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBengali) "👨 পুরুষ" else "👨 Male", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { gender = "female" },
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gender == "female") MedicalPrimary else Color(0xFFF1F5F9),
                                contentColor = if (gender == "female") Color.White else LightTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isBengali) "👩 নারী" else "👩 Female", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Height Input (Feet & Inches side-by-side)
                    Text(
                        text = if (isBengali) "উচ্চতা:" else "Height:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = feetInput,
                            onValueChange = { feetInput = it },
                            modifier = Modifier.weight(1f).testTag("bmi_feet_input"),
                            label = { Text(if (isBengali) "ফুট" else "Feet") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = inchesInput,
                            onValueChange = { inchesInput = it },
                            modifier = Modifier.weight(1f).testTag("bmi_inches_input"),
                            label = { Text(if (isBengali) "ইঞ্চি" else "Inches") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weight Input
                    Text(
                        text = if (isBengali) "বর্তমান ওজন (কেজি):" else "Current Weight (kg):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("bmi_weight_input"),
                        label = { Text(if (isBengali) "ওজন" else "Weight") },
                        trailingIcon = { Text("kg", color = LightTextSecondary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MedicalPrimary,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Calculate Button
                    Button(
                        onClick = { calculate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_calculate_bmi"),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isBengali) "বিএমআই হিসেব করুন" else "Calculate BMI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Calculated Result Section
        calculatedBmi?.let { bmi ->
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MedicalPrimary.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBengali) "আপনার বডি মাস ইনডেক্স ফলাফল" else "Your Body Mass Index Result",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LightTextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual BMI Meter
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(bmiColor.copy(alpha = 0.08f))
                                .border(3.dp, bmiColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val df = java.text.DecimalFormat("#.#")
                                Text(
                                    text = df.format(bmi),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bmiColor
                                )
                                Text(
                                    text = if (isBengali) "বিএমআই (BMI)" else "BMI Ratio",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bmiColor.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = bmiCategory,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = bmiColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recommendations card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(LightBackground)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💡", fontSize = 16.sp)
                                    Text(
                                        text = if (isBengali) "স্বাস্থ্যকর গাইড ও সতর্কতা পরামর্শ:" else "Healthy Guidance & Warnings:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MedicalPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = bmiAdvice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LightTextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                com.example.util.ReportGenerator.shareBmiReport(
                                    context = context,
                                    isBengali = isBengali,
                                    gender = gender,
                                    heightFeet = feetInput,
                                    heightInches = inchesInput,
                                    weightKg = weightInput,
                                    bmi = bmi,
                                    category = bmiCategory,
                                    advice = bmiAdvice
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_share_bmi_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)), 
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📄", fontSize = 16.sp)
                                Text(
                                    text = if (isBengali) "পিডিএফ স্বাস্থ্য রিপোর্ট শেয়ার করুন" else "Share PDF Health Report",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PregnancyCareScreen(viewModel: MainViewModel) {
    val isBengali by viewModel.isBengali.collectAsState()
    var prePregWeightInput by remember { mutableStateOf("55") } // in kg
    var currentWeightInput by remember { mutableStateOf("62") } // in kg
    var feetInput by remember { mutableStateOf("5") } // feet
    var inchesInput by remember { mutableStateOf("2") } // inches
    var currentWeek by remember { mutableStateOf(16f) } // slider from 1 to 40

    // Last Menstrual Period (LMP) input states for due date calculation
    var lmpDayInput by remember { mutableStateOf("15") }
    var lmpMonthInput by remember { mutableStateOf("10") }
    var lmpYearInput by remember { mutableStateOf("2025") }

    // Lab Values for Customized Pregnancy PDF Report
    var hbInput by remember { mutableStateOf("11.5") }
    var ogttInput by remember { mutableStateOf("4.8") }
    var bpInput by remember { mutableStateOf("120/80") }

    // Gestational age state/calculations
    val weekInt = currentWeek.toInt()
    val trimesterName = when {
        weekInt <= 12 -> if (isBengali) "প্রথম ত্রৈমাসিক (১ম - ১২তম সপ্তাহ)" else "First Trimester (Weeks 1 - 12)"
        weekInt <= 26 -> if (isBengali) "দ্বিতীয় ত্রৈমাসিক (১৩তম - ২৬তম সপ্তাহ)" else "Second Trimester (Weeks 13 - 26)"
        else -> if (isBengali) "তৃতীয় ত্রৈমাসিক (২৭তম - ৪০তম সপ্তাহ)" else "Third Trimester (Weeks 27 - 40)"
    }
    
    val trimesterColor = when {
        weekInt <= 12 -> Color(0xFFF59E0B) // Amber
        weekInt <= 26 -> MedicalPrimary    // Blue
        else -> Color(0xFFEC4899)          // Pink/Rose
    }

    // Interactive Size Fruit Comparison lookup
    val babyFruitComparison = when {
        weekInt <= 4 -> Pair(if (isBengali) "পোস্ত দানা (Poppy Seed)" else "Poppy Seed", "☘️")
        weekInt <= 8 -> Pair(if (isBengali) "ছোট ব্লুবেরি (Blueberry)" else "Blueberry", "🫐")
        weekInt <= 12 -> Pair(if (isBengali) "রসালো লেবু (Lime)" else "Lime", "🍋")
        weekInt <= 16 -> Pair(if (isBengali) "সুস্বাদু অ্যাভোকাডো (Avocado)" else "Avocado", "🥑")
        weekInt <= 20 -> Pair(if (isBengali) "মিষ্টি কলা (Banana)" else "Banana", "🍌")
        weekInt <= 24 -> Pair(if (isBengali) "সবুজ মিষ্টি ভুট্টা (Sweet Corn)" else "Sweet Corn", "🌽")
        weekInt <= 28 -> Pair(if (isBengali) "গোলাকার বেগুন (Eggplant)" else "Eggplant", "🍆")
        weekInt <= 32 -> Pair(if (isBengali) "রসালো ডাব/নারকেল (Coconut)" else "Coconut", "🥥")
        weekInt <= 36 -> Pair(if (isBengali) "মিষ্টি আনারস (Pineapple)" else "Pineapple", "🍍")
        else -> Pair(if (isBengali) "পাকা তরমুজ (Watermelon)" else "Watermelon", "🍉")
    }

    // Last Menstrual Period (LMP) to Expected Due Date (EDD) Calculator
    val lmpDay = lmpDayInput.toIntOrNull() ?: 15
    val lmpMonth = lmpMonthInput.toIntOrNull() ?: 10
    val lmpYear = lmpYearInput.toIntOrNull() ?: 2025

    val eddCalculated = remember(lmpDay, lmpMonth, lmpYear, isBengali) {
        try {
            val cal = java.util.Calendar.getInstance()
            cal.set(lmpYear, lmpMonth - 1, lmpDay)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 280) // Standard 280 days (40 weeks) gestations
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.get(java.util.Calendar.MONTH) + 1
            val year = cal.get(java.util.Calendar.YEAR)
            val monthText = when (month) {
                1 -> if (isBengali) "জানুয়ারি" else "January"
                2 -> if (isBengali) "ফেব্রুয়ারি" else "February"
                3 -> if (isBengali) "মার্চ" else "March"
                4 -> if (isBengali) "এপ্রিল" else "April"
                5 -> if (isBengali) "মে" else "May"
                6 -> if (isBengali) "জুন" else "June"
                7 -> if (isBengali) "জুলাই" else "July"
                8 -> if (isBengali) "আগস্ট" else "August"
                9 -> if (isBengali) "সেপ্টেম্বর" else "September"
                10 -> if (isBengali) "অক্টোবর" else "October"
                11 -> if (isBengali) "নভেম্বর" else "November"
                else -> if (isBengali) "ডিসেম্বর" else "December"
            }
            val today = java.util.Calendar.getInstance()
            today.set(java.util.Calendar.HOUR_OF_DAY, 0)
            today.set(java.util.Calendar.MINUTE, 0)
            today.set(java.util.Calendar.SECOND, 0)
            today.set(java.util.Calendar.MILLISECOND, 0)
            val diffMillis = cal.timeInMillis - today.timeInMillis
            val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
            
            Triple("$day $monthText $year", true, daysRemaining)
        } catch (e: Exception) {
            Triple(if (isBengali) "অকার্যকর তথ্য" else "Invalid Input", false, 0)
        }
    }

    // Weekly development details lookup
    val babyDevelopmentBn = when {
         weekInt <= 4 -> if (isBengali) "ভ্রূণ জরায়ুতে বসতি স্থাপন করছে। এই সময়ে বাচ্চার স্নায়ুতন্ত্র ও হার্ট বিকাশের প্রাথমিক রূপ লাভ করে।" else "The embryo is nesting in the uterus. The baby's nervous system and primitive heart are beginning to take shape during this time."
         weekInt <= 8 -> if (isBengali) "বাচ্চার আকার এখন একটি আঙুরের মতো। হৃদপিণ্ড সচল হয়েছে এবং স্পন্দন শোনা সম্ভব। হাত-পায়ের কুঁড়ি তৈরি হচ্ছে।" else "The baby is about the size of a grape. The heart is beating, which can now be heard. Tiny arm and leg buds are starting to form."
         weekInt <= 12 -> if (isBengali) "শিশুর অতি প্রয়োজনীয় অঙ্গসমূহ সম্পূর্ণ গঠিত হয়েছে এবং তারা নড়াচড়া শুরু করেছে (যদিও মা অনুভব করতে পারেন না)। নখ ও দাঁতের কুঁড়ি গঠিত হচ্ছে।" else "Critical organs are fully formed, and the baby has started moving (though the mother cannot feel it yet). Tiny nails and tooth buds are forming."
         weekInt <= 16 -> if (isBengali) "বাচ্চার মুখের পেশি চমৎকার কাজ করছে এবং সে এখন চোখ পিটপিট করতে পারে। মায়ের গর্ভে বাচ্চার লিঙ্গ এখন আল্ট্রাসাউন্ড সংযোগে বোঝা যায়।" else "The baby's facial muscles are wonderfully active, and they can blink. The baby's biological sex might be visible via ultrasound now."
         weekInt <= 20 -> if (isBengali) "বাচ্চা এখন মায়ের কথা সরাসরি শুনতে পায়! তার নড়াচড়া (কুইকেনিং) মা স্পষ্ট অনুভব করতে শুরু করেন। শরীরে সূক্ষ্ম চুল গজাতে থাকে।" else "The baby can hear your voice clearly! The mother will feel the baby's movements (quickening) distinctively now. Fine hair (lanugo) is growing."
         weekInt <= 24 -> if (isBengali) "বাচ্চার ফুসفوসের কৈশিক নালী গঠিত হচ্ছে। ত্বক কিছুটা কুঁচকানো এবং রক্তকণিকা গঠনে হাড়ের মজ্জা পুরোদমে সক্রিয়।" else "Capillaries inside the lungs are forming. The skin is wrinkled, and bone marrow is actively producing blood cells."
         weekInt <= 28 -> if (isBengali) "শিশুর চোখ এখন খুলতে ও বন্ধ হতে পারে। ফুসফুস ও মস্তিষ্ক খুব দ্রুত পূর্ণতা পাচ্ছে। ঘুমের একটি চক্র বা রুটিন তৈরি হয়।" else "The baby can open and close their eyes. Lungs and brain are maturing rapidly. A sleep-wake cycle is starting to establish."
         weekInt <= 32 -> if (isBengali) "বাচ্চার হাড় এখন শক্ত হলেও খুলির হাড় নরম থাকে যাতে জন্মের সময় সুবিধা হয়। বাচ্চার নড়াচড়া বেশ জোরালো অনুভূত হবে।" else "The baby's bones are hardening, though the skull remains soft to ease childbirth. You'll feel strong kicks and turns."
         weekInt <= 36 -> if (isBengali) "বাচ্চা এখন নিচে মাথা ঘুরিয়ে জন্মের সঠিক পজিশনে আসার প্রস্তুতি নিচ্ছে। চারপাশের প্রতিরক্ষামূলক ফ্লুইডের আস্তরণ কমতে শুরু করেছে।" else "The baby is preparing for birth by turning head-down. The protective layer of amniotic fluid begins to decrease."
         else -> if (isBengali) "বাচ্চা এখন পৃথিবীতে আসার জন্য সম্পূর্ণ প্রস্তুত। ফুসফুস এবং শরীরের চর্বি স্তর পুরোপুরি বিকশিত। শুভকামনা!" else "The baby is fully developed and ready to enter the world! Lungs and body fat layers are mature. Best wishes!"
    }

    val babyWeightEstimate = when {
         weekInt <= 8 -> if (isBengali) "১ গ্রাম" else "1 g"
         weekInt <= 12 -> if (isBengali) "১৪ গ্রাম" else "14 g"
         weekInt <= 16 -> if (isBengali) "১০০ গ্রাম" else "100 g"
         weekInt <= 20 -> if (isBengali) "৩০০ গ্রাম" else "300 g"
         weekInt <= 24 -> if (isBengali) "৬০০ গ্রাম" else "600 g"
         weekInt <= 28 -> if (isBengali) "১ কেজি" else "1 kg"
         weekInt <= 32 -> if (isBengali) "১.৭ কেজি" else "1.7 kg"
         weekInt <= 36 -> if (isBengali) "২.৬ কেজি" else "2.6 kg"
         else -> if (isBengali) "৩.২+ কেজি (সম্পূর্ণ প্রস্তুত)" else "3.2+ kg (Fully term)"
    }

    // Weight gain calculations
    val preWeight = prePregWeightInput.toDoubleOrNull() ?: 55.0
    val curWeight = currentWeightInput.toDoubleOrNull() ?: 62.0
    val feet = feetInput.toDoubleOrNull() ?: 5.0
    val inches = inchesInput.toDoubleOrNull() ?: 2.0
    
    val heightMeters = (feet * 0.3048) + (inches * 0.0254)
    val prePregBmi = if (heightMeters > 0) preWeight / (heightMeters * heightMeters) else 22.0
    
    val actualGain = curWeight - preWeight
    
    val (minRecTotal, maxRecTotal) = when {
        prePregBmi < 18.5 -> Pair(12.5, 18.0)
        prePregBmi < 25.0 -> Pair(11.5, 16.0)
        prePregBmi < 30.0 -> Pair(7.0, 11.5)
        else -> Pair(5.0, 9.0)
    }

    val weeklyRate = when {
        prePregBmi < 18.5 -> 0.5
        prePregBmi < 25.0 -> 0.45
        prePregBmi < 30.0 -> 0.3
        else -> 0.2
    }
    
    val expectedGainMax = if (weekInt <= 12) {
        1.5 + (weekInt * 0.1)
    } else {
        1.5 + ((weekInt - 12) * weeklyRate)
    }
    
    val expectedGainMin = if (weekInt <= 12) {
        0.5 + (weekInt * 0.05)
    } else {
        0.8 + ((weekInt - 12) * weeklyRate * 0.7)
    }

    val (gainStatus, gainStatusColor) = when {
        actualGain < expectedGainMin -> Pair(if (isBengali) "প্রয়োজনের চেয়ে কম বৃদ্ধি (Under-gained)" else "Under-gained", Color(0xFFF59E0B))
        actualGain > expectedGainMax -> Pair(if (isBengali) "অতিরিক্ত বৃদ্ধি (Over-gained)" else "Over-gained", StatusHigh)
        else -> Pair(if (isBengali) "আদর্শ ও স্বাস্থ্যকর বৃদ্ধি (On Track)" else "On Track", StatusNormal)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFFCE7F3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤰 MATERNITY CARE ASSISTANT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEC4899),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCE7F3))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "গর্ভকালীন সেবা",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEC4899)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "গর্ভবতী মায়ের সুস্থতা ও শিশুর সুরক্ষা ট্র্যাকার",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "বাচ্চার সাপ্তাহিক বৃদ্ধি, ওজন বাড়ার সঠিক গতি, অপরিহার্য ল্যাব টেস্টের নির্দেশিকা এবং নিরাপদ গাইডলাইন মিলিয়ে সুষম মাতৃকালীন সুস্থতা নিশ্চিত করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Section 1: Weekly Pregnancy Progress Calculator
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "১. শিশুর বর্তমান বৃদ্ধি ও প্রসব সময় ক্যালকুলেটর",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আপনার গর্ভকালীন সপ্তাহটি নির্বাচন করুন:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(trimesterColor.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "সপ্তাহ $weekInt",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = trimesterColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = currentWeek,
                        onValueChange = { currentWeek = it },
                        valueRange = 1f..40f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = trimesterColor,
                            activeTrackColor = trimesterColor,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("১ম সপ্তাহ", fontSize = 11.sp, color = LightTextSecondary)
                        Text("২০তম সপ্তাহ", fontSize = 11.sp, color = LightTextSecondary)
                        Text("৪০তম সপ্তাহ", fontSize = 11.sp, color = LightTextSecondary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(trimesterColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗓️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "বর্তমান ধাপ:",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = trimesterName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = trimesterColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delightful Visual Baby Size Fruit Comparison Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xD1A7F3D0), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(babyFruitComparison.second, fontSize = 28.sp)
                            }
                            Column {
                                Text(
                                    text = "বাচ্চার আনুমানিক আকার (Fruit/Veggie Size):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = babyFruitComparison.first,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("👶", fontSize = 18.sp)
                                    Text(
                                        text = "শিশুর অভ্যন্তরীণ বিকাশ:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFEDD5))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ওজন: ~$babyWeightEstimate",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = babyDevelopmentBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7C2D12),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1(b): Expected Due Date (EDD) Calculator from LMP
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "১(খ). প্রসবের সম্ভাব্য তারিখ (EDD) ও কাউন্টডাউন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFCE7F3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "শেষ মাসিকের তারিখ (LMP) ইনপুট দিন:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("দিন (Day)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = lmpDayInput,
                                onValueChange = { lmpDayInput = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Color(0xFFEC4899)
                                ),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("মাস (Month)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = lmpMonthInput,
                                onValueChange = { lmpMonthInput = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Color(0xFFEC4899)
                                ),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("বছর (Year)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = lmpYearInput,
                                onValueChange = { lmpYearInput = it.filter { c -> c.isDigit() }.take(4) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Color(0xFFEC4899)
                                ),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF1F2))
                            .border(1.dp, Color(0xFFFCE7F3), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📅", fontSize = 18.sp)
                                    Text(
                                        text = "সম্ভাবনা প্রসবের তারিখ (EDD):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFBE123C)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${eddCalculated.first} (${eddCalculated.second})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9F1239)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFFECDD3))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("প্রসবের বাকি আছে:", fontSize = 12.sp, color = Color(0xFFBE123C))
                                Text(
                                    text = if (eddCalculated.third > 0) "${eddCalculated.third} দিন" else if (eddCalculated.third == 0) "আজই আপনার প্রসবের দিন!" else "প্রসবের সময় অতিক্রান্ত হয়েছে",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFBE123C)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Weight Tracker and Calculator
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "২. গর্ভকালীন ওজন ট্র্যাকিং ও আইওএম (IOM) পরামর্শ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ওজন পরিবর্তন ইনপুট দিন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("পূর্বে ওজন (কেজি)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = prePregWeightInput,
                                onValueChange = { prePregWeightInput = it.filter { c -> c.isDigit() || c == '.' } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = MedicalPrimary
                                ),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("বর্তমান ওজন (কেজি)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = currentWeightInput,
                                onValueChange = { currentWeightInput = it.filter { c -> c.isDigit() || c == '.' } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = MedicalPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("উচ্চতা ফুট (Feet)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = feetInput,
                                onValueChange = { feetInput = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = MedicalPrimary
                                ),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("উচ্চতা ইঞ্চি (Inches)", style = MaterialTheme.typography.bodySmall, color = LightTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = inchesInput,
                                onValueChange = { inchesInput = it.filter { c -> c.isDigit() } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = MedicalPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val computedBmiStr = String.format("%.1f", prePregBmi)
                    val earnedGainStr = String.format("%.1f", actualGain)
                    val expectedMinStr = String.format("%.1f", expectedGainMin)
                    val expectedMaxStr = String.format("%.1f", expectedGainMax)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightBackground)
                            .border(1.dp, Color(0xFFEFF6FF), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("গর্ভপূর্ব বডি মাস ইনডেক্স (BMI):", style = MaterialTheme.typography.bodyMedium, color = LightTextSecondary)
                                Text("$computedBmiStr (BMI)", fontWeight = FontWeight.Bold, color = LightTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("মোট সংগৃহীত ওজন বৃদ্ধি (Gain):", style = MaterialTheme.typography.bodyMedium, color = LightTextSecondary)
                                Text("+$earnedGainStr কেজি", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("বর্তমান সপ্তাহে আশানুরূপ বৃদ্ধি:", style = MaterialTheme.typography.bodyMedium, color = LightTextSecondary)
                                Text("$expectedMinStr থেকে $expectedMaxStr কেজি", fontWeight = FontWeight.Bold, color = MedicalPrimary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ট্র্যাকার স্ট্যাটাস:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LightTextPrimary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(gainStatusColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = gainStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = gainStatusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "গাইডলাইন: আপনার ক্যাটাগরিতে (BMI: $computedBmiStr) গোটা গর্ভকালীন সময়ে মোট $minRecTotal থেকে $maxRecTotal কেজি ওজন বাড়া চিকিৎসাগতভাবে স্বাভাবিক ও কাম্য।",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Essential Pregnancy Lab Test Guidance & Parameters
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "৩. গর্ভকালীন আবশ্যক ল্যাব টেস্ট ও স্বাস্থ্য স্বাভাবিক মাত্রা",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val pregnancyLabs = listOf(
                PregnancyLabInfo(
                    "রক্তের হিমোগ্লোবিন (Hb Test)",
                    "রক্তস্বল্পতা (Anemia) নিরীক্ষা করতে। রক্তের আয়রন কমলে বাচ্চা ও মায়ের অক্সিজেন সরবরাহে ঘাটতি দেখা যায় এবং প্রসবকালীন অতিরিক্ত রক্তপাতের ঝুঁকি বৃদ্ধি পায়।",
                    "উচ্চ সতর্ক মান: ১১.০ g/dL এর নিচে থাকলে তা অ্যানিমিয়া বা রক্তাল্পতা নির্দেশ করে।",
                    "১ম ও ৩য় ত্রৈমাসিক: >১১.০ g/dL\n২য় ত্রৈমাসিক: >১০.৫ g/dL",
                    "🤰"
                ),
                PregnancyLabInfo(
                    "মুখের গ্লুকোজ সহনশীলতা (OGTT)",
                    "গর্ভকালীন ডায়াবেটিস (Gestational Diabetes) যাচাই করতে। গравни উন্মোষ বা ডায়াবেটিস শিশুর ওজন মারাত্মক বৃদ্ধি করে ও ডেলিভারিতে ঝুঁকি তৈরি করে।",
                    "খালি পেটে <৫.১ mmol/L\n১ ঘণ্টা পর <১০.০ mmol/L\n২ ঘণ্টা পর <৮.৫ mmol/L",
                    "স্বাভাবিক মাত্রা সমূহ অতিক্রম করলে ইনসুলিন বা কঠোর খাদ্যাভ্যাস চার্ট প্রয়োজন হতে পারে।",
                    "🩸"
                ),
                PregnancyLabInfo(
                    "রক্তের গ্রুপ ও আরএইচ ফ্যাক্টর (Rh Factor)",
                    "মায়ের রক্তের গ্রুপ নেগেটিভ (Rh-) এবং বাচ্চার গ্রুপ পজেটিভ (Rh+) হলে অ্যান্টিবডি গঠিত হয়ে পরবর্তী গর্ভধারণ মারাত্বক বাধার সম্মুখীন হয়।",
                    "आरএইচ নেগেটিভ (Rh-) হলে প্রসবকালীন সতর্কবার্তা অনুসরণে অ্যান্টি-ডি ইনজেকশন দিতে হবে।",
                    "পজেটিভ (+) অথবা নেগেটিভ (-)",
                    "💉"
                ),
                PregnancyLabInfo(
                    "রক্তচাপ পরীক্ষণ (Blood Pressure)",
                    "প্রি-এক্লাম্পসিয়া (Preeclampsia) নামক মারাত্মক ঝুঁকি এড়াতে। উচ্চ রক্তচাপ হলে মায়ের কিডনি ক্ষতিগ্রস্থ হয় এবং খিঁচুনির ভয় থাকে।",
                    "১৪০/৯০ mmHg এর সমান বা বেশি সীমানা স্পর্শ করলে জরুরি সতর্কবার্তা বিবেচনা করতে হবে।",
                    "১২০/৮০ mmHg এর কাছাকাছি নিরাপদ সীমা",
                    "🩺"
                ),
                PregnancyLabInfo(
                    "ইউরিন অ্যালবুমিন বা প্রোটিন (Urine Protein)",
                    "ইউরিনে প্রোটিন ক্ষরণ হওয়া এবং একই সঙ্গে উচ্চ রক্তচাপ থাকা প্রি-এক্লাম্পসিয়ার নিশ্চিত লক্ষণ।",
                    "স্বাভাবিক অবস্থায় প্রস্রাবে অ্যালবুমিন বা ক্ষরিত প্রোটিন অবশ্যই অনুপস্থিত (Nil) থাকা উচিত।",
                    "অনুপস্থিত (Trace/Nil)",
                    "🧪"
                ),
                PregnancyLabInfo(
                    "থাইরয়েড হরমোন (TSH Screening)",
                    "গর্ভকালীন থাইরয়েড হরমোন নিঃসরণ হ্রাসে গর্ভের বাচ্চার মস্তিষ্কের বিকাশ মারাত্মকভাবে ব্যাহত হতে পারে। বাচ্চার বুদ্ধি প্রতিবন্ধকতার ঝুঁকি এড়াতে এটি জরুরি।",
                    "১ম ত্রৈমাসিক: ০.১ - ২.৫ mIU/L\n২য় ত্রৈমাসিক: ০.২ - ৩.০ mIU/L\n৩য় ত্রৈমাসিক: ০.৩ - ৩.০ mIU/L",
                    "পরামর্শ: থাইরয়েডের তারতম্যে সকালের খালি পেটে থাইরক্সিন ঔষধ চিকিৎসকের পরামর্শ মোতাবেক দেওয়া হয়।",
                    "🦋"
                )
            )

            pregnancyLabs.forEach { lab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFEFF6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFCE7F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lab.icon, fontSize = 18.sp)
                            }
                            Text(
                                text = lab.testName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = lab.purpose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightTextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("নির্ধারিত লক্ষ্যমাত্রা:", fontSize = 11.sp, color = MedicalPrimary, fontWeight = FontWeight.Bold)
                                Text(lab.normalRange, style = MaterialTheme.typography.bodySmall, color = LightTextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("জরুরী সতর্কতা সংকেত:", fontSize = 11.sp, color = Color(0xFFC2410C), fontWeight = FontWeight.Bold)
                                Text(lab.warningSignal, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7C2D12))
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Emergency Warning Signs
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "৪. গর্ভকালীন ৫টি মারাত্বক বিপদের লক্ষণ (জরুরী সতর্ক থাকুন)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🚨", fontSize = 22.sp)
                        Text(
                            text = "অবিলম্বে হাসপাতাল বা ডাক্তারের শরণাপন্ন হোন:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val dangerSigns = listOf(
                        "১. যোনিপথে অতিরিক্ত রক্তস্রাব বা তরল ক্ষরণ শুরু হলে।",
                        "২. চোখে প্রচণ্ড ঝাপসা দেখা অথবা তীব্র অনবরত মাথাব্যথা হওয়া (প্রি-এক্লাম্পসিয়ার নিশ্চিত সোপান)।",
                        "৩. হাত-পা হঠাৎ ফুলে যাওয়া এবং প্রচণ্ড ওজন বৃদ্ধি পাওয়া।",
                        "৪. ডেলিভারির ডেট আসার পূর্বেই প্রচণ্ড পেটব্যাথা এবং তীব্র জ্বরের প্রাদুর্ভাব হওয়া।",
                        "五. গর্ভের শিশুর নড়াচড়া হঠাৎ লক্ষ্যণীয়ভাবে কমে যাওয়া বা বন্ধ হওয়া (১২ ঘন্টায় ১০ বারের কম)।"
                    )

                    dangerSigns.forEach { sign ->
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7F1D1D),
                            modifier = Modifier.padding(vertical = 4.dp),
                            lineHeight = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF87171))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো প্রকার অবহেলা করবেন না, প্রসব পরিকল্পনা আগে থেকেই ঠিক রাখুন।",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section 5: Trimester food list & avoiding list
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "৫. গর্ভবতী মায়ের সুষম খাদ্যাভ্যাস ও পরিহারযোগ্য খাবার",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("✅ पुষ্টিকর খাদ্যতালিকা:", fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val goods = listOf(
                            "ফোলেট সমৃদ্ধ শাকসবজি (পালং শাক, ডাল)",
                            "উচ্চ প্রোটিন (ডিম, লাল মাংস, দেশী ডাল)",
                            "ক্যালসিয়াম (দুধ, টক দই, ছোট মাছ)",
                            "পর্যাপ্ত লৌহ ও ফলমূল (আপেল, কলা, ডালিম)"
                        )
                        goods.forEach { g ->
                            Text("• $g", fontSize = 11.sp, color = Color(0xFF047857), modifier = Modifier.padding(vertical = 2.dp), lineHeight = 15.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("❌ অবশ্যই পরিহার করুন:", fontWeight = FontWeight.Bold, color = Color(0xFF9F1239), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val bads = listOf(
                            "কাঁচা বা অর্ধসিদ্ধ ডিম (সালমোনেলা ঝুঁকি)",
                            "অর্ধসিদ্ধ মাংস বা সোসেজ",
                            "কফি বা অতিরিক্ত ক্যাফেইন জাতীয় ড্রিংকস",
                            "পারদযুক্ত বড় সামুদ্রিক মাছ ও কাঁচা ফলমূল"
                        )
                        bads.forEach { b ->
                            Text("• $b", fontSize = 11.sp, color = Color(0xFFBE123C), modifier = Modifier.padding(vertical = 2.dp), lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        // Interactive Personalized Maternal Lab Records Inputs & PDF Generation Action
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "৬. আপনার পরীক্ষা নিরীক্ষার রিপোর্ট ইনপুট ও পিডিএফ জেনারেশন",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LightTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFBCFE8)), 
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "পিডিএফ রিপোর্টে আপনার কাস্টম ল্যাব মান যুক্ত করুন:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input 1: Hemoglobin
                    Text(
                        text = "রক্তের হিমোগ্লোবিন (Hb. Level) g/dL:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = hbInput,
                        onValueChange = { hbInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFFEC4899)
                        ),
                        singleLine = true,
                        placeholder = { Text("উদাহরণ: ১১.৫") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 2: Glucose OGTT
                    Text(
                        text = "গ্লুকোজ স্তর (OGTT Blood Glucose) mmol/L:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = ogttInput,
                        onValueChange = { ogttInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFFEC4899)
                        ),
                        singleLine = true,
                        placeholder = { Text("উদাহরণ: ৪.৮") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input 3: Blood Pressure
                    Text(
                        text = "রক্তচাপ (Blood Pressure) mmHg:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = bpInput,
                        onValueChange = { bpInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFFEC4899)
                        ),
                        singleLine = true,
                        placeholder = { Text("উদাহরণ: ১২০/৮০") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            com.example.util.ReportGenerator.sharePregnancyReport(
                                context = context,
                                isBengali = true, 
                                week = weekInt,
                                trimester = trimesterName,
                                edd = eddCalculated.first, 
                                fruitComparison = "${babyFruitComparison.second} ${babyFruitComparison.first}",
                                hbValue = hbInput,
                                glucoseValue = ogttInput,
                                bpValue = bpInput
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_share_pregnancy_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)), 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📄", fontSize = 16.sp)
                            Text(
                                text = "মাতৃত্বকালীন পিডিএফ রিপোর্ট শেয়ার করুন",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PregnancyLabInfo(
    val testName: String,
    val purpose: String,
    val warningSignal: String,
    val normalRange: String,
    val icon: String
)

fun com.example.util.HealthTip.getLocalizedTitle(isBengali: Boolean): String {
    return getLocalizedTitle(if (isBengali) "bn" else "en")
}

fun com.example.util.HealthTip.getLocalizedTitle(langCode: String): String {
    if (dayIndex == 0) return title
    return when (langCode) {
        "bn" -> title
        "hi" -> when (dayIndex) {
            1 -> "हाइड्रेशन और किडनी की देखभाल"
            2 -> "रक्त शर्करा (ग्लूकोज) प्रबंधन"
            3 -> "उच्च रक्तचाप और नमक का सेवन"
            4 -> "आवश्यक रक्त हीमोग्लोबिन"
            5 -> "चलना और स्वस्थ हृदय"
            6 -> "स्वस्थ लीवर कार्यप्रणाली"
            7 -> "रोग प्रतिरोधक क्षमता बढ़ाना"
            8 -> "कोलेस्ट्रॉल संतुलन और अनुपात"
            9 -> "हड्डियों और कैल्शियम की देखभाल"
            10 -> "दृष्टि पोषण और मार्गदर्शन"
            11 -> "फिल्ट्रेशन दर और क्रिएटिनिन"
            12 -> "मानसिक शांति और माइंडफुलनेस"
            24 -> "स्ट्रोक के लक्षण पहचानें"
            25 -> "संतुलित दोपहर का भोजन"
            26 -> "फेफड़ों और श्वास का व्यायाम"
            27 -> "कार्बोनेटेड सोडा से बचें"
            28 -> "कच्चे प्याज के फायदे"
            29 -> "रात में गर्म दूध के फायदे"
            30 -> "त्वचा की चमक"
            31 -> "सकारात्मकता और दीर्घायु"
            else -> title
        }
        "es" -> when (dayIndex) {
            1 -> "Hidratación y Cuidado Renal"
            2 -> "Control de Glucosa en Sangre"
            3 -> "Hipertensión e Ingesta de Sal"
            4 -> "Hemoglobina Esencial en Sangre"
            5 -> "Caminar por un Corazón Fuerte"
            6 -> "Función Hepática Saludable"
            7 -> "Refuerzo del Sistema Inmunitario"
            8 -> "Equilibrio y Proporción de Colesterol"
            9 -> "Cuidado de Huesos y Calcio"
            10 -> "Nutrición y Guía Visual"
            11 -> "Tasa de Filtración y Creatinina"
            12 -> "Paz Mental y Atención Plena"
            24 -> "Identificadores de Alerta de Ictus"
            25 -> "Plan de Almuerzo Balanceado"
            26 -> "Expansión Pulmonar y Respiración"
            27 -> "Evitar Bebidas Carbonatadas"
            28 -> "Beneficios de la Cebolla Cruda"
            29 -> "Beneficios de la Leche Tibia"
            30 -> "Radiación de la Piel y el Cuerpo"
            31 -> "Mente Positiva y Longevidad"
            else -> title
        }
        else -> when (dayIndex) {
            1 -> "Hydration and Kidney Care"
            2 -> "Blood Glucose Management"
            3 -> "Hypertension and Salt Intake"
            4 -> "Essential Blood Hemoglobin"
            5 -> "Walking and Strong Heart"
            6 -> "Live Healthy Liver Functioning"
            7 -> "Immune System Boosting"
            8 -> "Cholesterol Balance & Ratios"
            9 -> "Moth Bone and Calcium Care"
            10 -> "Eyesight Nutrition & Guidance"
            11 -> "Filtration Rate and Creatinine"
            12 -> "Mental Peace and Mindfulness"
            24 -> "Stroke Warning Identifiers"
            25 -> "Balanced Lunch Plan"
            26 -> "Lung Expansion and Breathing"
            27 -> "Avoid Carbonated Soda Drinks"
            28 -> "Raw Onions Immunity Benefits"
            29 -> "Warm Milk Night Benefits"
            30 -> "Skin and Body Radiance"
            31 -> "Positive Mind and Longevity"
            else -> title
        }
    }
}

fun com.example.util.HealthTip.getLocalizedCategory(isBengali: Boolean): String {
    return getLocalizedCategory(if (isBengali) "bn" else "en")
}

fun com.example.util.HealthTip.getLocalizedCategory(langCode: String): String {
    if (dayIndex == 0) return category
    return when (langCode) {
        "bn" -> category
        "hi" -> when {
            category.contains("হাইড্রেশন") -> "दैनिक हाइड्रेशन"
            category.contains("ডায়াবেটিস") -> "मधुमेह देखभाल"
            category.contains("হৃদ") -> "हृदय कल्याण"
            category.contains("রক্ত পরীক্ষা") -> "रक्त पोषण"
            category.contains("শারীরিক") -> "फिटनेस और व्यायाम"
            category.contains("লিভার") -> "लीवर स्वास्थ्य"
            category.contains("রোগ প্রতিরোধ") -> "रोग प्रतिरोधक क्षमता"
            category.contains("কোলেস্টেরল") -> "कोलेस्ट्रॉल की देखभाल"
            category.contains("হাড়ের") -> "हड्डियों का स्वास्थ्य"
            category.contains("চোখের") -> "नेत्र कल्याण"
            category.contains("কিডনি") -> "किडनी की देखभाल"
            category.contains("মানসিক") -> "मानसिक स्वास्थ्य"
            category.contains("জরুরি") -> "आपातकालीन टिप्पणी"
            category.contains("দুপুরের") -> "विशिष्ट दोपहर का भोजन"
            category.contains("শ্বসনতন্ত্র") -> "फेफड़ों की देखभाल"
            category.contains("কৃত্রিম") -> "प्रसंस्कृत खाद्य पदार्थ"
            category.contains("প্রাকৃতিক") -> "प्राकृतिक उपचार"
            category.contains("ঘুমের") -> "नींद अनुकूलक"
            category.contains("ত্বকের") -> "त्वचा की देखभाल"
            category.contains("সার্বিক") -> "समग्र कल्याण"
            else -> "सामान्य कल्याण"
        }
        "es" -> when {
            category.contains("হাইড্রেশন") -> "Hidratación Diaria"
            category.contains("ডায়াবেটিস") -> "Cuidado de la Diabetes"
            category.contains("হৃদ") -> "Bienestar del Corazón"
            category.contains("রক্ত পরীক্ষা") -> "Nutrición Sanguínea"
            category.contains("শারীরিক") -> "Gimnasia y Fitness"
            category.contains("লিভার") -> "Salud Hepática"
            category.contains("রোগ প্রতিরোধ") -> "Soporte Inmune"
            category.contains("কোলেস্টেরল") -> "Cuidado del Colesterol"
            category.contains("হাড়ের") -> "Salud Ósea"
            category.contains("চোখের") -> "Bienestar Visual"
            category.contains("কিডনি") -> "Cuidado Renal"
            category.contains("মানসিক") -> "Salud Mental"
            category.contains("জরুরি") -> "Nota de Emergencia"
            category.contains("দুপুরের") -> "Nutrición de Almuerzo"
            category.contains("শ্বসনতন্ত্র") -> "Cuidado Pulmonar"
            category.contains("কৃত্রিম") -> "Alimentos Procesados"
            category.contains("প্রাকৃতিক") -> "Remedios Naturales"
            category.contains("ঘুমের") -> "Optimizador de Sueño"
            category.contains("ত্বকের") -> "Cuidado de la Piel"
            category.contains("সার্বিক") -> "Bienestar General"
            else -> "Bienestar"
        }
        else -> when {
            category.contains("হাইড্রেশন") -> "Daily Hydration"
            category.contains("ডায়াবেটিস") -> "Diabetes Care"
            category.contains("হৃদ") -> "Heart Wellness"
            category.contains("রক্ত পরীক্ষা") -> "Blood Nutrition"
            category.contains("শারীরিক") -> "Fitness & Workout"
            category.contains("লিভার") -> "Liver Health"
            category.contains("রোগ প্রতিরোধ") -> "Immunity Support"
            category.contains("কোলেস্টেরল") -> "Cholesterol Care"
            category.contains("হাড়ের") -> "Bone Health"
            category.contains("চোখের") -> "Eye Wellness"
            category.contains("কিডনি") -> "Kidney Care"
            category.contains("মানসিক") -> "Mental Health"
            category.contains("জরুরি") -> "Emergency Note"
            category.contains("দুপুরের") -> "Lunch Nutrition"
            category.contains("শ্বসনতন্ত্র") -> "Lung Care"
            category.contains("কৃত্রিম") -> "Processed Foods"
            category.contains("প্রাকৃতিক") -> "Natural Remedies"
            category.contains("ঘুমের") -> "Sleep Optimizer"
            category.contains("ত্বকের") -> "Skin Care"
            category.contains("সার্বিক") -> "Overall Wellness"
            else -> "Wellness Care"
        }
    }
}

fun com.example.util.HealthTip.getLocalizedTip(isBengali: Boolean): String {
    return getLocalizedTip(if (isBengali) "bn" else "en")
}

fun com.example.util.HealthTip.getLocalizedTip(langCode: String): String {
    if (dayIndex == 0) return tip
    return when (langCode) {
        "bn" -> tip
        "hi" -> when (dayIndex) {
            1 -> "प्रतिदिन 2.5 से 3 लीटर साफ पानी पिएं। इससे किडनी को ठीक से काम करने और रक्तप्रवाह से अपशिष्ट पदार्थों को छानने में मदद मिलती है।"
            2 -> "अत्यधिक मीठे खाद्य पदार्थों से बचें। अपने दैनिक भोजन में फाइबर से भरपूर विकल्प जैसे कि ब्राउन राइस या ओट्स शामिल करें।"
            3 -> "दोपहर या रात के भोजन के समय ऊपर से कच्चा नमक बंद करें। भोजन में नमक की मात्रा सीमित रखें।"
            4 -> "हीमोग्लोबिन बढ़ाने के लिए भोजन में पालक, अनार, अंडे, और हरी पत्तेदार हरी सब्जियां शामिल करें।"
            5 -> "प्रतिदिन कम से कम 30 मिनट तेज चलें। इससे अच्छे कोलेस्ट्रॉल (HDL) को बढ़ाने और हानिकारक वसा को कम करने में मदद मिलती है।"
            6 -> "तले हुए खाद्य पदार्थों से दूर रहें। दैनिक आहार में एंटीऑक्सीडेंट से भरपूर हरी पत्तेदार सब्जियां और ताजे फलों का सेवन करें।"
            7 -> "अपने दैनिक आहार में विटामिन सी से भरपूर नींबू, आंवला, अमरूद और संतरे जैसे खट्टे फलों को शामिल करें।"
            8 -> "स्वस्थ दिल के लिए रिफाइंड तेल के बजाय सीमित मात्रा में सरसों के तेल या राइस ब्रान तेल का उपयोग करें।"
            9 -> "हड्डियों की मजबूती के लिए दूध, दही, छोटी मछलियाँ और पत्तेदार सब्जियाँ खाएँ। प्राकृतिक विटामिन डी के लिए सुबह की हल्की धूप लें।"
            10 -> "आँखों की रोशनी की सुरक्षा के लिए नारंगी, पीले और लाल फल (जैसे पपीता, कद्दू, गाजर) और ताजी वसायुक्त मछली खाएँ।"
            11 -> "रासायनिक रूप से परिरक्षित फास्ट फूड और डिब्बाबंद स्नैक्स से बचें। ब्लड शुगर और ब्लड प्रेशर पर कड़ा नियंत्रण रखें।"
            12 -> "तनाव कम करने और खुशहाल जीवन के लिए रोजाना 10-15 मिनट ध्यान लगाकर गहरी सांस लेने का अभ्यास करें।"
            24 -> "स्ट्रोक के शुरुआती संकेतों को पहचानें। रक्तचाप का स्वस्थ संतुलन और मानसिक शांति बनाए रखने से स्ट्रोक का खतरा 90% तक कम हो जाता है।"
            25 -> "दोपहर के भोजन में ककड़ी और गाजर के साथ ताज़ा सलाद शामिल करें। चावल की मात्रा सीमित कर हरी सब्जियां बढ़ाएं।"
            26 -> "सुबह पार्क में ताजी हवा में गहरी सांस लें और फेफड़ों की कार्यक्षमता बढ़ाने के लिए प्राणायाम का अभ्यास करें।"
            27 -> "पैकेट वाले मीठे जूस या कार्बोनेटेड सोडा से बचें। इसके बजाय नारियल पानी या घर की बनी छाछ (लस्सी) पिएं।"
            28 -> "कच्चे प्याज में कार्बनिक सल्फर होता है जो धमनियों को आराम देता है और रक्तचाप को नियंत्रित रखने में मदद करता।"
            29 -> "यदि आपको सोने में कठिनाई होती है, तो सोने से 30 मिनट पहले एक गिलास गुनगुना बिना चीनी वाला दूध पिएं।"
            30 -> "चीनी और तेल का सेवन कम करें। त्वचा को चमकदार बनाने के लिए पर्याप्त मात्रा में खीरा, नींबू पानी का उपयोग करें और भरपूर सोएं।"
            31 -> "सकारात्मक सोच रखने से मानसिक ऊर्जा बनी रहती है। प्रतिदिन परिवार के सदस्यों के साथ 10 मिनट बातचीत का आनंद लें।"
            else -> "स्वस्थ रहने के लिए अच्छी खुराक लें, पर्याप्त पानी पिएं और रोज़ाना व्यायाम करें।"
        }
        "es" -> when (dayIndex) {
            1 -> "Beba de 2.5 a 3 litros de agua limpia diariamente. Esto ayuda a los riñones a funcionar correctamente y filtrar los desechos."
            2 -> "Evite el exceso de azúcar. Adopte opciones ricas en fibra como arroz integral, avena o pan integral en sus comidas diarias."
            3 -> "Evite agregar sal cruda durante el almuerzo o la cena. Mantenga la cantidad de sal en los alimentos de manera muy limitada."
            4 -> "Para aumentar la hemoglobina, agregue espinacas, granadas, huevos y verduras de hoja verde a su menú."
            5 -> "Camine a paso ligero al menos 30 minutos al día. Esto ayuda a elevar el colesterol bueno (HDL) y reducir las grasas dañinas."
            6 -> "Deseche los alimentos fritos. Consuma diariamente verduras verdes ricas en antioxidantes y frutas frescas."
            7 -> "Incluya frutas cítricas ricas en vitamina C como limón, guayaba y naranjas en su plato de comida diario."
            8 -> "Para un corazón sano, cocine con aceite de mostaza o aceite de salvado de arroz en cantidades limitadas en lugar de aceites procesados."
            9 -> "Consuma leche, yogur y verduras de hoja diariamente para proteger la densidad ósea. Disfrute del sol de la mañana para obtener vitamina D."
            10 -> "Coma frutas de color naranja, amarillo y rojo (como papaya, calabaza, zanahorias) para proteger su salud visual."
            11 -> "Evite los alimentos procesados y enlatados. Mantenga el azúcar en sangre y la presión arterial bajo un control estricto."
            12 -> "Practique 10-15 minutos de respiración profunda rítmica diariamente para combatir el estrés y mantenerse saludable."
            24 -> "Reconozca los signos de un derrame cerebral. Controlar la presión arterial de forma segura reduce considerablemente los riesgos."
            25 -> "Incorpore ensaladas frescas en su almuerzo. Disminuya moderadamente la porción de arroz y aumente las verduras verdes."
            26 -> "Sienta el aire fresco por las mañanas y practique la respiración profunda pranayama para expandir sus pulmones."
            27 -> "Evite refrescos con alto contenido de gas o bebidas procesadas dulces. Elija jugo fresco de coco o suero de leche lassi."
            28 -> "La cebolla cruda contiene azufre orgánico que ayuda a relajar los vasos sanguíneos y mantener la presión bajo control."
            29 -> "Si experimenta dificultad para conciliar el sueño, beba un vaso de leche tibia sin azúcar 30 minutos antes de acostarse."
            30 -> "Reduzca el consumo de azúcares refinados y grasas trans. Coma pepino y limones frescos para dar brillo a la piel."
            31 -> "Una actitud de alegría y mente positiva mantiene el flujo de dopamina activo y saludable. Converse con su familia."
            else -> "Mantenga una alimentación saludable, beba abundante agua y manténgase físicamente activo todos los días para su bienestar general."
        }
        else -> when (dayIndex) {
            1 -> "Drink 2.5 to 3 liters of clean water daily. This helps the kidneys function properly and filter metabolic waste from the bloodstream."
            2 -> "Avoid excessive sugary products. Adopt high-fiber options such as brown rice, whole oats, or whole wheat bread in your daily meals."
            3 -> "Avoid adding extra raw salt on the side during lunch or dinner. Keep the overall salt content in curries extremely limited."
            4 -> "To boost hemoglobin, add spinach, pomegranates, lean red meats, liver, eggs, and local green leafy vegetables to your menu."
            5 -> "Walk briskly for at least 30 minutes daily. This helps raise high-density lipoprotein (HDL) and reduce low-density arterial fats."
            6 -> "Discard fatty, deep-fried snacks. Consume more antioxidant-rich leafy greens, broccoli, and tangy fresh fruits daily."
            7 -> "Include vitamin C rich local citrus fruits such as lemon, amla, guava, and oranges in your daily food plate."
            8 -> "For a healthy heart, cook with mustard oil or rice bran oil in limited amounts instead of standard processed seed oils."
            9 -> "Eat milk, yogurt, small fatty fish, and leafy vegetables daily to protect bone density. Enjoy early morning soft sunshine to get natural Vitamin D."
            10 -> "Eat orange, yellow, and red fruits (like papaya, pumpkin, carrots) and fresh fatty fish to protect your eyesight and support retinal cells."
            11 -> "Avoid chemically preserved fast foods and processed canned snacks. Keep blood sugar and blood pressure under pristine control."
            12 -> "Practice 10-15 minutes of rhythmic deep breathing daily to combat elevated cortisol stress and remain healthy and happy."
            24 -> "Recognize early signs of a stroke. Keeping a healthy balance of blood pressure and mental calm reduces stroke risks by up to 90%."
            25 -> "Incorporate fresh salads with sliced cucumber and carrots in your lunch. Moderately decrease the rice portion while scaling up green vegetables."
            26 -> "Breathe fresh air in local parks in the morning and practice pranayama deep breathing to expand arterial lung capacity to its fullest."
            27 -> "Ditch sugary packed juices or toxic carbonated sodas. Drink organic coconut water or light homemade buttermilk (lassi/ghol)."
            28 -> "Raw onions contain organic sulfur that helps relax arterial walls and naturally keeps elevated blood pressure under check."
            29 -> "If you experience trouble falling asleep, drink a glass of sugar-free lukewarm warm milk 30 minutes before bed."
            30 -> "Reduce refined sugar and oil intake. Consume plenty of raw cucumber, fresh lemon water, and sleep sufficiently to make your skin radiant."
            31 -> "A positive and mindful mindset keeps dopamine flowing securely. Enjoy 10 minutes of light-hearted chatting with family members daily."
            else -> "Keep eating healthy nutrition, drink plenty of water, and stay physically active every day for full-body wellness."
        }
    }
}

fun com.example.util.HealthTip.getLocalizedWarning(isBengali: Boolean): String {
    return getLocalizedWarning(if (isBengali) "bn" else "en")
}

fun com.example.util.HealthTip.getLocalizedWarning(langCode: String): String {
    if (dayIndex == 0) return warning
    return when (langCode) {
        "bn" -> warning
        "hi" -> when (dayIndex) {
            1 -> "निर्जलीकरण (पानी की कमी) से मूत्र संक्रमण, किडनी पर बोझ और दर्दनाक किडनी की पथरी का खतरा बढ़ जाता है।"
            2 -> "खाली पेट मीठे पेय या सोडा पीना इंसुलिन स्पाइक्स और ग्लूकोज के असंतुलन का कारण बन सकता है।"
            3 -> "कच्चा नमक रक्तचाप को बढ़ाता है, जिससे दिल के दौरे और स्ट्रोक का खतरा दोगुना हो जाता है।"
            4 -> "आयरन से भरपूर भोजन के तुरंत बाद चाय या कॉफी पीने से बचें, क्योंकि कैफीन आयरन के अवशोषण को रोकता है।"
            5 -> "घंटों एक जगह बैठे रहने से हृदय की कार्यक्षमता कम हो जाती है और पेट की चर्बी बढ़ती है।"
            6 -> "डॉक्टर की सलाह के बिना अक्सर दर्द निवारक दवाएं लेने से लीवर को गंभीर नुकसान पहुंच सकता है।"
            7 -> "अपर्याप्त नींद शरीर की रोग प्रतिरोधक क्षमता को कमजोर करती है, जिससे आप बार-बार वायरल संक्रमण के शिकार हो सकते हैं।"
            8 -> "गहरे तले हुए सड़क के किनारे मिलने वाले भोजन से धमनियों में थक्के जमते हैं, जिससे दिल का दौरा पड़ सकता है।"
            9 -> "अत्यधिक मीठे सोडा पेय हड्डियों को पतला और कमजोर बनाते हैं।"
            10 -> "अंधेरे कमरों में तेज रोशनी वाली स्क्रीन देखने से नींद में गड़बड़ी हो सकती है और आँखों को नुकसान पहुँचता है।"
            11 -> "अनियंत्रित उच्च रक्तचाप किडनी की महीन रक्त वाहिकाओं को नुकसान पहुँचाता है।"
            12 -> "सदा अत्यधिक तनाव में रहने से ग्लूकोज स्तर प्रभावित होकर डायबिटीज की समस्या को तेज कर सकता है।"
            24 -> "धीमी आवाज या हाथ-पैर में सुन्नता होने पर मरीज को देर किए बिना अस्पताल ले जाना चाहिए।"
            25 -> "भारी दोपहर के भोजन के पश्चात लेटने अथवा गहरी नींद सोने से पाचन की गति धीमी पड़ जाती है।"
            26 -> "तंबाकू के धुएं से बचें। दूसरों का धुआं भी आपके फेफड़ों को भारी नुकसान पहुँचाता है।"
            27 -> "डब्बाबंद शीतल पेय पदार्थों में मौजूद उच्च फ्रुक्टोज लीवर के स्वास्थ्य के लिए बहुत हानिकारक है।"
            28 -> "अत्यधिक गैस्ट्रिक होने पर खाली पेट प्याज खाने से सीने में जलन या दर्द की शिकायत हो सकती है।"
            29 -> "यदि आपको लैक्टोज से समस्या है, तो दूध के स्थान पर पेपरमिंट चाय पिएं जो पाचन को सुधारेगी।"
            30 -> "तेज धूप में बिना छाते के जाने से त्वचा पर समय से पूर्व झुर्रियां तथा काले निशान पड़ सकते हैं।"
            31 -> "अत्यधिक क्रोध अथवा मनमुटाव रखने से पूरे शरीर के अंदर तनाव व थकान का स्तर गंभीर हो जाता है।"
            else -> "कृपया डॉक्टर से सलाह लें यदि लक्षण बने रहते हैं।"
        }
        "es" -> when (dayIndex) {
            1 -> "La deshidratación aumenta el riesgo de infecciones urinarias, sobrecarga renal y dolorosos cálculos renales."
            2 -> "Beber refrescos azucarados con el estómago vacío puede desencadenar picos extremos de insulina en la sangre."
            3 -> "La sal cruda eleva la presión arterial, duplicando el riesgo de accidentes cerebrovasculares y cardiopatías."
            4 -> "Evite consumir té o café inmediatamente después de comer alimentos ricos en hierro, ya que bloquea el hierro."
            5 -> "Sentarse en un lugar durante horas reduce la eficiencia del bombeo del corazón y acumula grasa abdominal."
            6 -> "El consumo frecuente de analgésicos sin el consejo de un médico puede dañar gravemente su hígado."
            7 -> "El sueño inadecuado debilita el sistema inmune, haciéndole proner a las infecciones de virus recurrentes."
            8 -> "Los alimentos fritos en la calle depositan placa arterial de forma rápida, promoviendo problemas cardíacos."
            9 -> "Los refrescos excesivamente azucarados disminuyen la densidad mineral de sus huesos."
            10 -> "Mirar pantallas inteligentes muy brillantes en habitaciones oscuras puede causar fatiga visual permanente."
            11 -> "La presión arterial alta no controlada daña las delicadas nefronas del riñón."
            12 -> "Los altos niveles continuos de cortisol desequilibran el sistema nervioso general."
            24 -> "El habla arrastrada y la caída facial son alertas críticas. Diríjase a urgencias de inmediato."
            25 -> "Tomar una siesta pesada justo después de comer bloquea el flujo del tracto digestivo."
            26 -> "Inhalar humo de tabaco ajeno es igual de nocivo; evite estas zonas."
            27 -> "El jarabe de maíz alto en fructosa en las bebidas envasadas promueve hígado graso."
            28 -> "Comer cebollas crudas con el estómago vacío puede desencadenar acidez si tiene gastritis."
            29 -> "Si tiene intolerancia a la lactosa, elija una taza de té de menta nocturna reconfortante."
            30 -> "Bajo el sol fuerte, no protegerse la piel acelera manchas oscuras y arrugas prematuras."
            31 -> "Acumular rencor o enojo eleva la inflamación de las células de forma crónica."
            else -> "Por favor, consulte a su médico si tiene fiebre persistente."
        }
        else -> when (dayIndex) {
            1 -> "Dehydration dramatically increases the risk of urinary infections, kidney overload, and painful kidney stones."
            2 -> "Drinking carbonated soft drinks or sweetened sodas on an empty stomach can trigger extreme insulin spikes and glucose dysregulation."
            3 -> "Raw salt immediately elevates blood pressure, doubling your risk of sudden strokes and chronic cardiovascular disease."
            4 -> "Avoid consuming hot tea or filtered coffee immediately after iron-rich meals, as caffeine blocks non-heme iron absorption."
            5 -> "Sitting in one place for hours diminishes heart pumping efficiency and contributes to visceral abdominal fat accumulation."
            6 -> "Consuming high doses of painkillers (like NSAIDs) frequently without a licensed practitioner's direct advice can severely damage your liver."
            7 -> "Inadequate sleep weakens the immune system's cell generation, making you highly prone to recurrent viral infections."
            8 -> "Deep-fried street foods and saturated trans fats deposit arterial plaque rapidly, promoting premature heart attacks."
            9 -> "Excessive sugary sodas and carbonated beverages damage the gastrointestinal ability to absorb calcium, thinning the bones."
            10 -> "Staring at high-brightness smartphones or laptop screens in dark rooms can cause permanent sleep disturbance and damage photoreceptors."
            11 -> "Uncontrolled status of chronic high blood pressure damages delicate kidney nephrons and speeds up the decline of eGFR."
            12 -> "Chronic mental anxiety or untreated panic stress dysregulates the autonomic nervous system, promoting high blood sugar."
            24 -> "Slurred speech, facial drooping, or numbness in one arm are immediate signs of a stroke. Rush the patient to the nearest hospital immediately."
            25 -> "Taking a heavy afternoon nap immediately after a large lunch significantly delays gastric emptying and blocks digestive track flow."
            26 -> "Passive inhalation of tobacco smoke is equally dangerous. Always stay clear of public smoking zones to protect your bronchial tract."
            27 -> "High-fructose corn syrup in industrial beverages induces non-alcoholic fatty liver disease (NAFLD) and visceral flatulence."
            28 -> "If you have prone gastritis, consuming raw onions on an empty stomach can trigger severe abdominal cramps or heartburn."
            29 -> "If you are lactose intolerant, skip milk and drink peppermint herbal tea or warm water to soothe late-night digestion."
            30 -> "Exposing yourself to midday direct sunlight without a sun blocker or umbrella accelerates dark spots, sunburns, and wrinkles."
            31 -> "Carrying toxic thoughts, harbor-long grudges, or intense anger elevates chronic low-grade cellular inflammation."
            else -> "Consult a physician immediately if physical pain, breathing distress, or high fever persists for multiple days."
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrescriptionDetailScreen(
    prescription: com.example.data.PrescriptionDetail,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBengali by viewModel.isBengali.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // App top bar
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
                .drawBehind {
                    drawLine(
                        color = Color(0xFFEFF6FF),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.stopSpeaking()
                    onBack()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .testTag("rx_detail_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LightTextPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (languageCode) {
                        "bn" -> "প্রেসক্রিপশন বিবরণী"
                        "hi" -> "प्रिस्क्रिप्शन विवरण"
                        "es" -> "Detalle de la Prescripción"
                        else -> "Prescription Detail"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightTextPrimary
                )
                Text(
                    text = when (languageCode) {
                        "bn" -> "এআই দ্বারা ডিজিটালভাবে বিশ্লেষিত ও অনূদিত"
                        "hi" -> "AI द्वारा डिजिटल रूप से विश्लेषित और अनूदित"
                        "es" -> "Analizado y traducido digitalmente mediante IA"
                        else -> "Digitally parsed & translated via AI"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LightTextSecondary
                )
            }
        }

        // Quick Controls Card: Language Switcher, Read Aloud, PDF Share
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFEFF6FF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Language selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp)
                ) {
                    val languages = listOf(
                        "en" to "English",
                        "bn" to "বাংলা",
                        "hi" to "हिंदी",
                        "es" to "Español",
                        "ar" to "العربية",
                        "fr" to "Français"
                    )
                    val languageCode by viewModel.languageCode.collectAsState()
                    languages.forEach { (code, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (languageCode == code) MedicalPrimary else Color.Transparent)
                                .clickable { viewModel.setLanguageCode(code) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (languageCode == code) Color.White else LightTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // TTS and PDF actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Audio Play Aloud
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakPrescription()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSpeaking) Color(0xFFEF4444) else MedicalTertiary)
                            .size(36.dp)
                    ) {
                        Text(
                            text = if (isSpeaking) "⏹️" else "🔊",
                            fontSize = 14.sp
                        )
                    }

                    // Share PDF
                    IconButton(
                        onClick = {
                            com.example.util.ReportGenerator.sharePrescriptionReport(
                                context = context,
                                isBengali = isBengali,
                                prescription = prescription
                            )
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MedicalTertiary)
                            .size(36.dp)
                    ) {
                        Text(
                            text = "📤",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Doctor Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F766E).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🩺", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = prescription.doctorName.ifEmpty {
                                    when (languageCode) {
                                        "bn" -> "চিকিৎসকের নাম উল্লিখিত নেই"
                                        "hi" -> "चिकित्सक का नाम निर्दिष्ट नहीं है"
                                        "es" -> "Médico No Especificado"
                                        else -> "Doctor Name Not Specified"
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                            Text(
                                text = prescription.doctorSpecialty.ifEmpty {
                                    when (languageCode) {
                                        "bn" -> "ক্লিনিক্যাল বিশেষজ্ঞ"
                                        "hi" -> "नैदानिक विशेषज्ञ"
                                        "es" -> "Especialista Clínico"
                                        else -> "Clinical Specialist"
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            if (prescription.date.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${when (languageCode) {
                                        "bn" -> "তারিখ: "
                                        "hi" -> "दिनांक: "
                                        "es" -> "Fecha: "
                                        else -> "Date: "
                                    }} ${prescription.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Symptoms & Diagnosis (If present)
            if (prescription.symptomsAndDiagnosis.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // subtle amber
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when (languageCode) {
                                    "bn" -> "লক্ষণ ও রোগ নির্ণয়"
                                    "hi" -> "लक्षण और नैदानिक नोट"
                                    "es" -> "Síntomas y Notas Clínicas"
                                    else -> "Symptoms & Clinical Notes"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = prescription.symptomsAndDiagnosis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF78350F),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Prescribed Medicines Title
            item {
                Text(
                    text = when (languageCode) {
                        "bn" -> "নির্ধারিত ঔষধসমূহ"
                        "hi" -> "निर्धारित दवाएं"
                        "es" -> "Medicamentos Recetados"
                        else -> "Prescribed Medicines"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Medicines List
            if (prescription.medicines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (languageCode) {
                                    "bn" -> "কোনো ঔষধের বিবরণ পাওয়া যায়নি।"
                                    "hi" -> "स्कैन में कोई दवा नहीं मिली।"
                                    "es" -> "No se detectaron medicamentos en el escaneo."
                                    else -> "No medicines detected in scan."
                                },
                                color = LightTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(prescription.medicines) { med ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_item_${med.name.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💊", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = med.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LightTextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Badges flow row for guidelines
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (med.dosage.isNotEmpty()) {
                                    val dosagePrefix = when (languageCode) {
                                        "bn" -> "ডোজ: "
                                        "hi" -> "खुराक: "
                                        "es" -> "Dosis: "
                                        else -> "Dosage: "
                                    }
                                    MedBadge(label = "$dosagePrefix${med.dosage}", bgColor = Color(0xFFEFF6FF), textColor = MedicalPrimary)
                                }
                                if (med.frequency.isNotEmpty()) {
                                    MedBadge(label = med.frequency, bgColor = Color(0xFFECFDF5), textColor = Color(0xFF047857))
                                }
                                if (med.timing.isNotEmpty()) {
                                    MedBadge(label = med.timing, bgColor = Color(0xFFFFF7ED), textColor = Color(0xFFC2410C))
                                }
                                if (med.duration.isNotEmpty()) {
                                    MedBadge(label = med.duration, bgColor = Color(0xFFF5F3FF), textColor = Color(0xFF6D28D9))
                                }
                            }

                            val purpose = when (languageCode) {
                                "bn" -> med.purposeBen
                                "hi" -> med.purposeHi.ifEmpty { med.purposeEng }
                                "es" -> med.purposeEs.ifEmpty { med.purposeEng }
                                "ar" -> med.purposeAr.ifEmpty { med.purposeEng }
                                "fr" -> med.purposeFr.ifEmpty { med.purposeEng }
                                else -> med.purposeEng
                            }
                            if (purpose.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = when (languageCode) {
                                                "bn" -> "কার্যকারিতা / কেন খেতে হবে:"
                                                "hi" -> "उद्देश्य / सेवन का कारण:"
                                                "es" -> "Propósito / Por qué consumir:"
                                                else -> "Purpose / Why to consume:"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LightTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = purpose,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LightTextPrimary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Diagnostic recommendations (if present)
            if (prescription.recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = when (languageCode) {
                            "bn" -> "প্রস্তাবিত পরীক্ষা সমূহের নির্দেশক"
                            "hi" -> "अनुशंसित आकलन"
                            "es" -> "Evaluaciones Recomendadas"
                            else -> "Recommended Assessments"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp)
                    )
                }

                items(prescription.recommendations) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFEFF6FF))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔬", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rec.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTextPrimary
                                )
                            }
                            val explanation = when (languageCode) {
                                "bn" -> rec.descBen
                                "hi" -> rec.descHi.ifEmpty { rec.descEng }
                                "es" -> rec.descEs.ifEmpty { rec.descEng }
                                "ar" -> rec.descAr.ifEmpty { rec.descEng }
                                "fr" -> rec.descFr.ifEmpty { rec.descEng }
                                else -> rec.descEng
                            }
                            if (explanation.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LightTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // General Advice & Guidance Card
            item {
                val adviceText = when (languageCode) {
                    "bn" -> prescription.adviceBen
                    "hi" -> prescription.adviceHi.ifEmpty { prescription.adviceEng }
                    "es" -> prescription.adviceEs.ifEmpty { prescription.adviceEng }
                    "ar" -> prescription.adviceAr.ifEmpty { prescription.adviceEng }
                    "fr" -> prescription.adviceFr.ifEmpty { prescription.adviceEng }
                    else -> prescription.adviceEng
                }
                if (adviceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)), // smooth green
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (languageCode) {
                                        "bn" -> "চিকিৎসকের মূল উপদেশ ও পরামর্শ"
                                        "hi" -> "डॉक्टर की सामान्य सलाह और जीवनशैली युक्तियाँ"
                                        "es" -> "Consejos Generales del Médico y Estilo de Vida"
                                        else -> "Doctor's General Advice & Lifestyle Tips"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = adviceText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF065F46),
                                lineHeight = 18.sp
                            )
                            
                            if (prescription.followUpDate.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF059669), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    val followUpLabel = when (languageCode) {
                                        "bn" -> "ফলো-আপের সময়: "
                                        "hi" -> "अनुवर्ती तिथि: "
                                        "es" -> "Fecha de Seguimiento: "
                                        else -> "Follow-up Date: "
                                    }
                                    Text(
                                        text = "$followUpLabel${prescription.followUpDate}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedBadge(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


