package com.billbharo.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.billbharo.R
import com.billbharo.data.models.Invoice
import com.billbharo.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        } else {
            // Show error - permission denied
        }
    }
    
    // Observe voice result and navigate to NewInvoice screen
    LaunchedEffect(uiState.voiceResult) {
        uiState.voiceResult?.let { result ->
            navController.navigate(
                "${Screen.NewInvoice.route}?item=${result.itemName}&qty=${result.quantity}&price=${result.price}"
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewInvoice.route) }
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = stringResource(R.string.create_new_invoice)
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.welcome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Voice Recording Button (New Feature)
                item {
                    VoiceRecordingCard(
                        isRecording = uiState.isVoiceRecording,
                        recordingStatus = uiState.voiceRecordingStatus,
                        onRecordClick = {
                            if (uiState.isVoiceRecording) {
                                viewModel.stopVoiceRecording()
                            } else {
                                // Check permission before starting
                                val permissionStatus = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                )
                                if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startVoiceRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onResultReceived = { itemName, quantity, price ->
                            // Navigate to NewInvoice with pre-filled data
                            navController.navigate(
                                "${Screen.NewInvoice.route}?item=$itemName&qty=$quantity&price=$price"
                            )
                        }
                    )
                }

                // Dashboard Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardCard(
                            title = stringResource(R.string.total_sales_today),
                            value = "₹${String.format("%.2f", uiState.todaySales)}",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardCard(
                            title = stringResource(R.string.pending_credit),
                            value = "₹${String.format("%.2f", uiState.pendingCredit)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Today's Invoices Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Invoices (${uiState.totalInvoicesToday})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.recentInvoices.isEmpty()) {
                    item {
                        EmptyInvoicesCard()
                    }
                } else {
                    items(uiState.recentInvoices) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onClick = { /* Navigate to invoice detail */ },
                            onShare = { viewModel.shareInvoice(invoice) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = dateFormat.format(invoice.timestamp)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.customerName ?: "Walk-in Customer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${invoice.invoiceNumber} • $timeString",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (invoice.items.isNotEmpty()) {
                    Text(
                        text = "${invoice.items.size} ${stringResource(R.string.items_text)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", invoice.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = when (invoice.paymentMode.name) {
                            "CASH" -> MaterialTheme.colorScheme.tertiaryContainer
                            "UPI" -> MaterialTheme.colorScheme.secondaryContainer
                            "CREDIT" -> if (invoice.isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = invoice.paymentMode.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                if (!invoice.pdfPath.isNullOrEmpty()) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Invoice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyInvoicesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.no_data),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap + to create your first invoice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Voice Recording Card - Prominent UI for voice-to-invoice on Home Screen
 * Design: Circular mic button (80dp), green background, toggle recording state
 */
@Composable
fun VoiceRecordingCard(
    isRecording: Boolean,
    recordingStatus: String,
    onRecordClick: () -> Unit,
    onResultReceived: (itemName: String, quantity: Double, price: Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                Color(0xFF4CAF50) // Primary green from design system
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Circular Mic Button (80dp as per design system)
            FloatingActionButton(
                onClick = onRecordClick,
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.White
                },
                contentColor = if (isRecording) {
                    Color.White
                } else {
                    Color(0xFF4CAF50)
                }
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.KeyboardVoice,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Main Text
            Text(
                text = if (isRecording) "Recording..." else "Speak to create bill",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Hindi Hint (as per screenshot)
            Text(
                text = if (isRecording) recordingStatus else "उदाहरण: दान शेड, एक तोनी",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            // Recording Status Indicator
            if (isRecording) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
