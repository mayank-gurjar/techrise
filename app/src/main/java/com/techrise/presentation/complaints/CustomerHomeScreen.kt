package com.techrise.presentation.complaints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techrise.presentation.theme.*
import kotlinx.coroutines.delay
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.NewsResponse
import com.techrise.data.remote.BannerResponse
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.techrise.R
import java.text.SimpleDateFormat
import java.util.*

class HeaderWaveShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - 20f)
            quadraticTo(
                size.width * 0.5f, size.height + 10f,
                0f, size.height - 35f
            )
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

enum class CustomerScreen {
    DASHBOARD,
    COMPLAINTS,
    FEEDBACK,
    NEWS,
    SUPPORT,
    PROFILE
}

@Composable
fun CustomHeader(
    currentScreen: CustomerScreen,
    email: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE65100),
                        Color(0xFFFF9100)
                    )
                ),
                shape = HeaderWaveShape()
            )
    ) {
        // Decorative background circles
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-40).dp)
                .background(Color.White.copy(alpha = 0.08f), shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-20).dp, y = 30.dp)
                .background(Color.White.copy(alpha = 0.05f), shape = CircleShape)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 16.dp)
            ) {
                if (currentScreen == CustomerScreen.DASHBOARD) {
                    Text(
                        text = "Tech Rise Portal",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        val screenTitle = when (currentScreen) {
                            CustomerScreen.COMPLAINTS -> "My Complaints"
                            CustomerScreen.FEEDBACK -> "Feedback Center"
                            CustomerScreen.NEWS -> "News Bulletins"
                            CustomerScreen.SUPPORT -> "Support Center"
                            CustomerScreen.PROFILE -> "User Profile"
                            else -> ""
                        }
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }

            if (currentScreen == CustomerScreen.DASHBOARD) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 16.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: CustomerViewModel,
    onCreateComplaintClick: () -> Unit,
    onComplaintClick: (id: String) -> Unit,
    onLogout: () -> Unit
) {
    val complaintsState by viewModel.complaintsState.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    val bannersState by viewModel.bannersState.collectAsState()
    var currentScreen by remember { mutableStateOf(CustomerScreen.DASHBOARD) }

    LaunchedEffect(Unit) {
        viewModel.loadComplaints()
        viewModel.loadNewsFeed()
        viewModel.loadBanners()
    }

    Scaffold(
        containerColor = Color(0xFFFFF8F4),
        topBar = {
            CustomHeader(
                currentScreen = currentScreen,
                email = viewModel.getEmail(),
                onBack = { currentScreen = CustomerScreen.DASHBOARD },
                onLogout = {
                    viewModel.logout()
                    onLogout()
                }
            )
        },
        floatingActionButton = {
            if (currentScreen == CustomerScreen.COMPLAINTS) {
                ExtendedFloatingActionButton(
                    onClick = onCreateComplaintClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Complaint") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.loadComplaints()
                    viewModel.loadNewsFeed()
                    viewModel.loadBanners()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentScreen) {
                CustomerScreen.DASHBOARD -> DashboardContent(
                    viewModel = viewModel,
                    complaintsCount = (complaintsState as? ComplaintsUiState.Success)?.complaints?.size ?: 0,
                    activeComplaintsCount = (complaintsState as? ComplaintsUiState.Success)?.complaints?.count { it.status != "RESOLVED" } ?: 0,
                    newsCount = newsState.size,
                    banners = bannersState,
                    onNavigate = { screen -> currentScreen = screen }
                )
                CustomerScreen.COMPLAINTS -> TrackerTabContent(
                    state = complaintsState,
                    onComplaintClick = onComplaintClick,
                    onRetry = { viewModel.loadComplaints() }
                )
                CustomerScreen.NEWS -> NewsTabContent(news = newsState)
                CustomerScreen.SUPPORT -> SupportTabContent()
                CustomerScreen.FEEDBACK -> FeedbackScreenContent(
                    viewModel = viewModel,
                    complaints = (complaintsState as? ComplaintsUiState.Success)?.complaints ?: emptyList()
                )
                CustomerScreen.PROFILE -> ProfileScreenContent(viewModel = viewModel)
            }
            }
        }
    }
}

data class SlideData(
    val title: String,
    val description: String,
    val background: Brush,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageRes: Int? = null,
    val imageBitmap: ImageBitmap? = null,
    val redirectUrl: String? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidingBanner(banners: List<BannerResponse>) {
    val context = LocalContext.current
    val slides = if (banners.isNotEmpty()) {
        banners.map { banner ->
            val bitmap = try {
                val cleanBase64 = if (banner.imageBase64.contains(",")) {
                    banner.imageBase64.substringAfter(",")
                } else {
                    banner.imageBase64
                }
                val byteArray = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val androidBitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                androidBitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }

            SlideData(
                title = banner.title,
                description = if (!banner.redirectUrl.isNullOrBlank()) "Tap to open link" else "Click to view detail",
                background = Brush.linearGradient(listOf(Color(0xFFFF8F00), Color(0xFFFF5722))),
                icon = Icons.Default.Send,
                imageRes = null,
                imageBitmap = bitmap,
                redirectUrl = banner.redirectUrl
            )
        }
    } else {
        listOf(
            SlideData(
                title = "Stay Informed.\nStay Ahead.",
                description = "Your gateway to a better community",
                background = Brush.linearGradient(listOf(Color(0xFFFFE57F), Color(0xFFFF8F00))),
                icon = Icons.Default.Send,
                imageRes = R.drawable.default_banner_cityscape
            ),
            SlideData(
                title = "Need Technical\nSupport?",
                description = "We are here 24/7 to solve your queries",
                background = Brush.linearGradient(listOf(Color(0xFFFF8F00), Color(0xFFFF3D00))),
                icon = Icons.Default.Phone,
                imageRes = null
            ),
            SlideData(
                title = "Your Feedback\nMatters",
                description = "Rate resolved tickets and help us improve",
                background = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))),
                icon = Icons.Default.Star,
                imageRes = null
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-scroll effect that pauses when manual swipe is in progress
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (!pagerState.isScrollInProgress) {
                if (pagerState.pageCount > 0) {
                    val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) { page ->
            val data = slides[page]
            
            // Calculate scale and alpha based on page offset to create beautiful peeking cards
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(
                start = 0.92f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            )
            val alpha = lerp(
                start = 0.7f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            )

            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable {
                        if (!data.redirectUrl.isNullOrBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.redirectUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(data.background)
                    ) {
                        if (data.imageBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = data.imageBitmap,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (data.imageRes != null) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = data.imageRes),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Soft dark overlay only for custom/non-cityscape slides to ensure readability
                        if (data.imageRes != R.drawable.default_banner_cityscape && data.imageBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Top Right Dots/Indicators inside the card overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(slides.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val dotColor = if (data.imageRes == R.drawable.default_banner_cityscape) {
                                if (isSelected) Color(0xFF1F2937) else Color(0xFF1F2937).copy(alpha = 0.4f)
                            } else {
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = dotColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Content layout inside the card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(1.dp)) // Push content to middle/bottom

                        Column(modifier = Modifier.fillMaxWidth()) {
                            val textColor = if (data.imageRes == R.drawable.default_banner_cityscape) Color(0xFF1F2937) else Color.White
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 24.sp
                                ),
                                color = textColor,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = data.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (data.imageRes == R.drawable.default_banner_cityscape) Color(0xFF4B5563) else Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }

                        // Explore Now Button pill
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (data.imageRes == R.drawable.default_banner_cityscape) Color(0xFFE65100) else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Explore Now",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
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
fun DashboardCard(
    title: String,
    subtitle: String,
    info: String,
    gradient: Brush,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkText) Color(0xFF1F2937) else Color.White
    val subTextColor = if (isDarkText) Color(0xFF4B5563) else Color.White.copy(alpha = 0.8f)
    val iconColor = if (isDarkText) Color(0xFFFF7A00) else Color(0xFFFF7A00)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Watermark icon in background bottom-right
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDarkText) Color(0xFFE65100).copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Title & Subtext + Top Right Circle Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = subTextColor,
                            maxLines = 2
                        )
                    }

                    // White Circle Icon Container
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom pill/strip container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDarkText) Color(0xFFFF9100).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        fontSize = 12.sp
                    )
                    
                    // Small White Circle Chevron Icon
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (isDarkText) Color(0xFF1F2937) else Color(0xFFFF7A00),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    viewModel: CustomerViewModel,
    complaintsCount: Int,
    activeComplaintsCount: Int,
    newsCount: Int,
    banners: List<BannerResponse>,
    onNavigate: (CustomerScreen) -> Unit
) {
    val username = viewModel.getEmail().substringBefore("@").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Greeting with Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray),
                    fontSize = 16.sp
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937)
                    ),
                    fontSize = 26.sp
                )
            }
            // Rounded User Avatar Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFB300), Color(0xFFFF5722))
                        ),
                        shape = CircleShape
                    )
                    .clickable { onNavigate(CustomerScreen.PROFILE) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        SlidingBanner(banners = banners)

        Spacer(modifier = Modifier.height(16.dp))

        // Symmetric 2x2 grid layout
        // Row 1: Complaint (Orange) & News (Yellow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Complaint",
                subtitle = "Track & file complaints easily",
                info = if (activeComplaintsCount == 1) "1 Active" else "$activeComplaintsCount Active",
                gradient = Brush.verticalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF5252))),
                icon = Icons.Default.List,
                isDarkText = false,
                onClick = { onNavigate(CustomerScreen.COMPLAINTS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            DashboardCard(
                title = "News",
                subtitle = "Latest announcements and updates",
                info = if (newsCount == 1) "1 Update" else "$newsCount Updates",
                gradient = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300))),
                icon = Icons.Default.Notifications,
                isDarkText = true,
                onClick = { onNavigate(CustomerScreen.NEWS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Feedback (Yellow) & Support (Orange)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Feedback",
                subtitle = "Rate resolved cases & help us improve",
                info = "Write a review",
                gradient = Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300))),
                icon = Icons.Default.Star,
                isDarkText = true,
                onClick = { onNavigate(CustomerScreen.FEEDBACK) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            DashboardCard(
                title = "Support",
                subtitle = "FAQ & direct contact with support team",
                info = "Get Help",
                gradient = Brush.verticalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF5252))),
                icon = Icons.Default.Phone,
                isDarkText = false,
                onClick = { onNavigate(CustomerScreen.SUPPORT) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreenContent(
    viewModel: CustomerViewModel,
    complaints: List<ComplaintResponse>
) {
    val context = LocalContext.current
    var selectedComplaintId by remember { mutableStateOf<String?>(null) }
    var ratingInput by remember { mutableStateOf(0) }
    var commentInput by remember { mutableStateOf("") }

    val resolvedComplaints = complaints.filter { it.status == "RESOLVED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Feedback Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Help us improve our service by rating your resolved complaints.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (resolvedComplaints.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No resolved complaints available to rate yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(resolvedComplaints) { complaint ->
                val hasFeedback = complaint.rating != null && complaint.rating > 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasFeedback) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else if (selectedComplaintId == complaint.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = complaint.title, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (hasFeedback) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Submitted",
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        val resolvedDateStr = if (complaint.updatedAt != null) {
                            val date = Date(complaint.updatedAt._seconds * 1000)
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
                        } else {
                            "Just now"
                        }
                        Text(
                            text = "Resolved on: $resolvedDateStr", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (hasFeedback) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Your Rating:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            StarRating(
                                rating = complaint.rating ?: 0,
                                onRatingSelected = {} // Read-only
                            )
                            if (!complaint.feedbackComment.isNullOrBlank()) {
                                Text(
                                    text = "Your Comment:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "\"${complaint.feedbackComment}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        } else {
                            if (selectedComplaintId == complaint.id) {
                                Divider(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = "Rate this support experience:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                StarRating(
                                    rating = ratingInput, 
                                    onRatingSelected = { ratingInput = it }
                                )
                                
                                OutlinedTextField(
                                    value = commentInput,
                                    onValueChange = { commentInput = it },
                                    label = { Text("Write your review comment...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    minLines = 2
                                )

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            id = complaint.id,
                                            rating = ratingInput,
                                            comment = commentInput
                                        ) { success, error ->
                                            if (success) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Feedback submitted successfully!",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                selectedComplaintId = null
                                                ratingInput = 0
                                                commentInput = ""
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Failed to submit feedback: $error",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    enabled = ratingInput > 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Text("Submit Feedback")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        selectedComplaintId = complaint.id
                                        ratingInput = 0
                                        commentInput = ""
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Rate Support Service")
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
fun StarRating(
    rating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Row {
        (1..5).forEach { index ->
            IconButton(onClick = { onRatingSelected(index) }) {
                Icon(
                    imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (index <= rating) Color(0xFFFFD700) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun NewsTabContent(news: List<NewsResponse>) {
    if (news.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No news updates at this time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(news) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val date = Date(item.createdAt._seconds * 1000)
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        
                        Text(
                            text = "Published $formattedDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupportTabContent() {
    val faqs = listOf(
        Pair("How do I raise a complaint?", "Tap the 'New Complaint' button on the Tracker screen. Provide a clear title, description, and assign the priority level. Submit, and our team will be notified immediately."),
        Pair("What are the priority levels?", "We resolve HIGH priority issues within 24 hours. MEDIUM priority targets 48 hours, and LOW priority targets 72 hours."),
        Pair("How can I track the complaint resolution?", "Select any complaint from your Tracker list. A visual timeline is generated detailing when it was registered, when staff members were assigned, and when it is completed.")
    )

    val context = LocalContext.current
    val supportPhone = "08062179339"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direct support cards
        item {
            Text(
                text = "Contact Support",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Contact Support Line",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "08062179339",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call Support"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Support Email",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "support@techrise.com",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // FAQs Section
        item {
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        items(faqs) { faq ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = faq.first,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = faq.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerTabContent(
    state: ComplaintsUiState,
    onComplaintClick: (id: String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is ComplaintsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ComplaintsUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        is ComplaintsUiState.Success -> {
            if (state.complaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No complaints found. Tap 'New Complaint' to submit one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.complaints) { complaint ->
                        ComplaintCard(complaint = complaint, onClick = { onComplaintClick(complaint.id) })
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun ComplaintCard(complaint: ComplaintResponse, onClick: () -> Unit) {
    val statusColor = when (complaint.status.toUpperCase()) {
        "PENDING" -> MaterialTheme.colorScheme.error
        "IN_PROGRESS" -> Color(0xFFE65100) // Deep Orange
        "RESOLVED" -> Color(0xFF2E7D32) // Green
        else -> MaterialTheme.colorScheme.outline
    }

    val priorityColor = when (complaint.priority.toUpperCase()) {
        "HIGH" -> MaterialTheme.colorScheme.error
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF757575)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = complaint.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = complaint.priority,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }

                // Date formatting
                val dateStr = if (complaint.createdAt != null) {
                    val date = Date(complaint.createdAt._seconds * 1000)
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
                } else {
                    "Just now"
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    viewModel: CustomerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileState by viewModel.profileState.collectAsState()
    val updateProfileState by viewModel.updateProfileState.collectAsState()

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Load profile once when screen is shown
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // React to profile details loading
    LaunchedEffect(profileState) {
        if (profileState is ProfileUiState.Success) {
            val profile = (profileState as ProfileUiState.Success).profile
            name = profile.name
            mobile = profile.mobile
            email = profile.email
        }
    }

    // React to update state
    LaunchedEffect(updateProfileState) {
        when (updateProfileState) {
            is UpdateProfileUiState.Success -> {
                android.widget.Toast.makeText(context, "Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateProfileState()
            }
            is UpdateProfileUiState.Error -> {
                val error = (updateProfileState as UpdateProfileUiState.Error).message
                android.widget.Toast.makeText(context, "Update failed: $error", android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetUpdateProfileState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (profileState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFF9100)
                )
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = (profileState as ProfileUiState.Error).message,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { viewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
            else -> {
                // profileState is Success or Idle (with values loaded)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Icon/Avatar Card
                    Card(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFB300), Color(0xFFFF5722))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fields Container Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                            )

                            Divider(color = Color(0xFFF3F4F6))

                            // Name Field
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF9100)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF9100),
                                    focusedLabelColor = Color(0xFFFF9100)
                                )
                            )

                            // Email Field (Read-only)
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color(0xFFE5E7EB),
                                    disabledLabelColor = Color.Gray,
                                    disabledTextColor = Color.Gray
                                )
                            )

                            // Mobile Field
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFFF9100)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF9100),
                                    focusedLabelColor = Color(0xFFFF9100)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                android.widget.Toast.makeText(context, "Name cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.updateProfile(name, mobile)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        enabled = updateProfileState !is UpdateProfileUiState.Loading
                    ) {
                        if (updateProfileState is UpdateProfileUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
