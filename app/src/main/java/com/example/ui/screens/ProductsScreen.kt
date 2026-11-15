package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AssignedPackaging
import com.example.data.model.PackagingItem
import com.example.data.model.Product
import com.example.data.model.ProductPurchase
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductsScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel,
    openPurchaseOnLaunch: Boolean = false,
    onPurchaseHandled: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var purchaseProductTarget by remember { mutableStateOf<Product?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(openPurchaseOnLaunch) }

    if (openPurchaseOnLaunch && !showPurchaseDialog) {
        showPurchaseDialog = true
        onPurchaseHandled()
    }

    val filteredProducts = remember(snapshot.products, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) snapshot.products
        else snapshot.products.filter {
            it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Products & Purchases",
            subtitle = "${snapshot.products.size} SKU(s) • Total Stock: ${snapshot.dashboard.productStockUnits} units",
            actionLabel = if (selectedTab == 0) "+ Add Product" else "+ Record Purchase",
            onActionClick = {
                if (selectedTab == 0) {
                    editingProduct = null
                    showProductForm = true
                } else {
                    purchaseProductTarget = snapshot.products.firstOrNull()
                    showPurchaseDialog = true
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = AutonWhite, contentColor = AutonNavy) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Products (${snapshot.products.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Purchase Log (${snapshot.productPurchases.size})") })
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search product name, SKU, or category..."
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredProducts.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = "No products found",
                    description = "Add your Autonstyle products with SKU, pricing, stock thresholds, and assigned packaging.",
                    actionLabel = "+ Add Product",
                    onAction = {
                        editingProduct = null
                        showProductForm = true
                    }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showProductForm = true
                            },
                            onPurchase = {
                                purchaseProductTarget = product
                                showPurchaseDialog = true
                            },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        } else {
            if (snapshot.productPurchases.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.AddShoppingCart,
                    title = "No product purchases logged",
                    description = "Record supplier purchases to automatically increase product stock and track purchase costs.",
                    actionLabel = if (snapshot.products.isNotEmpty()) "+ Record Purchase" else null,
                    onAction = if (snapshot.products.isNotEmpty()) {
                        {
                            purchaseProductTarget = snapshot.products.firstOrNull()
                            showPurchaseDialog = true
                        }
                    } else null
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(snapshot.productPurchases, key = { it.id }) { purchase ->
                        ProductPurchaseCard(purchase)
                    }
                }
            }
        }
    }

    if (showProductForm) {
        ProductFormDialog(
            initial = editingProduct,
            packagingItems = snapshot.packagingItems,
            onDismiss = { showProductForm = false },
            onSave = { product ->
                viewModel.saveProduct(product) { showProductForm = false }
            }
        )
    }

    if (showPurchaseDialog) {
        ProductPurchaseDialog(
            products = snapshot.products,
            initialProduct = purchaseProductTarget,
            onDismiss = {
                showPurchaseDialog = false
                onPurchaseHandled()
            },
            onConfirm = { productId, supplier, date, qty, unitPrice, totalCost, notes, updatePrice ->
                viewModel.recordProductPurchase(
                    productId = productId,
                    supplier = supplier,
                    purchaseDate = date,
                    quantity = qty,
                    unitPrice = unitPrice,
                    totalCost = totalCost,
                    notes = notes,
                    updateProductPurchasePrice = updatePrice
                ) {
                    showPurchaseDialog = false
                    onPurchaseHandled()
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onPurchase: () -> Unit,
    onDelete: () -> Unit
) {
    val stockBg = when {
        product.availableStock <= 0L -> AutonRedSoft
        product.availableStock <= product.minStockLevel -> AutonWarningSoft
        else -> AutonSuccessSoft
    }
    val stockFg = when {
        product.availableStock <= 0L -> AutonRed
        product.availableStock <= product.minStockLevel -> AutonWarning
        else -> AutonSuccess
    }
    val stockLabel = when {
        product.availableStock <= 0L -> "OUT OF STOCK"
        product.availableStock <= product.minStockLevel -> "LOW STOCK (${product.availableStock})"
        else -> "IN STOCK: ${product.availableStock}"
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
                    Text(product.name, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                    Text(
                        text = "SKU: ${product.sku}  •  ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                }
                StatusPill(label = stockLabel, backgroundColor = stockBg, textColor = stockFg)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricMini("Cost", BdtFormatter.format(product.purchasePrice), Modifier.weight(1f))
                MetricMini("Selling", BdtFormatter.format(product.sellingPrice), Modifier.weight(1f))
                MetricMini("Min Alert", "${product.minStockLevel} pcs", Modifier.weight(1f))
                if (product.damagedStock > 0L) {
                    MetricMini("Damaged", "${product.damagedStock} pcs", Modifier.weight(1f), isAlert = true)
                }
            }

            if (product.assignedPackaging.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    product.assignedPackaging.forEach { pkg ->
                        StatusPill(
                            label = "${pkg.quantityPerUnit}x ${pkg.packagingName}",
                            backgroundColor = AutonNavySoft,
                            textColor = AutonNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AutonBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = AutonLime, contentColor = AutonNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Purchase Stock", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Product", tint = AutonNavy)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Product", tint = AutonRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, modifier: Modifier = Modifier, isAlert: Boolean = false) {
    Column(
        modifier = modifier
            .background(if (isAlert) AutonRedSoft else AutonCanvas, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AutonTextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
            color = if (isAlert) AutonRed else AutonTextPrimary
        )
    }
}

@Composable
private fun ProductPurchaseCard(purchase: ProductPurchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(purchase.productName, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                Text(
                    text = "${purchase.purchaseDate} • Supplier: ${purchase.supplier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonTextSecondary
                )
                if (purchase.notes.isNotBlank()) {
                    Text(purchase.notes, style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${purchase.quantity} pcs",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                    color = AutonSuccess
                )
                Text(
                    text = BdtFormatter.format(purchase.totalCost, true),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                    color = AutonNavy
                )
                Text(
                    text = "@ ${BdtFormatter.format(purchase.unitPrice, true)}/unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonTextSecondary
                )
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    initial: Product?,
    packagingItems: List<PackagingItem>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Interior") }
    var purchasePrice by remember { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(initial?.sellingPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.availableStock?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(initial?.minStockLevel?.toString() ?: "5") }

    val selectedPackagingMap = remember {
        mutableStateMapOf<String, Long>().apply {
            initial?.assignedPackaging?.forEach { put(it.packagingId, it.quantityPerUnit) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Autonstyle Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name*") },
                    modifier = Modifier.fillMaxWidth().testTag("prod_name_input"),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU*") },
                        modifier = Modifier.weight(1f).testTag("prod_sku_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category*") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Purchase (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Available Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Min Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Assign Packaging Materials (per 1 unit sold)", style = MaterialTheme.typography.titleSmall, color = AutonNavy)
                if (packagingItems.isEmpty()) {
                    Text(
                        "No packaging materials added yet. Add them in Packaging tab to assign.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                } else {
                    packagingItems.forEach { pkg ->
                        val isChecked = selectedPackagingMap.containsKey(pkg.id)
                        val qtyPerUnit = selectedPackagingMap[pkg.id] ?: 1L
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedPackagingMap.remove(pkg.id)
                                    else selectedPackagingMap[pkg.id] = 1L
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedPackagingMap[pkg.id] = 1L
                                    else selectedPackagingMap.remove(pkg.id)
                                }
                            )
                            Text(
                                "${pkg.name} (${BdtFormatter.format(pkg.purchasePrice, true)})",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (isChecked) {
                                OutlinedTextField(
                                    value = qtyPerUnit.toString(),
                                    onValueChange = { txt ->
                                        val parsed = txt.toLongOrNull()?.coerceIn(1L, 100L) ?: 1L
                                        selectedPackagingMap[pkg.id] = parsed
                                    },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(72.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val assigned = packagingItems
                        .filter { selectedPackagingMap.containsKey(it.id) }
                        .map {
                            AssignedPackaging(
                                packagingId = it.id,
                                packagingName = it.name,
                                quantityPerUnit = selectedPackagingMap[it.id] ?: 1L
                            )
                        }
                    val model = (initial ?: Product()).copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category.trim().ifBlank { "General" },
                        purchasePrice = purchasePrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        availableStock = stock.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                        minStockLevel = minStock.toLongOrNull()?.coerceAtLeast(0L) ?: 5L,
                        assignedPackaging = assigned
                    )
                    onSave(model)
                },
                enabled = name.isNotBlank() && sku.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPurchaseDialog(
    products: List<Product>,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onConfirm: (
        productId: String,
        supplier: String,
        date: String,
        qty: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        updatePrice: Boolean
    ) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(initialProduct ?: products.firstOrNull()) }
    var productExpanded by remember { mutableStateOf(false) }
    var supplier by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(DhakaDateTime.todayString()) }
    var quantityText by remember { mutableStateOf("10") }
    var unitPriceText by remember { mutableStateOf((initialProduct?.purchasePrice ?: 0.0).toString()) }
    var totalOverride by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var updateProductPrice by remember { mutableStateOf(true) }

    val qty = quantityText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val unitPrice = unitPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val autoTotal = qty * unitPrice
    val displayTotal = totalOverride ?: BdtFormatter.formatPlain(autoTotal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Product Purchase") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (products.isEmpty()) {
                    Text("Please create a product first before recording a purchase.", color = AutonRed)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (Stock: ${it.availableStock})" } ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Product*") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false }
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.name} (${prod.sku})") },
                                    onClick = {
                                        selectedProduct = prod
                                        unitPriceText = prod.purchasePrice.toString()
                                        totalOverride = null
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Supplier Name*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = { purchaseDate = it },
                        label = { Text("Purchase Date (DD-MM-YYYY)*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = {
                                quantityText = it
                                totalOverride = null
                            },
                            label = { Text("Quantity*") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = {
                                unitPriceText = it
                                totalOverride = null
                            },
                            label = { Text("Unit Price (৳)*") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = displayTotal,
                        onValueChange = { totalOverride = it },
                        label = { Text("Total Purchase Cost (BDT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { updateProductPrice = !updateProductPrice }
                    ) {
                        Checkbox(checked = updateProductPrice, onCheckedChange = { updateProductPrice = it })
                        Text("Update product's default unit cost price", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val prod = selectedProduct
            val totalCost = displayTotal.toDoubleOrNull()?.coerceAtLeast(0.0) ?: autoTotal
            Button(
                onClick = {
                    if (prod != null) {
                        onConfirm(
                            prod.id,
                            supplier,
                            purchaseDate,
                            qty,
                            unitPrice,
                            totalCost,
                            notes,
                            updateProductPrice
                        )
                    }
                },
                enabled = prod != null && supplier.isNotBlank() && qty > 0L && DhakaDateTime.isValidDateFormat(purchaseDate),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Add Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
