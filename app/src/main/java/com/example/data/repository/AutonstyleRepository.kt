package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AssignedPackaging
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingBreakdownItem
import com.example.data.model.PackagingItem
import com.example.data.model.PackagingPurchase
import com.example.data.model.Product
import com.example.data.model.ProductPurchase
import com.example.data.model.ProductReturn
import com.example.data.model.StockTransaction
import com.example.util.DhakaDateTime
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AutonstyleRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun productsCol(userId: String) = userDoc(userId).collection("products")
    private fun productPurchasesCol(userId: String) = userDoc(userId).collection("productPurchases")
    private fun packagingItemsCol(userId: String) = userDoc(userId).collection("packagingItems")
    private fun packagingPurchasesCol(userId: String) = userDoc(userId).collection("packagingPurchases")
    private fun ordersCol(userId: String) = userDoc(userId).collection("orders")
    private fun returnsCol(userId: String) = userDoc(userId).collection("returns")
    private fun expensesCol(userId: String) = userDoc(userId).collection("expenses")
    private fun transactionsCol(userId: String) = userDoc(userId).collection("stockTransactions")

    private inline fun <reified T> observeCollection(
        userId: String,
        collectionName: String
    ): Flow<List<T>> {
        val col = userDoc(userId).collection(collectionName)
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    T::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, col.path)
                throw error
            }
    }

    fun observeProducts(userId: String): Flow<List<Product>> =
        observeCollection<Product>(userId, "products")
            .map { list -> list.sortedBy { it.name.lowercase() } }

    fun observeProductPurchases(userId: String): Flow<List<ProductPurchase>> =
        observeCollection<ProductPurchase>(userId, "productPurchases")
            .map { list -> list.sortedByDescending { it.purchaseEpochMs } }

    fun observePackagingItems(userId: String): Flow<List<PackagingItem>> =
        observeCollection<PackagingItem>(userId, "packagingItems")
            .map { list -> list.sortedBy { it.name.lowercase() } }

    fun observePackagingPurchases(userId: String): Flow<List<PackagingPurchase>> =
        observeCollection<PackagingPurchase>(userId, "packagingPurchases")
            .map { list -> list.sortedByDescending { it.purchaseEpochMs } }

    fun observeOrders(userId: String): Flow<List<Order>> =
        observeCollection<Order>(userId, "orders")
            .map { list -> list.sortedByDescending { it.orderEpochMs } }

    fun observeReturns(userId: String): Flow<List<ProductReturn>> =
        observeCollection<ProductReturn>(userId, "returns")
            .map { list -> list.sortedByDescending { it.returnEpochMs } }

    fun observeExpenses(userId: String): Flow<List<Expense>> =
        observeCollection<Expense>(userId, "expenses")
            .map { list -> list.sortedByDescending { it.expenseEpochMs } }

    fun observeStockTransactions(userId: String): Flow<List<StockTransaction>> =
        observeCollection<StockTransaction>(userId, "stockTransactions")
            .map { list -> list.sortedByDescending { it.txEpochMs } }

    // --- PRODUCTS CRUD ---

    suspend fun saveProduct(
        userId: String,
        product: Product
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, productsCol(userId).path) {
        val docRef = if (product.id.isBlank()) {
            productsCol(userId).document(UUID.randomUUID().toString())
        } else {
            productsCol(userId).document(product.id)
        }
        val packagingList = product.assignedPackaging.map { it.toMap() }
        if (product.id.isBlank()) {
            val payload = mapOf(
                "userId" to userId,
                "name" to product.name.trim(),
                "sku" to product.sku.trim(),
                "category" to product.category.trim(),
                "purchasePrice" to product.purchasePrice.coerceAtLeast(0.0),
                "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
                "availableStock" to product.availableStock.coerceAtLeast(0L),
                "minStockLevel" to product.minStockLevel.coerceAtLeast(0L),
                "damagedStock" to product.damagedStock.coerceAtLeast(0L),
                "assignedPackaging" to packagingList,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "name" to product.name.trim(),
                "sku" to product.sku.trim(),
                "category" to product.category.trim(),
                "purchasePrice" to product.purchasePrice.coerceAtLeast(0.0),
                "sellingPrice" to product.sellingPrice.coerceAtLeast(0.0),
                "availableStock" to product.availableStock.coerceAtLeast(0L),
                "minStockLevel" to product.minStockLevel.coerceAtLeast(0L),
                "damagedStock" to product.damagedStock.coerceAtLeast(0L),
                "assignedPackaging" to packagingList,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        }
        docRef.id
    }

    suspend fun updateProductMinStock(
        userId: String,
        productId: String,
        minStockLevel: Long
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, productsCol(userId).document(productId).path) {
        productsCol(userId).document(productId).update(
            mapOf(
                "minStockLevel" to minStockLevel.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun deleteProduct(userId: String, productId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, productsCol(userId).document(productId).path) {
            productsCol(userId).document(productId).delete().await()
            Unit
        }

    // --- PRODUCT PURCHASE (ATOMIC STOCK INCREASE + AUDIT TRAIL) ---

    suspend fun recordProductPurchase(
        userId: String,
        productId: String,
        supplier: String,
        purchaseDate: String,
        quantity: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        updateProductPurchasePrice: Boolean = true
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, productPurchasesCol(userId).path) {
        require(quantity > 0L) { "Purchase quantity must be greater than zero." }
        require(unitPrice >= 0.0 && totalCost >= 0.0) { "Prices cannot be negative." }
        require(DhakaDateTime.isValidDateFormat(purchaseDate)) { "Purchase date must be DD-MM-YYYY." }

        val purchaseId = UUID.randomUUID().toString()
        val txId = UUID.randomUUID().toString()
        val prodRef = productsCol(userId).document(productId)
        val purchaseRef = productPurchasesCol(userId).document(purchaseId)
        val txRef = transactionsCol(userId).document(txId)
        val epochMs = DhakaDateTime.parseDateOrNow(purchaseDate)

        db.runTransaction { tx ->
            val prodSnap = tx.get(prodRef)
            val product = prodSnap.toObject(Product::class.java)
                ?: throw IllegalStateException("Product not found.")

            val before = product.availableStock
            val after = before + quantity
            val newPurchasePrice = if (updateProductPurchasePrice) unitPrice else product.purchasePrice

            tx.update(
                prodRef,
                mapOf(
                    "availableStock" to after,
                    "purchasePrice" to newPurchasePrice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                purchaseRef,
                mapOf(
                    "userId" to userId,
                    "productId" to productId,
                    "productName" to product.name,
                    "sku" to product.sku,
                    "supplier" to supplier.trim(),
                    "purchaseDate" to purchaseDate.trim(),
                    "purchaseEpochMs" to epochMs,
                    "quantity" to quantity,
                    "unitPrice" to unitPrice,
                    "totalCost" to totalCost,
                    "notes" to notes.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                txRef,
                mapOf(
                    "userId" to userId,
                    "itemType" to "PRODUCT",
                    "itemId" to productId,
                    "itemName" to product.name,
                    "txType" to "PRODUCT_PURCHASE",
                    "quantityDelta" to quantity,
                    "stockBefore" to before,
                    "stockAfter" to after,
                    "unitCost" to unitPrice,
                    "totalValue" to totalCost,
                    "referenceId" to purchaseId,
                    "referenceLabel" to "Purchase (${supplier.trim()})",
                    "txDate" to purchaseDate.trim(),
                    "txEpochMs" to epochMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            purchaseId
        }.await()
    }

    // --- PACKAGING CRUD & PURCHASE & WASTE ---

    suspend fun savePackagingItem(
        userId: String,
        item: PackagingItem
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, packagingItemsCol(userId).path) {
        val docRef = if (item.id.isBlank()) {
            packagingItemsCol(userId).document(UUID.randomUUID().toString())
        } else {
            packagingItemsCol(userId).document(item.id)
        }
        if (item.id.isBlank()) {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "name" to item.name.trim(),
                    "purchasePrice" to item.purchasePrice.coerceAtLeast(0.0),
                    "purchasedQty" to item.purchasedQty.coerceAtLeast(0L),
                    "availableQty" to item.availableQty.coerceAtLeast(0L),
                    "minStockLevel" to item.minStockLevel.coerceAtLeast(0L),
                    "usedQty" to item.usedQty.coerceAtLeast(0L),
                    "wastedQty" to item.wastedQty.coerceAtLeast(0L),
                    "supplier" to item.supplier.trim(),
                    "lastPurchaseDate" to item.lastPurchaseDate.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "name" to item.name.trim(),
                    "purchasePrice" to item.purchasePrice.coerceAtLeast(0.0),
                    "minStockLevel" to item.minStockLevel.coerceAtLeast(0L),
                    "availableQty" to item.availableQty.coerceAtLeast(0L),
                    "supplier" to item.supplier.trim(),
                    "lastPurchaseDate" to item.lastPurchaseDate.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        docRef.id
    }

    suspend fun updatePackagingMinStock(
        userId: String,
        packagingId: String,
        minStockLevel: Long
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, packagingItemsCol(userId).document(packagingId).path) {
        packagingItemsCol(userId).document(packagingId).update(
            mapOf(
                "minStockLevel" to minStockLevel.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun deletePackagingItem(userId: String, packagingId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, packagingItemsCol(userId).document(packagingId).path) {
            packagingItemsCol(userId).document(packagingId).delete().await()
            Unit
        }

    suspend fun recordPackagingPurchase(
        userId: String,
        packagingId: String,
        supplier: String,
        purchaseDate: String,
        quantity: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        alsoLogPackagingExpense: Boolean = true
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, packagingPurchasesCol(userId).path) {
        require(quantity > 0L) { "Purchase quantity must be greater than zero." }
        require(DhakaDateTime.isValidDateFormat(purchaseDate)) { "Purchase date must be DD-MM-YYYY." }

        val purchaseId = UUID.randomUUID().toString()
        val txId = UUID.randomUUID().toString()
        val expId = "exp_pkg_$purchaseId"
        val pkgRef = packagingItemsCol(userId).document(packagingId)
        val purchaseRef = packagingPurchasesCol(userId).document(purchaseId)
        val txRef = transactionsCol(userId).document(txId)
        val expRef = expensesCol(userId).document(expId)
        val epochMs = DhakaDateTime.parseDateOrNow(purchaseDate)

        db.runTransaction { tx ->
            val pkgSnap = tx.get(pkgRef)
            val pkg = pkgSnap.toObject(PackagingItem::class.java)
                ?: throw IllegalStateException("Packaging item not found.")
            val expSnap = if (alsoLogPackagingExpense) tx.get(expRef) else null

            val before = pkg.availableQty
            val after = before + quantity

            tx.update(
                pkgRef,
                mapOf(
                    "availableQty" to after,
                    "purchasedQty" to (pkg.purchasedQty + quantity),
                    "purchasePrice" to unitPrice,
                    "supplier" to supplier.trim(),
                    "lastPurchaseDate" to purchaseDate.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                purchaseRef,
                mapOf(
                    "userId" to userId,
                    "packagingId" to packagingId,
                    "packagingName" to pkg.name,
                    "supplier" to supplier.trim(),
                    "purchaseDate" to purchaseDate.trim(),
                    "purchaseEpochMs" to epochMs,
                    "quantity" to quantity,
                    "unitPrice" to unitPrice,
                    "totalCost" to totalCost,
                    "notes" to notes.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                txRef,
                mapOf(
                    "userId" to userId,
                    "itemType" to "PACKAGING",
                    "itemId" to packagingId,
                    "itemName" to pkg.name,
                    "txType" to "PACKAGING_PURCHASE",
                    "quantityDelta" to quantity,
                    "stockBefore" to before,
                    "stockAfter" to after,
                    "unitCost" to unitPrice,
                    "totalValue" to totalCost,
                    "referenceId" to purchaseId,
                    "referenceLabel" to "Pkg Purchase (${supplier.trim()})",
                    "txDate" to purchaseDate.trim(),
                    "txEpochMs" to epochMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            if (alsoLogPackagingExpense && totalCost > 0.0 && expSnap != null && !expSnap.exists()) {
                tx.set(
                    expRef,
                    mapOf(
                        "userId" to userId,
                        "category" to ExpenseCategory.PACKAGING.name,
                        "title" to "Packaging Purchase: ${pkg.name} (x$quantity)",
                        "amount" to totalCost,
                        "expenseDate" to purchaseDate.trim(),
                        "expenseEpochMs" to epochMs,
                        "notes" to "Supplier: ${supplier.trim()}",
                        "linkedReferenceId" to purchaseId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            purchaseId
        }.await()
    }

    suspend fun recordPackagingWaste(
        userId: String,
        packagingId: String,
        wastedQty: Long,
        reason: String,
        dateStr: String = DhakaDateTime.todayString()
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, packagingItemsCol(userId).document(packagingId).path) {
        require(wastedQty > 0L) { "Wasted quantity must be greater than zero." }
        val pkgRef = packagingItemsCol(userId).document(packagingId)
        val txId = UUID.randomUUID().toString()
        val txRef = transactionsCol(userId).document(txId)
        val epochMs = DhakaDateTime.parseDateOrNow(dateStr)

        db.runTransaction { tx ->
            val pkg = tx.get(pkgRef).toObject(PackagingItem::class.java)
                ?: throw IllegalStateException("Packaging item not found.")
            if (pkg.availableQty < wastedQty) {
                throw IllegalStateException("Insufficient ${pkg.name} stock (Available: ${pkg.availableQty}).")
            }
            val before = pkg.availableQty
            val after = before - wastedQty
            tx.update(
                pkgRef,
                mapOf(
                    "availableQty" to after,
                    "wastedQty" to (pkg.wastedQty + wastedQty),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                txRef,
                mapOf(
                    "userId" to userId,
                    "itemType" to "PACKAGING",
                    "itemId" to packagingId,
                    "itemName" to pkg.name,
                    "txType" to "PACKAGING_WASTE",
                    "quantityDelta" to -wastedQty,
                    "stockBefore" to before,
                    "stockAfter" to after,
                    "unitCost" to pkg.purchasePrice,
                    "totalValue" to (wastedQty * pkg.purchasePrice),
                    "referenceId" to txId,
                    "referenceLabel" to "Wasted: ${reason.ifBlank { "Damaged/Lost" }}",
                    "txDate" to dateStr,
                    "txEpochMs" to epochMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            Unit
        }.await()
    }

    // --- ORDERS & ATOMIC STOCK + PACKAGING DEDUCTION ---

    suspend fun createOrder(
        userId: String,
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
        autoLogCourierExpense: Boolean
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, ordersCol(userId).path) {
        require(items.isNotEmpty()) { "Order must contain at least one product." }
        require(DhakaDateTime.isValidDateFormat(orderDate)) { "Order date must be DD-MM-YYYY." }

        val orderId = UUID.randomUUID().toString()
        val orderRef = ordersCol(userId).document(orderId)
        val courierExpRef = expensesCol(userId).document("exp_ord_$orderId")
        val epochMs = DhakaDateTime.parseDateOrNow(orderDate)

        val subtotal = items.sumOf { it.lineTotal }
        val totalAmount = (subtotal - discountAmount + deliveryCharge).coerceAtLeast(0.0)
        val shouldDeductNow = initialStatus == OrderStatus.PACKED ||
            initialStatus == OrderStatus.DISPATCHED ||
            initialStatus == OrderStatus.DELIVERED

        db.runTransaction { tx ->
            val expSnap = if (shouldDeductNow && autoLogCourierExpense && deliveryCharge > 0.0) {
                tx.get(courierExpRef)
            } else null

            if (!shouldDeductNow) {
                tx.set(
                    orderRef,
                    mapOf(
                        "userId" to userId,
                        "invoiceNumber" to invoiceNumber.trim(),
                        "customerName" to customerName.trim(),
                        "customerPhone" to customerPhone.trim(),
                        "customerAddress" to customerAddress.trim(),
                        "orderDate" to orderDate.trim(),
                        "orderEpochMs" to epochMs,
                        "items" to items.map { it.toMap() },
                        "subtotal" to subtotal,
                        "discountAmount" to discountAmount.coerceAtLeast(0.0),
                        "deliveryCharge" to deliveryCharge.coerceAtLeast(0.0),
                        "totalAmount" to totalAmount,
                        "status" to initialStatus.name,
                        "courierName" to courierName.trim(),
                        "trackingNumber" to trackingNumber.trim(),
                        "deliveryNote" to deliveryNote.trim(),
                        "stockDeducted" to false,
                        "productCostTotal" to 0.0,
                        "packagingCostTotal" to 0.0,
                        "packagingBreakdown" to emptyList<Map<String, Any>>(),
                        "returnedAmount" to 0.0,
                        "returnedQtyTotal" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                return@runTransaction orderId
            }

            // Aggregate quantities per product
            val productQtyMap = linkedMapOf<String, Long>()
            for (item in items) {
                productQtyMap[item.productId] = (productQtyMap[item.productId] ?: 0L) + item.quantity
            }

            // Step 1: Read all Product documents FIRST
            val loadedProducts = linkedMapOf<String, Product>()
            for (pid in productQtyMap.keys) {
                val p = tx.get(productsCol(userId).document(pid)).toObject(Product::class.java)
                    ?: throw IllegalStateException("Product $pid not found.")
                loadedProducts[pid] = p
            }

            // Aggregate required packaging quantities across all order items
            val packagingQtyMap = linkedMapOf<String, Long>()
            for ((pid, orderedQty) in productQtyMap) {
                val product = loadedProducts.getValue(pid)
                for (assign in product.assignedPackaging) {
                    if (assign.packagingId.isNotBlank() && assign.quantityPerUnit > 0L) {
                        val needed = orderedQty * assign.quantityPerUnit
                        packagingQtyMap[assign.packagingId] =
                            (packagingQtyMap[assign.packagingId] ?: 0L) + needed
                    }
                }
            }

            // Step 2: Read all PackagingItem documents BEFORE any writes
            val loadedPackaging = linkedMapOf<String, PackagingItem>()
            for (pkgId in packagingQtyMap.keys) {
                val pkg = tx.get(packagingItemsCol(userId).document(pkgId))
                    .toObject(PackagingItem::class.java)
                    ?: throw IllegalStateException("Assigned packaging item $pkgId not found.")
                loadedPackaging[pkgId] = pkg
            }

            // Step 3: Validate stock sufficiency
            for ((pid, neededQty) in productQtyMap) {
                val prod = loadedProducts.getValue(pid)
                if (prod.availableStock < neededQty) {
                    throw IllegalStateException(
                        "Insufficient stock for ${prod.name} (Need $neededQty, Available ${prod.availableStock})."
                    )
                }
            }
            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                if (pkg.availableQty < neededQty) {
                    throw IllegalStateException(
                        "Insufficient packaging '${pkg.name}' (Need $neededQty, Available ${pkg.availableQty})."
                    )
                }
            }

            // Step 4: Compute costs and perform ALL writes
            val updatedItems = items.map { item ->
                val prod = loadedProducts.getValue(item.productId)
                item.copy(unitCostPrice = prod.purchasePrice)
            }
            val productCostTotal = updatedItems.sumOf { it.quantity * it.unitCostPrice }

            val breakdown = mutableListOf<PackagingBreakdownItem>()
            var packagingCostTotal = 0.0
            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                val cost = neededQty * pkg.purchasePrice
                packagingCostTotal += cost
                breakdown.add(
                    PackagingBreakdownItem(
                        packagingId = pkgId,
                        packagingName = pkg.name,
                        quantityUsed = neededQty,
                        unitCost = pkg.purchasePrice,
                        totalCost = cost
                    )
                )
            }

            for ((pid, neededQty) in productQtyMap) {
                val prod = loadedProducts.getValue(pid)
                val before = prod.availableStock
                val after = before - neededQty
                tx.update(
                    productsCol(userId).document(pid),
                    mapOf(
                        "availableStock" to after,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txId = UUID.randomUUID().toString()
                tx.set(
                    transactionsCol(userId).document(txId),
                    mapOf(
                        "userId" to userId,
                        "itemType" to "PRODUCT",
                        "itemId" to pid,
                        "itemName" to prod.name,
                        "txType" to "ORDER_DEDUCTION",
                        "quantityDelta" to -neededQty,
                        "stockBefore" to before,
                        "stockAfter" to after,
                        "unitCost" to prod.purchasePrice,
                        "totalValue" to (neededQty * prod.purchasePrice),
                        "referenceId" to orderId,
                        "referenceLabel" to "Order ${invoiceNumber.trim()}",
                        "txDate" to orderDate.trim(),
                        "txEpochMs" to epochMs,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                val before = pkg.availableQty
                val after = before - neededQty
                tx.update(
                    packagingItemsCol(userId).document(pkgId),
                    mapOf(
                        "availableQty" to after,
                        "usedQty" to (pkg.usedQty + neededQty),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txId = UUID.randomUUID().toString()
                tx.set(
                    transactionsCol(userId).document(txId),
                    mapOf(
                        "userId" to userId,
                        "itemType" to "PACKAGING",
                        "itemId" to pkgId,
                        "itemName" to pkg.name,
                        "txType" to "ORDER_DEDUCTION",
                        "quantityDelta" to -neededQty,
                        "stockBefore" to before,
                        "stockAfter" to after,
                        "unitCost" to pkg.purchasePrice,
                        "totalValue" to (neededQty * pkg.purchasePrice),
                        "referenceId" to orderId,
                        "referenceLabel" to "Order ${invoiceNumber.trim()}",
                        "txDate" to orderDate.trim(),
                        "txEpochMs" to epochMs,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            tx.set(
                orderRef,
                mapOf(
                    "userId" to userId,
                    "invoiceNumber" to invoiceNumber.trim(),
                    "customerName" to customerName.trim(),
                    "customerPhone" to customerPhone.trim(),
                    "customerAddress" to customerAddress.trim(),
                    "orderDate" to orderDate.trim(),
                    "orderEpochMs" to epochMs,
                    "items" to updatedItems.map { it.toMap() },
                    "subtotal" to subtotal,
                    "discountAmount" to discountAmount.coerceAtLeast(0.0),
                    "deliveryCharge" to deliveryCharge.coerceAtLeast(0.0),
                    "totalAmount" to totalAmount,
                    "status" to initialStatus.name,
                    "courierName" to courierName.trim(),
                    "trackingNumber" to trackingNumber.trim(),
                    "deliveryNote" to deliveryNote.trim(),
                    "stockDeducted" to true,
                    "productCostTotal" to productCostTotal,
                    "packagingCostTotal" to packagingCostTotal,
                    "packagingBreakdown" to breakdown.map { it.toMap() },
                    "returnedAmount" to 0.0,
                    "returnedQtyTotal" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (autoLogCourierExpense && deliveryCharge > 0.0 && expSnap != null && !expSnap.exists()) {
                tx.set(
                    courierExpRef,
                    mapOf(
                        "userId" to userId,
                        "category" to ExpenseCategory.COURIER.name,
                        "title" to "Order Courier: ${invoiceNumber.trim()}",
                        "amount" to deliveryCharge,
                        "expenseDate" to orderDate.trim(),
                        "expenseEpochMs" to epochMs,
                        "notes" to "Courier: ${courierName.trim()}",
                        "linkedReferenceId" to orderId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            orderId
        }.await()
    }

    suspend fun transitionOrderStatus(
        userId: String,
        orderId: String,
        targetStatus: OrderStatus,
        courierName: String? = null,
        trackingNumber: String? = null,
        autoLogCourierExpense: Boolean = true
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, ordersCol(userId).document(orderId).path) {
        val orderRef = ordersCol(userId).document(orderId)
        val courierExpRef = expensesCol(userId).document("exp_ord_$orderId")
        val todayStr = DhakaDateTime.todayString()
        val nowMs = System.currentTimeMillis()

        db.runTransaction { tx ->
            val order = tx.get(orderRef).toObject(Order::class.java)
                ?: throw IllegalStateException("Order not found.")

            if (order.statusEnum == OrderStatus.CANCELLED) {
                throw IllegalStateException("Cancelled orders cannot be updated.")
            }

            val finalCourier = courierName?.trim() ?: order.courierName
            val finalTracking = trackingNumber?.trim() ?: order.trackingNumber

            val requiresDeduction = (targetStatus == OrderStatus.PACKED ||
                targetStatus == OrderStatus.DISPATCHED ||
                targetStatus == OrderStatus.DELIVERED) && !order.stockDeducted

            if (!requiresDeduction) {
                // Status change without duplicate stock deduction!
                tx.update(
                    orderRef,
                    mapOf(
                        "status" to targetStatus.name,
                        "courierName" to finalCourier,
                        "trackingNumber" to finalTracking,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                return@runTransaction Unit
            }

            // Perform atomic stock + packaging deduction ONCE
            val expSnap = if (autoLogCourierExpense && order.deliveryCharge > 0.0) {
                tx.get(courierExpRef)
            } else null

            val productQtyMap = linkedMapOf<String, Long>()
            for (item in order.items) {
                productQtyMap[item.productId] = (productQtyMap[item.productId] ?: 0L) + item.quantity
            }

            val loadedProducts = linkedMapOf<String, Product>()
            for (pid in productQtyMap.keys) {
                val p = tx.get(productsCol(userId).document(pid)).toObject(Product::class.java)
                    ?: throw IllegalStateException("Product $pid not found.")
                loadedProducts[pid] = p
            }

            val packagingQtyMap = linkedMapOf<String, Long>()
            for ((pid, orderedQty) in productQtyMap) {
                val prod = loadedProducts.getValue(pid)
                for (assign in prod.assignedPackaging) {
                    if (assign.packagingId.isNotBlank() && assign.quantityPerUnit > 0L) {
                        packagingQtyMap[assign.packagingId] =
                            (packagingQtyMap[assign.packagingId] ?: 0L) + (orderedQty * assign.quantityPerUnit)
                    }
                }
            }

            val loadedPackaging = linkedMapOf<String, PackagingItem>()
            for (pkgId in packagingQtyMap.keys) {
                val pkg = tx.get(packagingItemsCol(userId).document(pkgId))
                    .toObject(PackagingItem::class.java)
                    ?: throw IllegalStateException("Packaging item $pkgId not found.")
                loadedPackaging[pkgId] = pkg
            }

            for ((pid, neededQty) in productQtyMap) {
                val prod = loadedProducts.getValue(pid)
                if (prod.availableStock < neededQty) {
                    throw IllegalStateException(
                        "Insufficient stock for ${prod.name} (Need $neededQty, Available ${prod.availableStock})."
                    )
                }
            }
            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                if (pkg.availableQty < neededQty) {
                    throw IllegalStateException(
                        "Insufficient packaging '${pkg.name}' (Need $neededQty, Available ${pkg.availableQty})."
                    )
                }
            }

            val updatedItems = order.items.map { item ->
                val prod = loadedProducts.getValue(item.productId)
                item.copy(unitCostPrice = prod.purchasePrice)
            }
            val productCostTotal = updatedItems.sumOf { it.quantity * it.unitCostPrice }

            val breakdown = mutableListOf<PackagingBreakdownItem>()
            var packagingCostTotal = 0.0
            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                val cost = neededQty * pkg.purchasePrice
                packagingCostTotal += cost
                breakdown.add(
                    PackagingBreakdownItem(
                        packagingId = pkgId,
                        packagingName = pkg.name,
                        quantityUsed = neededQty,
                        unitCost = pkg.purchasePrice,
                        totalCost = cost
                    )
                )
            }

            for ((pid, neededQty) in productQtyMap) {
                val prod = loadedProducts.getValue(pid)
                val before = prod.availableStock
                val after = before - neededQty
                tx.update(
                    productsCol(userId).document(pid),
                    mapOf("availableStock" to after, "updatedAt" to FieldValue.serverTimestamp())
                )
                val txId = UUID.randomUUID().toString()
                tx.set(
                    transactionsCol(userId).document(txId),
                    mapOf(
                        "userId" to userId,
                        "itemType" to "PRODUCT",
                        "itemId" to pid,
                        "itemName" to prod.name,
                        "txType" to "ORDER_DEDUCTION",
                        "quantityDelta" to -neededQty,
                        "stockBefore" to before,
                        "stockAfter" to after,
                        "unitCost" to prod.purchasePrice,
                        "totalValue" to (neededQty * prod.purchasePrice),
                        "referenceId" to orderId,
                        "referenceLabel" to "Order ${order.invoiceNumber}",
                        "txDate" to todayStr,
                        "txEpochMs" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            for ((pkgId, neededQty) in packagingQtyMap) {
                val pkg = loadedPackaging.getValue(pkgId)
                val before = pkg.availableQty
                val after = before - neededQty
                tx.update(
                    packagingItemsCol(userId).document(pkgId),
                    mapOf(
                        "availableQty" to after,
                        "usedQty" to (pkg.usedQty + neededQty),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txId = UUID.randomUUID().toString()
                tx.set(
                    transactionsCol(userId).document(txId),
                    mapOf(
                        "userId" to userId,
                        "itemType" to "PACKAGING",
                        "itemId" to pkgId,
                        "itemName" to pkg.name,
                        "txType" to "ORDER_DEDUCTION",
                        "quantityDelta" to -neededQty,
                        "stockBefore" to before,
                        "stockAfter" to after,
                        "unitCost" to pkg.purchasePrice,
                        "totalValue" to (neededQty * pkg.purchasePrice),
                        "referenceId" to orderId,
                        "referenceLabel" to "Order ${order.invoiceNumber}",
                        "txDate" to todayStr,
                        "txEpochMs" to nowMs,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            tx.update(
                orderRef,
                mapOf(
                    "status" to targetStatus.name,
                    "courierName" to finalCourier,
                    "trackingNumber" to finalTracking,
                    "stockDeducted" to true,
                    "items" to updatedItems.map { it.toMap() },
                    "productCostTotal" to productCostTotal,
                    "packagingCostTotal" to packagingCostTotal,
                    "packagingBreakdown" to breakdown.map { it.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (autoLogCourierExpense && order.deliveryCharge > 0.0 && expSnap != null && !expSnap.exists()) {
                tx.set(
                    courierExpRef,
                    mapOf(
                        "userId" to userId,
                        "category" to ExpenseCategory.COURIER.name,
                        "title" to "Order Courier: ${order.invoiceNumber}",
                        "amount" to order.deliveryCharge,
                        "expenseDate" to order.orderDate,
                        "expenseEpochMs" to order.orderEpochMs,
                        "notes" to "Courier: $finalCourier",
                        "linkedReferenceId" to orderId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            Unit
        }.await()
    }

    suspend fun cancelOrder(
        userId: String,
        orderId: String,
        restoreProductStock: Boolean
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, ordersCol(userId).document(orderId).path) {
        val orderRef = ordersCol(userId).document(orderId)
        val todayStr = DhakaDateTime.todayString()
        val nowMs = System.currentTimeMillis()

        db.runTransaction { tx ->
            val order = tx.get(orderRef).toObject(Order::class.java)
                ?: throw IllegalStateException("Order not found.")
            if (order.statusEnum == OrderStatus.CANCELLED) {
                throw IllegalStateException("Order is already cancelled.")
            }
            if (order.returnedQtyTotal > 0L) {
                throw IllegalStateException("Orders with processed returns cannot be cancelled.")
            }

            if (order.stockDeducted && restoreProductStock) {
                val productQtyMap = linkedMapOf<String, Long>()
                for (item in order.items) {
                    productQtyMap[item.productId] = (productQtyMap[item.productId] ?: 0L) + item.quantity
                }
                val loadedProducts = linkedMapOf<String, Product>()
                for (pid in productQtyMap.keys) {
                    val prod = tx.get(productsCol(userId).document(pid)).toObject(Product::class.java)
                    if (prod != null) loadedProducts[pid] = prod
                }
                for ((pid, qty) in productQtyMap) {
                    val prod = loadedProducts[pid] ?: continue
                    val before = prod.availableStock
                    val after = before + qty
                    tx.update(
                        productsCol(userId).document(pid),
                        mapOf("availableStock" to after, "updatedAt" to FieldValue.serverTimestamp())
                    )
                    val txId = UUID.randomUUID().toString()
                    tx.set(
                        transactionsCol(userId).document(txId),
                        mapOf(
                            "userId" to userId,
                            "itemType" to "PRODUCT",
                            "itemId" to pid,
                            "itemName" to prod.name,
                            "txType" to "ORDER_CANCEL_RESTORE",
                            "quantityDelta" to qty,
                            "stockBefore" to before,
                            "stockAfter" to after,
                            "unitCost" to prod.purchasePrice,
                            "totalValue" to (qty * prod.purchasePrice),
                            "referenceId" to orderId,
                            "referenceLabel" to "Cancelled ${order.invoiceNumber}",
                            "txDate" to todayStr,
                            "txEpochMs" to nowMs,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            // Note: Packaging materials are NEVER restored automatically.
            tx.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            Unit
        }.await()
    }

    // --- PRODUCT RETURN PROCESSING (ATOMIC, NO PACKAGING RESTORE, NO DUPLICATE PROCESSING) ---

    suspend fun processProductReturn(
        userId: String,
        orderId: String,
        productId: String,
        returnedQty: Long,
        reason: String,
        returnCourierCharge: Double,
        isReceived: Boolean,
        isResellable: Boolean,
        returnDate: String
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, returnsCol(userId).path) {
        require(returnedQty > 0L) { "Returned quantity must be greater than zero." }
        require(reason.isNotBlank()) { "Return reason is required." }
        require(returnCourierCharge >= 0.0) { "Return courier charge cannot be negative." }
        require(DhakaDateTime.isValidDateFormat(returnDate)) { "Return date must be DD-MM-YYYY." }

        val returnId = UUID.randomUUID().toString()
        val orderRef = ordersCol(userId).document(orderId)
        val prodRef = productsCol(userId).document(productId)
        val returnRef = returnsCol(userId).document(returnId)
        val returnExpRef = expensesCol(userId).document("exp_ret_$returnId")
        val epochMs = DhakaDateTime.parseDateOrNow(returnDate)

        db.runTransaction { tx ->
            val order = tx.get(orderRef).toObject(Order::class.java)
                ?: throw IllegalStateException("Order not found.")
            if (order.statusEnum == OrderStatus.CANCELLED) {
                throw IllegalStateException("Cannot process return on a cancelled order.")
            }
            if (!order.stockDeducted) {
                throw IllegalStateException("Cannot return an order before it is packed or dispatched.")
            }

            val prodSnap = tx.get(prodRef)
            val product = prodSnap.toObject(Product::class.java)
                ?: throw IllegalStateException("Product not found.")
            val expSnap = if (returnCourierCharge > 0.0) tx.get(returnExpRef) else null

            val targetIndex = order.items.indexOfFirst { it.productId == productId }
            if (targetIndex < 0) {
                throw IllegalStateException("Product not found in this order.")
            }
            val item = order.items[targetIndex]
            val remainingReturnable = item.quantity - item.returnedQty
            if (returnedQty > remainingReturnable) {
                throw IllegalStateException(
                    "Duplicate or excess return blocked: only $remainingReturnable unit(s) remain returnable for ${item.productName}."
                )
            }

            val discountRatio = if (order.subtotal > 0.0) {
                ((order.subtotal - order.discountAmount).coerceAtLeast(0.0)) / order.subtotal
            } else 1.0
            val refundAmount = returnedQty * item.unitSellingPrice * discountRatio
            val restoredCostAmount = if (isReceived && isResellable) {
                returnedQty * item.unitCostPrice
            } else 0.0

            val updatedItems = order.items.mapIndexed { index, orderItem ->
                if (index == targetIndex) {
                    orderItem.copy(returnedQty = orderItem.returnedQty + returnedQty)
                } else orderItem
            }
            val newReturnedQtyTotal = order.returnedQtyTotal + returnedQty
            val totalOrderedUnits = updatedItems.sumOf { it.quantity }
            val newStatus = if (newReturnedQtyTotal >= totalOrderedUnits) {
                OrderStatus.RETURNED
            } else {
                OrderStatus.PARTIAL_RETURN
            }

            // Update product stock ONLY if received. NEVER restore packaging materials!
            if (isReceived) {
                if (isResellable) {
                    val before = product.availableStock
                    val after = before + returnedQty
                    tx.update(
                        prodRef,
                        mapOf(
                            "availableStock" to after,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val txId = UUID.randomUUID().toString()
                    tx.set(
                        transactionsCol(userId).document(txId),
                        mapOf(
                            "userId" to userId,
                            "itemType" to "PRODUCT",
                            "itemId" to productId,
                            "itemName" to product.name,
                            "txType" to "RETURN_RESELLABLE",
                            "quantityDelta" to returnedQty,
                            "stockBefore" to before,
                            "stockAfter" to after,
                            "unitCost" to item.unitCostPrice,
                            "totalValue" to (returnedQty * item.unitCostPrice),
                            "referenceId" to returnId,
                            "referenceLabel" to "Return ${order.invoiceNumber} (Resellable)",
                            "txDate" to returnDate.trim(),
                            "txEpochMs" to epochMs,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val beforeDamaged = product.damagedStock
                    val afterDamaged = beforeDamaged + returnedQty
                    tx.update(
                        prodRef,
                        mapOf(
                            "damagedStock" to afterDamaged,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val txId = UUID.randomUUID().toString()
                    tx.set(
                        transactionsCol(userId).document(txId),
                        mapOf(
                            "userId" to userId,
                            "itemType" to "PRODUCT",
                            "itemId" to productId,
                            "itemName" to product.name,
                            "txType" to "RETURN_DAMAGED",
                            "quantityDelta" to returnedQty,
                            "stockBefore" to beforeDamaged,
                            "stockAfter" to afterDamaged,
                            "unitCost" to item.unitCostPrice,
                            "totalValue" to (returnedQty * item.unitCostPrice),
                            "referenceId" to returnId,
                            "referenceLabel" to "Return ${order.invoiceNumber} (Damaged)",
                            "txDate" to returnDate.trim(),
                            "txEpochMs" to epochMs,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            tx.update(
                orderRef,
                mapOf(
                    "items" to updatedItems.map { it.toMap() },
                    "returnedAmount" to (order.returnedAmount + refundAmount),
                    "returnedQtyTotal" to newReturnedQtyTotal,
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            tx.set(
                returnRef,
                mapOf(
                    "userId" to userId,
                    "orderId" to orderId,
                    "invoiceNumber" to order.invoiceNumber,
                    "productId" to productId,
                    "productName" to item.productName,
                    "returnedQty" to returnedQty,
                    "refundAmount" to refundAmount,
                    "restoredCostAmount" to restoredCostAmount,
                    "reason" to reason.trim(),
                    "returnCourierCharge" to returnCourierCharge,
                    "isReceived" to isReceived,
                    "isResellable" to isResellable,
                    "returnDate" to returnDate.trim(),
                    "returnEpochMs" to epochMs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            if (returnCourierCharge > 0.0 && expSnap != null && !expSnap.exists()) {
                tx.set(
                    returnExpRef,
                    mapOf(
                        "userId" to userId,
                        "category" to ExpenseCategory.RETURN_COURIER.name,
                        "title" to "Return Courier: ${order.invoiceNumber} (${item.productName})",
                        "amount" to returnCourierCharge,
                        "expenseDate" to returnDate.trim(),
                        "expenseEpochMs" to epochMs,
                        "notes" to "Reason: ${reason.trim()}",
                        "linkedReferenceId" to returnId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            returnId
        }.await()
    }

    // --- EXPENSE MANAGEMENT ---

    suspend fun saveExpense(
        userId: String,
        expense: Expense
    ): Result<String> = runCatchingFirestore(OperationType.WRITE, expensesCol(userId).path) {
        require(DhakaDateTime.isValidDateFormat(expense.expenseDate)) { "Expense date must be DD-MM-YYYY." }
        val epochMs = DhakaDateTime.parseDateOrNow(expense.expenseDate)
        val docRef = if (expense.id.isBlank()) {
            expensesCol(userId).document(UUID.randomUUID().toString())
        } else {
            expensesCol(userId).document(expense.id)
        }
        if (expense.id.isBlank()) {
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "category" to expense.category,
                    "title" to expense.title.trim(),
                    "amount" to expense.amount.coerceAtLeast(0.0),
                    "expenseDate" to expense.expenseDate.trim(),
                    "expenseEpochMs" to epochMs,
                    "notes" to expense.notes.trim(),
                    "linkedReferenceId" to expense.linkedReferenceId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "category" to expense.category,
                    "title" to expense.title.trim(),
                    "amount" to expense.amount.coerceAtLeast(0.0),
                    "expenseDate" to expense.expenseDate.trim(),
                    "expenseEpochMs" to epochMs,
                    "notes" to expense.notes.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        docRef.id
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, expensesCol(userId).document(expenseId).path) {
            expensesCol(userId).document(expenseId).delete().await()
            Unit
        }

    // --- BACKUP & RESTORE ---

    suspend fun exportBackupJson(userId: String): Result<String> =
        runCatchingFirestore(OperationType.LIST, userDoc(userId).path) {
            val root = JSONObject().apply {
                put("app", "Autonstyle Business Manager")
                put("schemaVersion", 1)
                put("exportedAtDhaka", DhakaDateTime.formatDateTime(System.currentTimeMillis()))
                put("userId", userId)
            }

            val collections = listOf(
                "products",
                "productPurchases",
                "packagingItems",
                "packagingPurchases",
                "orders",
                "returns",
                "expenses",
                "stockTransactions"
            )
            val dataObj = JSONObject()
            for (colName in collections) {
                val snap = userDoc(userId).collection(colName).get().await()
                val arr = JSONArray()
                for (doc in snap.documents) {
                    val docJson = JSONObject()
                    docJson.put("_docId", doc.id)
                    doc.data?.forEach { (k, v) ->
                        if (k != "createdAt" && k != "updatedAt") {
                            docJson.put(k, toJsonValue(v))
                        }
                    }
                    arr.put(docJson)
                }
                dataObj.put(colName, arr)
            }
            root.put("collections", dataObj)
            root.toString(2)
        }

    suspend fun restoreBackupJson(userId: String, jsonText: String): Result<Int> =
        runCatchingFirestore(OperationType.WRITE, userDoc(userId).path) {
            val root = JSONObject(jsonText)
            val collectionsObj = root.getJSONObject("collections")
            var restoredCount = 0

            val collectionOrder = listOf(
                "packagingItems",
                "products",
                "productPurchases",
                "packagingPurchases",
                "orders",
                "returns",
                "expenses",
                "stockTransactions"
            )

            val immutableCollections = setOf(
                "productPurchases",
                "packagingPurchases",
                "returns",
                "stockTransactions"
            )

            val mutableWithUpdatedAt = setOf(
                "packagingItems",
                "products",
                "orders",
                "expenses"
            )

            for (colName in collectionOrder) {
                if (!collectionsObj.has(colName)) continue
                val arr = collectionsObj.getJSONArray(colName)
                val colRef = userDoc(userId).collection(colName)
                val existingIds = colRef.get().await().documents.map { it.id }.toSet()

                var batch = db.batch()
                var batchOps = 0
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val docId = item.optString("_docId").takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString()

                    // Never overwrite existing immutable ledger entries
                    if (docId in existingIds && colName in immutableCollections) continue
                    // Skip existing mutable records to avoid duplicating/overwriting live updates unless absent
                    if (docId in existingIds) continue

                    val payload = mutableMapOf<String, Any>()
                    val keys = item.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key == "_docId" || key == "createdAt" || key == "updatedAt") continue
                        payload[key] = fromJsonValue(item.get(key))
                    }
                    payload["userId"] = userId
                    payload["createdAt"] = FieldValue.serverTimestamp()
                    if (colName in mutableWithUpdatedAt) {
                        payload["updatedAt"] = FieldValue.serverTimestamp()
                    }

                    batch.set(colRef.document(docId), payload)
                    batchOps++
                    restoredCount++
                    if (batchOps >= 400) {
                        batch.commit().await()
                        batch = db.batch()
                        batchOps = 0
                    }
                }
                if (batchOps > 0) {
                    batch.commit().await()
                }
            }
            restoredCount
        }

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (k, v) -> put(k.toString(), toJsonValue(v)) }
            }
            is List<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }
            else -> value
        }
    }

    private fun fromJsonValue(value: Any): Any {
        return when (value) {
            is JSONObject -> {
                val map = mutableMapOf<String, Any>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = fromJsonValue(value.get(k))
                }
                map
            }
            is JSONArray -> {
                val list = mutableListOf<Any>()
                for (i in 0 until value.length()) {
                    list.add(fromJsonValue(value.get(i)))
                }
                list
            }
            is Int -> value.toLong()
            else -> value
        }
    }

    private suspend inline fun <T> runCatchingFirestore(
        operationType: OperationType,
        path: String,
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, operationType, path)
            Result.failure(e)
        }
    }
}
