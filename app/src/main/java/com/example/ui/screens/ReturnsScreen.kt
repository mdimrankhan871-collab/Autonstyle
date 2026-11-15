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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.ProductReturn
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonSuccess
import com.example.ui.theme.AutonSuccessSoft
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWarning
import com.example.ui.theme.AutonWarningSoft
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter

@Composable
fun ReturnsScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel
) {
    val eligibleOrders = remember(snapshot.orders) {
        snapshot.orders.filter {
            it.stockDeducted &&
                it.statusEnum != OrderStatus.CANCELLED &&
                it.items.any { item -> item.quantity > item.returnedQty }
        }
    }
    var showOrderPicker by remember { mutableStateOf(false) }
    var selectedOrderForReturn by remember { mutableStateOf<Order?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Product Returns",
            subtitle = "${snapshot.returns.size} Return(s) • Packaging remains permanently deducted",
            actionLabel = if (eligibleOrders.isNotEmpty()) "+ Process Return" else null,
            onActionClick = if (eligibleOrders.isNotEmpty()) {
                { showOrderPicker = true }
            } else null
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (snapshot.returns.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.AssignmentReturn,
                title = "No product returns recorded",
                description = "Process returns from packed/dispatched orders to route resellable stock back to inventory or damaged stock.",
                actionLabel = if (eligibleOrders.isNotEmpty()) "+ Process Return" else null,
                onAction = if (eligibleOrders.isNotEmpty()) {
                    { showOrderPicker = true }
                } else null
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(snapshot.returns, key = { it.id }) { ret ->
                    ReturnCard(ret)
                }
            }
        }
    }

    if (showOrderPicker) {
        SelectOrderForReturnDialog(
            orders = eligibleOrders,
            onDismiss = { showOrderPicker = false },
            onSelect = { ord ->
                showOrderPicker = false
                selectedOrderForReturn = ord
            }
        )
    }

    selectedOrderForReturn?.let { order ->
        ProcessReturnDialog(
            order = order,
            onDismiss = { selectedOrderForReturn = null },
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
                    selectedOrderForReturn = null
                }
            }
        )
    }
}

@Composable
private fun ReturnCard(ret: ProductReturn) {
    val (label, bg, fg) = when {
        !ret.isReceived -> Triple("Not Received", AutonWarningSoft, AutonWarning)
        ret.isResellable -> Triple("Resellable (+Stock)", AutonSuccessSoft, AutonSuccess)
        else -> Triple("Damaged Inventory", AutonRedSoft, AutonRed)
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
                    Text("${ret.productName} (x${ret.returnedQty})", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                    Text("Invoice: ${ret.invoiceNumber} • ${ret.returnDate}", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                    Text("Reason: ${ret.reason}", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                }
                StatusPill(label, bg, fg)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Refund Adj: ${BdtFormatter.format(ret.refundAmount)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                    color = AutonNavy
                )
                Text(
                    "Return Courier: ${BdtFormatter.format(ret.returnCourierCharge)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                    color = AutonRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectOrderForReturnDialog(
    orders: List<Order>,
    onDismiss: () -> Unit,
    onSelect: (Order) -> Unit
) {
    var selected by remember { mutableStateOf(orders.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Order to Return") },
        text = {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selected?.let { "${it.invoiceNumber} • ${it.customerName}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Order") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    orders.forEach { ord ->
                        DropdownMenuItem(
                            text = { Text("${ord.invoiceNumber} • ${ord.customerName} (${ord.orderDate})") },
                            onClick = {
                                selected = ord
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let(onSelect) },
                enabled = selected != null,
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
