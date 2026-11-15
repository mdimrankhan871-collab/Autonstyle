package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.StockTransaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonNavySoft
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonSuccess
import com.example.ui.theme.AutonSuccessSoft
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter
import com.example.util.DhakaDateTime
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

@Composable
fun LedgerAndBackupScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel,
    userEmail: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filterType by remember { mutableStateOf<String?>(null) }

    val filteredTx = remember(snapshot.stockTransactions, filterType) {
        if (filterType == null) snapshot.stockTransactions
        else snapshot.stockTransactions.filter { it.itemType == filterType }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportBackupJson().onSuccess { json ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(StandardCharsets.UTF_8))
                            out.flush()
                        }
                        viewModel.emitMessage("Full Cloud Backup exported successfully")
                    } catch (e: Exception) {
                        viewModel.emitMessage("Failed to write backup: ${e.localizedMessage}")
                    }
                }.onFailure { err ->
                    viewModel.emitMessage("Backup failed: ${err.localizedMessage}")
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    viewModel.restoreBackupJson(text)
                } else {
                    viewModel.emitMessage("Selected backup file is empty")
                }
            } catch (e: Exception) {
                viewModel.emitMessage("Could not read backup: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Backup, Restore & Account",
                subtitle = "Signed in as ${userEmail ?: "Authorized Owner"}"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AutonWhite),
                border = BorderStroke(1.dp, AutonBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cloud Firestore Backup & Restore", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                    Text(
                        "Export a complete JSON snapshot of all products, purchases, packaging, orders, returns, expenses, and transaction history, or restore from a saved backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                backupExportLauncher.launch("Autonstyle_Backup_${DhakaDateTime.todayString()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AutonNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp), tint = AutonLime)
                            Spacer(Modifier.width(6.dp))
                            Text("Backup JSON")
                        }
                        OutlinedButton(
                            onClick = { backupRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, AutonRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AutonRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Transaction History (${filteredTx.size})",
                subtitle = "Immutable atomic stock movement audit trail"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("All") })
                FilterChip(selected = filterType == "PRODUCT", onClick = { filterType = "PRODUCT" }, label = { Text("Products") })
                FilterChip(selected = filterType == "PACKAGING", onClick = { filterType = "PACKAGING" }, label = { Text("Packaging") })
            }
        }

        if (filteredTx.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.History,
                    title = "No stock transactions yet",
                    description = "Every purchase, order deduction, packaging waste, and return automatically writes an immutable audit entry here."
                )
            }
        } else {
            items(filteredTx, key = { it.id }) { tx ->
                TransactionRowCard(tx)
            }
        }
    }
}

@Composable
private fun TransactionRowCard(tx: StockTransaction) {
    val isPositive = tx.quantityDelta >= 0L
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = tx.txType.replace("_", " "),
                        backgroundColor = if (isPositive) AutonSuccessSoft else AutonRedSoft,
                        textColor = if (isPositive) AutonSuccess else AutonRed
                    )
                    StatusPill(tx.itemType, AutonNavySoft, AutonNavy)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(tx.itemName, style = MaterialTheme.typography.titleSmall, color = AutonNavy)
                Text(
                    "${tx.referenceLabel} • ${DhakaDateTime.formatDateTime(tx.txEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonTextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPositive) "+${tx.quantityDelta}" else "${tx.quantityDelta}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                    color = if (isPositive) AutonSuccess else AutonRed
                )
                Text(
                    text = "${tx.stockBefore} → ${tx.stockAfter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AutonTextSecondary
                )
                Text(
                    text = BdtFormatter.format(tx.totalValue, true),
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonNavy
                )
            }
        }
    }
}
