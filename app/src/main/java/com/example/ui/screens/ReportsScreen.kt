package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonTextPrimary
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.DhakaDateTime
import com.example.util.ExportFormat
import com.example.util.ReportBuilder
import com.example.util.ReportExporter
import com.example.util.ReportType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    snapshot: BusinessSnapshot,
    viewModel: AutonstyleViewModel
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(ReportType.SALES) }
    var pendingFormat by remember { mutableStateOf(ExportFormat.CSV) }

    val table = remember(selectedType, snapshot) {
        ReportBuilder.build(
            type = selectedType,
            products = snapshot.products,
            productPurchases = snapshot.productPurchases,
            packagingItems = snapshot.packagingItems,
            packagingPurchases = snapshot.packagingPurchases,
            orders = snapshot.orders,
            returns = snapshot.returns,
            expenses = snapshot.expenses
        )
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null) {
            ReportExporter.exportToUri(context, uri, table, pendingFormat)
                .onSuccess { viewModel.emitMessage("Exported ${table.title} as ${pendingFormat.label}") }
                .onFailure { err -> viewModel.emitMessage("Export failed: ${err.localizedMessage}") }
        }
    }

    fun launchExport(format: ExportFormat) {
        pendingFormat = format
        val fileName = "Autonstyle_${selectedType.fileSlug}_${DhakaDateTime.todayString()}.${format.extension}"
        createDocLauncher.launch(fileName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Reports & Exports",
            subtitle = "All 8 business reports with CSV, Excel (.xlsx) & PDF export"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReportType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.title) }
                )
            }
        }

        // Export Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { launchExport(ExportFormat.EXCEL) },
                colors = ButtonDefaults.buttonColors(containerColor = AutonNavy, contentColor = AutonWhite),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(17.dp), tint = AutonLime)
                Spacer(Modifier.width(6.dp))
                Text("Excel", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { launchExport(ExportFormat.CSV) },
                colors = ButtonDefaults.buttonColors(containerColor = AutonLime, contentColor = AutonNavy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("CSV", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { launchExport(ExportFormat.PDF) },
                colors = ButtonDefaults.buttonColors(containerColor = AutonRed, contentColor = AutonWhite),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("PDF", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Report Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AutonWhite),
            border = BorderStroke(1.dp, AutonBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(table.title, style = MaterialTheme.typography.titleLarge, color = AutonNavy)
                Text(table.subtitle, style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)

                val hScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .background(AutonNavy, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        table.headers.forEach { header ->
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AutonWhite,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }

                    if (table.rows.isEmpty()) {
                        Text(
                            "No records available for this report yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AutonTextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp)
                        )
                    } else {
                        table.rows.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .background(if (idx % 2 == 0) AutonWhite else AutonCanvas)
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AutonTextPrimary,
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = AutonBorder)
                        }
                    }
                }

                if (table.summaryLines.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AutonCanvas, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        table.summaryLines.forEach { line ->
                            Text(line, style = MaterialTheme.typography.titleSmall, color = AutonNavy)
                        }
                    }
                }
            }
        }
    }
}
