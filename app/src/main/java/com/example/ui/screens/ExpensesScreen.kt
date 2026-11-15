package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonNavySoft
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter
import com.example.util.DhakaDateTime

@Composable
fun ExpensesScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel,
    openAddOnLaunch: Boolean = false,
    onAddHandled: () -> Unit = {}
) {
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showForm by remember { mutableStateOf(openAddOnLaunch) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    if (openAddOnLaunch && !showForm) {
        showForm = true
        onAddHandled()
    }

    val filtered = remember(snapshot.expenses, categoryFilter) {
        if (categoryFilter == null) snapshot.expenses
        else snapshot.expenses.filter { it.categoryEnum == categoryFilter }
    }
    val totalFiltered = filtered.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "Expense Management",
            subtitle = "Total: ${BdtFormatter.format(totalFiltered, true)} (${filtered.size} entries)",
            actionLabel = "+ Add Expense",
            onActionClick = {
                editingExpense = null
                showForm = true
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoryFilter == null,
                onClick = { categoryFilter = null },
                label = { Text("All") }
            )
            ExpenseCategory.entries.forEach { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { categoryFilter = cat },
                    label = { Text(cat.label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Rounded.Payments,
                title = "No expenses recorded",
                description = "Track Facebook Ads, Courier, Return Courier, Packaging, Website Maintenance, and Transportation.",
                actionLabel = "+ Add Expense",
                onAction = {
                    editingExpense = null
                    showForm = true
                }
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { exp ->
                    ExpenseCard(
                        expense = exp,
                        onEdit = {
                            editingExpense = exp
                            showForm = true
                        },
                        onDelete = { viewModel.deleteExpense(exp.id) }
                    )
                }
            }
        }
    }

    if (showForm) {
        ExpenseFormDialog(
            initial = editingExpense,
            onDismiss = {
                showForm = false
                onAddHandled()
            },
            onSave = { exp ->
                viewModel.saveExpense(exp) {
                    showForm = false
                    onAddHandled()
                }
            }
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLinked = expense.linkedReferenceId.isNotBlank()
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = expense.categoryEnum.label,
                        backgroundColor = if (expense.categoryEnum == ExpenseCategory.RETURN_COURIER) AutonRedSoft else AutonNavySoft,
                        textColor = if (expense.categoryEnum == ExpenseCategory.RETURN_COURIER) AutonRed else AutonNavy
                    )
                    if (isLinked) {
                        StatusPill("Auto-Linked", AutonNavySoft, AutonNavy)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(expense.title, style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                Text("${expense.expenseDate}${if (expense.notes.isNotBlank()) " • ${expense.notes}" else ""}", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    BdtFormatter.format(expense.amount, true),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                    color = AutonRed
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit Expense", tint = AutonNavy)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Expense", tint = AutonRed)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormDialog(
    initial: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    var category by remember { mutableStateOf(initial?.categoryEnum ?: ExpenseCategory.FACEBOOK_ADS) }
    var catExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(initial?.expenseDate ?: DhakaDateTime.todayString()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Record Business Expense" else "Edit Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title / Description*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (৳)*") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (DD-MM-YYYY)*") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: -1.0
            Button(
                onClick = {
                    val model = (initial ?: Expense()).copy(
                        category = category.name,
                        title = title.trim(),
                        amount = amount,
                        expenseDate = date.trim(),
                        notes = notes.trim()
                    )
                    onSave(model)
                },
                enabled = title.isNotBlank() && amount >= 0.0 && DhakaDateTime.isValidDateFormat(date),
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
