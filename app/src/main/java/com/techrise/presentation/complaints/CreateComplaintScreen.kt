package com.techrise.presentation.complaints

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateComplaintScreen(
    viewModel: CustomerViewModel,
    onBack: () -> Unit
) {
    val createState by viewModel.createComplaintState.collectAsState()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
        }
    }

    LaunchedEffect(createState) {
        if (createState is CreateComplaintUiState.Success) {
            viewModel.resetCreateState()
            onBack()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color(0xFFFFF8F4),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFE65100),
                                androidx.compose.ui.graphics.Color(0xFFFF9100)
                            )
                        ),
                        shape = HeaderWaveShape()
                    )
            ) {
                // Decorative background circles
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-45).dp)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f), shape = androidx.compose.foundation.shape.CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 25.dp)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f), shape = androidx.compose.foundation.shape.CircleShape)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(top = 16.dp),
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
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Generate Complaint",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Explain the issue you are experiencing",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Complaint Title") },
                placeholder = { Text("e.g. Broken portal links") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description") },
                placeholder = { Text("Describe the issue, error codes, and steps to reproduce...") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 6,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Camera Option UI
            Text(
                text = "Attach Image of the Issue (Optional)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (imageBitmap == null) {
                Surface(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Take Photo of the Issue",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = "Issue Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
                        onClick = { imageBitmap = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Photo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    val finalDescription = if (imageBitmap != null) {
                        "$description\n\n[Image Attached via Camera]"
                    } else {
                        description
                    }
                    val base64Str = imageBitmap?.let { bitmap ->
                        val outputStream = java.io.ByteArrayOutputStream()
                        val maxDimension = 600
                        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val width = if (aspectRatio > 1) maxDimension else (maxDimension * aspectRatio).toInt()
                            val height = if (aspectRatio > 1) (maxDimension / aspectRatio).toInt() else maxDimension
                            Bitmap.createScaledBitmap(bitmap, width, height, true)
                        } else {
                            bitmap
                        }
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val byteArray = outputStream.toByteArray()
                        "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                    }
                    viewModel.createComplaint(title.trim(), finalDescription.trim(), "MEDIUM", base64Str)
                },
                enabled = title.isNotBlank() && description.isNotBlank() && createState !is CreateComplaintUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (createState is CreateComplaintUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error Display
            if (createState is CreateComplaintUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (createState as CreateComplaintUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
