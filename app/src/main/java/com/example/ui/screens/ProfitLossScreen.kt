package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ReportPeriod
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonNavySoft
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonSuccess
import com.example.ui.theme.AutonTextPrimary
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter
import java.util.Locale

@Composable
fun ProfitLossScreen(snapshot: BusinessSnapshot) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    val pnl = when (selectedPeriod) {
        ReportPeriod.DAILY -> snapshot.dailyPnl
        ReportPeriod.WEEKLY -> snapshot.weeklyPnl
        ReportPeriod.MONTHLY -> snapshot.monthlyPnl
        ReportPeriod.ALL_TIME -> snapshot.allTimePnl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Profit & Loss Statement",
            subtitle = "Exact accounting in BDT (৳) • Timezone: Asia/Dhaka"
        )

        TabRow(selectedTabIndex = selectedPeriod.ordinal, containerColor = AutonWhite, contentColor = AutonNavy) {
            ReportPeriod.entries.forEach { period ->
                Tab(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    text = { Text(period.label) }
                )
            }
        }

        // Net Profit Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AutonNavy),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${pnl.periodLabel} Net Profit",
                        style = MaterialTheme.typography.titleMedium,
                        color = AutonWhite.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        BdtFormatter.format(pnl.netProfit, true),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = MonoFontFamily),
                        color = if (pnl.netProfit >= 0.0) AutonLime else AutonRed
                    )
                    Text(
                        "Margin: ${String.format(Locale.US, "%.1f%%", pnl.profitMarginPercent)} • ${pnl.activeOrderCount} active order(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AutonWhite.copy(alpha = 0.72f)
                    )
                }
            }
        }

        // Line-by-line P&L Statement
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AutonWhite),
            border = BorderStroke(1.dp, AutonBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accounting Breakdown", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                HorizontalDivider(color = AutonBorder)

                PnlLineRow("Gross Product Sales (after order discounts)", BdtFormatter.format(pnl.grossSales, true), AutonTextPrimary)
                PnlLineRow("Less: Cancelled Order Revenue", "-${BdtFormatter.format(pnl.cancelledAdjustment, true)}", AutonRed)
                PnlLineRow("Less: Returned Order Adjustments", "-${BdtFormatter.format(pnl.returnedAdjustment, true)}", AutonRed)
                HorizontalDivider(color = AutonBorder)
                PnlLineRow("Net Sales Revenue", BdtFormatter.format(pnl.netSalesRevenue, true), AutonNavy, bold = true)

                Spacer(modifier = Modifier.height(4.dp))
                PnlLineRow("Less: Product Cost (COGS, net of resellable returns)", "-${BdtFormatter.format(pnl.productCost, true)}", AutonRed)
                PnlLineRow("Less: Packaging Material Cost (consumed + wasted)", "-${BdtFormatter.format(pnl.packagingMaterialCost, true)}", AutonRed)
                PnlLineRow("Less: Delivery Expenses (Courier)", "-${BdtFormatter.format(pnl.deliveryExpenses, true)}", AutonRed)
                PnlLineRow("Less: Return Expenses (Return Courier)", "-${BdtFormatter.format(pnl.returnExpenses, true)}", AutonRed)
                PnlLineRow("Less: Other Operating Expenses (Ads, Web, Transport, Other)", "-${BdtFormatter.format(pnl.otherExpenses, true)}", AutonRed)
                HorizontalDivider(color = AutonNavy)
                PnlLineRow(
                    "Actual Net Profit",
                    BdtFormatter.format(pnl.netProfit, true),
                    if (pnl.netProfit >= 0.0) AutonSuccess else AutonRed,
                    bold = true
                )

                if (pnl.packagingCashPurchasesExcluded > 0.0) {
                    Surface(
                        color = AutonNavySoft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Deduplication Guard: ${BdtFormatter.format(pnl.packagingCashPurchasesExcluded, true)} in Packaging stock cash purchases is excluded from Operating Expenses above because actual Packaging Material Cost is deducted per dispatched order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AutonNavy,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Product-wise Profit Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AutonWhite),
            border = BorderStroke(1.dp, AutonBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Product-wise Profit", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                if (snapshot.productProfitRows.isEmpty()) {
                    Text("No active product sales yet.", style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
                } else {
                    snapshot.productProfitRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AutonCanvas, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.productName, style = MaterialTheme.typography.titleSmall, color = AutonNavy)
                                Text(
                                    "Sold: ${row.unitsSold} • Ret: ${row.unitsReturned} • Net Rev: ${BdtFormatter.format(row.netRevenue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AutonTextSecondary
                                )
                                Text(
                                    "Prod Cost: ${BdtFormatter.format(row.productCost)} • Pkg Cost: ${BdtFormatter.format(row.packagingCost, true)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AutonTextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    BdtFormatter.format(row.grossProfit, true),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                    color = if (row.grossProfit >= 0) AutonSuccess else AutonRed
                                )
                                Text(
                                    "${String.format(Locale.US, "%.1f%%", row.marginPercent)} margin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutonTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PnlLineRow(
    label: String,
    amount: String,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (bold) AutonNavy else AutonTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            style = (if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium).copy(
                fontFamily = MonoFontFamily,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
            ),
            color = valueColor
        )
    }
}
