package com.billbharo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbharo.data.models.Invoice
import com.billbharo.data.repository.InvoiceRepository
import com.billbharo.domain.utils.GeminiAudioTranscriber
import com.billbharo.domain.utils.GeminiInvoiceParser
import com.billbharo.domain.utils.ShareHelper
import com.billbharo.domain.utils.VoiceTranscriptionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val shareHelper: ShareHelper,
    private val geminiAudioTranscriber: GeminiAudioTranscriber,
    private val geminiInvoiceParser: GeminiInvoiceParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var voiceRecognitionJob: Job? = null
    
    override fun onCleared() {
        super.onCleared()
        // Cancel on ViewModel destruction (e.g., back button)
        voiceRecognitionJob?.cancel()
        geminiAudioTranscriber.stopRecording()
    }

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val todaySales = invoiceRepository.getTotalSalesByDateRange(today, tomorrow)
                
                // Collect all invoices
                invoiceRepository.getAllInvoices().collect { invoices ->
                    val todayInvoices = invoices.filter { invoice ->
                        invoice.timestamp >= today && invoice.timestamp < tomorrow
                    }
                    
                    val pendingCredit = invoices.filter { !it.isPaid }.sumOf { it.totalAmount }
                    
                    _uiState.value = HomeUiState(
                        todaySales = todaySales,
                        pendingCredit = pendingCredit,
                        recentInvoices = todayInvoices.take(10),
                        totalInvoicesToday = todayInvoices.size,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    error = e.message ?: "Error loading data",
                    isLoading = false
                )
            }
        }
    }
    
    fun refresh() {
        loadDashboardData()
    }

    fun shareInvoice(invoice: Invoice) {
        viewModelScope.launch {
            if (invoice.pdfPath.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    error = "PDF not available for this invoice"
                )
                return@launch
            }

            val result = if (!invoice.customerPhone.isNullOrEmpty()) {
                shareHelper.shareToWhatsAppContact(invoice.pdfPath, invoice.customerPhone)
            } else {
                shareHelper.shareViaWhatsApp(invoice.pdfPath)
            }

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to share: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Start voice recording for invoice creation
     */
    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(
            isVoiceRecording = true,
            voiceRecordingStatus = "Initializing..."
        )
        
        voiceRecognitionJob = viewModelScope.launch {
            try {
                geminiAudioTranscriber.transcribeAudio("hi-IN").collect { result ->
                    when (result) {
                        is VoiceTranscriptionResult.Ready -> {
                            _uiState.value = _uiState.value.copy(
                                voiceRecordingStatus = "Ready"
                            )
                        }
                        is VoiceTranscriptionResult.Recording -> {
                            _uiState.value = _uiState.value.copy(
                                voiceRecordingStatus = "🎤 Speak now..."
                            )
                        }
                        is VoiceTranscriptionResult.Processing -> {
                            _uiState.value = _uiState.value.copy(
                                voiceRecordingStatus = "🤖 Processing with Gemini..."
                            )
                        }
                        is VoiceTranscriptionResult.Success -> {
                            // Parse transcription into structured data
                            processVoiceTranscription(result.transcription)
                        }
                        is VoiceTranscriptionResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isVoiceRecording = false,
                                voiceRecordingStatus = "",
                                error = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVoiceRecording = false,
                    voiceRecordingStatus = "",
                    error = "Voice recording failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Stop voice recording - signals stop but allows processing to complete
     */
    fun stopVoiceRecording() {
        // Don't cancel the job - let it complete naturally
        // Just signal the audio recorder to stop recording
        geminiAudioTranscriber.stopRecording()
        
        // Update UI to show processing
        _uiState.value = _uiState.value.copy(
            voiceRecordingStatus = "⏹️ Stopped, processing..."
        )
    }
    
    /**
     * Process voice transcription with Gemini parser
     */
    private fun processVoiceTranscription(transcription: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    voiceRecordingStatus = "Parsing invoice data..."
                )
                
                val result = geminiInvoiceParser.parseInvoiceItem(transcription)
                
                // Success - store result for navigation
                _uiState.value = _uiState.value.copy(
                    isVoiceRecording = false,
                    voiceRecordingStatus = "",
                    voiceResult = VoiceResult(
                        itemName = result.itemName,
                        quantity = result.quantity,
                        price = result.price
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVoiceRecording = false,
                    voiceRecordingStatus = "",
                    error = "Failed to parse: ${e.message}"
                )
            }
        }
    }
}

data class HomeUiState(
    val todaySales: Double = 0.0,
    val pendingCredit: Double = 0.0,
    val lowStockCount: Int = 0,
    val recentInvoices: List<Invoice> = emptyList(),
    val totalInvoicesToday: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Voice recording state
    val isVoiceRecording: Boolean = false,
    val voiceRecordingStatus: String = "",
    val voiceResult: VoiceResult? = null
)

/**
 * Voice recognition result for invoice creation
 */
data class VoiceResult(
    val itemName: String,
    val quantity: Double,
    val price: Double
)
