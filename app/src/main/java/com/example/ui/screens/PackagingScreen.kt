package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PackagingItem
import com.example.data.model.PackagingPurchase
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchInputBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
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
fun PackagingScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PackagingItem?>(null) }
    var purchaseTarget by remember { mutableStateOf<PackagingItem?>(null) }
    var wasteTarget by remember { mutableStateOf<PackagingItem?>(null) }

    val filteredItems = remember(snapshot.packagingItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) snapshot.packagingItems
        else snapshot.packagingItems.filter {
            it.name.lowercase().contains(q) || it.supplier.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Packaging Inventory",
            subtitle = "${snapshot.packagingItems.size} Material(s) • Available: ${snapshot.dashboard.packagingStockUnits} pcs",
            actionLabel = "+ Add Material",
            onActionClick = {
                editingItem = null
                showFormDialog = true
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        TabRow(selectedTabIndex = selectedTab, containerColor = AutonWhite, contentColor = AutonNavy) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Materials (${snapshot.packagingItems.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Purchase Log (${snapshot.packagingPurchases.size})") })
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            SearchInputBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search packaging item or supplier (e.g. Poly Bag, Sticker)..."
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredItems.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Layers,
                    title = "No packaging materials yet",
                    description = "Add Poly Bags, Thank You Cards, Stickers, or Boxes, then assign them to products.",
                    actionLabel = "+ Add Material",
                    onAction = {
                        editingItem = null
                        showFormDialog = true
                    }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredItems, key = { it.id }) { item ->
                        PackagingCard(
                            item = item,
                            onPurchase = { purchaseTarget = item },
                            onWaste = { wasteTarget = item },
                            onEdit = {
                                editingItem = item
                                showFormDialog = true
                            },
                            onDelete = { viewModel.deletePackagingItem(item.id) }
                        )
                    }
                }
            }
        } else {
            if (snapshot.packagingPurchases.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.Layers,
                    title = "No packaging purchases logged",
                    description = "Use 'Purchase Stock' on any packaging material to restock and record supplier costs."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(snapshot.packagingPurchases, key = { it.id }) { p ->
                        PackagingPurchaseCard(p)
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        PackagingFormDialog(
            initial = editingItem,
            onDismiss = { showFormDialog = false },
            onSave = { item ->
                viewModel.savePackagingItem(item) { showFormDialog = false }
            }
        )
    }

    purchaseTarget?.let { pkg ->
        PackagingPurchaseDialog(
            item = pkg,
            onDismiss = { purchaseTarget = null },
            onConfirm = { supplier, date, qty, unitPrice, totalCost, notes, alsoLogExp ->
                viewModel.recordPackagingPurchase(
                    packagingId = pkg.id,
                    supplier = supplier,
                    purchaseDate = date,
                    quantity = qty,
                    unitPrice = unitPrice,
                    totalCost = totalCost,
                    notes = notes,
                    alsoLogPackagingExpense = alsoLogExp
                ) { purchaseTarget = null }
            }
        )
    }

    wasteTarget?.let { pkg ->
        PackagingWasteDialog(
            item = pkg,
            onDismiss = { wasteTarget = null },
            onConfirm = { qty, reason ->
                viewModel.recordPackagingWaste(pkg.id, qty, reason) { wasteTarget = null }
            }
        )
    }
}

@Composable
private fun PackagingCard(
    item: PackagingItem,
    onPurchase: () -> Unit,
    onWaste: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = when {
        item.availableQty <= 0L -> AutonRedSoft
        item.availableQty <= item.minStockLevel -> AutonWarningSoft
        else -> AutonSuccessSoft
    }
    val fg = when {
        item.availableQty <= 0L -> AutonRed
        item.availableQty <= item.minStockLevel -> AutonWarning
        else -> AutonSuccess
    }
    val label = when {
        item.availableQty <= 0L -> "OUT OF STOCK"
        item.availableQty <= item.minStockLevel -> "LOW (${item.availableQty})"
        else -> "AVAIL: ${item.availableQty}"
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
                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                    Text(
                        text = "Unit Cost: ${BdtFormatter.format(item.purchasePrice, true)}" +
                            if (item.supplier.isNotBlank()) " • Supplier: ${item.supplier}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                    if (item.lastPurchaseDate.isNotBlank()) {
                        Text("Last Purchase: ${item.lastPurchaseDate}", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                    }
                }
                StatusPill(label, bg, fg)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Purchased", item.purchasedQty.toString(), Modifier.weight(1f))
                StatCell("Used", item.usedQty.toString(), Modifier.weight(1f))
                StatCell("Wasted", item.wastedQty.toString(), Modifier.weight(1f))
                StatCell("Min Alert", item.minStockLevel.toString(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AutonBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPurchase,
                        colors = ButtonDefaults.buttonColors(containerColor = AutonLime, contentColor = AutonNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Purchase", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    OutlinedButton(onClick = onWaste, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(15.dp), tint = AutonWarning)
                        Spacer(Modifier.width(4.dp))
                        Text("Waste", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Packaging", tint = AutonNavy)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Packaging", tint = AutonRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AutonCanvas, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AutonTextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily), color = AutonTextPrimary)
    }
}

@Composable
private fun PackagingPurchaseCard(p: PackagingPurchase) {
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
                Text(p.packagingName, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                Text("${p.purchaseDate} • Supplier: ${p.supplier}", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+${p.quantity} pcs", style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily), color = AutonSuccess)
                Text(BdtFormatter.format(p.totalCost, true), style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily), color = AutonNavy)
            }
        }
    }
}

@Composable
private fun PackagingFormDialog(
    initial: PackagingItem?,
    onDismiss: () -> Unit,
    onSave: (PackagingItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unitPrice by remember { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var availableQty by remember { mutableStateOf(initial?.availableQty?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(initial?.minStockLevel?.toString() ?: "20") }
    var supplier by remember { mutableStateOf(initial?.supplier ?: "") }
    var lastDate by remember { mutableStateOf(initial?.lastPurchaseDate?.ifBlank { DhakaDateTime.todayString() } ?: DhakaDateTime.todayString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Packaging Material" else "Edit Packaging Material") },
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
                    label = { Text("Packaging Item Name* (e.g. Poly Bag)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = { Text("Unit Price (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = availableQty,
                        onValueChange = { availableQty = it },
                        label = { Text("Available Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("Min Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastDate,
                        onValueChange = { lastDate = it },
                        label = { Text("Date (DD-MM-YYYY)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val avail = availableQty.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    val model = (initial ?: PackagingItem()).copy(
                        name = name.trim(),
                        purchasePrice = unitPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        availableQty = avail,
                        purchasedQty = initial?.purchasedQty ?: avail,
                        minStockLevel = minStock.toLongOrNull()?.coerceAtLeast(0L) ?: 20L,
                        supplier = supplier.trim(),
                        lastPurchaseDate = lastDate.trim()
                    )
                    onSave(model)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Save Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PackagingPurchaseDialog(
    item: PackagingItem,
    onDismiss: () -> Unit,
    onConfirm: (
        supplier: String,
        date: String,
        qty: Long,
        unitPrice: Double,
        totalCost: Double,
        notes: String,
        alsoLogExp: Boolean
    ) -> Unit
) {
    var supplier by remember { mutableStateOf(item.supplier) }
    var date by remember { mutableStateOf(DhakaDateTime.todayString()) }
    var qtyText by remember { mutableStateOf("100") }
    var unitPriceText by remember { mutableStateOf(item.purchasePrice.toString()) }
    var notes by remember { mutableStateOf("") }
    var alsoLogExp by remember { mutableStateOf(true) }

    val qty = qtyText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val unitPrice = unitPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalCost = qty * unitPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Purchase Date (DD-MM-YYYY)*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("Unit Price (৳)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Text(
                    "Total Cost: ${BdtFormatter.format(totalCost, true)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = AutonNavy
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { alsoLogExp = !alsoLogExp }
                ) {
                    Checkbox(checked = alsoLogExp, onCheckedChange = { alsoLogExp = it })
                    Text("Log cash purchase in Packaging Expenses (not double-counted in P&L)", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(supplier, date, qty, unitPrice, totalCost, notes, alsoLogExp) },
                enabled = supplier.isNotBlank() && qty > 0L && DhakaDateTime.isValidDateFormat(date),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Add Packaging Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PackagingWasteDialog(
    item: PackagingItem,
    onDismiss: () -> Unit,
    onConfirm: (qty: Long, reason: String) -> Unit
) {
    var qtyText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("Torn / Damaged") }
    val qty = qtyText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Waste • ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Available: ${item.availableQty} pcs", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Wasted Quantity*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(qty, reason) },
                enabled = qty in 1L..item.availableQty,
                colors = ButtonDefaults.buttonColors(containerColor = AutonRed)
            ) {
                Text("Record Waste")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
