package com.billbharo.ui.screens.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billbharo.data.local.entities.CustomerEntity
import com.billbharo.data.models.Invoice
import com.billbharo.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KhataUiState(
    val customers: List<CustomerWithCredit> = emptyList(),
    val totalCredit: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

data class CustomerWithCredit(
    val customer: CustomerEntity,
    val creditInvoices: List<Invoice> = emptyList(),
    val totalCreditAmount: Double = 0.0
)

@HiltViewModel
class KhataViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KhataUiState())
    val uiState: StateFlow<KhataUiState> = _uiState.asStateFlow()

    init {
        loadCustomersWithCredit()
    }

    private fun loadCustomersWithCredit() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                customerRepository.getCustomersWithCredit().collect { customers ->
                    val customersWithCredit = customers.map { customer ->
                        val phone = customer.phone ?: ""
                        var creditInvoices = emptyList<Invoice>()
                        var totalCredit = 0.0
                        
                        if (phone.isNotEmpty()) {
                            customerRepository.getCustomerCreditInvoices(phone).collect { invoices ->
                                creditInvoices = invoices
                                totalCredit = invoices.sumOf { it.totalAmount }
                            }
                        }
                        
                        CustomerWithCredit(
                            customer = customer,
                            creditInvoices = creditInvoices,
                            totalCreditAmount = totalCredit
                        )
                    }
                    
                    val total = customersWithCredit.sumOf { it.totalCreditAmount }
                    
                    _uiState.value = KhataUiState(
                        customers = customersWithCredit,
                        totalCredit = total,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = KhataUiState(
                    error = e.message ?: "Error loading customers",
                    isLoading = false
                )
            }
        }
    }

    fun searchCustomers(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isEmpty()) {
            loadCustomersWithCredit()
            return
        }
        
        viewModelScope.launch {
            try {
                customerRepository.searchCustomers(query).collect { customers ->
                    val customersWithCredit = customers.map { customer ->
                        CustomerWithCredit(
                            customer = customer,
                            totalCreditAmount = customer.totalCreditAmount
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        customers = customersWithCredit,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error searching customers"
                )
            }
        }
    }

    fun refresh() {
        loadCustomersWithCredit()
    }
}
