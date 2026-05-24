package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.admin.AdminViewModel
import com.example.admin.FirestoreDoc
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: AdminViewModel = viewModel()
    val context = LocalContext.current

    val screenState by viewModel.appScreen.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isImageUploading by viewModel.isImageUploading.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Handle feedback messages as clean native toasts
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CleanSlateBg)
        ) {
            when (screenState) {
                "login" -> LoginScreen(viewModel)
                "dashboard" -> DashboardConsole(viewModel)
                "settings" -> SettingsScreen(viewModel)
            }

            // Global translucent loading progress cover
            if (isSyncing || isImageUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {}, // Consume clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = DeepBlueCorp,
                                strokeWidth = 5.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isImageUploading) "📤 Uploading Image to ImgBB..." else "⚡ Processing Database Query...",
                                color = CharcoalBlack,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🔑 Admin Login Panel with standard, beautiful branding.
 * Features customizable Bot Token & Chat ID fields and an automatic developer bypass logic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(viewModel: AdminViewModel) {
    var botTokenInput by remember { mutableStateOf(viewModel.prefs.telegramBotToken) }
    var chatIdInput by remember { mutableStateOf(viewModel.prefs.telegramChatId) }
    var userOtpInput by remember { mutableStateOf("") }
    val isOtpSent by viewModel.isOtpSent.collectAsState()

    // Safe counter for developer bypass
    var titleTapCount by remember { mutableStateOf(0) }
    var showBypassDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            // Elegant Visual Brand Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepBlueCorp, VibrantGreenAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Shield Logo",
                    tint = Color.White,
                    modifier = Modifier.size(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title (Hold to bypass secret trigger)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.combinedClickable(
                    onClick = {
                        titleTapCount += 1
                        if (titleTapCount >= 5) {
                            showBypassDialog = true
                            titleTapCount = 0
                        }
                    },
                    onLongClick = {
                        showBypassDialog = true
                    }
                )
            ) {
                Text(
                    text = "SOHAM TALLY ACADEMY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepBlueCorp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.testTag("login_brand_title")
                )
                Text(
                    text = "Latur's Trusted Accountant Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MutedSlate,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "⚡ ADMİN CONSOLE ⚡",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VibrantGreenAccent,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Authentication Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = CardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Telegram API Configuration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepBlueCorp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text("Telegram Bot Token") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_token_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = chatIdInput,
                        onValueChange = { chatIdInput = it },
                        label = { Text("Telegram Chat ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_chatid_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.triggerTelegramOtp(botTokenInput, chatIdInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlueCorp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_send_otp")
                    ) {
                        Icon(imageVector = Icons.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📩 Send OTP To Bot Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    if (isOtpSent) {
                        Divider(modifier = Modifier.padding(vertical = 20.dp))

                        Text(
                            text = "🔐 OTP Verification Area",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantGreenAccent,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = userOtpInput,
                            onValueChange = { userOtpInput = it },
                            label = { Text("Enter 6-Digit Verification Code") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_otp_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.verifyOtp(userOtpInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreenAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_verify_login")
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Admin Console 🟢", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    // Hidden Bypass Modal for testing support
    if (showBypassDialog) {
        Dialog(onDismissRequest = { showBypassDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = CardBorder(),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛠️ System Dev Bypass",
                        color = DeepBlueCorp,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This allows testing the Admin Panel Firestore functions in local/emulator mode immediately! Code matches महेश सर prefix code.",
                        fontSize = 13.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            showBypassDialog = false
                            viewModel.verifyOtp("770944")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlueCorp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Unlock Dashboard Instantly", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showBypassDialog = false }) {
                        Text("Cancel", color = MutedSlate)
                    }
                }
            }
        }
    }
}

/**
 * 📊 Dynamic Admin Console with responsive multi-tabs bottom design element.
 */
@Composable
fun DashboardConsole(viewModel: AdminViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header Unit
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlueCorp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VibrantGreenAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Soham Academy",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Project: ${viewModel.prefs.firebaseProjectId}",
                                color = ActiveBlueLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { viewModel.refreshAllData() }) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setScreen("settings") }) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.logOut() }) {
                            Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                }

                if (isSyncing) {
                    LinearProgressIndicator(
                        color = VibrantGreenAccent,
                        trackColor = DeepBlueCorp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        // Active Dashboard Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "gallery_success" -> LeftTabGallerySuccess(viewModel)
                "home_news" -> CenterTabHomeNews(viewModel)
                "enquiries" -> RightTabEnquiries(viewModel)
            }
        }

        // Bottom Navigation Bar
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
        ) {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "gallery_success",
                    onClick = { viewModel.setTab("gallery_success") },
                    icon = { Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = "Gallery") },
                    label = { Text("Gallery", fontWeight = FontWeight.Bold) },
                    colors = selectTabColors()
                )

                NavigationBarItem(
                    selected = activeTab == "home_news",
                    onClick = { viewModel.setTab("home_news") },
                    icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home/News", fontWeight = FontWeight.Bold) },
                    colors = selectTabColors()
                )

                NavigationBarItem(
                    selected = activeTab == "enquiries",
                    onClick = { viewModel.setTab("enquiries") },
                    icon = { Icon(imageVector = Icons.Filled.Receipt, contentDescription = "Requests") },
                    label = { Text("Requests", fontWeight = FontWeight.Bold) },
                    colors = selectTabColors()
                )
            }
        }
    }
}

/**
 * 🏆 LEFT TAB: Gallery & Success manager
 * Handles database items under `galleries_and_success` firestore folder.
 */
@Composable
fun LeftTabGallerySuccess(viewModel: AdminViewModel) {
    val galleryFeed by viewModel.galleryFeed.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (galleryFeed.isEmpty()) {
            EmptyListPlaceholder(
                icon = Icons.Filled.PhotoLibrary,
                text = "No Student Gallery elements found inside 'galleries_and_success' Firestore database."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(galleryFeed) { item ->
                    GalleryCard(item = item, onDelete = { viewModel.removeGalleryItem(item.id) })
                }
            }
        }

        // Add action button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = DeepBlueCorp,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_gallery")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Success Story")
        }
    }

    if (showAddDialog) {
        AddGalleryDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }
}

@Composable
fun GalleryCard(item: FirestoreDoc, onDelete: () -> Unit) {
    var expandedDescDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedDescDialog = true }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = item.getStr("imageUrl").ifBlank { item.getStr("image") },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.getStr("title", "Student Success"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueCorp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.getStr("description").ifBlank { "No explanation written" },
                    fontSize = 11.sp,
                    color = MutedSlate,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Modal to read massive success details
    if (expandedDescDialog) {
        Dialog(onDismissRequest = { expandedDescDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = CardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(20.dp)) {
                    item {
                        Text(
                            text = item.getStr("title", "Success Detail Story"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepBlueCorp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = item.getStr("imageUrl").ifBlank { item.getStr("image") },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Full Biography Details:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibrantGreenAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.getStr("description"),
                            fontSize = 14.sp,
                            color = CharcoalBlack,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { expandedDescDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlueCorp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done Reading")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Dialog to post Student Success stories.
 * Supports up to 10,000 characters description and raw ImgBB upload integration.
 */
@Composable
fun AddGalleryDialog(viewModel: AdminViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("🏆 Certified Accountant") }
    var imageUrl by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadLocalImage(it) { uploadedUrl ->
                imageUrl = uploadedUrl
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = CardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text(
                        text = "🏆 Post Success Portfolio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepBlueCorp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Headline Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Image Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanSlateBg),
                        border = CardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Story Banner Setup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlueCorp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Image Direct URL Link") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MutedSlate),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload From Mobile File (ImgBB)")
                            }

                            if (imageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column {
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { if (it.length <= 10000) desc = it },
                            label = { Text("Success Biography / Review") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            maxLines = 10
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Max 10k character limit rule",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                            Text(
                                text = "${desc.length} / 10000",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (desc.length > 9500) Color.Red else MutedSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss", color = MutedSlate)
                        }

                        Button(
                            onClick = {
                                viewModel.publishGallerySuccess(imageUrl, desc, title)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreenAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Publish🟢", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📰 CENTER TAB: Home (News feeds & special offers)
 * Connected with Firestore `news` collection.
 */
@Composable
fun CenterTabHomeNews(viewModel: AdminViewModel) {
    val newsFeed by viewModel.newsFeed.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (newsFeed.isEmpty()) {
            EmptyListPlaceholder(
                icon = Icons.Filled.Home,
                text = "No News active alerts or offers displayed on student screen. Add a new one!"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(newsFeed) { item ->
                    NewsItemCard(item = item, onDelete = { viewModel.removeNews(item.id) })
                }
            }
        }

        // Add action button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = DeepBlueCorp,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_news")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Publish News")
        }
    }

    if (showAddDialog) {
        AddNewsDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
    }
}

@Composable
fun NewsItemCard(item: FirestoreDoc, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = CardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Optional Header Banner
            val imageLink = item.getStr("imageUrl").ifBlank { item.getStr("image") }
            if (imageLink.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = imageLink,
                        contentDescription = "Alert banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Badge(
                        containerColor = VibrantGreenAccent,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "OFFER ACTIVE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.getStr("title", "Academy Bulletin"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepBlueCorp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val price = item.getStr("price")
                val dateTime = item.getStr("dateTime")

                if (price.isNotBlank() || dateTime.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (price.isNotBlank()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Fee: $price", fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(labelColor = DarkGreenAccent)
                            )
                        }
                        if (dateTime.isNotBlank()) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Batch: $dateTime", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = item.getStr("description").ifBlank { item.getStr("text") },
                    fontSize = 13.sp,
                    color = CharcoalBlack,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "System stamp: ${item.id}",
                    fontSize = 10.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Detailed add news modal supporting ImgBB image file triggers.
 */
@Composable
fun AddNewsDialog(viewModel: AdminViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var shortDesc by remember { mutableStateOf("") }
    var longDesc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("₹ 4000/-") }
    var dateTime by remember { mutableStateOf("New batch starting next Monday, 10:00 AM") }
    var imageUrl by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadLocalImage(it) { uploadedUrl ->
                imageUrl = uploadedUrl
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = CardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(20.dp)) {
                item {
                    Text(
                        text = "📰 Publish Announcement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepBlueCorp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Alert/Offer Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Fee Pricing (e.g. ₹ 4000/-)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = { dateTime = it },
                        label = { Text("Date & Timing (Batch schedules)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Image Layout setup
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanSlateBg),
                        border = CardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Header Banner Configuration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlueCorp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it },
                                label = { Text("Past direct JPEG image URL") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MutedSlate),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Image File (ImgBB)")
                            }

                            if (imageUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = shortDesc,
                        onValueChange = { shortDesc = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = longDesc,
                        onValueChange = { longDesc = it },
                        label = { Text("Extended Syllabus / Offer Details (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MutedSlate)
                        }

                        Button(
                            onClick = {
                                viewModel.publishNews(
                                    title = title,
                                    shortDesc = shortDesc,
                                    longDesc = longDesc,
                                    price = price,
                                    dateTimeString = dateTime,
                                    imageUrl = imageUrl
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreenAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Publish Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📩 RIGHT TAB: Enquiry Requests list
 * Combines collections `enquiries` and `enquiry` for flawless coverage.
 */
@Composable
fun RightTabEnquiries(viewModel: AdminViewModel) {
    val enquiries by viewModel.enquiries.collectAsState()

    if (enquiries.isEmpty()) {
        EmptyListPlaceholder(
            icon = Icons.Filled.Receipt,
            text = "No Student Enquiry submissions found inside Firestore Database folder 'enquiries'."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📩 Submitted Enquiries (${enquiries.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepBlueCorp
                    )
                    Text(
                        text = "Real-time sync Active",
                        fontSize = 11.sp,
                        color = VibrantGreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(enquiries) { item ->
                EnquiryItemCard(item = item, onDelete = { viewModel.removeEnquiry(item.id) })
            }
        }
    }
}

@Composable
fun EnquiryItemCard(item: FirestoreDoc, onDelete: () -> Unit) {
    val context = LocalContext.current

    // Extract enquiry attributes safely
    val name = item.getStr("name").ifBlank { item.getStr("fullName", "Potential student") }
    val phone = item.getStr("phone").ifBlank { item.getStr("mobile", "N/A") }
    val email = item.getStr("email").ifBlank { "N/A" }
    val city = item.getStr("city").ifBlank { item.getStr("location", "Latur") }
    val edu = item.getStr("education").ifBlank { item.getStr("qualification", "Graduate") }
    val interest = item.getStr("course").ifBlank { item.getStr("courseInterest", "Tally Prime") }
    val userMsg = item.getStr("message").ifBlank { item.getStr("question", "No custom notes") }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = CardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(VibrantGreenAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = DeepBlueCorp
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Solve Inquiry", tint = Color.Red)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Meta Details info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "📞 Mobile: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Text(text = phone, fontSize = 13.sp, color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "✉️ Email: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Text(text = email, fontSize = 13.sp, color = CharcoalBlack)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "📍 Location: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Text(text = city, fontSize = 13.sp, color = CharcoalBlack)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🎓 Qualification: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Text(text = edu, fontSize = 13.sp, color = CharcoalBlack)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "📖 Course: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Text(
                        text = interest,
                        fontSize = 13.sp,
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.Black
                    )
                }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(text = "📝 Student Message:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MutedSlate)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CleanSlateBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = userMsg,
                            fontSize = 13.sp,
                            color = CharcoalBlack,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Call
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueCorp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Student", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // WhatsApp Connect
                Button(
                    onClick = {
                        try {
                            val cleanNumber = phone.replace("+", "").replace(" ", "")
                            val url = "https://api.whatsapp.com/send?phone=91$cleanNumber&text=Hello%20$name!%20Soham%20Tally%20Academy%20Latur%20welcomes%20you.%20Mahesh%20Sir%20received%20your%20admission%20interest!"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open WhatsApp link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreenAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * ⚙️ SETTINGS SCREEN: Manage remote Firebase secrets dynamically
 */
@Composable
fun SettingsScreen(viewModel: AdminViewModel) {
    var fbBlockText by remember { mutableStateOf("") }
    
    var botToken by remember { mutableStateOf(viewModel.prefs.telegramBotToken) }
    var chatId by remember { mutableStateOf(viewModel.prefs.telegramChatId) }
    var imgbbKey by remember { mutableStateOf(viewModel.prefs.imgbbApiKey) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBarHeader(title = "⚙️ Admin Settings", onBack = { viewModel.setScreen("dashboard") })

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Firebase Project Setup Configuration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueCorp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Copy-paste the complete Firebase configuration JS object block generated from Firebase console directly below. This parsing algorithm extracts critical tokens instantly.",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fbBlockText,
                    onValueChange = { fbBlockText = it },
                    placeholder = {
                        Text(
                            text = "Paste standard snippet:\nconst firebaseConfig = {\n  apiKey: \"...\",\n  projectId: \"sohamtally-org\"\n};"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val parsed = viewModel.prefs.parseAndSaveFirebaseBlock(fbBlockText)
                        if (parsed) {
                            viewModel.showFeedback("🎉 Firebase credentials parsed and initialized dynamically!")
                            viewModel.refreshAllData()
                        } else {
                            viewModel.showFeedback("⚠️ No standard matches found inside pasted string. Verify block variables.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueCorp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💡 Parse & Save Object", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Divider()
            }

            item {
                Text(
                    text = "System Coordinates Overview",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlueCorp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.prefs.firebaseProjectId,
                    onValueChange = { viewModel.prefs.firebaseProjectId = it.trim() },
                    label = { Text("Firestore Project ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = viewModel.prefs.firebaseApiKey,
                    onValueChange = { viewModel.prefs.firebaseApiKey = it.trim() },
                    label = { Text("Firestore Key Token") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = botToken,
                    onValueChange = {
                        botToken = it
                        viewModel.prefs.telegramBotToken = it.trim()
                    },
                    label = { Text("Telegram Token credentials") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = chatId,
                    onValueChange = {
                        chatId = it
                        viewModel.prefs.telegramChatId = it.trim()
                    },
                    label = { Text("Telegram Group ID (-100xxxx)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = imgbbKey,
                    onValueChange = {
                        imgbbKey = it
                        viewModel.prefs.imgbbApiKey = it.trim()
                    },
                    label = { Text("ImgBB API Secret Token") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.prefs.resetToDefaults()
                            viewModel.showFeedback("♻️ Credentials reset to default standard Tally workspace!")
                            viewModel.setScreen("login")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Defaults", color = Color.Red)
                    }

                    Button(
                        onClick = {
                            viewModel.setScreen("dashboard")
                            viewModel.refreshAllData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreenAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save & Apply All🟢", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 🎨 Shared style utilities
 */
@Composable
fun TopAppBarHeader(title: String, onBack: () -> Unit) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlueCorp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun EmptyListPlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MutedSlate.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = MutedSlate,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun selectTabColors(): NavigationBarItemColors {
    return NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        unselectedIconColor = MutedSlate,
        selectedTextColor = DeepBlueCorp,
        unselectedTextColor = MutedSlate,
        indicatorColor = DeepBlueCorp
    )
}

@Composable
fun CardBorder(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, LightSlateBorder)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
