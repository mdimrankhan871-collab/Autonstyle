package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWarning
import com.example.ui.theme.AutonWarningSoft
import com.example.ui.theme.AutonWhite
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot

@Composable
fun StockAlertsScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel,
    onNavigateToProducts: () -> Unit,
    onNavigateToPackaging: () -> Unit
) {
    val alerts = snapshot.stockAlerts
    var editTarget by remember { mutableStateOf<Triple<Boolean, String, Pair<String, Long>>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Stock Alerts",
            subtitle = "${alerts.totalAlerts} active alert(s) • Customize minimum thresholds per item"
        )

        if (alerts.totalAlerts == 0) {
            EmptyStateCard(
                icon = Icons.Rounded.CheckCircle,
                title = "All inventory levels are healthy",
                description = "No products or packaging materials are currently below their minimum stock thresholds."
            )
        }

        // 1. Out of Stock Products
        AlertGroupSection(
            title = "Out of Stock Products (${alerts.outOfStockProducts.size})",
            badgeBg = AutonRedSoft,
            badgeFg = AutonRed,
            items = alerts.outOfStockProducts.map {
                AlertRowItem(
                    id = it.id,
                    name = it.name,
                    subtitle = "SKU: ${it.sku} • Available: 0 • Min: ${it.minStockLevel}",
                    badge = "OUT OF STOCK",
                    currentMin = it.minStockLevel,
                    isProduct = true
                )
            },
            onEditMin = { item -> editTarget = Triple(true, item.id, item.name to item.currentMin) },
            onRestock = onNavigateToProducts
        )

        // 2. Low Product Stock
        AlertGroupSection(
            title = "Low Product Stock (${alerts.lowProductStock.size})",
            badgeBg = AutonWarningSoft,
            badgeFg = AutonWarning,
            items = alerts.lowProductStock.map {
                AlertRowItem(
                    id = it.id,
                    name = it.name,
                    subtitle = "SKU: ${it.sku} • Available: ${it.availableStock} • Min: ${it.minStockLevel}",
                    badge = "LOW: ${it.availableStock}",
                    currentMin = it.minStockLevel,
                    isProduct = true
                )
            },
            onEditMin = { item -> editTarget = Triple(true, item.id, item.name to item.currentMin) },
            onRestock = onNavigateToProducts
        )

        // 3. Out of Stock Packaging
        AlertGroupSection(
            title = "Out of Stock Packaging (${alerts.outOfStockPackaging.size})",
            badgeBg = AutonRedSoft,
            badgeFg = AutonRed,
            items = alerts.outOfStockPackaging.map {
                AlertRowItem(
                    id = it.id,
                    name = it.name,
                    subtitle = "Available: 0 • Min: ${it.minStockLevel}",
                    badge = "OUT OF STOCK",
                    currentMin = it.minStockLevel,
                    isProduct = false
                )
            },
            onEditMin = { item -> editTarget = Triple(false, item.id, item.name to item.currentMin) },
            onRestock = onNavigateToPackaging
        )

        // 4. Low Packaging Stock
        AlertGroupSection(
            title = "Low Packaging Stock (${alerts.lowPackagingStock.size})",
            badgeBg = AutonWarningSoft,
            badgeFg = AutonWarning,
            items = alerts.lowPackagingStock.map {
                AlertRowItem(
                    id = it.id,
                    name = it.name,
                    subtitle = "Available: ${it.availableQty} • Min: ${it.minStockLevel}",
                    badge = "LOW: ${it.availableQty}",
                    currentMin = it.minStockLevel,
                    isProduct = false
                )
            },
            onEditMin = { item -> editTarget = Triple(false, item.id, item.name to item.currentMin) },
            onRestock = onNavigateToPackaging
        )
    }

    editTarget?.let { (isProduct, id, nameAndMin) ->
        var minText by remember(id) { mutableStateOf(nameAndMin.second.toString()) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Min Stock Level • ${nameAndMin.first}") },
            text = {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text("Custom Minimum Stock Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                val parsed = minText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                Button(
                    onClick = {
                        if (isProduct) viewModel.updateProductMinStock(id, parsed)
                        else viewModel.updatePackagingMinStock(id, parsed)
                        editTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private data class AlertRowItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val badge: String,
    val currentMin: Long,
    val isProduct: Boolean
)

@Composable
private fun AlertGroupSection(
    title: String,
    badgeBg: Color,
    badgeFg: Color,
    items: List<AlertRowItem>,
    onEditMin: (AlertRowItem) -> Unit,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                if (items.isNotEmpty()) {
                    OutlinedButton(onClick = onRestock, shape = RoundedCornerShape(8.dp)) {
                        Text("Restock", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (items.isEmpty()) {
                Text("None", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, color = AutonNavy)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(item.badge, badgeBg, badgeFg)
                            TextButton(onClick = { onEditMin(item) }) {
                                Text("Min", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
