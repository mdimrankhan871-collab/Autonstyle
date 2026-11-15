package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AssignedPackaging(
    val packagingId: String = "",
    val packagingName: String = "",
    val quantityPerUnit: Long = 1L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "packagingId" to packagingId,
        "packagingName" to packagingName,
        "quantityPerUnit" to quantityPerUnit
    )
}

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val availableStock: Long = 0L,
    val minStockLevel: Long = 5L,
    val damagedStock: Long = 0L,
    val assignedPackaging: List<AssignedPackaging> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductPurchase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val supplier: String = "",
    val purchaseDate: String = "",
    val purchaseEpochMs: Long = 0L,
    val quantity: Long = 0L,
    val unitPrice: Double = 0.0,
    val totalCost: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PackagingItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val purchasePrice: Double = 0.0,
    val purchasedQty: Long = 0L,
    val availableQty: Long = 0L,
    val minStockLevel: Long = 20L,
    val usedQty: Long = 0L,
    val wastedQty: Long = 0L,
    val supplier: String = "",
    val lastPurchaseDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PackagingPurchase(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packagingId: String = "",
    val packagingName: String = "",
    val supplier: String = "",
    val purchaseDate: String = "",
    val purchaseEpochMs: Long = 0L,
    val quantity: Long = 0L,
    val unitPrice: Double = 0.0,
    val totalCost: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    PACKED("Packed"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    PARTIAL_RETURN("Partial Return"),
    RETURNED("Returned");

    companion object {
        fun fromString(raw: String): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val quantity: Long = 1L,
    val unitSellingPrice: Double = 0.0,
    val unitCostPrice: Double = 0.0,
    val lineTotal: Double = 0.0,
    val returnedQty: Long = 0L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "sku" to sku,
        "quantity" to quantity,
        "unitSellingPrice" to unitSellingPrice,
        "unitCostPrice" to unitCostPrice,
        "lineTotal" to lineTotal,
        "returnedQty" to returnedQty
    )
}

@IgnoreExtraProperties
data class PackagingBreakdownItem(
    val packagingId: String = "",
    val packagingName: String = "",
    val quantityUsed: Long = 0L,
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "packagingId" to packagingId,
        "packagingName" to packagingName,
        "quantityUsed" to quantityUsed,
        "unitCost" to unitCost,
        "totalCost" to totalCost
    )
}

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val orderDate: String = "",
    val orderEpochMs: Long = 0L,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = OrderStatus.PENDING.name,
    val courierName: String = "",
    val trackingNumber: String = "",
    val deliveryNote: String = "",
    val stockDeducted: Boolean = false,
    val productCostTotal: Double = 0.0,
    val packagingCostTotal: Double = 0.0,
    val packagingBreakdown: List<PackagingBreakdownItem> = emptyList(),
    val returnedAmount: Double = 0.0,
    val returnedQtyTotal: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: OrderStatus
        get() = OrderStatus.fromString(status)
}

@IgnoreExtraProperties
data class ProductReturn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val invoiceNumber: String = "",
    val productId: String = "",
    val productName: String = "",
    val returnedQty: Long = 0L,
    val refundAmount: Double = 0.0,
    val restoredCostAmount: Double = 0.0,
    val reason: String = "",
    val returnCourierCharge: Double = 0.0,
    val isReceived: Boolean = true,
    val isResellable: Boolean = true,
    val returnDate: String = "",
    val returnEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ExpenseCategory(val label: String) {
    FACEBOOK_ADS("Facebook Ads"),
    COURIER("Courier"),
    RETURN_COURIER("Return Courier"),
    PACKAGING("Packaging"),
    WEBSITE_MAINTENANCE("Website Maintenance"),
    TRANSPORTATION("Transportation"),
    OTHER("Other Expenses");

    companion object {
        fun fromString(raw: String): ExpenseCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = ExpenseCategory.OTHER.name,
    val title: String = "",
    val amount: Double = 0.0,
    val expenseDate: String = "",
    val expenseEpochMs: Long = 0L,
    val notes: String = "",
    val linkedReferenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromString(category)
}

@IgnoreExtraProperties
data class StockTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemType: String = "PRODUCT",
    val itemId: String = "",
    val itemName: String = "",
    val txType: String = "",
    val quantityDelta: Long = 0L,
    val stockBefore: Long = 0L,
    val stockAfter: Long = 0L,
    val unitCost: Double = 0.0,
    val totalValue: Double = 0.0,
    val referenceId: String = "",
    val referenceLabel: String = "",
    val txDate: String = "",
    val txEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
