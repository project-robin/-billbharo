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

/**
 * The ViewModel for the [HomeScreen].
 *
 * This class is responsible for loading dashboard data, handling user interactions such as
 * refreshing the data, sharing invoices, and managing the voice-to-invoice workflow.
 *
 * @property invoiceRepository The repository for accessing invoice data.
 * @property shareHelper A utility for sharing files.
 * @property geminiAudioTranscriber The service for transcribing audio to text.
 * @property geminiInvoiceParser The service for parsing transcribed text into structured data.
 */
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

    /**
     * Cleans up resources when the ViewModel is destroyed.
     *
     * This ensures that any ongoing voice recognition is cancelled to prevent leaks.
     */
    override fun onCleared() {
        super.onCleared()
        voiceRecognitionJob?.cancel()
        geminiAudioTranscriber.stopRecording()
    }

    init {
        loadDashboardData()
    }

    /**
     * Loads the necessary data for the dashboard from the repository.
     *
     * This includes today's sales, total pending credit, and a list of recent invoices.
     */
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

                invoiceRepository.getAllInvoices().collect { invoices ->
                    val todayInvoices = invoices.filter { it.timestamp >= today && it.timestamp < tomorrow }
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

    /**
     * Reloads the dashboard data.
     */
    fun refresh() {
        loadDashboardData()
    }

    /**
     * Shares a given invoice, typically via WhatsApp.
     *
     * @param invoice The [Invoice] to be shared.
     */
    fun shareInvoice(invoice: Invoice) {
        viewModelScope.launch {
            if (invoice.pdfPath.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(error = "PDF not available for this invoice")
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

    /**
     * Clears the current error message from the UI state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Starts the voice recognition and parsing process for creating a new invoice.
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
                            _uiState.value = _uiState.value.copy(voiceRecordingStatus = "Ready")
                        }
                        is VoiceTranscriptionResult.Recording -> {
                            _uiState.value = _uiState.value.copy(voiceRecordingStatus = "🎤 Speak now...")
                        }
                        is VoiceTranscriptionResult.Processing -> {
                            _uiState.value = _uiState.value.copy(voiceRecordingStatus = "🤖 Processing with Gemini...")
                        }
                        is VoiceTranscriptionResult.Success -> {
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
     * Stops the voice recording.
     *
     * This signals the transcriber to stop listening but allows any ongoing processing to complete.
     */
    fun stopVoiceRecording() {
        geminiAudioTranscriber.stopRecording()
        _uiState.value = _uiState.value.copy(voiceRecordingStatus = "⏹️ Stopped, processing...")
    }

    /**
     * Processes the transcribed text with the Gemini parser to extract structured invoice data.
     *
     * @param transcription The text output from the speech-to-text engine.
     */
    private fun processVoiceTranscription(transcription: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(voiceRecordingStatus = "Parsing invoice data...")
                val result = geminiInvoiceParser.parseInvoiceItem(transcription)

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

/**
 * Represents the UI state for the [HomeScreen].
 *
 * @property todaySales The total sales amount for the current day.
 * @property pendingCredit The total outstanding credit amount.
 * @property lowStockCount The number of items with low stock.
 * @property recentInvoices A list of the most recent invoices.
 * @property totalInvoicesToday The total number of invoices created today.
 * @property isLoading A flag indicating if the screen is currently loading data.
 * @property error An optional error message to be displayed.
 * @property isVoiceRecording A flag indicating if voice recording is active.
 * @property voiceRecordingStatus A string describing the current status of the voice recording.
 * @property voiceResult The result of a successful voice parsing operation.
 */
data class HomeUiState(
    val todaySales: Double = 0.0,
    val pendingCredit: Double = 0.0,
    val lowStockCount: Int = 0,
    val recentInvoices: List<Invoice> = emptyList(),
    val totalInvoicesToday: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isVoiceRecording: Boolean = false,
    val voiceRecordingStatus: String = "",
    val voiceResult: VoiceResult? = null
)

/**
 * A data class to hold the structured result of a voice recognition and parsing operation.
 *
 * This is used to pass the parsed data to the New Invoice screen.
 *
 * @property itemName The name of the parsed item.
 * @property quantity The quantity of the item.
 * @property price The price of the item.
 */
data class VoiceResult(
    val itemName: String,
    val quantity: Double,
    val price: Double
)
