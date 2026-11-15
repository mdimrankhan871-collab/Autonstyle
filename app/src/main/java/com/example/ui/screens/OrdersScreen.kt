package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.Product
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonLimeSoft
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonNavySoft
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonSuccess
import com.example.ui.theme.AutonSuccessSoft
import com.example.ui.theme.AutonTextPrimary
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWarning
import com.example.ui.theme.AutonWarningSoft
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter
import com.example.util.DhakaDateTime

@Composable
fun OrdersScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel,
    openCreateOnLaunch: Boolean = false,
    onCreateHandled: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(openCreateOnLaunch) }
    var returnTargetOrder by remember { mutableStateOf<Order?>(null) }
    var cancelTargetOrder by remember { mutableStateOf<Order?>(null) }

    if (openCreateOnLaunch && !showCreateDialog) {
        showCreateDialog = true
        onCreateHandled()
    }

    val filteredOrders = remember(snapshot.orders, searchQuery, statusFilter) {
        val q = searchQuery.trim().lowercase()
        snapshot.orders.filter { order ->
            val matchesStatus = statusFilter == null || order.statusEnum == statusFilter
            val matchesQuery = q.isEmpty() ||
                order.invoiceNumber.lowercase().contains(q) ||
                order.customerName.lowercase().contains(q) ||
                order.customerPhone.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Order Management",
            subtitle = "${snapshot.orders.size} Order(s) • Auto product & packaging deduction",
            actionLabel = "+ New Order",
            onActionClick = { showCreateDialog = true }
        )
        Spacer(modifier = Modifier.height(10.dp))

        SearchInputBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search invoice #, customer name, or phone..."
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { statusFilter = null },
                label = { Text("All (${snapshot.orders.size})") }
            )
            OrderStatus.entries.forEach { st ->
                val count = snapshot.orders.count { it.statusEnum == st }
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { statusFilter = st },
                    label = { Text("${st.label} ($count)") }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (filteredOrders.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.ShoppingBag,
                title = "No orders match your filter",
                description = "Create a customer order with invoice number, items, discount, and courier details.",
                actionLabel = "+ New Order",
                onAction = { showCreateDialog = true }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredOrders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onPack = {
                            viewModel.transitionOrderStatus(order.id, OrderStatus.PACKED)
                        },
                        onDispatch = {
                            viewModel.transitionOrderStatus(order.id, OrderStatus.DISPATCHED)
                        },
                        onDeliver = {
                            viewModel.transitionOrderStatus(order.id, OrderStatus.DELIVERED)
                        },
                        onReturn = { returnTargetOrder = order },
                        onCancel = { cancelTargetOrder = order }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrderDialog(
            products = snapshot.products,
            packagingItems = snapshot.packagingItems,
            onDismiss = {
                showCreateDialog = false
                onCreateHandled()
            },
            onCreate = { inv, custName, custPhone, custAddr, date, items, discount, delivery, status, courier, tracking, note, autoExp ->
                viewModel.createOrder(
                    inv, custName, custPhone, custAddr, date, items, discount, delivery,
                    status, courier, tracking, note, autoExp
                ) {
                    showCreateDialog = false
                    onCreateHandled()
                }
            }
        )
    }

    returnTargetOrder?.let { order ->
        ProcessReturnDialog(
            order = order,
            onDismiss = { returnTargetOrder = null },
            onSubmit = { productId, qty, reason, courierFee, isReceived, isResellable, retDate ->
                viewModel.processProductReturn(
                    orderId = order.id,
                    productId = productId,
                    returnedQty = qty,
                    reason = reason,
                    returnCourierCharge = courierFee,
                    isReceived = isReceived,
                    isResellable = isResellable,
                    returnDate = retDate
                ) {
                    returnTargetOrder = null
                }
            }
        )
    }

    cancelTargetOrder?.let { order ->
        CancelOrderDialog(
            order = order,
            onDismiss = { cancelTargetOrder = null },
            onConfirm = { restoreStock ->
                viewModel.cancelOrder(order.id, restoreStock) {
                    cancelTargetOrder = null
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(
    order: Order,
    onPack: () -> Unit,
    onDispatch: () -> Unit,
    onDeliver: () -> Unit,
    onReturn: () -> Unit,
    onCancel: () -> Unit
) {
    val statusBg = when (order.statusEnum) {
        OrderStatus.DELIVERED -> AutonSuccessSoft
        OrderStatus.CANCELLED, OrderStatus.RETURNED -> AutonRedSoft
        OrderStatus.PARTIAL_RETURN -> AutonWarningSoft
        else -> AutonNavySoft
    }
    val statusFg = when (order.statusEnum) {
        OrderStatus.DELIVERED -> AutonSuccess
        OrderStatus.CANCELLED, OrderStatus.RETURNED -> AutonRed
        OrderStatus.PARTIAL_RETURN -> AutonWarning
        else -> AutonNavy
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.invoiceNumber, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                    Text(
                        "${order.customerName}  •  ${order.customerPhone.ifBlank { "No phone" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AutonTextPrimary
                    )
                    if (order.customerAddress.isNotBlank()) {
                        Text(order.customerAddress, style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                    }
                    Text(
                        "Date: ${order.orderDate}" + if (order.courierName.isNotBlank()) " • ${order.courierName} ${order.trackingNumber}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(order.statusEnum.label, statusBg, statusFg)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        BdtFormatter.format(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        color = AutonNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AutonCanvas, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName}" +
                                if (item.returnedQty > 0L) " (Ret: ${item.returnedQty})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AutonTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = BdtFormatter.format(item.lineTotal),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                            color = AutonNavy
                        )
                    }
                }
                if (order.discountAmount > 0.0 || order.deliveryCharge > 0.0) {
                    HorizontalDivider(color = AutonBorder, modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "Subtotal: ${BdtFormatter.format(order.subtotal)}  •  Discount: -${BdtFormatter.format(order.discountAmount)}  •  Delivery: +${BdtFormatter.format(order.deliveryCharge)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                }
            }

            if (order.stockDeducted) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AutonLimeSoft,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = "Stock Deducted", tint = AutonNavy, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Stock & Packaging Deducted  •  Prod Cost: ${BdtFormatter.format(order.productCostTotal)}  •  Pkg Cost: ${BdtFormatter.format(order.packagingCostTotal, true)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AutonNavy
                        )
                    }
                }
            }

            if (order.packagingBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    order.packagingBreakdown.forEach { b ->
                        StatusPill(
                            label = "-${b.quantityUsed} ${b.packagingName} (${BdtFormatter.format(b.totalCost, true)})",
                            backgroundColor = AutonCanvas,
                            textColor = AutonTextSecondary
                        )
                    }
                }
            }

            if (order.statusEnum != OrderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.statusEnum == OrderStatus.PENDING) {
                        Button(
                            onClick = onPack,
                            colors = ButtonDefaults.buttonColors(containerColor = AutonNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pack & Deduct", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onDispatch,
                            colors = ButtonDefaults.buttonColors(containerColor = AutonLime, contentColor = AutonNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dispatch & Deduct", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (order.statusEnum == OrderStatus.PACKED) {
                        Button(
                            onClick = onDispatch,
                            colors = ButtonDefaults.buttonColors(containerColor = AutonNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Dispatched", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (order.statusEnum == OrderStatus.DISPATCHED) {
                        Button(
                            onClick = onDeliver,
                            colors = ButtonDefaults.buttonColors(containerColor = AutonSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Delivered", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    val hasReturnable = order.stockDeducted && order.items.any { it.quantity > it.returnedQty }
                    if (hasReturnable) {
                        OutlinedButton(onClick = onReturn, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Rounded.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Return", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (order.returnedQtyTotal == 0L && order.statusEnum != OrderStatus.DELIVERED) {
                        OutlinedButton(
                            onClick = onCancel,
                            border = BorderStroke(1.dp, AutonRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AutonRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cancel", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderDialog(
    products: List<Product>,
    packagingItems: List<PackagingItem>,
    onDismiss: () -> Unit,
    onCreate: (
        invoiceNumber: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        orderDate: String,
        items: List<OrderItem>,
        discount: Double,
        deliveryCharge: Double,
        status: OrderStatus,
        courier: String,
        tracking: String,
        note: String,
        autoLogCourierExpense: Boolean
    ) -> Unit
) {
    var invoiceNumber by remember { mutableStateOf(DhakaDateTime.generateInvoiceNumber()) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf(DhakaDateTime.todayString()) }
    var discountText by remember { mutableStateOf("0") }
    var deliveryText by remember { mutableStateOf("80") }
    var courierName by remember { mutableStateOf("Steadfast") }
    var trackingNumber by remember { mutableStateOf("") }
    var deliveryNote by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(OrderStatus.PACKED) }
    var autoLogCourier by remember { mutableStateOf(true) }

    val lineItems = remember { mutableStateListOf<OrderItem>() }
    var pickerProduct by remember { mutableStateOf(products.firstOrNull()) }
    var pickerExpanded by remember { mutableStateOf(false) }
    var pickerQty by remember { mutableStateOf("1") }
    var pickerPrice by remember { mutableStateOf((products.firstOrNull()?.sellingPrice ?: 0.0).toString()) }

    val pkgMap = remember(packagingItems) { packagingItems.associateBy { it.id } }
    val prodMap = remember(products) { products.associateBy { it.id } }

    // Live atomic deduction preview
    val neededPackaging = remember(lineItems.toList(), products, packagingItems) {
        val map = linkedMapOf<String, Long>()
        for (item in lineItems) {
            val prod = prodMap[item.productId] ?: continue
            for (assign in prod.assignedPackaging) {
                map[assign.packagingId] = (map[assign.packagingId] ?: 0L) + (item.quantity * assign.quantityPerUnit)
            }
        }
        map
    }
    val estProdCost = lineItems.sumOf {
        val p = prodMap[it.productId]
        it.quantity * (p?.purchasePrice ?: it.unitCostPrice)
    }
    val estPkgCost = neededPackaging.entries.sumOf { (pkgId, qty) ->
        qty * (pkgMap[pkgId]?.purchasePrice ?: 0.0)
    }

    val subtotal = lineItems.sumOf { it.lineTotal }
    val discount = discountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val delivery = deliveryText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val grandTotal = (subtotal - discount + delivery).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Customer Order") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (products.isEmpty()) {
                    Text("Add at least one product in Products tab before creating orders.", color = AutonRed)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("Invoice #*") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = orderDate,
                            onValueChange = { orderDate = it },
                            label = { Text("Date (DD-MM-YYYY)*") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name*") },
                        modifier = Modifier.fillMaxWidth().testTag("order_customer_input"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = courierName,
                            onValueChange = { courierName = it },
                            label = { Text("Courier") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Add Order Items", style = MaterialTheme.typography.titleSmall, color = AutonNavy)

                    ExposedDropdownMenuBox(
                        expanded = pickerExpanded,
                        onExpandedChange = { pickerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = pickerProduct?.let { "${it.name} (Avail: ${it.availableStock})" } ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pickerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = pickerExpanded,
                            onDismissRequest = { pickerExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} (Stock: ${prod.availableStock} • ${BdtFormatter.format(prod.sellingPrice)})") },
                                    onClick = {
                                        pickerProduct = prod
                                        pickerPrice = prod.sellingPrice.toString()
                                        pickerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pickerQty,
                            onValueChange = { pickerQty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pickerPrice,
                            onValueChange = { pickerPrice = it },
                            label = { Text("Price (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val p = pickerProduct ?: return@Button
                                val q = pickerQty.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                                val price = pickerPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: p.sellingPrice
                                val existingIdx = lineItems.indexOfFirst { it.productId == p.id }
                                if (existingIdx >= 0) {
                                    val prev = lineItems[existingIdx]
                                    val newQ = prev.quantity + q
                                    lineItems[existingIdx] = prev.copy(
                                        quantity = newQ,
                                        unitSellingPrice = price,
                                        lineTotal = newQ * price
                                    )
                                } else {
                                    lineItems.add(
                                        OrderItem(
                                            productId = p.id,
                                            productName = p.name,
                                            sku = p.sku,
                                            quantity = q,
                                            unitSellingPrice = price,
                                            unitCostPrice = p.purchasePrice,
                                            lineTotal = q * price
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AutonNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Item")
                        }
                    }

                    lineItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AutonCanvas, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${item.quantity}x ${item.productName} = ${BdtFormatter.format(item.lineTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { lineItems.removeAt(idx) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = AutonRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = deliveryText,
                            onValueChange = { deliveryText = it },
                            label = { Text("Delivery Charge (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = trackingNumber,
                        onValueChange = { trackingNumber = it },
                        label = { Text("Tracking # / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Order Status on Save:", style = MaterialTheme.typography.labelLarge, color = AutonNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(OrderStatus.PENDING, OrderStatus.PACKED, OrderStatus.DISPATCHED).forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { autoLogCourier = !autoLogCourier }
                    ) {
                        Checkbox(checked = autoLogCourier, onCheckedChange = { autoLogCourier = it })
                        Text("Auto-log delivery charge as Courier Expense", style = MaterialTheme.typography.bodySmall)
                    }

                    if (lineItems.isNotEmpty()) {
                        Surface(
                            color = AutonNavySoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Total: ${BdtFormatter.format(grandTotal)}  •  Prod Cost: ${BdtFormatter.format(estProdCost)}  •  Pkg Cost: ${BdtFormatter.format(estPkgCost, true)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AutonNavy
                                )
                                if (neededPackaging.isNotEmpty()) {
                                    val pkgText = neededPackaging.entries.joinToString(", ") { (id, qty) ->
                                        "${qty}x ${pkgMap[id]?.name ?: id}"
                                    }
                                    Text(
                                        "Auto Packaging Deduction: $pkgText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AutonTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        invoiceNumber,
                        customerName,
                        customerPhone,
                        customerAddress,
                        orderDate,
                        lineItems.toList(),
                        discount,
                        delivery,
                        selectedStatus,
                        courierName,
                        trackingNumber,
                        deliveryNote,
                        autoLogCourier
                    )
                },
                enabled = lineItems.isNotEmpty() &&
                    invoiceNumber.isNotBlank() &&
                    customerName.isNotBlank() &&
                    DhakaDateTime.isValidDateFormat(orderDate),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Save Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessReturnDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmit: (
        productId: String,
        qty: Long,
        reason: String,
        courierCharge: Double,
        isReceived: Boolean,
        isResellable: Boolean,
        returnDate: String
    ) -> Unit
) {
    val returnableItems = remember(order) { order.items.filter { it.quantity > it.returnedQty } }
    var selectedItem by remember { mutableStateOf(returnableItems.firstOrNull()) }
    var itemExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var courierFeeText by remember { mutableStateOf("0") }
    var isReceived by remember { mutableStateOf(true) }
    var isResellable by remember { mutableStateOf(true) }
    var returnDate by remember { mutableStateOf(DhakaDateTime.todayString()) }

    val maxQty = selectedItem?.let { it.quantity - it.returnedQty } ?: 0L
    val qty = qtyText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Process Return • ${order.invoiceNumber}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = itemExpanded,
                    onExpandedChange = { itemExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.let { "${it.productName} (Max returnable: ${it.quantity - it.returnedQty})" } ?: "No returnable items",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Product*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                        returnableItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.productName} (Remaining: ${item.quantity - item.returnedQty})") },
                                onClick = {
                                    selectedItem = item
                                    itemExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Return Qty (Max $maxQty)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = courierFeeText,
                        onValueChange = { courierFeeText = it },
                        label = { Text("Return Courier (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = returnDate,
                    onValueChange = { returnDate = it },
                    label = { Text("Return Date (DD-MM-YYYY)*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Return Reason*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Returned product received?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isReceived, onCheckedChange = { isReceived = it })
                }

                if (isReceived) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Condition: Resellable?", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (isResellable) "Adds back to Available Product Stock"
                                else "Logs into Damaged Inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isResellable) AutonSuccess else AutonRed
                            )
                        }
                        Switch(checked = isResellable, onCheckedChange = { isResellable = it })
                    }
                }

                Surface(color = AutonWarningSoft, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Note: Packaging materials are NEVER restored automatically on returns. Return courier charge is auto-added to Expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            val item = selectedItem
            Button(
                onClick = {
                    if (item != null) {
                        onSubmit(
                            item.productId,
                            qty,
                            reason,
                            courierFeeText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                            isReceived,
                            isResellable,
                            returnDate
                        )
                    }
                },
                enabled = item != null && qty in 1L..maxQty && reason.isNotBlank() && DhakaDateTime.isValidDateFormat(returnDate),
                colors = ButtonDefaults.buttonColors(containerColor = AutonRed)
            ) {
                Text("Process Return")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CancelOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (restoreProductStock: Boolean) -> Unit
) {
    var restoreStock by remember { mutableStateOf(order.stockDeducted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel ${order.invoiceNumber}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cancelling adjusts sales revenue to ৳0 in Profit & Loss.")
                if (order.stockDeducted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { restoreStock = !restoreStock }
                    ) {
                        Checkbox(checked = restoreStock, onCheckedChange = { restoreStock = it })
                        Text("Restore deducted product units back to available stock", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Packaging materials already used will remain deducted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(restoreStock) },
                colors = ButtonDefaults.buttonColors(containerColor = AutonRed)
            ) {
                Text("Confirm Cancel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Order") }
        }
    )
}
