const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_sujit";
const ADMIN_EMAIL = "sujitmehta696@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function aliceDb() {
  return testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
}

function bobDb() {
  return testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
}

function adminDb() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
}

function spoofedUnverifiedAdminDb() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
}

function makeUserCreatePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Alice Trader",
    email: "alice@example.com",
    photoUrl: "",
    virtualCash: 100000.0,
    initialCash: 100000.0,
    totalRealizedPnl: 0.0,
    totalTrades: 0,
    winningTrades: 0,
    portfolioResetsUsed: 0,
    preferredTheme: "DARK_OLED",
    watchlist: ["RELIANCE", "TCS", "GOLD_24K"],
    subscriptionActive: false,
    adFree: false,
    planId: "",
    planName: "",
    planStartedAtMillis: 0,
    planExpiresAtMillis: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("subscription_plans").get());
  await assertFails(unauth.collection("admin_secrets").doc("famgateway").get());
  await assertFails(unauth.collection("payment_orders").get());
  await assertFails(unauth.collection("leaderboard_entries").get());
});

test("Zero-Trust Subscription Lock: users can create free profile, update cash/watchlist<=5, NEVER modify subscriptionActive or planExpiresAtMillis", async () => {
  const db = aliceDb();
  const payload = makeUserCreatePayload(ALICE_UID);

  // Cannot create profile with subscriptionActive=true
  await assertFails(
    db.collection("users").doc(ALICE_UID).set(makeUserCreatePayload(ALICE_UID, { subscriptionActive: true }))
  );

  // Valid free profile creation succeeds
  await assertSucceeds(db.collection("users").doc(ALICE_UID).set(payload));

  // Valid cash & watchlist update succeeds
  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).update({
      virtualCash: 95000.0,
      watchlist: ["RELIANCE", "TCS", "INFY", "GOLD_24K", "NIFTY50"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Free user cannot exceed 5 watchlist items
  await assertFails(
    db.collection("users").doc(ALICE_UID).update({
      watchlist: ["A", "B", "C", "D", "E", "F"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Free user cannot unlock Midnight Gold theme
  await assertFails(
    db.collection("users").doc(ALICE_UID).update({
      preferredTheme: "MIDNIGHT_GOLD",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // CRITICAL: Client CANNOT self-activate Pro subscription or extend planExpiresAtMillis
  await assertFails(
    db.collection("users").doc(ALICE_UID).update({
      subscriptionActive: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    db.collection("users").doc(ALICE_UID).update({
      planExpiresAtMillis: Date.now() + 99999999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/list denied
  await assertFails(bobDb().collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb().collection("users").get());
});

test("Admin Secrets & Admin Email Verification: only verified sujitmehta696@gmail.com can access /admin_secrets and /users list", async () => {
  const admin = adminDb();
  const alice = aliceDb();
  const spoof = spoofedUnverifiedAdminDb();
  const now = new Date(Date.now() - 1000);

  // Admin can write /admin_secrets/famgateway
  await assertSucceeds(
    admin.collection("admin_secrets").doc("famgateway").set({
      apiKey: "fg_test_12345",
      webhookSecret: "whsec_test_12345",
      backendUrl: "https://aurum-backend.onrender.com",
      updatedAt: now,
    })
  );

  // Regular user & spoofed unverified admin CANNOT read or write /admin_secrets
  await assertFails(alice.collection("admin_secrets").doc("famgateway").get());
  await assertFails(alice.collection("admin_secrets").doc("famgateway").set({
    apiKey: "hacked",
    webhookSecret: "hacked",
    backendUrl: "https://evil.com",
    updatedAt: now,
  }));
  await assertFails(spoof.collection("admin_secrets").doc("famgateway").get());

  // Admin can list users; regular user cannot
  await assertSucceeds(admin.collection("users").get());
  await assertFails(alice.collection("users").get());
});

test("Positions, Leverage/Bracket Pro gating, Trade Journal, SIP Plans, and Price Alerts", async () => {
  const alice = aliceDb();
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(makeUserCreatePayload(ALICE_UID)));

  // Free user can open standard 1x position without SL/TP
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("positions").doc("RELIANCE").set({
      userId: ALICE_UID,
      symbol: "RELIANCE",
      name: "Reliance Industries",
      category: "STOCK",
      sector: "Energy",
      quantity: 10.0,
      avgPrice: 2850.0,
      leverage: 1,
      marginUsed: 28500.0,
      stopLossPrice: 0.0,
      takeProfitPrice: 0.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Free user CANNOT open 5x leveraged position or create SIP plan
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("positions").doc("TCS").set({
      userId: ALICE_UID,
      symbol: "TCS",
      name: "Tata Consultancy",
      category: "STOCK",
      sector: "IT",
      quantity: 5.0,
      avgPrice: 3900.0,
      leverage: 5,
      marginUsed: 3900.0,
      stopLossPrice: 3800.0,
      takeProfitPrice: 4100.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("sip_plans").doc("sip_1").set({
      userId: ALICE_UID,
      symbol: "NIFTY50",
      assetName: "Nifty 50 Index",
      amountInr: 5000.0,
      frequency: "MONTHLY",
      active: true,
      totalInvested: 0.0,
      installmentsCompleted: 0,
      lastExecutedAtMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Simulate backend Admin SDK activating Alice's Pro subscription
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      subscriptionActive: true,
      adFree: true,
      planId: "pro_monthly",
      planName: "Pro Monthly",
      planStartedAtMillis: Date.now() - 1000,
      planExpiresAtMillis: Date.now() + 30 * 86400000,
    });
  });

  // Now Alice CAN open 5x leveraged bracket position, SIP plan, Midnight Gold theme, and >5 watchlist!
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("positions").doc("TCS").set({
      userId: ALICE_UID,
      symbol: "TCS",
      name: "Tata Consultancy",
      category: "STOCK",
      sector: "IT",
      quantity: 5.0,
      avgPrice: 3900.0,
      leverage: 5,
      marginUsed: 3900.0,
      stopLossPrice: 3800.0,
      takeProfitPrice: 4100.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("sip_plans").doc("sip_1").set({
      userId: ALICE_UID,
      symbol: "NIFTY50",
      assetName: "Nifty 50 Index",
      amountInr: 5000.0,
      frequency: "MONTHLY",
      active: true,
      totalInvested: 0.0,
      installmentsCompleted: 0,
      lastExecutedAtMillis: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      preferredTheme: "MIDNIGHT_GOLD",
      watchlist: ["A", "B", "C", "D", "E", "F"],
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Leaderboard anti-spoofing & payment_orders read-only isolation", async () => {
  const alice = aliceDb();
  const bob = bobDb();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(makeUserCreatePayload(ALICE_UID)));

  // Free user cannot spoof proMember=true on leaderboard
  await assertFails(
    alice.collection("leaderboard_entries").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      netWorth: 105000.0,
      returnPct: 5.0,
      proMember: true,
      updatedAt: now,
    })
  );

  // Free user with proMember=false succeeds
  await assertSucceeds(
    alice.collection("leaderboard_entries").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      netWorth: 105000.0,
      returnPct: 5.0,
      proMember: false,
      updatedAt: now,
    })
  );

  // Clients cannot write payment_orders
  await assertFails(
    alice.collection("payment_orders").doc("ord_1").set({
      orderId: "ord_1",
      uid: ALICE_UID,
      status: "PAID",
    })
  );

  // Seed order via backend, confirm Alice can read hers and Bob cannot
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("payment_orders").doc("ord_1").set({
      orderId: "ord_1",
      uid: ALICE_UID,
      planId: "pro_monthly",
      planName: "Pro Monthly",
      amountInr: 199.0,
      validityDays: 30,
      status: "PAID",
      checkoutUrl: "https://famgateway.in/pay/ord_1",
      createdAtMillis: Date.now(),
    });
  });

  await assertSucceeds(alice.collection("payment_orders").doc("ord_1").get());
  await assertFails(bob.collection("payment_orders").doc("ord_1").get());
});

test("App Config & Referral Banner + Subscription Plan Management: users read-only, Admin writes with bounds & URL validation", async () => {
  const admin = adminDb();
  const alice = aliceDb();
  const now = new Date(Date.now() - 1000);

  const validConfig = {
    adMobEnabled: true,
    bannerAdUnitId: "ca-app-pub-3940256099942544/6300978111",
    rewardedAdUnitId: "ca-app-pub-3940256099942544/5224354917",
    startingVirtualCash: 100000.0,
    rewardedAdBonus: 25000.0,
    backendBaseUrl: "https://aurum-backend.onrender.com",
    enableReferralCard: true,
    referralCardTitle: "Open Free Demat & Earn Bonus",
    referralCardSubtitle: "Trade live NSE stocks with zero delivery brokerage",
    referralButtonLabel: "Open Offer",
    referralUrl: "https://partner.example.com/ref/aurum",
    updatedAt: now,
  };

  // Non-admin CANNOT write /app_config/global or /subscription_plans
  await assertFails(alice.collection("app_config").doc("global").set(validConfig));
  await assertFails(
    alice.collection("subscription_plans").doc("pro_monthly").set({
      name: "Free Hack Plan",
      priceInr: 1.0,
      validityDays: 365,
      discountBadge: "HACK",
      tagline: "Bypassed",
      active: true,
      sortOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin CANNOT write invalid referralUrl scheme (e.g., javascript:) or out-of-bounds title/price
  await assertFails(
    admin.collection("app_config").doc("global").set({
      ...validConfig,
      referralUrl: "javascript:alert(1)",
    })
  );
  await assertFails(
    admin.collection("app_config").doc("global").set({
      ...validConfig,
      referralCardTitle: "X".repeat(90),
    })
  );
  await assertFails(
    admin.collection("subscription_plans").doc("bad_plan").set({
      name: "Negative Plan",
      priceInr: 0.0,
      validityDays: 30,
      discountBadge: "",
      tagline: "",
      active: true,
      sortOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin CAN write & update valid /app_config/global and /subscription_plans
  await assertSucceeds(admin.collection("app_config").doc("global").set(validConfig));
  await assertSucceeds(
    admin.collection("subscription_plans").doc("pro_yearly").set({
      name: "Pro Yearly",
      priceInr: 999.0,
      validityDays: 365,
      discountBadge: "BEST VALUE",
      tagline: "Full year institutional access",
      active: true,
      sortOrder: 3,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("subscription_plans").doc("pro_yearly").update({
      priceInr: 899.0,
      discountBadge: "50% OFF",
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Authenticated user CAN read /app_config/global and /subscription_plans, CANNOT read other app_config doc IDs or update
  await assertSucceeds(alice.collection("app_config").doc("global").get());
  await assertFails(alice.collection("app_config").doc("other_doc").get());
  await assertFails(
    alice.collection("app_config").doc("global").update({
      enableReferralCard: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(alice.collection("subscription_plans").get());
});

test("Expired subscription is denied; active unexpired Pro unlocks pending_orders, watchlists, baskets, short/option positions, journalTag, and Neon theme", async () => {
  const alice = aliceDb();
  const bob = bobDb();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(makeUserCreatePayload(ALICE_UID)));

  // Free user CANNOT access Pro subcollections or NEON_TERMINAL theme
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      preferredTheme: "NEON_TERMINAL",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("pending_orders").doc("ord_1").set({
      userId: ALICE_UID,
      symbol: "RELIANCE",
      name: "Reliance Industries",
      category: "STOCK",
      sector: "Energy",
      side: "BUY",
      orderKind: "GTT",
      productType: "CNC",
      quantity: 2.0,
      limitPrice: 2800.0,
      triggerPrice: 2805.0,
      leverage: 1,
      stopLossPrice: 0.0,
      takeProfitPrice: 0.0,
      trailingSlDistance: 0.0,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("watchlists").doc("wl_2").set({
      userId: ALICE_UID,
      name: "Swing Setups",
      symbols: ["RELIANCE", "TCS"],
      sortOrder: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("baskets").doc("bsk_1").set({
      userId: ALICE_UID,
      name: "Nifty Leaders",
      items: ["RELIANCE|BUY|1|1", "TCS|BUY|1|1"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Expired Pro subscription (subscriptionActive=true, planExpiresAtMillis < now) MUST STILL BE DENIED
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      subscriptionActive: true,
      adFree: true,
      planId: "pro_monthly",
      planName: "Pro Monthly",
      planStartedAtMillis: Date.now() - 40 * 86400000,
      planExpiresAtMillis: Date.now() - 10 * 86400000,
    });
  });

  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      preferredTheme: "NEON_TERMINAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("pending_orders").doc("ord_exp").set({
      userId: ALICE_UID,
      symbol: "TCS",
      name: "Tata Consultancy",
      category: "STOCK",
      sector: "IT",
      side: "BUY",
      orderKind: "LIMIT",
      productType: "CNC",
      quantity: 1.0,
      limitPrice: 3900.0,
      triggerPrice: 0.0,
      leverage: 1,
      stopLossPrice: 0.0,
      takeProfitPrice: 0.0,
      trailingSlDistance: 0.0,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Active & unexpired Pro subscription unlocks everything
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      subscriptionActive: true,
      planExpiresAtMillis: Date.now() + 30 * 86400000,
    });
  });

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      preferredTheme: "NEON_TERMINAL",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("pending_orders").doc("ord_1").set({
      userId: ALICE_UID,
      symbol: "RELIANCE",
      name: "Reliance Industries",
      category: "STOCK",
      sector: "Energy",
      side: "BUY",
      orderKind: "GTT",
      productType: "MIS",
      quantity: 2.0,
      limitPrice: 2800.0,
      triggerPrice: 2805.0,
      leverage: 5,
      stopLossPrice: 2700.0,
      takeProfitPrice: 3000.0,
      trailingSlDistance: 25.0,
      status: "OPEN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("watchlists").doc("wl_2").set({
      userId: ALICE_UID,
      name: "Swing Setups",
      symbols: ["RELIANCE", "TCS", "INFY"],
      sortOrder: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("baskets").doc("bsk_1").set({
      userId: ALICE_UID,
      name: "Nifty Leaders",
      items: ["RELIANCE|BUY|1|1", "TCS|BUY|1|1", "HDFCBANK|BUY|2|1"],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("positions").doc("NIFTY24400CE").set({
      userId: ALICE_UID,
      symbol: "NIFTY24400CE",
      name: "NIFTY 24400 CE",
      category: "OPTION",
      sector: "F&O",
      positionSide: "LONG",
      quantity: 25.0,
      avgPrice: 142.5,
      leverage: 1,
      marginUsed: 3562.5,
      stopLossPrice: 0.0,
      takeProfitPrice: 0.0,
      trailingSlDistance: 0.0,
      highWaterPrice: 142.5,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("trades").doc("tr_1").set({
      userId: ALICE_UID,
      symbol: "RELIANCE",
      name: "Reliance Industries",
      category: "STOCK",
      sector: "Energy",
      side: "SELL",
      orderType: "SHORT_SELL",
      quantity: 5.0,
      executionPrice: 2950.0,
      totalValue: 14750.0,
      leverage: 5,
      realizedPnl: 0.0,
      stopLossPrice: 0.0,
      takeProfitPrice: 0.0,
      journalTag: "FOLLOWED_PLAN",
      notes: "MIS 5x Short",
      createdAt: now,
    })
  );
  // Pro user can update journalTag only, cannot tamper with executionPrice
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("trades").doc("tr_1").update({
      journalTag: "FOMO",
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("trades").doc("tr_1").update({
      realizedPnl: 99999.0,
    })
  );

  // Cross-user isolation on Pro subcollections
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("pending_orders").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("watchlists").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("baskets").get());
});

