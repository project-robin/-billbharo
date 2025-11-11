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

/**
 * Represents the UI state for the [KhataScreen].
 *
 * @property customers A list of customers with their credit details.
 * @property totalCredit The total outstanding credit across all customers.
 * @property isLoading A flag indicating if the screen is currently loading data.
 * @property error An optional error message to be displayed.
 * @property searchQuery The current search query entered by the user.
 */
data class KhataUiState(
    val customers: List<CustomerWithCredit> = emptyList(),
    val totalCredit: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

/**
 * A data class that combines a [CustomerEntity] with their credit-related information.
 *
 * @property customer The customer entity.
 * @property creditInvoices A list of the customer's unpaid credit invoices.
 * @property totalCreditAmount The total outstanding credit amount for the customer.
 */
data class CustomerWithCredit(
    val customer: CustomerEntity,
    val creditInvoices: List<Invoice> = emptyList(),
    val totalCreditAmount: Double = 0.0
)

/**
 * The ViewModel for the [KhataScreen].
 *
 * This class is responsible for fetching and managing the list of customers with credit,
 * handling search functionality, and providing the UI state to the screen.
 *
 * @property customerRepository The repository for accessing customer and invoice data.
 */
@HiltViewModel
class KhataViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KhataUiState())
    val uiState: StateFlow<KhataUiState> = _uiState.asStateFlow()

    init {
        loadCustomersWithCredit()
    }

    /**
     * Loads the list of customers with outstanding credit from the repository.
     */
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

    /**
     * Searches for customers based on a query string and updates the UI state.
     *
     * @param query The search query to filter customers by.
     */
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

    /**
     * Reloads the list of customers with credit.
     */
    fun refresh() {
        loadCustomersWithCredit()
    }
}
