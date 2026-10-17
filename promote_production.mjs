import { google } from 'googleapis';

const PACKAGE_NAME = 'com.kunxxslabs.antigravityremote';
const SERVICE_ACCOUNT_KEY = './service-account.json';

async function promoteToProduction() {
    console.log('🚀 Promoting Version Code 2 to Production Track...');

    const auth = new google.auth.GoogleAuth({
        keyFile: SERVICE_ACCOUNT_KEY,
        scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    const play = google.androidpublisher({ version: 'v3', auth });

    const edit = await play.edits.insert({ packageName: PACKAGE_NAME });
    const editId = edit.data.id;
    console.log(`✅ Edit session created (ID: ${editId})`);

    try {
        console.log('🚀 Assigning version 2 to Production Track...');
        await play.edits.tracks.update({
            packageName: PACKAGE_NAME,
            editId: editId,
            track: 'production',
            requestBody: {
                track: 'production',
                releases: [
                    {
                        versionCodes: ['2'],
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
        console.log('✅ Assigned to Production Track successfully!');

        console.log('💾 Committing edit session (Sending for Review)...');
        await play.edits.commit({
            packageName: PACKAGE_NAME,
            editId: editId,
            changesNotSentForReview: false,
        });

        console.log('\n==================================================');
        console.log('🎉 SENT TO PRODUCTION & SUBMITTED FOR REVIEW SUCCESSFULLY!');
        console.log('Track: Production (Version Code 2)');
        console.log('==================================================\n');

    } catch (err) {
        console.error('❌ Error during promotion:', err.message || err);
        if (err.errors) {
            console.error('Details:', JSON.stringify(err.errors, null, 2));
        }
        console.log('🗑️ Aborting edit session...');
        await play.edits.delete({ packageName: PACKAGE_NAME, editId: editId }).catch(() => {});
    }
}

promoteToProduction();
