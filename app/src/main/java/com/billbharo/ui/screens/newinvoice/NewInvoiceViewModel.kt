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
import com.billbharo.domain.utils.GeminiAudioTranscriber
import com.billbharo.domain.utils.VoiceTranscriptionResult
import com.billbharo.domain.utils.GeminiInvoiceParser
import com.billbharo.domain.utils.GeminiParsingException
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
    private val geminiInvoiceParser: GeminiInvoiceParser, // PURE AI MODE - no fallback parser
    private val geminiAudioTranscriber: GeminiAudioTranscriber // Gemini-only audio transcription
) : ViewModel() {

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
            voiceStatus = "Initializing Gemini...",
            voiceInputText = ""
        )

        voiceRecognitionJob = viewModelScope.launch {
            try {
                // GEMINI-ONLY TRANSCRIPTION: No Android SpeechRecognizer used
                geminiAudioTranscriber.transcribeAudio("hi-IN").collect { result ->
                    when (result) {
                        is VoiceTranscriptionResult.Ready -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "Ready - Tap to speak"
                            )
                        }
                        is VoiceTranscriptionResult.Recording -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "🎤 Recording... Speak now"
                            )
                        }
                        is VoiceTranscriptionResult.Processing -> {
                            _uiState.value = _uiState.value.copy(
                                voiceStatus = "🤖 Transcribing with Gemini AI..."
                            )
                        }
                        is VoiceTranscriptionResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                voiceInputText = result.transcription,
                                voiceStatus = "✅ Processing transcription..."
                            )
                            // Now parse the Gemini transcription into structured data
                            processVoiceInput(result.transcription)
                        }
                        is VoiceTranscriptionResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isVoiceInputActive = false,
                                voiceStatus = "",
                                errorMessage = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceStatus = "",
                    errorMessage = "Gemini transcription failed: ${e.message}"
                )
            }
        }
    }

    fun stopVoiceRecognition() {
        voiceRecognitionJob?.cancel()
        geminiAudioTranscriber.stopRecording()
        _uiState.value = _uiState.value.copy(
            isVoiceInputActive = false,
            voiceStatus = "",
            voiceInputText = ""
        )
    }

    fun updateVoiceInputText(text: String) {
        _uiState.value = _uiState.value.copy(voiceInputText = text)
    }

    // PURE AI MODE: Only Gemini - no fallbacks, exposes AI limitations for testing
    private fun processVoiceInput(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    voiceStatus = "Processing with Gemini AI..."
                )

                // ONLY Gemini - throws exceptions on failure
                val geminiResult = geminiInvoiceParser.parseInvoiceItem(text)
                
                // ✅ Gemini succeeded - auto-fill ALL fields (no confidence filtering)
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    voiceRecognizedItemName = geminiResult.itemName,
                    voiceRecognizedQuantity = geminiResult.quantity.toString(),
                    voiceRecognizedPrice = geminiResult.price.toString(),
                    showAddItemDialog = true
                )
                
            } catch (e: GeminiParsingException) {
                // Detailed AI failure context for debugging
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    errorMessage = "Gemini AI Failed:\n${e.message}\n\nPlease try again with clearer speech."
                )
            } catch (e: IllegalStateException) {
                // API key not configured
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    errorMessage = "Setup Required: ${e.message}"
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Network timeout
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    errorMessage = "Gemini request timed out (15s). Check your internet connection."
                )
            } catch (e: Exception) {
                // Unexpected error
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    errorMessage = "Unexpected error: ${e.javaClass.simpleName} - ${e.message}"
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
