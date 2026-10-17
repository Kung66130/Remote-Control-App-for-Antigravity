import { google } from 'googleapis';
import fs from 'fs';
import path from 'path';

const PACKAGE_NAME = 'com.kunxxslabs.antigravityremote';
const SERVICE_ACCOUNT_KEY = './service-account.json';
const AAB_FILE_PATH = './release/Antigravity-Remote-1.2.aab';

async function publishToPlayStore() {
    console.log('🚀 Starting Automated Play Store Handoff...');

    // 1. Authenticate
    const auth = new google.auth.GoogleAuth({
        keyFile: SERVICE_ACCOUNT_KEY,
        scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    const play = google.androidpublisher({ version: 'v3', auth });

    // 2. Create Edit Session
    console.log('📝 Creating Play Store Edit session...');
    const edit = await play.edits.insert({ packageName: PACKAGE_NAME });
    const editId = edit.data.id;
    console.log(`✅ Edit session created (ID: ${editId})`);

    try {
        // 3. Upload AAB Bundle
        console.log(`📦 Uploading signed AAB bundle (${AAB_FILE_PATH})...`);
        const bundleSize = fs.statSync(AAB_FILE_PATH).size;
        console.log(`File size: ${(bundleSize / (1024 * 1024)).toFixed(2)} MB`);

        const bundleRes = await play.edits.bundles.upload({
            packageName: PACKAGE_NAME,
            editId: editId,
            media: {
                mimeType: 'application/octet-stream',
                body: fs.createReadStream(AAB_FILE_PATH),
            },
        });

        const versionCode = bundleRes.data.versionCode;
        console.log(`🎉 AAB Uploaded Successfully! Version Code: ${versionCode}`);

        // 4. Assign to Production Track
        console.log(`🚀 Assigning version ${versionCode} to Production Track...`);
        await play.edits.tracks.update({
            packageName: PACKAGE_NAME,
            editId: editId,
            track: 'production',
            requestBody: {
                track: 'production',
                releases: [
                    {
                        versionCodes: [versionCode.toString()],
                        status: 'completed',
                        releaseNotes: [
                            {
                                language: 'th-TH',
                                text: 'เวอร์ชัน 1.2: แก้ไขปัญหาหน้าจอดำ (Blank Screen) หลังสแกน QR Code จากคอมพิวเตอร์, ปรับปรุงระบบแปลงและเชื่อมต่อ Antigravity URL อัตโนมัติ, เพิ่มปุ่ม Open in Browser สำหรับเปิดในเบราว์เซอร์หลัก',
                            },
                            {
                                language: 'en-US',
                                text: 'v1.2: Fix blank screen issue when connecting via PC QR code, automatically resolve and connect to Google Antigravity URL, and add Open in Browser shortcut in app bar.',
                            },
                        ],
                    },
                ],
            },
        });
        console.log('✅ Assigned to Internal Track successfully!');

        // 5. Commit Changes
        console.log('💾 Committing edit session to Google Play Console...');
        await play.edits.commit({
            packageName: PACKAGE_NAME,
            editId: editId,
        });

        console.log('\n==================================================');
        console.log('🎉 PUBLICATION HANDOFF COMPLETE!');
        console.log(`App Package: ${PACKAGE_NAME}`);
        console.log(`Track: Internal Testing (Version ${versionCode})`);
        console.log('==================================================\n');

    } catch (err) {
        console.error('❌ Error during release upload:', err.message || err);
        if (err.errors) {
            console.error('Details:', JSON.stringify(err.errors, null, 2));
        }
        console.log('🗑️ Aborting edit session...');
        await play.edits.delete({ packageName: PACKAGE_NAME, editId: editId }).catch(() => {});
    }
}

publishToPlayStore();
