# Privacy Policy for CookingUp 🔒

**Last Updated: May 20, 2024**

CookingUp is built with a **Privacy-First** philosophy. We believe your data belongs to you and should never leave your device without your explicit action.

## 1. 100% Offline-First
CookingUp is designed to work entirely offline. 
- The application **does not have permission to access the internet** (`android.permission.INTERNET` is strictly forbidden in our source code).
- No data is ever synced to a cloud, server, or third-party service.

## 2. No Data Collection
We do not collect, store, or transmit any personal information.
- **No Analytics:** We do not use Google Analytics, Firebase, or any other telemetry SDKs.
- **No Crash Reporting:** We do not send crash logs to any external servers.
- **No Accounts:** You do not need to create an account or provide an email address to use the app.

## 3. Local Storage
All your recipes, images, and settings are stored locally on your device using:
- **Room Database:** An encrypted local database.
- **Scoped Storage:** We only access the files you explicitly select or export.

## 4. User-Initiated Sharing
The only way data leaves the app is when **you** choose to share it.
- When you use the "Share" feature, the app generates a `content://` URI for a `.curcp` or `.culbr` file.
- This data is handed over to the Android system's `Intent.createChooser`, allowing you to pick exactly which app (e.g., Signal, Email, etc.) receives the file.

## 5. Permissions
- **Camera:** Used only if you choose to scan a physical recipe via OCR or take a food photo.
- **Storage/Media:** Used only to save or load your own recipe files and images.

## 6. Open Source
CookingUp is Open Source software licensed under the **GPL-3.0**. You or any security auditor can verify our privacy claims by reviewing the source code on GitHub.

## 7. Contact
Since we don't collect your data, we have no way to contact you. If you have questions, please open an issue on our [GitHub Repository](https://github.com/io-github-cookingup/CookingUp).
