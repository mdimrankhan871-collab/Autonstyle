package com.example.domain

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.Product
import com.example.data.model.ProductReturn
import com.example.util.BdtFormatter
import com.example.util.DhakaDateTime
import com.example.util.ReportBuilder
import com.example.util.ReportExporter
import com.example.util.ReportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingEngineTest {

    @Test
    fun profitAndLoss_followsFormulaWithoutDoubleCountingPackagingCashOutlays() {
        val now = 1_794_699_893_000L
        val orders = listOf(
            Order(
                id = "o1",
                invoiceNumber = "AS-1",
                orderDate = "14-11-2026",
                orderEpochMs = now,
                items = listOf(
                    OrderItem(
                        productId = "p1",
                        productName = "Car Perfume",
                        quantity = 3,
                        unitSellingPrice = 500.0,
                        unitCostPrice = 200.0,
                        lineTotal = 1500.0,
                        returnedQty = 1
                    )
                ),
                subtotal = 1500.0,
                discountAmount = 0.0,
                deliveryCharge = 80.0,
                totalAmount = 1580.0,
                status = OrderStatus.PARTIAL_RETURN.name,
                stockDeducted = true,
                productCostTotal = 600.0,
                packagingCostTotal = 30.0,
                returnedAmount = 500.0,
                returnedQtyTotal = 1
            )
        )
        val returns = listOf(
            ProductReturn(
                id = "r1",
                orderId = "o1",
                productId = "p1",
                returnedQty = 1,
                refundAmount = 500.0,
                restoredCostAmount = 200.0,
                returnCourierCharge = 60.0,
                isReceived = true,
                isResellable = true,
                returnDate = "14-11-2026",
                returnEpochMs = now
            )
        )
        val expenses = listOf(
            Expense(id = "e1", category = ExpenseCategory.COURIER.name, amount = 80.0, expenseEpochMs = now),
            Expense(id = "e2", category = ExpenseCategory.RETURN_COURIER.name, amount = 60.0, expenseEpochMs = now),
            Expense(id = "e3", category = ExpenseCategory.FACEBOOK_ADS.name, amount = 150.0, expenseEpochMs = now),
            // Cash spent buying packaging stock must NOT be double-counted alongside packagingMaterialCost
            Expense(id = "e4", category = ExpenseCategory.PACKAGING.name, amount = 500.0, expenseEpochMs = now)
        )

        val pnl = AccountingEngine.computeProfitAndLoss(
            orders = orders,
            returns = returns,
            expenses = expenses,
            packagingItems = emptyList<PackagingItem>(),
            stockTransactions = emptyList(),
            startEpochMs = 0L,
            endEpochMs = Long.MAX_VALUE,
            periodLabel = "Test"
        )

        // Net Sales = 1500 - 500 = 1000
        assertEquals(1000.0, pnl.netSalesRevenue, 0.01)
        // Net Product Cost = 600 - 200 (resellable) = 400
        assertEquals(400.0, pnl.productCost, 0.01)
        // Packaging Material Cost = 30 (never restored)
        assertEquals(30.0, pnl.packagingMaterialCost, 0.01)
        // Delivery = 80, Return = 60, Other = 150
        assertEquals(80.0, pnl.deliveryExpenses, 0.01)
        assertEquals(60.0, pnl.returnExpenses, 0.01)
        assertEquals(150.0, pnl.otherExpenses, 0.01)
        assertEquals(500.0, pnl.packagingCashPurchasesExcluded, 0.01)
        // Net Profit = 1000 - 400 - 30 - 80 - 60 - 150 = 280
        assertEquals(280.0, pnl.netProfit, 0.01)
    }

    @Test
    fun reportExporters_generateValidCsvAndXlsxBytes() {
        val table = ReportBuilder.build(
            type = ReportType.PRODUCT_STOCK,
            products = listOf(
                Product(id = "p1", name = "Car Perfume", sku = "AS-1", category = "Interior", purchasePrice = 200.0, sellingPrice = 500.0, availableStock = 10, minStockLevel = 3)
            ),
            productPurchases = emptyList(),
            packagingItems = emptyList(),
            packagingPurchases = emptyList(),
            orders = emptyList(),
            returns = emptyList(),
            expenses = emptyList()
        )
        val csv = ReportExporter.generateCsvBytes(table)
        val xlsx = ReportExporter.generateXlsxBytes(table)

        assertTrue(String(csv).contains("Car Perfume"))
        // XLSX starts with ZIP magic bytes PK (0x50, 0x4B)
        assertEquals(0x50.toByte(), xlsx[0])
        assertEquals(0x4B.toByte(), xlsx[1])
        assertEquals("৳1,250", BdtFormatter.format(1250.0))
        assertTrue(DhakaDateTime.isValidDateFormat("14-11-2026"))
    }
}
