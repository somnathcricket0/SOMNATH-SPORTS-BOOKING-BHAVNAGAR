const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_player_1";
const BOB_UID = "bob_player_2";
const ADMIN_UID = "admin_vishal";
const ADMIN_EMAIL = "vishaldhandhala@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildBookingPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    passCode: "SSB-10482A",
    playerName: "Rahul Jadeja",
    playerMobile: "9876543210",
    boxType: "CRICKET",
    date: "2026-11-25",
    startHour: 18,
    durationHours: 2,
    timeLabel: "06:00 PM - 08:00 PM",
    tierLabel: "Mon-Fri Night (6 PM - 6 AM)",
    totalAmount: 1300,
    advanceAmount: 650,
    balanceAmount: 650,
    utrNumber: "432918273645",
    status: "PENDING_VERIFICATION",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildSlotLockPayload(uid, bookingId, date, boxType, hour, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    bookingId,
    boxType,
    date,
    hour,
    status: "PENDING_VERIFICATION",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(
    unauthDb
      .collection("slot_locks")
      .where("status", "in", ["PENDING_VERIFICATION", "VERIFIED"])
      .get()
  );
});

test("Player profile: owner can create and update valid 10-digit profile; cross-user & invalid mobile fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Invalid mobile (<10 digits) fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      playerName: "Alice",
      mobileNumber: "98765",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      playerName: "Alice Gohil",
      mobileNumber: "9876543210",
      email: "alice@example.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      playerName: "Alice R. Gohil",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      playerName: "Hijack",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Atomic booking + slot_locks WriteBatch succeeds and blocks double-booking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  const bookingId = "booking_alice_1";
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("bookings").doc(bookingId), buildBookingPayload(ALICE_UID));
  batch.set(
    aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"),
    buildSlotLockPayload(ALICE_UID, bookingId, "2026-11-25", "CRICKET", 18)
  );
  batch.set(
    aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_19"),
    buildSlotLockPayload(ALICE_UID, bookingId, "2026-11-25", "CRICKET", 19)
  );
  await assertSucceeds(batch.commit());

  // Bob queries public slot_locks cleanly
  await assertSucceeds(
    bobDb
      .collection("slot_locks")
      .where("date", "==", "2026-11-25")
      .where("boxType", "==", "CRICKET")
      .where("status", "in", ["PENDING_VERIFICATION", "VERIFIED"])
      .get()
  );

  // Bob cannot read Alice's private booking PII
  await assertFails(bobDb.collection("bookings").doc(bookingId).get());
  await assertFails(bobDb.collection("bookings").get());

  // Bob tries to double-book hour 19 (already locked) -> must FAIL
  const bobBookingId = "booking_bob_overlap";
  const conflictBatch = bobDb.batch();
  conflictBatch.set(
    bobDb.collection("bookings").doc(bobBookingId),
    buildBookingPayload(BOB_UID, { startHour: 19, durationHours: 1, totalAmount: 700, advanceAmount: 350, balanceAmount: 350 })
  );
  conflictBatch.set(
    bobDb.collection("slot_locks").doc("2026-11-25_CRICKET_19"),
    buildSlotLockPayload(BOB_UID, bobBookingId, "2026-11-25", "CRICKET", 19)
  );
  await assertFails(conflictBatch.commit());
});

test("Player cannot self-approve booking, inject id field, or query unfiltered bookings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  // Self-approval on create rejected
  const badBookingId = "booking_self_verify";
  const badBatch = aliceDb.batch();
  badBatch.set(
    aliceDb.collection("bookings").doc(badBookingId),
    buildBookingPayload(ALICE_UID, { status: "VERIFIED" })
  );
  badBatch.set(
    aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"),
    buildSlotLockPayload(ALICE_UID, badBookingId, "2026-11-25", "CRICKET", 18)
  );
  await assertFails(badBatch.commit());

  // Valid create
  const bookingId = "booking_alice_2";
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("bookings").doc(bookingId), buildBookingPayload(ALICE_UID));
  batch.set(
    aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"),
    buildSlotLockPayload(ALICE_UID, bookingId, "2026-11-25", "CRICKET", 18)
  );
  await assertSucceeds(batch.commit());

  // Filtered query by userId succeeds; unfiltered fails
  await assertSucceeds(aliceDb.collection("bookings").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("bookings").get());

  // Player cannot self-verify on update
  await assertFails(
    aliceDb.collection("bookings").doc(bookingId).update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Player CAN cancel while PENDING_VERIFICATION and delete their slot_lock
  const cancelBatch = aliceDb.batch();
  cancelBatch.update(aliceDb.collection("bookings").doc(bookingId), {
    status: "CANCELLED",
    updatedAt: new Date(Date.now() - 500),
  });
  cancelBatch.delete(aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"));
  await assertSucceeds(cancelBatch.commit());

  // Terminal state lock: cannot un-cancel
  await assertFails(
    aliceDb.collection("bookings").doc(bookingId).update({
      status: "PENDING_VERIFICATION",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

test("Admin verification: verified admin email can list & verify/reject; spoofed unverified email fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bookingId = "booking_for_admin";
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("bookings").doc(bookingId), buildBookingPayload(ALICE_UID));
  batch.set(
    aliceDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"),
    buildSlotLockPayload(ALICE_UID, bookingId, "2026-11-25", "CRICKET", 18)
  );
  await assertSucceeds(batch.commit());

  // Spoof attack: matching email but email_verified: false MUST FAIL
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("bookings").get());
  await assertFails(
    spoofDb.collection("bookings").doc(bookingId).update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Legitimate verified admin succeeds
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("bookings").get());

  const verifyBatch = adminDb.batch();
  verifyBatch.update(adminDb.collection("bookings").doc(bookingId), {
    status: "VERIFIED",
    adminNote: "UTR verified via Axis Bank",
    updatedAt: new Date(Date.now() - 500),
  });
  verifyBatch.update(adminDb.collection("slot_locks").doc("2026-11-25_CRICKET_18"), {
    status: "VERIFIED",
    updatedAt: new Date(Date.now() - 500),
  });
  await assertSucceeds(verifyBatch.commit());
});

test("Phone OTP authenticated user (no email) can create profile, booking, and act as admin via admins/{uid}", async () => {
  const phoneUid = "phone_player_98765";
  const phoneDb = testEnv.authenticatedContext(phoneUid, {
    phone_number: "+919876543210",
  }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    phoneDb.collection("users").doc(phoneUid).set({
      userId: phoneUid,
      playerName: "Phone Player",
      mobileNumber: "9876543210",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bookingId = "booking_phone_1";
  const batch = phoneDb.batch();
  batch.set(
    phoneDb.collection("bookings").doc(bookingId),
    buildBookingPayload(phoneUid, {
      boxType: "VOLLEYBALL",
      date: "2026-11-26",
      startHour: 19,
      durationHours: 1,
      totalAmount: 700,
      advanceAmount: 350,
      balanceAmount: 350,
    })
  );
  batch.set(
    phoneDb.collection("slot_locks").doc("2026-11-26_VOLLEYBALL_19"),
    buildSlotLockPayload(phoneUid, bookingId, "2026-11-26", "VOLLEYBALL", 19)
  );
  await assertSucceeds(batch.commit());

  // Non-admin phone user cannot list all bookings
  await assertFails(phoneDb.collection("bookings").get());

  // Seed /admins/{phoneUid} using rules-bypassed admin context
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(phoneUid).set({ role: "admin" });
  });

  // Now phoneUid has admin verification privileges
  await assertSucceeds(phoneDb.collection("bookings").get());
  await assertSucceeds(
    phoneDb.collection("bookings").doc(bookingId).update({
      status: "VERIFIED",
      adminNote: "Approved by phone admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
