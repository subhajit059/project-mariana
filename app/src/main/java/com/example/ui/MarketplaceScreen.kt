package com.example.ui

import com.example.ui.theme.*
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketplaceSite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // ViewModel State Flow Collectors
    val marketplaces by viewModel.marketplaces.collectAsStateWithLifecycle()
    val filteredMarketplaces by viewModel.filteredMarketplaces.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isGeminiLoading by viewModel.isGeminiLoading.collectAsStateWithLifecycle()
    
    val isScraping by viewModel.isScraping.collectAsStateWithLifecycle()
    val scraperProgress by viewModel.scraperProgress.collectAsStateWithLifecycle()
    val scraperLogs by viewModel.scraperLogs.collectAsStateWithLifecycle()
    
    val bridgeRedirectState by viewModel.bridgeRedirectState.collectAsStateWithLifecycle()

    // Local UI states
    var isDeveloperHubOpen by remember { mutableStateOf(false) }
    var isGeminiChatOpen by remember { mutableStateOf(false) }
    var inputQueryText by remember { mutableStateOf("") }
    var activeDevTab by remember { mutableStateOf(0) } // 0: Scraper logs, 1: Configurations, 2: Add market

    // Form inputs for Custom Site Creation
    var formName by remember { mutableStateOf("") }
    var formUrl by remember { mutableStateOf("") }
    var formTruthUrl by remember { mutableStateOf("") }
    var formSelector by remember { mutableStateOf("a.official-link") }
    var formDesc by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Drawing a gorgeous Marian and Strange Blue celestial gradient
                val backgroundBrush = Brush.verticalGradient(
                    colors = listOf(
                        MarianBlueDark,          // Deep classic liturgical Marian Blue top surface
                        RoyalNavy,               // Mysterious rich space navy blue transition
                        StrangeBlueMidnight      // Dark cosmic strange blue deep abyss at the bottom
                    )
                )
                drawRect(brush = backgroundBrush)

                // 2. Main Radial Celestial Glow centered at the top representing majestic light filtering down
                val sunGlow = Brush.radialGradient(
                    colors = listOf(
                        StrangeCyan.copy(alpha = 0.24f),       // Luminous cenote cyan center glow
                        MarianBlue.copy(alpha = 0.12f),       // Faded Marian classic blue aura
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.height * 0.5f
                )
                drawRect(brush = sunGlow)

                // 3. Render realistic soft cosmic ray beams radiating downwards
                val rayColor = StrangeBlue
                val rayCount = 10
                for (i in 0 until rayCount) {
                    val startX = size.width / 2f
                    val startY = 0f
                    
                    // Angles radiating between 55 to 125 degrees
                    val angleRad = (Math.PI * (0.32 + (i.toFloat() / (rayCount - 1)) * 0.36)).toFloat()
                    val endX = startX + (size.height * Math.cos(angleRad.toDouble())).toFloat()
                    val endY = size.height
                    
                    drawLine(
                        color = rayColor.copy(alpha = 0.02f + 0.08f * (1f - Math.abs(i - rayCount / 2f) / (rayCount / 2f))),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = size.width * 0.07f,
                        cap = StrokeCap.Round
                    )
                }

                // 4. Render floating luminous celestial bubble indicators
                val bubblePoints = listOf(
                    Offset(size.width * 0.15f, size.height * 0.35f),
                    Offset(size.width * 0.25f, size.height * 0.60f),
                    Offset(size.width * 0.38f, size.height * 0.22f),
                    Offset(size.width * 0.52f, size.height * 0.78f),
                    Offset(size.width * 0.72f, size.height * 0.45f),
                    Offset(size.width * 0.85f, size.height * 0.70f),
                    Offset(size.width * 0.12f, size.height * 0.85f),
                    Offset(size.width * 0.65f, size.height * 0.18f)
                )
                val bubbleSizes = listOf(6f, 13f, 5f, 15f, 10f, 9f, 12f, 8f)
                for (idx in bubblePoints.indices) {
                    val pos = bubblePoints[idx]
                    val r = bubbleSizes[idx]
                    drawCircle(
                        color = StrangeCyan.copy(alpha = 0.18f),
                        radius = r,
                        center = pos,
                        style = Stroke(width = 1.2f)
                    )
                    drawCircle(
                        color = IceCyan.copy(alpha = 0.12f),
                        radius = r * 0.5f,
                        center = pos - Offset(r * 0.2f, r * 0.2f)
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Main Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Brand Logo & Configuration Panel Toggle styled to match Sleek Design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlossySmileyLogo(
                        modifier = Modifier
                            .size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MARKETPLACE AGGREGATOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StrangeCyan.copy(alpha = 0.8f),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Portal Hub",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // w-10 h-10 rounded-full with Settings Toggle using Theme Color tokens
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StrangeBlue.copy(alpha = 0.2f))
                        .border(1.dp, StrangeCyan.copy(alpha = 0.3f), CircleShape)
                        .clickable { isDeveloperHubOpen = true }
                        .testTag("dev_settings_button")
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(StrangeCyan, CircleShape)
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Welcome Greeting: Restored personalization per user's directive
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Your move, Subhajit!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .testTag("personal_greeting_text")
                            .animateContentSize()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Simplified Search Container & Keyword Shortcut Chips
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.98f)
                            .padding(vertical = 8.dp)
                    ) {
                        // Clean Search Input Bar using themed custom colors
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(StrangeBlueMidnight.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                                .border(
                                    BorderStroke(1.dp, StrangeBlue.copy(alpha = 0.49f)),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search icon",
                                tint = StrangeCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputQueryText.isEmpty()) {
                                    Text(
                                        text = "Search secure marketplaces...",
                                        color = Color(0xFF64748B),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = inputQueryText,
                                    onValueChange = { 
                                        inputQueryText = it
                                        viewModel.updateSearchQuery(it)
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController?.hide()
                                    }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("simplified_search_input")
                                )
                            }

                            if (inputQueryText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        inputQueryText = ""
                                        viewModel.updateSearchQuery("")
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear search text",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Downside Market Keyword Search Chips (clickable shortcuts)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val keywords = listOf("All", "Indian", "Hollywood", "4K", "Anime", "Korean", "Software", "Game")
                            
                            // Let's draw horizontal layout for keywords
                            Box(modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(keywords) { keyword ->
                                        val isSelected = if (keyword == "All") inputQueryText.isEmpty() else inputQueryText.equals(keyword, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSelected) Color(0xFF2563EB).copy(alpha = 0.22f) 
                                                    else Color(0xFF0A1323).copy(alpha = 0.5f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF3B82F6) 
                                                    else getMarketplaceThemeColor(keyword).copy(alpha = 0.35f),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    val query = if (keyword == "All") "" else keyword
                                                    inputQueryText = query
                                                    viewModel.updateSearchQuery(query)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            getMarketplaceThemeColor(keyword),
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = keyword,
                                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Section Title: ACTIVE DESTINATIONS
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTIVE DESTINATIONS",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${filteredMarketplaces.size} Verified",
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Empty list configuration fallback state
                if (filteredMarketplaces.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag("empty_markets_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x331E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "No sites match",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Marketplaces Listed",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "No results match your search query \"$searchQuery\". Try adding a new marketplace or cleaning your search."
                                    } else {
                                        "Initialize database seed or click Settings in top right to create custom shortcuts."
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Marketplace List Rendering
                    items(filteredMarketplaces, key = { it.id }) { market ->
                        val isResolving = bridgeRedirectState?.site?.id == market.id
                        MarketplaceCardItem(
                            market = market,
                            isResolving = isResolving,
                            onRedirect = { viewModel.launchSecureBridge(context, market) },
                            onDelete = { viewModel.deleteMarketplaceSite(market) }
                        )
                    }
                }

                // Sleek Interface Footer Banner
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2563EB).copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF3B82F6).copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Providing direct source cart links",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD).copy(alpha = 0.8f),
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "OF SECONDARY WEBSITE DIRECT LINKS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom 3-dot decoration from design layout
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF2563EB), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF1E3A8A), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF1E3A8A), CircleShape))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // 1. IN-APP SECURE ROUTING GATEWAY DIALOG (/go/[site_id])
        bridgeRedirectState?.let { state ->
            Dialog(
                onDismissRequest = { viewModel.dismissBridgeRedirect() },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("bridge_router_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF2563EB))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.size(46.dp),
                                color = Color(0xFF60A5FA),
                                strokeWidth = 3.dp,
                                trackColor = Color(0xFF1E293B)
                            )
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Routing Security Lock",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "SECURE BRIDGE TRANSACTION",
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Routing to ${state.site.name}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Internal Route Indicator: /go/${state.site.id}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Current Database URL: ${state.site.currentUrl}",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.W500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF3B82F6),
                            trackColor = Color(0xFF1E293B)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.step,
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.height(32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.dismissBridgeRedirect() }
                        ) {
                            Text("Abort Route Verification", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. DIALOG: GEMINI ASSISTANT RESPONSIVE PANEL
        if (isGeminiChatOpen) {
            Dialog(
                onDismissRequest = { isGeminiChatOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .padding(16.dp)
                        .testTag("gemini_chat_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF3B82F6), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini Market Assistant",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { isGeminiChatOpen = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Exit chat", tint = Color.White)
                            }
                        }

                        Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 8.dp))

                        // Message History List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            reverseLayout = false,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            if (chatHistory.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "✨ Secure Query Console",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "AI-powered tool checking, decentralized link safety advice, and cryptographic routing questions in real-time.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        listOf(
                                            "Which markets are currently online?",
                                            "How does the automated daily scraper verify sites?",
                                            "Create the postgres schema for supabase database",
                                            "Explain secure bridge redirection"
                                        ).forEach { query ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.sendGeminiQuery(query) }
                                                    .padding(12.dp)
                                            ) {
                                                Text(query, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(chatHistory) { msg ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(
                                                    color = if (msg.isUser) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                                                    shape = RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (msg.isUser) 12.dp else 0.dp,
                                                        bottomEnd = if (msg.isUser) 0.dp else 12.dp
                                                    )
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = msg.message,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (isGeminiLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color(0xFF60A5FA),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Gemini is analyzing...",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom message field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var innerChatText by remember { mutableStateOf("") }
                            BasicTextField(
                                value = innerChatText,
                                onValueChange = { innerChatText = it },
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (innerChatText.isNotEmpty()) {
                                        viewModel.sendGeminiQuery(innerChatText)
                                        innerChatText = ""
                                    }
                                }),
                                decorationBox = { innerTextField ->
                                    if (innerChatText.isEmpty()) {
                                        Text("Ask follow-up query...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = {
                                    if (innerChatText.isNotEmpty()) {
                                        viewModel.sendGeminiQuery(innerChatText)
                                        innerChatText = ""
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { viewModel.clearChat() }) {
                                Text("Clear Console Logs", color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                            Text(
                                "AI suggestions verified through room-cache",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. DIALOG: ARCHITECTURE AND DEVELOPER HUB (SUPABASE + SCRAPER GENERATOR)
        if (isDeveloperHubOpen) {
            Dialog(
                onDismissRequest = { isDeveloperHubOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .padding(12.dp)
                        .testTag("developer_hub_dialog"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Developer Admin Center",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Setup and Configuration Generator",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { isDeveloperHubOpen = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Exit Hub", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab Selection (Material 3 style)
                        TabRow(
                            selectedTabIndex = activeDevTab,
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF10B981),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Tab(
                                selected = activeDevTab == 0,
                                onClick = { activeDevTab = 0 },
                                text = { Text("Scraper Console", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = activeDevTab == 1,
                                onClick = { activeDevTab = 1 },
                                text = { Text("Deploy Codes", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = activeDevTab == 2,
                                onClick = { activeDevTab = 2 },
                                text = { Text("Add Market", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tab Contents
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (activeDevTab) {
                                0 -> {
                                    // SCRAPER SIMULATION LOGGER CONSOLE
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Verification Logging Pipeline",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Button(
                                                onClick = { viewModel.runScraperCheck() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                enabled = !isScraping,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                if (isScraping) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(12.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Crawling...", fontSize = 11.sp)
                                                    }
                                                } else {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.Refresh, contentDescription = "Run", modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Trigger Scraper", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (scraperProgress.isNotEmpty()) {
                                            Text(
                                                text = "⚡ Active Task: $scraperProgress",
                                                color = Color(0xFFF59E0B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // SSH / CMD styled console
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                reverseLayout = true
                                            ) {
                                                if (scraperLogs.isEmpty()) {
                                                    item {
                                                        Text(
                                                            text = "Consoles clear. Ready to run Node scraper link simulator execution. Click 'Trigger Scraper' above to simulate official forums parsing and live DB updates.",
                                                            color = Color(0xFF475569),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                } else {
                                                    // Display in reversed order to see freshest first or normal
                                                    items(scraperLogs.reversed()) { logLine ->
                                                        Text(
                                                            text = logLine,
                                                            color = if (logLine.contains("🚨") || logLine.contains("ALERT")) Color(0xFFEF4444)
                                                                    else if (logLine.contains("🎉") || logLine.contains("Success")) Color(0xFF10B981)
                                                                    else if (logLine.contains("🟢")) Color(0xFF34D399)
                                                                    else if (logLine.contains("🔍")) Color(0xFF38BDF8)
                                                                    else Color(0xFFE2E8F0),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.padding(vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    // DEPLOYMENT CONFIGURATIONS (Supabase schema, scripts, workflow)
                                    val configurationList = listOf(
                                        "Supabase Database SQL Schema" to getSqlCode(),
                                        "NodeJS Scraper Agent (scraper.js)" to getScraperJsCode(),
                                        "GitHub Actions daily Cron Job (YML)" to getGithubYmlCode()
                                    )
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(configurationList) { (title, code) ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = title,
                                                        color = Color(0xFF38BDF8),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "COPY CODE",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .clickable {
                                                                clipboardManager.setText(AnnotatedString(code))
                                                                Toast.makeText(context, "Copied code block safely", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .padding(4.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(160.dp)
                                                        .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                        .padding(10.dp)
                                                ) {
                                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                        item {
                                                            Text(
                                                                text = code,
                                                                color = Color(0xFF94A3B8),
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    // ADD CUSTOM SECURE SEED MANUAL MARKETPLACE
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = "Include Custom Shortcuts & Routing Selector",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = formName,
                                                onValueChange = { formName = it },
                                                label = { Text("Marketplace Name", color = Color(0xFF94A3B8)) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = formUrl,
                                                onValueChange = { formUrl = it },
                                                label = { Text("Active URL / Onion Mirror", color = Color(0xFF94A3B8)) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = formTruthUrl,
                                                onValueChange = { formTruthUrl = it },
                                                label = { Text("Source of Truth URL (e.g. tracking forum)", color = Color(0xFF94A3B8)) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = formSelector,
                                                onValueChange = { formSelector = it },
                                                label = { Text("HTML Selector Element", color = Color(0xFF94A3B8)) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        item {
                                            OutlinedTextField(
                                                value = formDesc,
                                                onValueChange = { formDesc = it },
                                                label = { Text("Short Description", color = Color(0xFF94A3B8)) },
                                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        item {
                                            Button(
                                                onClick = {
                                                    if (formName.isBlank() || formUrl.isBlank() || formTruthUrl.isBlank()) {
                                                        Toast.makeText(context, "Please configure all required fields", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        viewModel.addMarketplaceSite(
                                                            name = formName,
                                                            currentUrl = formUrl,
                                                            sourceOfTruthUrl = formTruthUrl,
                                                            selector = formSelector,
                                                            desc = formDesc
                                                        )
                                                        Toast.makeText(context, "Added market to secure ledger!", Toast.LENGTH_SHORT).show()
                                                        
                                                        // Reset form fields
                                                        formName = ""
                                                        formUrl = ""
                                                        formTruthUrl = ""
                                                        formSelector = "a.official-link"
                                                        formDesc = ""
                                                        
                                                        activeDevTab = 0 // jump to log screen
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Submit To Database", color = Color.Black, fontWeight = FontWeight.Bold)
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
    }
}

   /**
 * Custom Marketplace list item design following glassmorphism guidelines
 */
@Composable
fun MarketplaceCardItem(
    market: MarketplaceSite,
    isResolving: Boolean = false,
    onRedirect: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = getMarketplaceThemeColor(market.name)
    val shimmerBrush = rememberShimmerBrush(showShimmer = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("market_card_${market.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isResolving) Color(0xFF040A1A) else Color(0xFF121826).copy(alpha = 0.60f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (isResolving) 1.5.dp else 1.dp,
            color = if (isResolving) Color(0xFF3B82F6).copy(alpha = 0.85f) else accentColor.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isResolving) {
                // REDIRECT RESOLVING SKELETON STATE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shimmers initials box with an integrated rotating spinner
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF60A5FA),
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            // Skeleton bar for name
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Skeleton bar for status / details
                            Box(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }

                    // Loading/Resolving Pill Button replacement
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(88.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF60A5FA), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Routing..",
                                color = Color(0xFF60A5FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Skeleton block for description
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(shimmerBrush)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush)
                    )
                }
            } else {
                // NORMAL STATE (with customized individual colors for simple & different aesthetic)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val splitParts = market.name.split(" ").filter { it.isNotEmpty() }
                        val initials = if (splitParts.isNotEmpty()) {
                            splitParts.take(2).map { it.first().uppercase() }.joinToString("")
                        } else {
                            market.name.take(2).uppercase()
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = market.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotColor = when (market.status) {
                                    "ONLINE" -> Color(0xFF10B981)
                                    "OFFLINE" -> Color(0xFFEF4444)
                                    "VERIFYING" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (market.status == "ONLINE") "Verified Status" else "Status: ${market.status}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (market.latencyMs > 0) {
                                    Text(
                                        text = " • ${market.latencyMs}ms",
                                        fontSize = 10.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    // Go Live Action Button (Sleek Style Pill matching individual accent colors)
                    Button(
                        onClick = onRedirect,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Go Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Description / Detail info underneath (if present)
                if (market.siteDescription.isNotBlank() || market.sourceOfTruthUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (market.siteDescription.isNotBlank()) {
                        Text(
                            text = market.siteDescription,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Security: PGP SHA-256",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569)
                        )
                        
                        // Delete Button
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove custom shortcut",
                                tint = Color(0xFFEF4444).copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper generators to return setup codes cleanly for developers
private fun getSqlCode(): String {
    return """
-- PostgreSQL / Supabase Tables Setup
CREATE TYPE market_status AS ENUM ('ONLINE', 'OFFLINE', 'VERIFYING', 'REDIRECTING');

CREATE TABLE public.marketplaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    current_url VARCHAR(255) NOT NULL,
    source_of_truth_url VARCHAR(255) NOT NULL,
    selector_pattern VARCHAR(100) DEFAULT 'a.official-link',
    status market_status DEFAULT 'ONLINE',
    latency_ms INTEGER DEFAULT 0,
    description TEXT,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Row Level Security (RLS) policies
ALTER TABLE public.marketplaces ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read" ON public.marketplaces 
    FOR SELECT USING (true);
    
CREATE POLICY "Service-role full admin" ON public.marketplaces 
    FOR ALL TO service_role USING (true) WITH CHECK (true);
""".trimIndent()
}

private fun getScraperJsCode(): String {
    return """
// Node.js scraper - run via daily GitHub action
const { createClient } = require('@supabase/supabase-js');
const axios = require('axios');
const cheerio = require('cheerio');

const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SERVICE_ROLE_KEY);

async function run() {
  const { data: marketplaces } = await supabase.from('marketplaces').select('*');
  for (const m of marketplaces) {
    try {
      const res = await axios.get(m.source_of_truth_url);
      const $ = cheerio.load(res.data);
      const extracted = $(m.selector_pattern).attr('href') || $(m.selector_pattern).text();
      
      if (extracted && extracted.trim() !== m.current_url) {
        await supabase.from('marketplaces')
          .update({ current_url: extracted.trim(), last_updated: new Date() })
          .eq('id', m.id);
        console.log(`Updated link for ${"$"}{m.name}`);
      }
    } catch (e) {
      console.error(e);
    }
  }
}
run();
""".trimIndent()
}

private fun getGithubYmlCode(): String {
    return """
name: Marketplace Link Scraper
on:
  schedule:
    - cron: '0 0 * * *' # Daily at midnight
  workflow_dispatch:
jobs:
  run-scraper:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm install @supabase/supabase-js axios cheerio
      - env:
          SUPABASE_URL: ${"$"}{{ secrets.SUPABASE_URL }}
          SUPABASE_SERVICE_ROLE_KEY: ${"$"}{{ secrets.SUPABASE_SERVICE_ROLE_KEY }}
        run: node scraper.js
""".trimIndent()
}

@Composable
fun GlossySmileyLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val radius = width / 2f
        val center = Offset(width / 2f, height / 2f)

        // 1. Shadow backing
        drawCircle(
            color = Color(0x3B000000),
            radius = radius,
            center = center + Offset(0f, radius * 0.08f)
        )

        // 2. Base plate Smiley Cap - Glossy Blue-to-Cyan base
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8), // Glowing cyan
                    Color(0xFF0284C7), // Saturated blue
                    Color(0xFF0369A1)  // Rich deep blue border
                ),
                center = center - Offset(radius * 0.15f, radius * 0.15f),
                radius = radius * 1.05f
            ),
            radius = radius,
            center = center
        )

        // 3. Inner glossy ring highlighting high-contrast reflection rim
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height * 0.7f
            ),
            radius = radius * 0.94f,
            center = center,
            style = Stroke(width = radius * 0.04f)
        )

        // 4. Left Eye Highlight bubble
        val leftEyeCenter = Offset(width * 0.36f, height * 0.34f)
        val eyeRadius = radius * 0.11f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE0F2FE),
                    Color(0xFF0284C7)
                ),
                center = leftEyeCenter,
                radius = eyeRadius
            ),
            radius = eyeRadius,
            center = leftEyeCenter
        )
        // High specular gloss dot for Left Eye
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.35f,
            center = leftEyeCenter - Offset(eyeRadius * 0.25f, eyeRadius * 0.25f)
        )

        // Right Eye Highlight bubble
        val rightEyeCenter = Offset(width * 0.64f, height * 0.34f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE0F2FE),
                    Color(0xFF0284C7)
                ),
                center = rightEyeCenter,
                radius = eyeRadius
            ),
            radius = eyeRadius,
            center = rightEyeCenter
        )
        // High specular gloss dot for Right Eye
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.35f,
            center = rightEyeCenter - Offset(eyeRadius * 0.25f, eyeRadius * 0.25f)
        )

        // 5. Smiling mouth: Thick glassy stroke Arc
        val smileWidth = width * 0.50f
        val smileHeight = height * 0.32f
        val mouthPath = Path().apply {
            addArc(
                oval = Rect(
                    left = center.x - smileWidth / 2f,
                    top = center.y - smileHeight / 3f,
                    right = center.x + smileWidth / 2f,
                    bottom = center.y + smileHeight * 0.8f
                ),
                startAngleDegrees = 15f,
                sweepAngleDegrees = 150f
            )
        }

        // Draw shadow line
        drawPath(
            path = mouthPath,
            color = Color(0xFF0369A1).copy(alpha = 0.5f),
            style = Stroke(
                width = radius * 0.16f,
                cap = StrokeCap.Round
            )
        )

        // Draw primary cyan highlight cap path
        drawPath(
            path = mouthPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFBAE6FD), // soft baby cyan highlight
                    Color(0xFF0284C7)  // deep blue solid base
                )
            ),
            style = Stroke(
                width = radius * 0.13f,
                cap = StrokeCap.Round
            )
        )

        // Draw inner white glossy specular light streak reflecting along the bottom-lip contour
        drawPath(
            path = mouthPath,
            color = Color.White.copy(alpha = 0.72f),
            style = Stroke(
                width = radius * 0.04f,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * Custom color accents based on category patterns to fulfill "all market place colar ar simple and diffrent" rule.
 */
fun getMarketplaceThemeColor(name: String): Color {
    val clean = name.lowercase()
    return when {
        clean.contains("indian") -> Color(0xFFF97316)  // Vibrant Amber/Orange
        clean.contains("hollywood") -> Color(0xFF8B5CF6) // Royal Violet/Purple
        clean.contains("4k") -> Color(0xFFEAB308)        // Shiny Gold/Yellow
        clean.contains("anime") -> Color(0xFFEC4899)     // Bright Pink/Magenta
        clean.contains("korean") -> Color(0xFF14B8A6)    // Fresh Jade Teal
        clean.contains("software") -> Color(0xFF60A5FA)  // Electric Sky Blue
        clean.contains("game") -> Color(0xFF22C55E)      // Gamer Green
        else -> Color(0xFF3B82F6)                        // Deep Oceanic Blue default
    }
}

/**
 * Animated shimmering skeleton loading brush adhering to the dark blue/black theme.
 */
@Composable
fun rememberShimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_coordinate"
        )
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF040A17), // Solid deep navy base
                Color(0xFF0A1E43), // Ambient dark blue shine
                Color(0xFF2563EB).copy(alpha = 0.45f), // High contrast ice-blue glow
                Color(0xFF0A1E43), // Ambient dark blue shine
                Color(0xFF040A17)  // Solid deep navy base
            ),
            start = Offset(translateAnimation - 350f, translateAnimation - 350f),
            end = Offset(translateAnimation + 350f, translateAnimation + 350f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

