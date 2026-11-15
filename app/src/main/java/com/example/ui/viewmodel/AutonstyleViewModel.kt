package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.PackagingPurchase
import com.example.data.model.Product
import com.example.data.model.ProductPurchase
import com.example.data.model.ProductReturn
import com.example.data.model.StockTransaction
import com.example.data.repository.AutonstyleRepository
import com.example.domain.AccountingEngine
import com.example.domain.DashboardSummary
import com.example.domain.ProductProfitRow
import com.example.domain.ProfitAndLossReport
import com.example.domain.ReportPeriod
import com.example.domain.StockAlertsSummary
import com.example.util.DhakaDateTime
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class BusinessSnapshot(
    val products: List<Product> = emptyList(),
    val productPurchases: List<ProductPurchase> = emptyList(),
    val packagingItems: List<PackagingItem> = emptyList(),
    val packagingPurchases: List<PackagingPurchase> = emptyList(),
    val orders: List<Order> = emptyList(),
    val returns: List<ProductReturn> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val stockTransactions: List<StockTransaction> = emptyList(),
    val dashboard: DashboardSummary = AccountingEngine.computeDashboardSummary(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    val stockAlerts: StockAlertsSummary = AccountingEngine.computeStockAlerts(emptyList(), emptyList()),
    val dailyPnl: ProfitAndLossReport = AccountingEngine.computeProfitAndLossForPeriod(
        ReportPeriod.DAILY, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    val weeklyPnl: ProfitAndLossReport = AccountingEngine.computeProfitAndLossForPeriod(
        ReportPeriod.WEEKLY, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    val monthlyPnl: ProfitAndLossReport = AccountingEngine.computeProfitAndLossForPeriod(
        ReportPeriod.MONTHLY, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    val allTimePnl: ProfitAndLossReport = AccountingEngine.computeProfitAndLossForPeriod(
        ReportPeriod.ALL_TIME, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    val productProfitRows: List<ProductProfitRow> = emptyList()
)

class AutonstyleViewModel(
    private val repository: AutonstyleRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val coreInventoryFlow: Flow<Triple<List<Product>, List<ProductPurchase>, List<PackagingItem>>> =
        combine(
            repository.observeProducts(currentUserId),
            repository.observeProductPurchases(currentUserId),
            repository.observePackagingItems(currentUserId)
        ) { products, prodPurchases, packaging ->
            Triple(products, prodPurchases, packaging)
        }

    private val ordersAndReturnsFlow: Flow<Triple<List<PackagingPurchase>, List<Order>, List<ProductReturn>>> =
        combine(
            repository.observePackagingPurchases(currentUserId),
            repository.observeOrders(currentUserId),
            repository.observeReturns(currentUserId)
        ) { pkgPurchases, orders, returns ->
            Triple(pkgPurchases, orders, returns)
        }

    val businessState: StateFlow<UiState<BusinessSnapshot>> = combine(
        coreInventoryFlow,
        ordersAndReturnsFlow,
        repository.observeExpenses(currentUserId),
        repository.observeStockTransactions(currentUserId)
    ) { core, ordRet, expenses, transactions ->
        val products = core.first
        val prodPurchases = core.second
        val packaging = core.third
        val pkgPurchases = ordRet.first
        val orders = ordRet.second
        val returns = ordRet.third

        val nowMs = System.currentTimeMillis()
        BusinessSnapshot(
            products = products,
            productPurchases = prodPurchases,
            packagingItems = packaging,
            packagingPurchases = pkgPurchases,
            orders = orders,
            returns = returns,
            expenses = expenses,
            stockTransactions = transactions,
            dashboard = AccountingEngine.computeDashboardSummary(
                products, packaging, orders, returns, expenses, prodPurchases, pkgPurchases, nowMs
            ),
            stockAlerts = AccountingEngine.computeStockAlerts(products, packaging),
            dailyPnl = AccountingEngine.computeProfitAndLossForPeriod(
                ReportPeriod.DAILY, orders, returns, expenses, packaging, transactions, nowMs
            ),
            weeklyPnl = AccountingEngine.computeProfitAndLossForPeriod(
                ReportPeriod.WEEKLY, orders, returns, expenses, packaging, transactions, nowMs
            ),
            monthlyPnl = AccountingEngine.computeProfitAndLossForPeriod(
                ReportPeriod.MONTHLY, orders, returns, expenses, packaging, transactions, nowMs
            ),
            allTimePnl = AccountingEngine.computeProfitAndLossForPeriod(
                ReportPeriod.ALL_TIME, orders, returns, expenses, packaging, transactions, nowMs
            ),
            productProfitRows = AccountingEngine.computeProductWiseProfit(products, orders, returns)
        )
    }
        .map<BusinessSnapshot, UiState<BusinessSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing business data", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load Autonstyle business data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun saveProduct(product: Product, onSuccess: () -> Unit = {}) {
        launchAction("Product saved") {
            repository.saveProduct(currentUserId, product).map { onSuccess() }
        }
    }

    fun deleteProduct(productId: String) {
        launchAction("Product deleted") {
            repository.deleteProduct(currentUserId, productId)
        }
    }

    fun updateProductMinStock(productId: String, minStock: Long) {
        launchAction("Minimum product stock updated") {
            repository.updateProductMinStock(currentUserId, productId, minStock)
        }
    }

    fun recordProductPurchase(
        productId: String,
        supplier: String,
        purchaseDate: String,
        quantity: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        updateProductPurchasePrice: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        launchAction("Purchase recorded & product stock increased") {
            repository.recordProductPurchase(
                currentUserId,
                productId,
                supplier,
                purchaseDate,
                quantity,
                unitPrice,
                totalCost,
                notes,
                updateProductPurchasePrice
            ).map { onSuccess() }
        }
    }

    fun savePackagingItem(item: PackagingItem, onSuccess: () -> Unit = {}) {
        launchAction("Packaging item saved") {
            repository.savePackagingItem(currentUserId, item).map { onSuccess() }
        }
    }

    fun deletePackagingItem(packagingId: String) {
        launchAction("Packaging item deleted") {
            repository.deletePackagingItem(currentUserId, packagingId)
        }
    }

    fun updatePackagingMinStock(packagingId: String, minStock: Long) {
        launchAction("Minimum packaging stock updated") {
            repository.updatePackagingMinStock(currentUserId, packagingId, minStock)
        }
    }

    fun recordPackagingPurchase(
        packagingId: String,
        supplier: String,
        purchaseDate: String,
        quantity: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        alsoLogPackagingExpense: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        launchAction("Packaging restocked") {
            repository.recordPackagingPurchase(
                currentUserId,
                packagingId,
                supplier,
                purchaseDate,
                quantity,
                unitPrice,
                totalCost,
                notes,
                alsoLogPackagingExpense
            ).map { onSuccess() }
        }
    }

    fun recordPackagingWaste(
        packagingId: String,
        wastedQty: Long,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        launchAction("Packaging waste recorded") {
            repository.recordPackagingWaste(currentUserId, packagingId, wastedQty, reason).map { onSuccess() }
        }
    }

    fun createOrder(
        invoiceNumber: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        orderDate: String,
        items: List<OrderItem>,
        discountAmount: Double,
        deliveryCharge: Double,
        initialStatus: OrderStatus,
        courierName: String,
        trackingNumber: String,
        deliveryNote: String,
        autoLogCourierExpense: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val successMsg = if (initialStatus == OrderStatus.PENDING) {
            "Order created as Pending"
        } else {
            "Order created & stock + packaging deducted atomically"
        }
        launchAction(successMsg) {
            repository.createOrder(
                currentUserId,
                invoiceNumber,
                customerName,
                customerPhone,
                customerAddress,
                orderDate,
                items,
                discountAmount,
                deliveryCharge,
                initialStatus,
                courierName,
                trackingNumber,
                deliveryNote,
                autoLogCourierExpense
            ).map { onSuccess() }
        }
    }

    fun transitionOrderStatus(
        orderId: String,
        targetStatus: OrderStatus,
        courierName: String? = null,
        trackingNumber: String? = null,
        autoLogCourierExpense: Boolean = true,
        onSuccess: () -> Unit = {}
    ) {
        launchAction("Order updated to ${targetStatus.label}") {
            repository.transitionOrderStatus(
                currentUserId,
                orderId,
                targetStatus,
                courierName,
                trackingNumber,
                autoLogCourierExpense
            ).map { onSuccess() }
        }
    }

    fun cancelOrder(orderId: String, restoreProductStock: Boolean, onSuccess: () -> Unit = {}) {
        launchAction("Order cancelled") {
            repository.cancelOrder(currentUserId, orderId, restoreProductStock).map { onSuccess() }
        }
    }

    fun processProductReturn(
        orderId: String,
        productId: String,
        returnedQty: Long,
        reason: String,
        returnCourierCharge: Double,
        isReceived: Boolean,
        isResellable: Boolean,
        returnDate: String = DhakaDateTime.todayString(),
        onSuccess: () -> Unit = {}
    ) {
        launchAction("Return processed") {
            repository.processProductReturn(
                currentUserId,
                orderId,
                productId,
                returnedQty,
                reason,
                returnCourierCharge,
                isReceived,
                isResellable,
                returnDate
            ).map { onSuccess() }
        }
    }

    fun saveExpense(expense: Expense, onSuccess: () -> Unit = {}) {
        launchAction("Expense saved") {
            repository.saveExpense(currentUserId, expense).map { onSuccess() }
        }
    }

    fun deleteExpense(expenseId: String) {
        launchAction("Expense deleted") {
            repository.deleteExpense(currentUserId, expenseId)
        }
    }

    suspend fun exportBackupJson(): Result<String> = repository.exportBackupJson(currentUserId)

    fun restoreBackupJson(jsonText: String, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.restoreBackupJson(currentUserId, jsonText)
            _isBusy.value = false
            res.onSuccess { count ->
                _messages.tryEmit("Restored $count record(s) into Cloud Firestore")
                onDone(count)
            }.onFailure { err ->
                _messages.tryEmit("Restore failed: ${err.localizedMessage ?: "Invalid backup"}")
            }
        }
    }

    fun emitMessage(message: String) {
        _messages.tryEmit(message)
    }

    private fun launchAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            res.onSuccess {
                _messages.tryEmit(successMessage)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Operation failed")
            }
        }
    }

    private companion object {
        const val TAG = "AutonstyleVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
