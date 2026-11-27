"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const express = require("express");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");

const PORT = parseInt(process.env.PORT || "3000", 10);
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "gen-lang-client-0240859276";
const FIRESTORE_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-tradepro-64a5ed74-1b3d-4e46-a3b4-4c4b2c0155e2";
const PUBLIC_BACKEND_BASE_URL = (process.env.PUBLIC_BACKEND_BASE_URL || "").replace(/\/+$/, "");

function loadServiceAccountCredential() {
  const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  const credsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;

  if (rawJson && rawJson.trim().length > 0) {
    try {
      const trimmed = rawJson.trim();
      const decoded = trimmed.startsWith("{")
        ? trimmed
        : Buffer.from(trimmed, "base64").toString("utf8");
      const parsed = JSON.parse(decoded);
      if (!parsed.client_email || !parsed.private_key) {
        throw new Error("Missing client_email or private_key in FIREBASE_SERVICE_ACCOUNT_JSON");
      }
      return admin.credential.cert(parsed);
    } catch (err) {
      console.error(
        "[FATAL] Invalid FIREBASE_SERVICE_ACCOUNT_JSON environment variable:",
        err.message
      );
      process.exit(1);
    }
  }

  if (credsPath && credsPath.trim().length > 0) {
    const resolved = path.resolve(credsPath.trim());
    if (!fs.existsSync(resolved)) {
      console.error(
        `[FATAL] GOOGLE_APPLICATION_CREDENTIALS file not found at: ${resolved}`
      );
      process.exit(1);
    }
    try {
      const parsed = JSON.parse(fs.readFileSync(resolved, "utf8"));
      return admin.credential.cert(parsed);
    } catch (err) {
      console.error(
        `[FATAL] Failed to parse GOOGLE_APPLICATION_CREDENTIALS JSON at ${resolved}:`,
        err.message
      );
      process.exit(1);
    }
  }

  console.error(
    "[FATAL] Missing Firebase Admin SDK credentials. Set FIREBASE_SERVICE_ACCOUNT_JSON (raw or base64 JSON) or GOOGLE_APPLICATION_CREDENTIALS (path to service account JSON). applicationDefault() fallback is intentionally disabled."
  );
  process.exit(1);
}

const credential = loadServiceAccountCredential();
const firebaseApp = admin.initializeApp({
  credential,
  projectId: FIREBASE_PROJECT_ID,
});

const db = getFirestore(firebaseApp, FIRESTORE_DATABASE_ID);

const app = express();
app.disable("x-powered-by");

app.use(
  express.json({
    limit: "256kb",
    verify: (req, _res, buf) => {
      req.rawBody = buf;
    },
  })
);

function isSafeOrderId(id) {
  return typeof id === "string" && /^[a-zA-Z0-9_-]{4,128}$/.test(id);
}

async function getGatewaySecrets() {
  let firestoreSecrets = {};
  try {
    const snap = await db.collection("admin_secrets").doc("famgateway").get();
    if (snap.exists) {
      firestoreSecrets = snap.data() || {};
    }
  } catch (err) {
    console.warn("[WARN] Could not read /admin_secrets/famgateway:", err.message);
  }

  return {
    apiKey: (firestoreSecrets.apiKey || process.env.FAMGATEWAY_API_KEY || "").trim(),
    webhookSecret: (
      firestoreSecrets.webhookSecret ||
      process.env.FAMGATEWAY_WEBHOOK_SECRET ||
      ""
    ).trim(),
    backendUrl: (firestoreSecrets.backendUrl || PUBLIC_BACKEND_BASE_URL || "").trim().replace(/\/+$/, ""),
  };
}

async function requireFirebaseUser(req, res, next) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Missing Authorization Bearer token" });
  }
  const idToken = authHeader.slice(7).trim();
  if (!idToken) {
    return res.status(401).json({ error: "Empty Firebase ID token" });
  }
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    req.user = decoded;
    return next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired Firebase ID token" });
  }
}

async function activateProSubscriptionTx(orderId, utr = "") {
  const nowMillis = Date.now();
  const orderRef = db.collection("payment_orders").doc(orderId);

  return db.runTransaction(async (tx) => {
    const orderSnap = await tx.get(orderRef);
    if (!orderSnap.exists) {
      throw new Error("Order not found");
    }
    const order = orderSnap.data();
    const userRef = db.collection("users").doc(order.uid);
    const userSnap = await tx.get(userRef);

    if (order.status === "PAID") {
      const existingUser = userSnap.exists ? userSnap.data() : {};
      return {
        alreadyPaid: true,
        uid: order.uid,
        planId: order.planId,
        planName: order.planName,
        planStartedAtMillis: existingUser.planStartedAtMillis || order.verifiedAtMillis || nowMillis,
        planExpiresAtMillis: existingUser.planExpiresAtMillis || nowMillis,
      };
    }

    const existingUser = userSnap.exists ? userSnap.data() : {};
    const currentExpiry = Number(existingUser.planExpiresAtMillis || 0);
    const startBase = currentExpiry > nowMillis ? currentExpiry : nowMillis;
    const validityDays = Math.max(1, Number(order.validityDays || 30));
    const planStartedAtMillis =
      existingUser.subscriptionActive && existingUser.planStartedAtMillis
        ? Number(existingUser.planStartedAtMillis)
        : nowMillis;
    const planExpiresAtMillis = startBase + validityDays * 86400000;

    tx.update(orderRef, {
      status: "PAID",
      utr: String(utr || order.utr || ""),
      verifiedAtMillis: nowMillis,
    });

    if (userSnap.exists) {
      tx.update(userRef, {
        subscriptionActive: true,
        adFree: true,
        planId: order.planId,
        planName: order.planName,
        planStartedAtMillis,
        planExpiresAtMillis,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } else {
      tx.set(userRef, {
        userId: order.uid,
        displayName: "Pro Trader",
        email: "",
        photoUrl: "",
        virtualCash: 100000.0,
        initialCash: 100000.0,
        totalRealizedPnl: 0.0,
        totalTrades: 0,
        winningTrades: 0,
        portfolioResetsUsed: 0,
        preferredTheme: "DARK_OLED",
        watchlist: ["RELIANCE", "TCS", "NIFTY50", "GOLD_24K", "INFY"],
        subscriptionActive: true,
        adFree: true,
        planId: order.planId,
        planName: order.planName,
        planStartedAtMillis,
        planExpiresAtMillis,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    const leaderboardRef = db.collection("leaderboard_entries").doc(order.uid);
    const lbSnap = await tx.get(leaderboardRef);
    if (lbSnap.exists) {
      tx.update(leaderboardRef, {
        proMember: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    return {
      alreadyPaid: false,
      uid: order.uid,
      planId: order.planId,
      planName: order.planName,
      planStartedAtMillis,
      planExpiresAtMillis,
    };
  });
}

app.get("/health", async (_req, res) => {
  const secrets = await getGatewaySecrets();
  res.json({
    ok: true,
    service: "aurum-trade-famgateway-backend",
    projectId: FIREBASE_PROJECT_ID,
    databaseId: FIRESTORE_DATABASE_ID,
    famgatewayConfigured: Boolean(secrets.apiKey),
    webhookSecretConfigured: Boolean(secrets.webhookSecret),
    timestamp: new Date().toISOString(),
  });
});

app.get("/payment-complete", (_req, res) => {
  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.send(
    `<!doctype html><html><body style="background:#05070D;color:#F5C451;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center"><div><h2>Payment Submitted</h2><p style="color:#E2E8F0">Verifying your Pro subscription automatically...</p></div></body></html>`
  );
});

app.post("/api/famgateway/create-order", requireFirebaseUser, async (req, res) => {
  try {
    const uid = req.user.uid;
    const planId = typeof req.body?.planId === "string" ? req.body.planId.trim() : "";
    if (!isSafeOrderId(planId)) {
      return res.status(400).json({ error: "Invalid planId" });
    }

    // Server-authoritative pricing: strictly read exact priceInr & validityDays from Firestore (never trust client-submitted values)
    const planSnap = await db.collection("subscription_plans").doc(planId).get();
    if (!planSnap.exists) {
      return res.status(404).json({ error: "Subscription plan not found" });
    }
    const plan = planSnap.data();
    if (plan.active !== true) {
      return res.status(400).json({ error: "Selected subscription plan is currently inactive" });
    }

    const amountInr = Number(plan.priceInr);
    const validityDays = Number(plan.validityDays);
    if (!Number.isFinite(amountInr) || amountInr < 1 || amountInr > 100000) {
      return res.status(400).json({ error: "Invalid plan price configuration in Firestore" });
    }
    if (!Number.isInteger(validityDays) || validityDays < 1 || validityDays > 3650) {
      return res.status(400).json({ error: "Invalid plan validityDays configuration in Firestore" });
    }

    const secrets = await getGatewaySecrets();
    if (!secrets.apiKey) {
      return res.status(503).json({
        error: "FamGateway API Key is not configured. Admin must configure /admin_secrets/famgateway or FAMGATEWAY_API_KEY.",
      });
    }

    const orderId = `AT_${Date.now()}_${crypto.randomBytes(4).toString("hex")}`;
    const baseUrl =
      secrets.backendUrl ||
      `${req.protocol}://${req.get("host")}`;

    const fgPayload = {
      amount: Number(amountInr.toFixed(2)),
      order_id: orderId,
      customer_name: req.user.name || "Aurum Trader",
      customer_email: req.user.email || `${uid}@aurumtrade.app`,
      redirect_url: `${baseUrl}/payment-complete?order_id=${encodeURIComponent(orderId)}`,
      webhook_url: `${baseUrl}/famgateway-webhook`,
    };

    const fgResponse = await fetch("https://famgateway.in/api/create-order", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Api-Key": secrets.apiKey,
      },
      body: JSON.stringify(fgPayload),
    });

    const fgRaw = await fgResponse.text();
    let fgJson = {};
    try {
      fgJson = JSON.parse(fgRaw);
    } catch {
      return res.status(502).json({ error: "Invalid response from FamGateway create-order" });
    }

    const checkoutUrl =
      fgJson.checkout_url ||
      fgJson.payment_url ||
      fgJson.data?.checkout_url ||
      fgJson.result?.payment_url ||
      "";

    if (!fgResponse.ok || !checkoutUrl) {
      return res.status(502).json({
        error: fgJson.message || fgJson.error || "FamGateway failed to create checkout URL",
      });
    }

    await db.collection("payment_orders").doc(orderId).set({
      orderId,
      uid,
      planId,
      planName: String(plan.name || "Pro Plan"),
      amountInr,
      validityDays,
      status: "PENDING",
      checkoutUrl,
      utr: "",
      createdAtMillis: Date.now(),
      verifiedAtMillis: 0,
    });

    return res.json({
      orderId,
      checkout_url: checkoutUrl,
      amountInr,
      planName: String(plan.name || "Pro Plan"),
      validityDays,
    });
  } catch (err) {
    console.error("[ERROR] create-order:", err);
    return res.status(500).json({ error: "Failed to create FamGateway order" });
  }
});

app.get("/api/famgateway/status", requireFirebaseUser, async (req, res) => {
  try {
    const uid = req.user.uid;
    const orderId = typeof req.query.orderId === "string" ? req.query.orderId.trim() : "";
    if (!isSafeOrderId(orderId)) {
      return res.status(400).json({ error: "Invalid orderId" });
    }

    const orderSnap = await db.collection("payment_orders").doc(orderId).get();
    if (!orderSnap.exists) {
      return res.status(404).json({ error: "Order not found" });
    }
    const order = orderSnap.data();
    if (order.uid !== uid) {
      return res.status(403).json({ error: "Forbidden: order belongs to another user" });
    }

    if (order.status === "PAID") {
      const userSnap = await db.collection("users").doc(uid).get();
      const user = userSnap.exists ? userSnap.data() : {};
      return res.json({
        orderId,
        status: "PAID",
        verified: true,
        planId: order.planId,
        planName: order.planName,
        planStartedAtMillis: user.planStartedAtMillis || order.verifiedAtMillis || Date.now(),
        planExpiresAtMillis: user.planExpiresAtMillis || Date.now(),
      });
    }

    const secrets = await getGatewaySecrets();
    if (!secrets.apiKey) {
      return res.status(503).json({ error: "FamGateway API Key not configured" });
    }

    const verifyUrl = `https://famgateway.in/api/verify-order.php?order_id=${encodeURIComponent(orderId)}`;
    const fgResponse = await fetch(verifyUrl, {
      method: "GET",
      headers: {
        "X-Api-Key": secrets.apiKey,
      },
    });

    const fgRaw = await fgResponse.text();
    let fgJson = {};
    try {
      fgJson = JSON.parse(fgRaw);
    } catch {
      return res.json({ orderId, status: "PENDING", verified: false });
    }

    const rawStatus = String(
      fgJson.status || fgJson.payment_status || fgJson.data?.status || ""
    ).toUpperCase();

    const isPaid =
      rawStatus === "SUCCESS" ||
      rawStatus === "PAID" ||
      rawStatus === "COMPLETED" ||
      fgJson.paid === true;

    if (!isPaid) {
      return res.json({
        orderId,
        status: rawStatus === "FAILED" ? "FAILED" : "PENDING",
        verified: false,
      });
    }

    const paidAmount = fgJson.amount ?? fgJson.data?.amount;
    if (paidAmount !== undefined && Number(paidAmount) < Number(order.amountInr)) {
      return res.status(400).json({ error: "Verified payment amount is less than order amount" });
    }

    const activation = await activateProSubscriptionTx(orderId, fgJson.utr || fgJson.data?.utr || "");
    return res.json({
      orderId,
      status: "PAID",
      verified: true,
      planId: activation.planId,
      planName: activation.planName,
      planStartedAtMillis: activation.planStartedAtMillis,
      planExpiresAtMillis: activation.planExpiresAtMillis,
    });
  } catch (err) {
    console.error("[ERROR] verify status:", err);
    return res.status(500).json({ error: "Failed to verify payment status" });
  }
});

app.post("/famgateway-webhook", async (req, res) => {
  try {
    const secrets = await getGatewaySecrets();
    if (!secrets.webhookSecret) {
      return res.status(503).json({ error: "Webhook secret is not configured" });
    }

    const providedSigRaw =
      req.headers["x-famgateway-signature"] ||
      req.headers["x-webhook-signature"] ||
      "";
    const providedSig = String(providedSigRaw).replace(/^sha256=/i, "").trim().toLowerCase();

    if (!providedSig || !req.rawBody) {
      return res.status(401).json({ error: "Missing HMAC signature" });
    }

    const expectedSig = crypto
      .createHmac("sha256", secrets.webhookSecret)
      .update(req.rawBody)
      .digest("hex")
      .toLowerCase();

    const providedBuf = Buffer.from(providedSig, "utf8");
    const expectedBuf = Buffer.from(expectedSig, "utf8");
    if (
      providedBuf.length !== expectedBuf.length ||
      !crypto.timingSafeEqual(providedBuf, expectedBuf)
    ) {
      return res.status(401).json({ error: "Invalid HMAC-SHA256 webhook signature" });
    }

    const body = req.body || {};
    const orderId = String(body.order_id || body.orderId || "").trim();
    const status = String(body.status || body.event || "").toUpperCase();
    const utr = String(body.utr || "");

    if (!isSafeOrderId(orderId)) {
      return res.status(400).json({ error: "Invalid order_id" });
    }

    if (status !== "SUCCESS" && status !== "PAID" && status !== "COMPLETED" && status !== "PAYMENT.SUCCESS") {
      return res.json({ received: true, ignored: true, status });
    }

    const orderSnap = await db.collection("payment_orders").doc(orderId).get();
    if (!orderSnap.exists) {
      return res.status(404).json({ error: "Order not found" });
    }
    const order = orderSnap.data();
    if (body.amount !== undefined && Number(body.amount) < Number(order.amountInr)) {
      return res.status(400).json({ error: "Paid amount mismatch" });
    }

    const activation = await activateProSubscriptionTx(orderId, utr);
    return res.json({ received: true, verified: true, ...activation });
  } catch (err) {
    console.error("[ERROR] famgateway-webhook:", err);
    return res.status(500).json({ error: "Webhook processing failed" });
  }
});

app.listen(PORT, () => {
  console.log(
    `Aurum Trade FamGateway backend listening on port ${PORT} (project=${FIREBASE_PROJECT_ID}, db=${FIRESTORE_DATABASE_ID})`
  );
});
