const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProduct(userId) {
  return {
    userId,
    name: "Autonstyle Car Perfume",
    sku: "AS-PERF-01",
    category: "Interior",
    purchasePrice: 250,
    sellingPrice: 550,
    availableStock: 20,
    minStockLevel: 5,
    damagedStock: 0,
    assignedPackaging: [
      { packagingId: "pkg_1", packagingName: "Poly Bag", quantityPerUnit: 1 }
    ],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validOrder(userId, stockDeducted = false) {
  return {
    userId,
    invoiceNumber: "INV-20261114-0001",
    customerName: "Rahim Uddin",
    customerPhone: "01711000000",
    customerAddress: "Dhanmondi, Dhaka",
    orderDate: "14-11-2026",
    orderEpochMs: 1794699893000,
    items: [
      {
        productId: "prod_1",
        productName: "Autonstyle Car Perfume",
        sku: "AS-PERF-01",
        quantity: 3,
        unitSellingPrice: 550,
        unitCostPrice: 250,
        lineTotal: 1650,
        returnedQty: 0,
      }
    ],
    subtotal: 1650,
    discountAmount: 50,
    deliveryCharge: 80,
    totalAmount: 1680,
    status: stockDeducted ? "PACKED" : "PENDING",
    courierName: "Steadfast",
    trackingNumber: "SF-99881",
    deliveryNote: "",
    stockDeducted,
    productCostTotal: stockDeducted ? 750 : 0,
    packagingCostTotal: stockDeducted ? 45 : 0,
    packagingBreakdown: stockDeducted ? [{ packagingId: "pkg_1", packagingName: "Poly Bag", quantityUsed: 3, unitCost: 15, totalCost: 45 }] : [],
    returnedAmount: 0,
    returnedQtyTotal: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated caller cannot read or write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(validProduct(ALICE_UID)));
});

test("Cross-user isolation: Bob cannot read or write Alice's products or orders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(validProduct(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").set(validOrder(ALICE_UID)));
});

test("Shadow update & ghost field rejected on Product create/update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = { ...validProduct(ALICE_UID), isAdminOverride: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set(payload));
});

test("Invalid date format rejected on Order create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badOrder = { ...validOrder(ALICE_UID), orderDate: "2026-11-14" };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1").set(badOrder));
});

test("Order stockDeducted flag cannot be reverted once true", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1");
  await assertSucceeds(orderRef.set(validOrder(ALICE_UID, true)));
  await assertFails(orderRef.update({ stockDeducted: false, updatedAt: serverTimestamp() }));
  await assertSucceeds(orderRef.update({ status: "DISPATCHED", updatedAt: serverTimestamp() }));
});

test("Immutable ledger collections reject updates and deletes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("stockTransactions").doc("tx_1");
  await assertSucceeds(txRef.set({
    userId: ALICE_UID,
    itemType: "PRODUCT",
    itemId: "prod_1",
    itemName: "Autonstyle Car Perfume",
    txType: "PRODUCT_PURCHASE",
    quantityDelta: 10,
    stockBefore: 10,
    stockAfter: 20,
    unitCost: 250,
    totalValue: 2500,
    referenceId: "pur_1",
    referenceLabel: "PO-001",
    txDate: "14-11-2026",
    txEpochMs: 1794699893000,
    createdAt: serverTimestamp(),
  }));
  await assertFails(txRef.update({ quantityDelta: 99 }));
  await assertFails(txRef.delete());
});
