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

/**
 * A UI-specific data class for an invoice item.
 *
 * This is used to hold the state of an item within the New Invoice screen, with properties
 * represented as strings for easy editing in text fields.
 *
 * @property id A unique identifier for the item.
 * @property name The name of the item.
 * @property quantity The quantity of the item as a string.
 * @property rate The price per unit of the item as a string.
 * @property amount The calculated total amount for the item.
 */
data class InvoiceItemUI(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val quantity: String = "",
    val rate: String = "",
    val amount: Double = 0.0
)

/**
 * Represents the UI state for the [NewInvoiceScreen].
 *
 * @property customerName The name of the customer.
 * @property customerPhone The phone number of the customer.
 * @property items A list of items in the current invoice.
 * @property subtotal The subtotal of the invoice.
 * @property cgst The Central GST amount.
 * @property sgst The State GST amount.
 * @property total The final total amount of the invoice.
 * @property paymentMode The selected payment mode (e.g., "cash", "upi", "credit").
 * @property isVoiceInputActive A flag indicating if voice input is currently active.
 * @property voiceInputText The text recognized from voice input.
 * @property voiceStatus A string describing the current status of the voice input process.
 * @property showAddItemDialog A flag to control the visibility of the "Add Item" dialog.
 * @property voiceRecognizedItemName The item name parsed from voice input.
 * @property voiceRecognizedQuantity The quantity parsed from voice input.
 * @property voiceRecognizedPrice The price parsed from voice input.
 * @property isLoading A flag indicating if a long-running operation is in progress.
 * @property errorMessage An optional error message to be displayed.
 * @property invoiceSaved A flag indicating if the invoice has been successfully saved.
 * @property pdfPath The file path of the generated PDF invoice.
 * @property showShareDialog A flag to control the visibility of the post-save share dialog.
 */
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

/**
 * The ViewModel for the [NewInvoiceScreen].
 *
 * This class manages the state of a new invoice being created, handling everything from
 * user input and voice recognition to saving the invoice and generating a PDF.
 */
@HiltViewModel
class NewInvoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val invoiceRepository: InvoiceRepository,
    private val gstCalculator: GstCalculator,
    private val pdfGenerator: PdfGenerator,
    private val shareHelper: ShareHelper,
    private val geminiInvoiceParser: GeminiInvoiceParser,
    private val geminiAudioTranscriber: GeminiAudioTranscriber
) : ViewModel() {

    private var voiceRecognitionJob: Job? = null
    private val _uiState = MutableStateFlow(NewInvoiceUiState())
    val uiState: StateFlow<NewInvoiceUiState> = _uiState.asStateFlow()

    /** Updates the customer's name in the UI state. */
    fun updateCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    /** Updates the customer's phone number in the UI state. */
    fun updateCustomerPhone(phone: String) {
        _uiState.value = _uiState.value.copy(customerPhone = phone)
    }

    /** Updates the selected payment mode in the UI state. */
    fun updatePaymentMode(mode: String) {
        _uiState.value = _uiState.value.copy(paymentMode = mode)
    }

    /** Shows the "Add Item" dialog. */
    fun showAddItemDialog() {
        _uiState.value = _uiState.value.copy(
            showAddItemDialog = true,
            voiceRecognizedItemName = "",
            voiceRecognizedQuantity = "",
            voiceRecognizedPrice = ""
        )
    }

    /** Hides the "Add Item" dialog. */
    fun hideAddItemDialog() {
        _uiState.value = _uiState.value.copy(
            showAddItemDialog = false,
            voiceRecognizedItemName = "",
            voiceRecognizedQuantity = "",
            voiceRecognizedPrice = ""
        )
    }

    /**
     * Adds a new item to the invoice.
     * @param name The name of the item.
     * @param quantity The quantity of the item as a string.
     * @param rate The rate of the item as a string.
     */
    fun addItem(name: String, quantity: String, rate: String) {
        try {
            val qty = quantity.toDoubleOrNull() ?: 0.0
            val rateValue = rate.toDoubleOrNull() ?: 0.0
            val amount = qty * rateValue
            val newItem = InvoiceItemUI(name = name, quantity = quantity, rate = rate, amount = amount)
            val updatedItems = _uiState.value.items + newItem
            _uiState.value = _uiState.value.copy(items = updatedItems, showAddItemDialog = false)
            calculateTotals()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid item values")
        }
    }

    /**
     * Removes an item from the invoice.
     * @param itemId The unique ID of the item to remove.
     */
    fun removeItem(itemId: Long) {
        val updatedItems = _uiState.value.items.filter { it.id != itemId }
        _uiState.value = _uiState.value.copy(items = updatedItems)
        calculateTotals()
    }

    /** Recalculates the subtotal, GST, and total for the invoice. */
    private fun calculateTotals() {
        val subtotal = _uiState.value.items.sumOf { it.amount }
        val gstResult = gstCalculator.calculateGst(subtotal)
        _uiState.value = _uiState.value.copy(
            subtotal = subtotal,
            cgst = gstResult.cgst,
            sgst = gstResult.sgst,
            total = gstResult.total
        )
    }

    /** Toggles the voice input state. */
    fun toggleVoiceInput() {
        if (_uiState.value.isVoiceInputActive) stopVoiceRecognition() else startVoiceRecognition()
    }

    /** Starts the voice recognition process. */
    fun startVoiceRecognition() {
        _uiState.value = _uiState.value.copy(isVoiceInputActive = true, voiceStatus = "Initializing Gemini...", voiceInputText = "")
        voiceRecognitionJob = viewModelScope.launch {
            try {
                geminiAudioTranscriber.transcribeAudio("hi-IN").collect { result ->
                    when (result) {
                        is VoiceTranscriptionResult.Ready -> _uiState.value = _uiState.value.copy(voiceStatus = "Ready - Tap to speak")
                        is VoiceTranscriptionResult.Recording -> _uiState.value = _uiState.value.copy(voiceStatus = "🎤 Recording... Speak now")
                        is VoiceTranscriptionResult.Processing -> _uiState.value = _uiState.value.copy(voiceStatus = "🤖 Transcribing with Gemini AI...")
                        is VoiceTranscriptionResult.Success -> {
                            _uiState.value = _uiState.value.copy(voiceInputText = result.transcription, voiceStatus = "✅ Processing transcription...")
                            processVoiceInput(result.transcription)
                        }
                        is VoiceTranscriptionResult.Error -> _uiState.value = _uiState.value.copy(isVoiceInputActive = false, voiceStatus = "", errorMessage = result.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isVoiceInputActive = false, voiceStatus = "", errorMessage = "Gemini transcription failed: ${e.message}")
            }
        }
    }

    /** Stops the voice recognition process. */
    fun stopVoiceRecognition() {
        voiceRecognitionJob?.cancel()
        geminiAudioTranscriber.stopRecording()
        _uiState.value = _uiState.value.copy(isVoiceInputActive = false, voiceStatus = "", voiceInputText = "")
    }

    /** Updates the recognized voice input text in the UI state. */
    fun updateVoiceInputText(text: String) {
        _uiState.value = _uiState.value.copy(voiceInputText = text)
    }

    /** Processes the transcribed voice input text to extract item details. */
    private fun processVoiceInput(text: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(voiceStatus = "Processing with Gemini AI...")
                val geminiResult = geminiInvoiceParser.parseInvoiceItem(text)
                _uiState.value = _uiState.value.copy(
                    isVoiceInputActive = false,
                    voiceInputText = "",
                    voiceStatus = "",
                    voiceRecognizedItemName = geminiResult.itemName,
                    voiceRecognizedQuantity = geminiResult.quantity.toString(),
                    voiceRecognizedPrice = geminiResult.price.toString(),
                    showAddItemDialog = true
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is GeminiParsingException -> "Gemini AI Failed:\n${e.message}\n\nPlease try again with clearer speech."
                    is IllegalStateException -> "Setup Required: ${e.message}"
                    is kotlinx.coroutines.TimeoutCancellationException -> "Gemini request timed out (15s). Check your internet connection."
                    else -> "Unexpected error: ${e.javaClass.simpleName} - ${e.message}"
                }
                _uiState.value = _uiState.value.copy(isVoiceInputActive = false, voiceInputText = "", voiceStatus = "", errorMessage = errorMessage)
            }
        }
    }

    /** Saves the current invoice, generates a PDF, and shows the share dialog. */
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val invoice = createInvoiceFromState()
                val invoiceId = invoiceRepository.insertInvoice(invoice)
                val savedInvoice = invoice.copy(id = invoiceId)
                val pdfResult = pdfGenerator.generateInvoicePdf(savedInvoice)

                if (pdfResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, invoiceSaved = true, pdfPath = pdfResult.getOrNull(), showShareDialog = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, invoiceSaved = true, errorMessage = "Invoice saved but PDF generation failed: ${pdfResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Failed to save invoice: ${e.message}")
            }
        }
    }

    /** Creates an [Invoice] object from the current UI state. */
    private fun createInvoiceFromState(): Invoice {
        val invoiceItems = _uiState.value.items.map {
            InvoiceItem(
                name = it.name,
                quantity = it.quantity.toDoubleOrNull() ?: 0.0,
                unit = "piece",
                rate = it.rate.toDoubleOrNull() ?: 0.0,
                amount = it.amount
            )
        }
        val paymentMode = PaymentMode.valueOf(_uiState.value.paymentMode.uppercase())
        return Invoice(
            invoiceNumber = "INV${System.currentTimeMillis()}",
            customerName = _uiState.value.customerName,
            customerPhone = _uiState.value.customerPhone,
            items = invoiceItems,
            subtotal = _uiState.value.subtotal,
            cgst = _uiState.value.cgst,
            sgst = _uiState.value.sgst,
            totalAmount = _uiState.value.total,
            paymentMode = paymentMode,
            isPaid = paymentMode != PaymentMode.CREDIT,
            timestamp = Date(),
            pdfPath = null
        )
    }

    /** Clears the current error message. */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** Updates the error message in the UI state. */
    fun updateError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    /** Shares the generated PDF via WhatsApp. */
    fun shareViaWhatsApp() {
        val pdfPath = _uiState.value.pdfPath ?: return
        val phoneNumber = _uiState.value.customerPhone
        viewModelScope.launch {
            val result = if (phoneNumber.isNotBlank()) shareHelper.shareToWhatsAppContact(pdfPath, phoneNumber) else shareHelper.shareViaWhatsApp(pdfPath)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to share: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /** Shares the generated PDF via other apps using the system share sheet. */
    fun shareViaOther() {
        val pdfPath = _uiState.value.pdfPath ?: return
        viewModelScope.launch {
            val result = shareHelper.shareViaIntent(pdfPath)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to share: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /** Opens the generated PDF in a viewer app. */
    fun openPdf() {
        val pdfPath = _uiState.value.pdfPath ?: return
        viewModelScope.launch {
            val result = shareHelper.openPdf(pdfPath)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to open PDF: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /** Dismisses the post-save share dialog. */
    fun dismissShareDialog() {
        _uiState.value = _uiState.value.copy(showShareDialog = false)
    }
}
