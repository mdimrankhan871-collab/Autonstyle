package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonLimeDark
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
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.util.BdtFormatter
import com.example.util.DhakaDateTime
import java.util.Calendar

enum class AppDestination(val label: String) {
    DASHBOARD("Dashboard"),
    ORDERS("Orders"),
    PRODUCTS("Products"),
    PACKAGING("Packaging"),
    RETURNS("Returns"),
    EXPENSES("Expenses"),
    PROFIT_LOSS("Profit & Loss"),
    ALERTS("Stock Alerts"),
    REPORTS("Reports"),
    LEDGER("History & Backup")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    snapshot: BusinessSnapshot,
    onNavigate: (AppDestination) -> Unit,
    onQuickNewOrder: () -> Unit,
    onQuickNewPurchase: () -> Unit,
    onQuickNewExpense: () -> Unit
) {
    val dash = snapshot.dashboard
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Branded Hero Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AutonNavy)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_autonstyle_banner_1794699893816),
                    contentDescription = "Autonstyle Operations Header",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AutonNavy.copy(alpha = 0.94f),
                                    AutonNavy.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Autonstyle Operations",
                                style = MaterialTheme.typography.headlineSmall,
                                color = AutonWhite
                            )
                            Text(
                                text = "Today: ${DhakaDateTime.todayString()}  •  Asia/Dhaka  •  BDT (৳)",
                                style = MaterialTheme.typography.bodySmall,
                                color = AutonLime
                            )
                        }
                        if (dash.lowStockAlertCount > 0) {
                            StatusPill(
                                label = "${dash.lowStockAlertCount} Stock Alert(s)",
                                backgroundColor = AutonRed,
                                textColor = AutonWhite
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onQuickNewOrder,
                            modifier = Modifier.testTag("dash_new_order_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AutonLime,
                                contentColor = AutonNavy
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Order", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onQuickNewPurchase,
                            modifier = Modifier.testTag("dash_new_purchase_btn"),
                            border = BorderStroke(1.dp, AutonWhite.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AutonWhite),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Purchase", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onQuickNewExpense,
                            modifier = Modifier.testTag("dash_new_expense_btn"),
                            border = BorderStroke(1.dp, AutonWhite.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AutonWhite),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Expense", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 10 Required Dashboard Metrics in Adaptive Grid
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 1000.dp -> 5
                maxWidth >= 660.dp -> 3
                else -> 2
            }
            val cards = listOf<@Composable (Modifier) -> Unit>(
                { mod ->
                    KpiMetricCard(
                        title = "Today's Sales",
                        value = BdtFormatter.format(dash.todaysSales),
                        subtitle = "Active net sales today",
                        icon = Icons.Rounded.Today,
                        accentColor = AutonNavy,
                        iconContainerColor = AutonNavySoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.ORDERS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Monthly Sales",
                        value = BdtFormatter.format(dash.monthlySales),
                        subtitle = "Current Dhaka month",
                        icon = Icons.Rounded.CalendarMonth,
                        accentColor = AutonNavy,
                        iconContainerColor = AutonNavySoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.PROFIT_LOSS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Net Profit",
                        value = BdtFormatter.format(dash.netProfit),
                        subtitle = "After COGS & all expenses",
                        icon = Icons.Rounded.TrendingUp,
                        accentColor = if (dash.netProfit >= 0) AutonLime else AutonRed,
                        iconContainerColor = AutonWhite.copy(alpha = 0.14f),
                        highlightDark = true,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.PROFIT_LOSS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Total Orders",
                        value = dash.totalOrders.toString(),
                        subtitle = "All recorded orders",
                        icon = Icons.Rounded.ShoppingBag,
                        accentColor = AutonNavy,
                        iconContainerColor = AutonNavySoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.ORDERS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Total Expenses",
                        value = BdtFormatter.format(dash.totalExpenses),
                        subtitle = "Ads, courier & overhead",
                        icon = Icons.Rounded.Payments,
                        accentColor = AutonRed,
                        iconContainerColor = AutonRedSoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.EXPENSES) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Product Stock",
                        value = "${dash.productStockUnits} pcs",
                        subtitle = "Value ${BdtFormatter.format(dash.productStockValue)}",
                        icon = Icons.Rounded.Inventory2,
                        accentColor = AutonSuccess,
                        iconContainerColor = AutonSuccessSoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.PRODUCTS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Packaging Stock",
                        value = "${dash.packagingStockUnits} pcs",
                        subtitle = "Value ${BdtFormatter.format(dash.packagingStockValue)}",
                        icon = Icons.Rounded.Layers,
                        accentColor = AutonLimeDark,
                        iconContainerColor = AutonLimeSoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.PACKAGING) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Low Stock Alerts",
                        value = dash.lowStockAlertCount.toString(),
                        subtitle = if (dash.lowStockAlertCount == 0) "All levels healthy" else "Tap to restock",
                        icon = Icons.Rounded.WarningAmber,
                        accentColor = if (dash.lowStockAlertCount > 0) AutonRed else AutonSuccess,
                        iconContainerColor = if (dash.lowStockAlertCount > 0) AutonRedSoft else AutonSuccessSoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.ALERTS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Returned Orders",
                        value = dash.returnedOrdersCount.toString(),
                        subtitle = "${snapshot.returns.size} return log(s)",
                        icon = Icons.Rounded.AssignmentReturn,
                        accentColor = AutonWarning,
                        iconContainerColor = AutonWarningSoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.RETURNS) }
                    )
                },
                { mod ->
                    KpiMetricCard(
                        title = "Total Purchase Amount",
                        value = BdtFormatter.format(dash.totalPurchaseAmount),
                        subtitle = "Products + Packaging",
                        icon = Icons.Rounded.LocalShipping,
                        accentColor = AutonNavy,
                        iconContainerColor = AutonNavySoft,
                        modifier = mod,
                        onClick = { onNavigate(AppDestination.PRODUCTS) }
                    )
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.chunked(columns).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCards.forEach { cardFn ->
                            cardFn(Modifier.weight(1f))
                        }
                        repeat(columns - rowCards.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 7-Day Sales Trend + Recent Orders
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 780.dp
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Sales7DayChartCard(
                        orders = snapshot.orders,
                        modifier = Modifier.weight(1f)
                    )
                    RecentOrdersCard(
                        orders = snapshot.orders.take(5),
                        onViewAll = { onNavigate(AppDestination.ORDERS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Sales7DayChartCard(orders = snapshot.orders, modifier = Modifier.fillMaxWidth())
                    RecentOrdersCard(
                        orders = snapshot.orders.take(5),
                        onViewAll = { onNavigate(AppDestination.ORDERS) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Sales7DayChartCard(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val dailyTotals = remember(orders) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance(DhakaDateTime.ZONE).apply {
            timeInMillis = DhakaDateTime.startOfTodayMs(now)
        }
        val days = (6 downTo 0).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val start = c.timeInMillis
            val end = start + 86_400_000L - 1L
            val label = DhakaDateTime.formatDate(start).substring(0, 5)
            val sum = orders
                .filter { it.statusEnum != OrderStatus.CANCELLED && it.orderEpochMs in start..end }
                .sumOf { (it.subtotal - it.discountAmount - it.returnedAmount).coerceAtLeast(0.0) }
            label to sum
        }
        days
    }
    val maxVal = (dailyTotals.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(100.0)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("7-Day Net Sales (BDT)", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                Text("Asia/Dhaka", style = MaterialTheme.typography.labelSmall, color = AutonTextSecondary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barCount = dailyTotals.size
                val slotWidth = size.width / barCount
                val barWidth = slotWidth * 0.52f
                dailyTotals.forEachIndexed { idx, (_, value) ->
                    val ratio = (value / maxVal).toFloat().coerceIn(0.04f, 1f)
                    val barHeight = size.height * ratio
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = if (idx == barCount - 1) AutonRed else AutonNavy,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyTotals.forEach { (label, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AutonTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentOrdersCard(
    orders: List<Order>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AutonWhite),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = AutonNavy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Recent Orders", style = MaterialTheme.typography.titleMedium, color = AutonNavy)
                }
                OutlinedButton(onClick = onViewAll, shape = RoundedCornerShape(8.dp)) {
                    Text("View All", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (orders.isEmpty()) {
                Text(
                    text = "No orders recorded yet. Tap 'New Order' above to create your first invoice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonTextSecondary,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                orders.forEach { order ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AutonCanvas)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${order.invoiceNumber} • ${order.customerName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = AutonTextPrimary
                            )
                            Text(
                                text = "${order.orderDate} • ${order.items.sumOf { it.quantity }} unit(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = AutonTextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = BdtFormatter.format(order.totalAmount),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                color = AutonNavy
                            )
                            StatusPill(
                                label = order.statusEnum.label,
                                backgroundColor = when (order.statusEnum) {
                                    OrderStatus.DELIVERED -> AutonSuccessSoft
                                    OrderStatus.CANCELLED, OrderStatus.RETURNED -> AutonRedSoft
                                    OrderStatus.PARTIAL_RETURN -> AutonWarningSoft
                                    else -> AutonNavySoft
                                },
                                textColor = when (order.statusEnum) {
                                    OrderStatus.DELIVERED -> AutonSuccess
                                    OrderStatus.CANCELLED, OrderStatus.RETURNED -> AutonRed
                                    OrderStatus.PARTIAL_RETURN -> AutonWarning
                                    else -> AutonNavy
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
