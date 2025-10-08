package com.billbharo.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbharo.data.models.Invoice
import com.billbharo.data.repository.InvoiceRepository
import com.billbharo.domain.utils.ShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val shareHelper: ShareHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
}

data class HomeUiState(
    val todaySales: Double = 0.0,
    val pendingCredit: Double = 0.0,
    val lowStockCount: Int = 0,
    val recentInvoices: List<Invoice> = emptyList(),
    val totalInvoicesToday: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)
