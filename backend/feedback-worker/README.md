# WaterMe feedback worker

This Worker receives feedback from the Android app and delivers it to `waterme.feedback@gmail.com` through Resend. The email API key stays on the Worker, not inside the APK.

## Setup

1. Create a Resend API key.
2. Verify a sender address or domain in Resend.
3. Copy `wrangler.toml.example` to `wrangler.toml`.
4. Set `FEEDBACK_FROM_EMAIL` in `wrangler.toml` to the verified sender, for example `WaterMe <feedback@your-domain.com>`.
5. Install the Worker dependencies:

```powershell
npm install
```

6. Log in to Cloudflare:

```powershell
npm run login
```

7. Add the Resend key as a Worker secret. Replace `re_xxxxxxxxx` with your real Resend API key when prompted:

```powershell
npm run secret:resend
```

Do not paste the API key into `worker.js`, `wrangler.toml`, or any Android file.

8. Deploy the Worker:

```powershell
npm run deploy
```

9. Configure the Android build with the deployed endpoint:

```powershell
.\gradlew.bat :app:assembleDebug -PwaterMeFeedbackEndpoint=https://your-worker.your-subdomain.workers.dev/feedback
```

For Android Studio, add this property to the local Gradle properties used for the build:

```properties
waterMeFeedbackEndpoint=https://your-worker.your-subdomain.workers.dev/feedback
```

Do not commit API keys or mailbox passwords. The Android app should only know the public HTTPS endpoint.
