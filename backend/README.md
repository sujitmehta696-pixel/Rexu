# Aurum Trade Backend (FamGateway v2.0 + Firebase Admin SDK)

## Render Deployment
1. Root Directory: `backend`
2. Build Command: `npm install`
3. Start Command: `npm start`
4. Required Environment Variables:
   - `FIREBASE_SERVICE_ACCOUNT_JSON` (raw or base64 service account JSON) **or** `GOOGLE_APPLICATION_CREDENTIALS`
   - `FIREBASE_PROJECT_ID=gen-lang-client-0240859276`
   - `FIRESTORE_DATABASE_ID=ai-studio-android-tradepro-64a5ed74-1b3d-4e46-a3b4-4c4b2c0155e2`
   - `PUBLIC_BACKEND_BASE_URL=https://<your-service>.onrender.com`
   - Optional (can also be configured in Admin APK under Gateway): `FAMGATEWAY_API_KEY`, `FAMGATEWAY_WEBHOOK_SECRET`
