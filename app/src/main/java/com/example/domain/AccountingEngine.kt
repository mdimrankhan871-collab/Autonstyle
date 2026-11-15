package com.example.domain

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.PackagingPurchase
import com.example.data.model.Product
import com.example.data.model.ProductPurchase
import com.example.data.model.ProductReturn
import com.example.data.model.StockTransaction
import com.example.util.DhakaDateTime

enum class ReportPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All Time")
}

data class DashboardSummary(
    val todaysSales: Double,
    val monthlySales: Double,
    val totalOrders: Int,
    val totalExpenses: Double,
    val netProfit: Double,
    val productStockUnits: Long,
    val productStockValue: Double,
    val packagingStockUnits: Long,
    val packagingStockValue: Double,
    val lowStockAlertCount: Int,
    val returnedOrdersCount: Int,
    val totalPurchaseAmount: Double
)

data class ProfitAndLossReport(
    val periodLabel: String,
    val grossSales: Double,
    val cancelledAdjustment: Double,
    val returnedAdjustment: Double,
    val netSalesRevenue: Double,
    val productCost: Double,
    val packagingMaterialCost: Double,
    val deliveryExpenses: Double,
    val returnExpenses: Double,
    val otherExpenses: Double,
    val packagingCashPurchasesExcluded: Double,
    val netProfit: Double,
    val profitMarginPercent: Double,
    val activeOrderCount: Int
)

data class StockAlertsSummary(
    val lowProductStock: List<Product>,
    val outOfStockProducts: List<Product>,
    val lowPackagingStock: List<PackagingItem>,
    val outOfStockPackaging: List<PackagingItem>
) {
    val totalAlerts: Int
        get() = lowProductStock.size + outOfStockProducts.size + lowPackagingStock.size + outOfStockPackaging.size
}

data class ProductProfitRow(
    val productId: String,
    val productName: String,
    val sku: String,
    val unitsSold: Long,
    val unitsReturned: Long,
    val netUnits: Long,
    val netRevenue: Double,
    val productCost: Double,
    val packagingCost: Double,
    val grossProfit: Double,
    val marginPercent: Double
)

object AccountingEngine {

    fun computeStockAlerts(
        products: List<Product>,
        packagingItems: List<PackagingItem>
    ): StockAlertsSummary {
        val outOfStockProducts = products.filter { it.availableStock <= 0L }.sortedBy { it.name }
        val lowProductStock = products.filter {
            it.availableStock in 1L..it.minStockLevel.coerceAtLeast(0L)
        }.sortedBy { it.availableStock }

        val outOfStockPackaging = packagingItems.filter { it.availableQty <= 0L }.sortedBy { it.name }
        val lowPackagingStock = packagingItems.filter {
            it.availableQty in 1L..it.minStockLevel.coerceAtLeast(0L)
        }.sortedBy { it.availableQty }

        return StockAlertsSummary(
            lowProductStock = lowProductStock,
            outOfStockProducts = outOfStockProducts,
            lowPackagingStock = lowPackagingStock,
            outOfStockPackaging = outOfStockPackaging
        )
    }

    fun computeDashboardSummary(
        products: List<Product>,
        packagingItems: List<PackagingItem>,
        orders: List<Order>,
        returns: List<ProductReturn>,
        expenses: List<Expense>,
        productPurchases: List<ProductPurchase>,
        packagingPurchases: List<PackagingPurchase>,
        nowMs: Long = System.currentTimeMillis()
    ): DashboardSummary {
        val startOfToday = DhakaDateTime.startOfTodayMs(nowMs)
        val startOfMonth = DhakaDateTime.startOfMonthMs(nowMs)

        val nonCancelledOrders = orders.filter { it.statusEnum != OrderStatus.CANCELLED }
        val todaysSales = nonCancelledOrders
            .filter { it.orderEpochMs >= startOfToday }
            .sumOf { ((it.subtotal - it.discountAmount) - it.returnedAmount).coerceAtLeast(0.0) }

        val monthlySales = nonCancelledOrders
            .filter { it.orderEpochMs >= startOfMonth }
            .sumOf { ((it.subtotal - it.discountAmount) - it.returnedAmount).coerceAtLeast(0.0) }

        val monthlyPnl = computeProfitAndLoss(
            orders = orders,
            returns = returns,
            expenses = expenses,
            packagingItems = packagingItems,
            stockTransactions = emptyList(),
            startEpochMs = 0L,
            endEpochMs = Long.MAX_VALUE,
            periodLabel = "All Time"
        )

        val totalExpensesDisplayed = expenses.sumOf { it.amount }
        val productStockUnits = products.sumOf { it.availableStock.coerceAtLeast(0L) }
        val productStockValue = products.sumOf { it.availableStock.coerceAtLeast(0L) * it.purchasePrice }
        val packagingStockUnits = packagingItems.sumOf { it.availableQty.coerceAtLeast(0L) }
        val packagingStockValue = packagingItems.sumOf { it.availableQty.coerceAtLeast(0L) * it.purchasePrice }
        val alerts = computeStockAlerts(products, packagingItems)
        val returnedOrdersCount = orders.count {
            it.statusEnum == OrderStatus.RETURNED || it.statusEnum == OrderStatus.PARTIAL_RETURN
        }
        val totalPurchaseAmount = productPurchases.sumOf { it.totalCost } +
            packagingPurchases.sumOf { it.totalCost }

        return DashboardSummary(
            todaysSales = todaysSales,
            monthlySales = monthlySales,
            totalOrders = orders.size,
            totalExpenses = totalExpensesDisplayed,
            netProfit = monthlyPnl.netProfit,
            productStockUnits = productStockUnits,
            productStockValue = productStockValue,
            packagingStockUnits = packagingStockUnits,
            packagingStockValue = packagingStockValue,
            lowStockAlertCount = alerts.totalAlerts,
            returnedOrdersCount = returnedOrdersCount,
            totalPurchaseAmount = totalPurchaseAmount
        )
    }

    fun computeProfitAndLossForPeriod(
        period: ReportPeriod,
        orders: List<Order>,
        returns: List<ProductReturn>,
        expenses: List<Expense>,
        packagingItems: List<PackagingItem>,
        stockTransactions: List<StockTransaction>,
        nowMs: Long = System.currentTimeMillis()
    ): ProfitAndLossReport {
        val start = when (period) {
            ReportPeriod.DAILY -> DhakaDateTime.startOfTodayMs(nowMs)
            ReportPeriod.WEEKLY -> DhakaDateTime.startOfWeekMs(nowMs)
            ReportPeriod.MONTHLY -> DhakaDateTime.startOfMonthMs(nowMs)
            ReportPeriod.ALL_TIME -> 0L
        }
        return computeProfitAndLoss(
            orders = orders,
            returns = returns,
            expenses = expenses,
            packagingItems = packagingItems,
            stockTransactions = stockTransactions,
            startEpochMs = start,
            endEpochMs = Long.MAX_VALUE,
            periodLabel = period.label
        )
    }

    fun computeProfitAndLoss(
        orders: List<Order>,
        returns: List<ProductReturn>,
        expenses: List<Expense>,
        packagingItems: List<PackagingItem>,
        stockTransactions: List<StockTransaction>,
        startEpochMs: Long,
        endEpochMs: Long,
        periodLabel: String
    ): ProfitAndLossReport {
        val periodOrders = orders.filter { it.orderEpochMs in startEpochMs..endEpochMs }
        val cancelledOrders = periodOrders.filter { it.statusEnum == OrderStatus.CANCELLED }
        val activeOrders = periodOrders.filter { it.statusEnum != OrderStatus.CANCELLED }

        val grossSales = periodOrders.sumOf { (it.subtotal - it.discountAmount).coerceAtLeast(0.0) }
        val cancelledAdjustment = cancelledOrders.sumOf { (it.subtotal - it.discountAmount).coerceAtLeast(0.0) }

        val periodReturns = returns.filter { it.returnEpochMs in startEpochMs..endEpochMs }
        val returnedAdjustment = periodReturns.sumOf { it.refundAmount }
        val netSalesRevenue = (grossSales - cancelledAdjustment - returnedAdjustment).coerceAtLeast(0.0)

        // Product COGS for active orders minus resellable return cost restored back to inventory
        val rawOrderProductCost = activeOrders.sumOf { it.productCostTotal }
        val resellableCostRestored = periodReturns
            .filter { it.isReceived && it.isResellable }
            .sumOf { it.restoredCostAmount }
        val productCost = (rawOrderProductCost - resellableCostRestored).coerceAtLeast(0.0)

        // Packaging Material Cost: consumed in dispatched/packed orders (never restored on return) + wasted packaging
        val orderPackagingCost = periodOrders
            .filter { it.stockDeducted }
            .sumOf { it.packagingCostTotal }
        val wastedPackagingCost = stockTransactions
            .filter { it.txType == "PACKAGING_WASTE" && it.txEpochMs in startEpochMs..endEpochMs }
            .sumOf { it.totalValue }
        val packagingMaterialCost = orderPackagingCost + wastedPackagingCost

        val periodExpenses = expenses.filter { it.expenseEpochMs in startEpochMs..endEpochMs }
        val deliveryExpenses = periodExpenses
            .filter { it.categoryEnum == ExpenseCategory.COURIER }
            .sumOf { it.amount }
        val returnExpenses = periodExpenses
            .filter { it.categoryEnum == ExpenseCategory.RETURN_COURIER }
            .sumOf { it.amount }
        // Exclude PACKAGING cash purchases from P&L operating expenses to avoid double-counting with Packaging Material Cost
        val packagingCashPurchasesExcluded = periodExpenses
            .filter { it.categoryEnum == ExpenseCategory.PACKAGING }
            .sumOf { it.amount }
        val otherExpenses = periodExpenses
            .filter {
                it.categoryEnum != ExpenseCategory.COURIER &&
                    it.categoryEnum != ExpenseCategory.RETURN_COURIER &&
                    it.categoryEnum != ExpenseCategory.PACKAGING
            }
            .sumOf { it.amount }

        val netProfit = netSalesRevenue -
            productCost -
            packagingMaterialCost -
            deliveryExpenses -
            returnExpenses -
            otherExpenses

        val margin = if (netSalesRevenue > 0.0) (netProfit / netSalesRevenue) * 100.0 else 0.0

        return ProfitAndLossReport(
            periodLabel = periodLabel,
            grossSales = grossSales,
            cancelledAdjustment = cancelledAdjustment,
            returnedAdjustment = returnedAdjustment,
            netSalesRevenue = netSalesRevenue,
            productCost = productCost,
            packagingMaterialCost = packagingMaterialCost,
            deliveryExpenses = deliveryExpenses,
            returnExpenses = returnExpenses,
            otherExpenses = otherExpenses,
            packagingCashPurchasesExcluded = packagingCashPurchasesExcluded,
            netProfit = netProfit,
            profitMarginPercent = margin,
            activeOrderCount = activeOrders.size
        )
    }

    fun computeProductWiseProfit(
        products: List<Product>,
        orders: List<Order>,
        returns: List<ProductReturn>
    ): List<ProductProfitRow> {
        val productById = products.associateBy { it.id }
        val returnsByProduct = returns.groupBy { it.productId }

        data class Acc(
            var name: String = "",
            var sku: String = "",
            var soldQty: Long = 0L,
            var grossRev: Double = 0.0,
            var prodCost: Double = 0.0,
            var pkgCost: Double = 0.0
        )

        val map = mutableMapOf<String, Acc>()
        val activeOrders = orders.filter { it.statusEnum != OrderStatus.CANCELLED }

        for (order in activeOrders) {
            val orderDiscountRatio = if (order.subtotal > 0.0) {
                ((order.subtotal - order.discountAmount).coerceAtLeast(0.0)) / order.subtotal
            } else 1.0

            val totalUnitsInOrder = order.items.sumOf { it.quantity }.coerceAtLeast(1L)
            for (item in order.items) {
                val acc = map.getOrPut(item.productId) {
                    Acc(
                        name = item.productName,
                        sku = item.sku
                    )
                }
                acc.soldQty += item.quantity
                acc.grossRev += item.lineTotal * orderDiscountRatio
                acc.prodCost += item.quantity * item.unitCostPrice
                // Allocate order's packaging cost proportionally or from product's assigned packaging ratio
                val share = item.quantity.toDouble() / totalUnitsInOrder.toDouble()
                acc.pkgCost += order.packagingCostTotal * share
            }
        }

        return map.entries.map { (productId, acc) ->
            val prodReturns = returnsByProduct[productId].orEmpty()
            val retQty = prodReturns.sumOf { it.returnedQty }
            val retRefund = prodReturns.sumOf { it.refundAmount }
            val restoredCost = prodReturns.filter { it.isReceived && it.isResellable }.sumOf { it.restoredCostAmount }

            val netRev = (acc.grossRev - retRefund).coerceAtLeast(0.0)
            val netCost = (acc.prodCost - restoredCost).coerceAtLeast(0.0)
            val grossProfit = netRev - netCost - acc.pkgCost
            val margin = if (netRev > 0.0) (grossProfit / netRev) * 100.0 else 0.0
            val p = productById[productId]
            ProductProfitRow(
                productId = productId,
                productName = p?.name ?: acc.name,
                sku = p?.sku ?: acc.sku,
                unitsSold = acc.soldQty,
                unitsReturned = retQty,
                netUnits = (acc.soldQty - retQty).coerceAtLeast(0L),
                netRevenue = netRev,
                productCost = netCost,
                packagingCost = acc.pkgCost,
                grossProfit = grossProfit,
                marginPercent = margin
            )
        }.sortedByDescending { it.grossProfit }
    }
}
