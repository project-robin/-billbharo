package com.billbharo.ui.screens.newinvoice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbharo.data.models.Invoice
import com.billbharo.data.models.InvoiceItem
import com.billbharo.data.models.PaymentMode
import com.billbharo.data.repository.InvoiceRepository
import com.billbharo.domain.utils.GstCalculator
import com.billbharo.domain.utils.PdfGenerator
import com.billbharo.domain.utils.ShareHelper
import com.billbharo.domain.utils.VoiceRecognitionHelper
import com.billbharo.domain.utils.VoiceInputParser
import com.billbharo.domain.utils.VoiceRecognitionResult
import com.billbharo.domain.utils.ParseResult
import com.billbharo.domain.utils.GeminiInvoiceParser
import com.billbharo.domain.utils.GeminiParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class InvoiceItemUI(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val quantity: String = "",
    val rate: String = "",
    val amount: Double = 0.0
)

data class NewInvoiceUiState(
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<InvoiceItemUI> = emptyList(),
    val subtotal: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val total: Double = 0.0,
    val paymentMode: String = "cash",
    val isVoiceInputActive: Boolean = false,
    val voiceInputText: String = "",
    val voiceStatus: String = "",
    val showAddItemDialog: Boolean = false,
    val voiceRecognizedItemName: String = "",
    val voiceRecognizedQuantity: String = "",
    val voiceRecognizedPrice: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val invoiceSaved: Boolean = false,
    val pdfPath: String? = null,
    val showShareDialog: Boolean = false
)

@HiltViewModel
class NewInvoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
    private val gstCalculator: GstCalculator,
    private val pdfGenerator: PdfGenerator,
    private val shareHelper: ShareHelper,
    private val voiceInputParser: VoiceInputParser,
    private val geminiInvoiceParser: GeminiInvoiceParser
) : ViewModel() {

    private val voiceRecognitionHelper by lazy { VoiceRecognitionHelper(context) }
    private var voiceRecognitionJob: Job? = null

    private val _uiState = MutableStateFlow(NewInvoiceUiState())
    val uiState: StateFlow<NewInvoiceUiState> = _uiState.asStateFlow()

    fun updateCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    fun updateCustomerPhone(phone: String) {
        _uiState.value = _uiState.value.copy(customerPhone = phone)
    }

    fun updatePaymentMode(mode: String) {
        _uiState.value = _uiState.value.copy(paymentMode = mode)
    }

    fun showAddItemDialog() {
        _uiState.value = _uiState.value.copy(
            showAddItemDialog = true,
            voiceRecognizedItemName = "",
            voiceRecognizedQuantity = "",
            voiceRecognizedPrice = ""
        )
    }

    fun hideAddItemDialog() {
        _uiState.value = _uiState.value.copy(
            showAddItemDialog = false,
            voiceRecognizedItemName = "",
            voiceRecognizedQuantity = "",
            voiceRecognizedPrice = ""
        )
    }

    fun addItem(name: String, quantity: String, rate: String) {
        try {
            val qty = quantity.toDoubleOrNull() ?: 0.0
            val rateValue = rate.toDoubleOrNull() ?: 0.0
            val amount = qty * rateValue

            val newItem = InvoiceItemUI(
                name = name,
                quantity = quantity,
                rate = rate,
                amount = amount
            )

            val updatedItems = _uiState.value.items + newItem
            _uiState.value = _uiState.value.copy(
                items = updatedItems,
                showAddItemDialog = false
            )
            calculateTotals()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid item values"
            )
        }
    }

    fun removeItem(itemId: Long) {
        val updatedItems = _uiState.value.items.filter { it.id != itemId }
        _uiState.value = _uiState.value.copy(items = updatedItems)
        calculateTotals()
    }

    private fun calculateTotals() {
        val subtotal = _uiState.value.items.sumOf { it.amount }
        val cgst = gstCalculator.calculateCGST(subtotal)
        val sgst = gstCalculator.calculateSGST(subtotal)
        val total = gstCalculator.calculateTotalWithGST(subtotal)

        _uiState.value = _uiState.value.copy(
            subtotal = subtotal,
            cgst = cgst,
            sgst = sgst,
            total = total
        )
    }

    fun toggleVoiceInput() {
        if (_uiState.value.isVoiceInputActive) {
            stopVoiceRecognition()
        } else {
            startVoiceRecognition()
        }
    }

    fun startVoiceRecognition() {
        _uiState.value = _uiState.value.copy(
            isVoiceInputActive = true,
            voiceStatus = "Initializing...",
            voiceInputText = ""
        )

        voiceRecognitionJob = viewModelScope.launch {
            try {
                voiceRecognitionHelper.startListening("hi-IN").collect { result ->
                    when (result) {
                        is VoiceRecognitionResult.Ready -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "Ready - Start speaking"
                            )
                        }
                        is VoiceRecognitionResult.Speaking -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "Listening..."
                            )
                        }
                        is VoiceRecognitionResult.Partial -> {
                            _uiState.value = _uiState.value.copy(
                                voiceInputText = result.text,
                                voiceStatus = "Processing..."
                            )
                        }
                        is VoiceRecognitionResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                voiceInputText = result.text,
                                voiceStatus = "Processing voice input..."
                            )
                            processVoiceInput(result.text)
                        }
                        is VoiceRecognitionResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isVoiceInputActive = false,
                                voiceStatus = "",
                                errorMessage = result.message
                            )
                        }
                        is VoiceRecognitionResult.EndSpeech -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "Processing..."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceStatus = "",
                    errorMessage = "Voice recognition failed: ${e.message}"
                )
            }
        }
    }

    fun stopVoiceRecognition() {
        voiceRecognitionJob?.cancel()
        voiceRecognitionHelper.stopListening()
        _uiState.value = _uiState.value.copy(
            isVoiceInputActive = false,
            voiceStatus = "",
            voiceInputText = ""
        )
    }

    fun updateVoiceInputText(text: String) {
        _uiState.value = _uiState.value.copy(voiceInputText = text)
    }

    // Stage 1: STT → Text; Stage 2: Text → Gemini → Structured Data (auto-fill if high confidence)
    private fun processVoiceInput(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Analyzing with AI..."
                )

                // Try Gemini first (preferred method)
                val geminiResult = geminiInvoiceParser.parseInvoiceItem(text)
                
                if (geminiResult != null) {
                    // ✅ Gemini succeeded with high confidence → Auto-fill form
                    _uiState.value = _uiState.value.copy(
                        isVoiceInputActive = false,
                        voiceInputText = "",
                        voiceStatus = "",
                        voiceRecognizedItemName = geminiResult.itemName,
                        voiceRecognizedQuantity = if (geminiResult.quantity > 0) geminiResult.quantity.toString() else "",
                        voiceRecognizedPrice = if (geminiResult.price > 0) geminiResult.price.toString() else "",
                        showAddItemDialog = true
                    )
                } else {
                    // ❌ Gemini failed/low confidence → Fallback to regex parser (backward compatibility)
                    val parseResult = voiceInputParser.parseVoiceInput(text)
                    
                    when (parseResult) {
                        is ParseResult.Success -> {
                            val firstItem = parseResult.items.firstOrNull()
                            if (firstItem != null) {
                                _uiState.value = _uiState.value.copy(
                                    isVoiceInputActive = false,
                                    voiceInputText = "",
                                    voiceStatus = "",
                                    voiceRecognizedItemName = firstItem.name,
                                    voiceRecognizedQuantity = if (firstItem.quantity > 0) firstItem.quantity.toString() else "",
                                    voiceRecognizedPrice = if (firstItem.price != null && firstItem.price > 0) firstItem.price.toString() else "",
                                    showAddItemDialog = true
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isVoiceInputActive = false,
                                    voiceInputText = "",
                                    voiceStatus = "",
                                    errorMessage = "Could not recognize any items"
                                )
                            }
                        }
                        is ParseResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isVoiceInputActive = false,
                                voiceInputText = "",
                                voiceStatus = "",
                                errorMessage = parseResult.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    errorMessage = "Failed to process voice input: ${e.message}"
                )
            }
        }
    }

    fun saveInvoice() {
        if (_uiState.value.customerName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Customer name is required")
            return
        }

        if (_uiState.value.items.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Add at least one item")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val invoiceItems = _uiState.value.items.map { item ->
                    InvoiceItem(
                        name = item.name,
                        quantity = item.quantity.toDoubleOrNull() ?: 0.0,
                        unit = "piece",
                        rate = item.rate.toDoubleOrNull() ?: 0.0,
                        amount = item.amount
                    )
                }

                val paymentMode = when (_uiState.value.paymentMode) {
                    "upi" -> PaymentMode.UPI
                    "credit" -> PaymentMode.CREDIT
                    else -> PaymentMode.CASH
                }

                val invoice = Invoice(
                    invoiceNumber = "INV${System.currentTimeMillis()}",
                    customerName = _uiState.value.customerName,
                    customerPhone = _uiState.value.customerPhone,
                    items = invoiceItems,
                    subtotal = _uiState.value.subtotal,
                    cgst = _uiState.value.cgst,
                    sgst = _uiState.value.sgst,
                    totalAmount = _uiState.value.total,
                    paymentMode = paymentMode,
                    isPaid = _uiState.value.paymentMode != "credit",
                    timestamp = Date(),
                    pdfPath = null
                )

                val invoiceId = invoiceRepository.insertInvoice(invoice)
                
                // Generate PDF
                val savedInvoice = invoice.copy(id = invoiceId)
                val pdfResult = pdfGenerator.generateInvoicePdf(savedInvoice)
                
                if (pdfResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        invoiceSaved = true,
                        pdfPath = pdfResult.getOrNull(),
                        showShareDialog = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        invoiceSaved = true,
                        errorMessage = "Invoice saved but PDF generation failed: ${pdfResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save invoice: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updateError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
    
    fun shareViaWhatsApp() {
        val pdfPath = _uiState.value.pdfPath ?: return
        val phoneNumber = _uiState.value.customerPhone
        
        viewModelScope.launch {
            val result = if (!phoneNumber.isNullOrEmpty()) {
                shareHelper.shareToWhatsAppContact(pdfPath, phoneNumber)
            } else {
                shareHelper.shareViaWhatsApp(pdfPath)
            }
            
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to share: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun shareViaOther() {
        val pdfPath = _uiState.value.pdfPath ?: return
        
        viewModelScope.launch {
            val result = shareHelper.shareViaIntent(pdfPath)
            
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to share: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun openPdf() {
        val pdfPath = _uiState.value.pdfPath ?: return
        
        viewModelScope.launch {
            val result = shareHelper.openPdf(pdfPath)
            
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to open PDF: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun dismissShareDialog() {
        _uiState.value = _uiState.value.copy(showShareDialog = false)
    }
}
