import { google } from 'googleapis';
import fs from 'fs';
import path from 'path';

const PACKAGE_NAME = 'com.kunxxslabs.antigravityremote';
const SERVICE_ACCOUNT_KEY = './service-account.json';
const AAB_FILE_PATH = './release/Antigravity-Remote-1.1.aab';

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

        // 4. Assign to Internal Testing Track
        console.log(`🚀 Assigning version ${versionCode} to Internal Testing Track...`);
        await play.edits.tracks.update({
            packageName: PACKAGE_NAME,
            editId: editId,
            track: 'internal',
            requestBody: {
                track: 'internal',
                releases: [
                    {
                        versionCodes: [versionCode.toString()],
                        status: 'completed',
                        releaseNotes: [
                            {
                                language: 'th-TH',
                                text: 'เวอร์ชัน 1.1: เพิ่มคู่มือการเชื่อมต่อ 3 ขั้นตอนตรงตาม Antigravity 2.0, หน้าต่างวางลิงก์เชื่อมต่อ (Paste Link Dialog), ปุ่มติดต่อเรา (Contact Us) และปรับปรุงระบบสลับภาษาให้สมบูรณ์ยิ่งขึ้น',
                            },
                            {
                                language: 'en-US',
                                text: 'v1.1: Added 3-step PC connection guide for Antigravity 2.0, Paste Link dialog with clipboard validation, Contact Us dialog, and bilingual support improvements.',
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
