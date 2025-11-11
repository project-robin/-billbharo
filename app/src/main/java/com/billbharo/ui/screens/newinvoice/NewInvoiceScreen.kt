package com.billbharo.ui.screens.newinvoice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.billbharo.R

/**
 * A composable function that displays the screen for creating a new invoice.
 *
 * This screen allows users to add items manually or via voice input, enter customer details,
 * select a payment mode, and save the final invoice as a PDF.
 *
 * @param navController The [NavController] for handling navigation.
 * @param viewModel The [NewInvoiceViewModel] that provides state and handles business logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    navController: NavController,
    viewModel: NewInvoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecognition()
        } else {
            viewModel.updateError("Microphone permission is required for voice input.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_invoice)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.isVoiceInputActive) {
                                viewModel.stopVoiceRecognition()
                            } else {
                                val permission = Manifest.permission.RECORD_AUDIO
                                if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    viewModel.startVoiceRecognition()
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (uiState.isVoiceInputActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.voice_input))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveInvoice() },
                    icon = {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    text = { Text(stringResource(R.string.save_invoice)) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CustomerInfoSection(
                    customerName = uiState.customerName,
                    customerPhone = uiState.customerPhone,
                    onNameChange = viewModel::updateCustomerName,
                    onPhoneChange = viewModel::updateCustomerPhone
                )
            }

            if (uiState.isVoiceInputActive) {
                item {
                    VoiceInputIndicator(
                        status = uiState.voiceStatus,
                        recognizedText = uiState.voiceInputText,
                        onStop = viewModel::stopVoiceRecognition
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = viewModel::showAddItemDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_item))
                }
            }

            if (uiState.items.isNotEmpty()) {
                items(uiState.items) { item ->
                    InvoiceItemCard(
                        item = item,
                        onDelete = { viewModel.removeItem(item.id) }
                    )
                }

                item {
                    TotalsSection(
                        subtotal = uiState.subtotal,
                        cgst = uiState.cgst,
                        sgst = uiState.sgst,
                        total = uiState.total
                    )
                }

                item {
                    PaymentModeSection(
                        selectedMode = uiState.paymentMode,
                        onModeSelected = viewModel::updatePaymentMode
                    )
                }
            } else {
                item {
                    EmptyItemsPlaceholder()
                }
            }
        }

        if (uiState.showAddItemDialog) {
            AddItemDialog(
                initialItemName = uiState.voiceRecognizedItemName,
                initialQuantity = uiState.voiceRecognizedQuantity,
                initialPrice = uiState.voiceRecognizedPrice,
                onDismiss = viewModel::hideAddItemDialog,
                onAdd = viewModel::addItem
            )
        }

        if (uiState.showShareDialog && uiState.pdfPath != null) {
            ShareInvoiceDialog(
                onShareWhatsApp = viewModel::shareViaWhatsApp,
                onShareOther = viewModel::shareViaOther,
                onOpenPdf = viewModel::openPdf,
                onDismiss = {
                    viewModel.dismissShareDialog()
                    navController.navigateUp()
                }
            )
        }

        uiState.errorMessage?.let { error ->
            ErrorSnackbar(message = error, onDismiss = viewModel::clearError)
        }
    }
}

/**
 * A composable section for entering customer information.
 *
 * @param customerName The current value for the customer's name.
 * @param customerPhone The current value for the customer's phone number.
 * @param onNameChange A callback for when the customer's name changes.
 * @param onPhoneChange A callback for when the customer's phone number changes.
 */
@Composable
fun CustomerInfoSection(
    customerName: String,
    customerPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.customer_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = customerName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.customer_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.customer_phone)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
        }
    }
}

/**
 * A composable that provides visual feedback during voice input.
 *
 * @param status A string describing the current status (e.g., "Listening...").
 * @param recognizedText The currently recognized text.
 * @param onStop A callback to stop the voice recognition.
 */
@Composable
fun VoiceInputIndicator(
    status: String,
    recognizedText: String,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Voice Input Active",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (recognizedText.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = recognizedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * A composable that displays a single item in the invoice list.
 *
 * @param item The [InvoiceItemUI] to display.
 * @param onDelete A callback to delete the item.
 */
@Composable
fun InvoiceItemCard(
    item: InvoiceItemUI,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.quantity)}: ${item.quantity} × ₹${item.rate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%.2f", item.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * A composable section that displays the invoice totals (subtotal, GST, total).
 *
 * @param subtotal The subtotal amount.
 * @param cgst The CGST amount.
 * @param sgst The SGST amount.
 * @param total The final total amount.
 */
@Composable
fun TotalsSection(
    subtotal: Double,
    cgst: Double,
    sgst: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TotalRow(stringResource(R.string.subtotal), subtotal)
            TotalRow(stringResource(R.string.cgst), cgst)
            TotalRow(stringResource(R.string.sgst), sgst)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            TotalRow(stringResource(R.string.total), total, isTotal = true)
        }
    }
}

/**
 * A helper composable for displaying a single row in the totals section.
 *
 * @param label The label for the value (e.g., "Subtotal").
 * @param value The numerical value.
 * @param isTotal A flag to indicate if this is the final total row, for styling purposes.
 */
@Composable
fun TotalRow(label: String, value: Double, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "₹${String.format("%.2f", value)}",
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * A composable section for selecting the payment mode.
 *
 * @param selectedMode The currently selected payment mode.
 * @param onModeSelected A callback for when a new payment mode is selected.
 */
@Composable
fun PaymentModeSection(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("cash", "upi", "credit").forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    "cash" -> stringResource(R.string.cash)
                                    "upi" -> stringResource(R.string.upi)
                                    "credit" -> stringResource(R.string.credit)
                                    else -> mode
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * A placeholder composable to be displayed when the invoice has no items.
 */
@Composable
fun EmptyItemsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.add_item),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A dialog for adding a new item to the invoice.
 *
 * @param initialItemName An initial value for the item name field.
 * @param initialQuantity An initial value for the quantity field.
 * @param initialPrice An initial value for the price field.
 * @param onDismiss A callback to dismiss the dialog.
 * @param onAdd A callback to add the new item, providing the name, quantity, and rate.
 */
@Composable
fun AddItemDialog(
    initialItemName: String = "",
    initialQuantity: String = "",
    initialPrice: String = "",
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var itemName by remember { mutableStateOf(initialItemName) }
    var quantity by remember { mutableStateOf(initialQuantity) }
    var rate by remember { mutableStateOf(initialPrice) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemName.isNotBlank() && quantity.isNotBlank() && rate.isNotBlank()) {
                        onAdd(itemName, quantity, rate)
                    }
                }
            ) {
                Text(stringResource(R.string.add_item))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * A dialog that appears after an invoice is saved, offering sharing options.
 *
 * @param onShareWhatsApp A callback to share the invoice via WhatsApp.
 * @param onShareOther A callback to share the invoice via other apps.
 * @param onOpenPdf A callback to open the generated PDF.
 * @param onDismiss A callback to dismiss the dialog.
 */
@Composable
fun ShareInvoiceDialog(
    onShareWhatsApp: () -> Unit,
    onShareOther: () -> Unit,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(text = "Invoice Saved!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your invoice has been saved as a PDF. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onShareWhatsApp()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share via WhatsApp")
                }
                OutlinedButton(
                    onClick = {
                        onShareOther()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share via Other Apps")
                }
                OutlinedButton(
                    onClick = {
                        onOpenPdf()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open PDF")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * A composable that displays a custom Snackbar for showing error messages.
 *
 * @param message The error message to display.
 * @param onDismiss A callback to dismiss the Snackbar.
 */
@Composable
fun ErrorSnackbar(message: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            action = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
            },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Text(message)
        }
    }
}
