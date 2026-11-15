package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AssignedPackaging
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PackagingItem
import com.example.data.model.Product
import com.example.util.DhakaDateTime
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AutonstyleRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun fullOrderPackagingAndReturnFlow_enforcesAtomicDeductionsAndNoPackagingRestore() = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID()}@autonstyle.com")
        val repo = AutonstyleRepository(firestore)
        val today = DhakaDateTime.todayString()

        // 1. Create packaging items: Poly Bag (৳5), Thank You Card (৳3), Sticker (৳2)
        val polyId = withTimeout(TIMEOUT_MS) {
            repo.savePackagingItem(
                uid,
                PackagingItem(name = "Poly Bag", purchasePrice = 5.0, availableQty = 50, purchasedQty = 50, minStockLevel = 10)
            ).getOrThrow()
        }
        val cardId = withTimeout(TIMEOUT_MS) {
            repo.savePackagingItem(
                uid,
                PackagingItem(name = "Thank You Card", purchasePrice = 3.0, availableQty = 50, purchasedQty = 50, minStockLevel = 10)
            ).getOrThrow()
        }
        val stickerId = withTimeout(TIMEOUT_MS) {
            repo.savePackagingItem(
                uid,
                PackagingItem(name = "Sticker", purchasePrice = 2.0, availableQty = 50, purchasedQty = 50, minStockLevel = 10)
            ).getOrThrow()
        }

        // 2. Create product Car Perfume with assigned packaging (1 each per unit)
        val productId = withTimeout(TIMEOUT_MS) {
            repo.saveProduct(
                uid,
                Product(
                    name = "Autonstyle Car Perfume",
                    sku = "AS-PERF-${UUID.randomUUID().toString().take(4)}",
                    category = "Interior",
                    purchasePrice = 200.0,
                    sellingPrice = 500.0,
                    availableStock = 10,
                    minStockLevel = 3,
                    assignedPackaging = listOf(
                        AssignedPackaging(polyId, "Poly Bag", 1),
                        AssignedPackaging(cardId, "Thank You Card", 1),
                        AssignedPackaging(stickerId, "Sticker", 1)
                    )
                )
            ).getOrThrow()
        }

        // 3. Record purchase (+5 units -> stock becomes 15)
        withTimeout(TIMEOUT_MS) {
            repo.recordProductPurchase(
                userId = uid,
                productId = productId,
                supplier = "AutoSupply BD",
                purchaseDate = today,
                quantity = 5,
                unitPrice = 200.0,
                totalCost = 1000.0,
                notes = "Restock"
            ).getOrThrow()
        }

        // 4. Create & Dispatch order for 3 Car Perfumes (deducts 3 products, 3 poly bags, 3 cards, 3 stickers)
        val orderId = withTimeout(TIMEOUT_MS) {
            repo.createOrder(
                userId = uid,
                invoiceNumber = "AS-TEST-001",
                customerName = "Karim Ahmed",
                customerPhone = "01711111111",
                customerAddress = "Banani, Dhaka",
                orderDate = today,
                items = listOf(
                    OrderItem(
                        productId = productId,
                        productName = "Autonstyle Car Perfume",
                        sku = "AS-PERF",
                        quantity = 3,
                        unitSellingPrice = 500.0,
                        unitCostPrice = 200.0,
                        lineTotal = 1500.0
                    )
                ),
                discountAmount = 0.0,
                deliveryCharge = 80.0,
                initialStatus = OrderStatus.PACKED,
                courierName = "Steadfast",
                trackingNumber = "SF123",
                deliveryNote = "",
                autoLogCourierExpense = true
            ).getOrThrow()
        }

        val productsAfterOrder = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == productId && it.availableStock == 12L } }
        }
        assertEquals(12L, productsAfterOrder.first { it.id == productId }.availableStock)

        val pkgAfterOrder = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePackagingItems(uid).first { list ->
                list.any { it.id == polyId && it.availableQty == 47L }
            }
        }
        assertEquals(47L, pkgAfterOrder.first { it.id == polyId }.availableQty)
        assertEquals(3L, pkgAfterOrder.first { it.id == polyId }.usedQty)
        assertEquals(47L, pkgAfterOrder.first { it.id == cardId }.availableQty)
        assertEquals(47L, pkgAfterOrder.first { it.id == stickerId }.availableQty)

        val ordersAfterPack = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeOrders(uid).first { list -> list.any { it.id == orderId && it.stockDeducted } }
        }
        val packedOrder = ordersAfterPack.first { it.id == orderId }
        assertEquals(600.0, packedOrder.productCostTotal, 0.01)
        assertEquals(30.0, packedOrder.packagingCostTotal, 0.01) // 3*(5+3+2) = 30

        // 5. Transitioning PACKED -> DISPATCHED must NOT duplicate stock deduction
        withTimeout(TIMEOUT_MS) {
            repo.transitionOrderStatus(uid, orderId, OrderStatus.DISPATCHED).getOrThrow()
        }
        val productsAfterDispatch = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { it.any { p -> p.id == productId } }
        }
        assertEquals(12L, productsAfterDispatch.first { it.id == productId }.availableStock)

        // 6. Process resellable return of 1 unit (product stock -> 13; packaging remains 47!)
        withTimeout(TIMEOUT_MS) {
            repo.processProductReturn(
                userId = uid,
                orderId = orderId,
                productId = productId,
                returnedQty = 1,
                reason = "Customer changed mind",
                returnCourierCharge = 60.0,
                isReceived = true,
                isResellable = true,
                returnDate = today
            ).getOrThrow()
        }

        val productsAfterReturn = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == productId && it.availableStock == 13L } }
        }
        assertEquals(13L, productsAfterReturn.first { it.id == productId }.availableStock)

        val pkgAfterReturn = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePackagingItems(uid).first { it.any { p -> p.id == polyId } }
        }
        assertEquals(47L, pkgAfterReturn.first { it.id == polyId }.availableQty)

        // 7. Process damaged return of 1 unit (availableStock stays 13, damagedStock becomes 1)
        withTimeout(TIMEOUT_MS) {
            repo.processProductReturn(
                userId = uid,
                orderId = orderId,
                productId = productId,
                returnedQty = 1,
                reason = "Broken cap in transit",
                returnCourierCharge = 0.0,
                isReceived = true,
                isResellable = false,
                returnDate = today
            ).getOrThrow()
        }
        val productsAfterDamaged = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { list -> list.any { it.id == productId && it.damagedStock == 1L } }
        }
        assertEquals(13L, productsAfterDamaged.first { it.id == productId }.availableStock)
        assertEquals(1L, productsAfterDamaged.first { it.id == productId }.damagedStock)

        // 8. Attempting excess/duplicate return of 2 units when only 1 remains must fail
        val overReturnResult = withTimeout(TIMEOUT_MS) {
            repo.processProductReturn(
                userId = uid,
                orderId = orderId,
                productId = productId,
                returnedQty = 2,
                reason = "Duplicate attempt",
                returnCourierCharge = 0.0,
                isReceived = true,
                isResellable = true,
                returnDate = today
            )
        }
        assertTrue(overReturnResult.isFailure)
    }

    @Test
    fun crossUserAndUnauthenticatedAccess_rejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@autonstyle.com")
        val repo = AutonstyleRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            repo.saveProduct(
                aliceUid,
                Product(name = "LED Headlight", sku = "AS-LED-1", category = "Lighting", purchasePrice = 1000.0, sellingPrice = 1800.0, availableStock = 5)
            ).getOrThrow()
        }

        // Bob cannot read Alice's products
        signInTestUser("bob_${UUID.randomUUID()}@autonstyle.com")
        var crossUserError: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProducts(aliceUid).first()
            }
        } catch (e: Exception) {
            crossUserError = e
        }
        assertNotNull(crossUserError)
        val crossFsErr = crossUserError!!.unwrapFirestoreException()
        assertNotNull(crossFsErr)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossFsErr!!.code)

        // Unauthenticated caller cannot read Alice's products
        auth.signOut()
        var unauthError: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProducts(aliceUid).first()
            }
        } catch (e: Exception) {
            unauthError = e
        }
        assertNotNull(unauthError)
        val unauthFsErr = unauthError!!.unwrapFirestoreException()
        assertNotNull(unauthFsErr)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthFsErr!!.code)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
