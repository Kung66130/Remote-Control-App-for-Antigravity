package com.example.antigravityremote.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.antigravityremote.util.TtsManager

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AntigravityWebScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val webViewState = rememberSaveable { android.os.Bundle() }
    var pageTitle by remember { mutableStateOf("Antigravity Remote") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    // Native Android Text-to-Speech Manager
    val ttsManager = remember(context) { TtsManager(context) }
    val isSpeaking by ttsManager.isSpeaking.collectAsState()

    // Shared Preferences for Auto-Read TTS
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("antigravity_prefs", android.content.Context.MODE_PRIVATE)
    }
    var isAutoReadEnabled by rememberSaveable {
        mutableStateOf(sharedPrefs.getBoolean("auto_read_tts", false))
    }

    val ttsScript = """
        (function() {
            if (window.__antigravityTtsBridgeInstalled) return;
            window.__antigravityTtsBridgeInstalled = true;

            var lastSpokenText = "";
            var debounceTimer = null;

            function getCleanText(el) {
                if (!el) return "";
                return (el.innerText || el.textContent || "").trim();
            }

            function isNoise(t) {
                if (!t) return true;
                var s = t.trim().toLowerCase();
                if (s.length < 3) return true;
                if (s === "google antigravity" || s === "antigravity" || s === "connected (online)" ||
                    s === "add context" || s === "media" || s === "mentions" || s === "actions") {
                    return true;
                }
                if (s.indexOf("google antigravity") === 0 && s.length < 40) return true;
                if (s.indexOf("antigravity") === 0 && s.length < 30) return true;
                if (s.indexOf("connected (online)") === 0) return true;
                if (s.indexOf("ask anything, @ to mention") === 0) return true;
                return false;
            }

            function extractLatestAssistantText() {
                // 1. Text selection / highlight in this frame
                var sel = window.getSelection ? window.getSelection().toString().trim() : "";
                if (sel.length > 0 && !isNoise(sel)) {
                    return sel;
                }

                // 2. Chat turns in Antigravity web UI (elements with class containing scroll-mt-4)
                var turns = document.querySelectorAll('[class*="group w-full scroll-mt-4"]');
                for (var i = turns.length - 1; i >= 0; i--) {
                    var turn = turns[i];
                    var ps = turn.querySelectorAll('p');
                    var collected = [];
                    var totalLen = 0;
                    for (var j = 0; j < ps.length; j++) {
                        var pt = getCleanText(ps[j]);
                        if (pt.length > 0 && !isNoise(pt)) {
                            collected.push(pt);
                            totalLen += pt.length;
                            // Option 1: Read only the main summary / first paragraph(s)
                            if (totalLen >= 80 || collected.length >= 2) {
                                break;
                            }
                        }
                    }
                    if (collected.length > 0) {
                        return collected.join(" ");
                    }

                    var selectTexts = turn.querySelectorAll('[class*="select-text"]');
                    for (var k = selectTexts.length - 1; k >= 0; k--) {
                        var st = getCleanText(selectTexts[k]);
                        if (st.length > 10 && !isNoise(st)) {
                            var firstPart = st.split(/\n\n|\r\n\r\n/)[0].trim();
                            return firstPart.length > 0 ? firstPart : st.substring(0, 200);
                        }
                    }
                }

                // 3. Fallback: first non-noise paragraph
                var allPs = document.querySelectorAll('p');
                for (var m = allPs.length - 1; m >= 0; m--) {
                    var p = allPs[m];
                    if (!p.closest('header, nav, button, input, textarea, form')) {
                        var text = getCleanText(p);
                        if (text.length > 15 && !isNoise(text)) {
                            return text;
                        }
                    }
                }

                return "";
            }

            function speakManual() {
                var text = extractLatestAssistantText();
                if (text && text.length > 0) {
                    if (window.AndroidTTS) {
                        window.AndroidTTS.speak(text);
                    }
                    return true;
                }
                return false;
            }

            function checkAutoRead() {
                if (!window.AndroidTTS || !window.AndroidTTS.isAutoReadEnabled()) return;
                var text = extractLatestAssistantText();
                if (text && text.length > 10 && text !== lastSpokenText) {
                    lastSpokenText = text;
                    window.AndroidTTS.speak(text);
                }
            }

            window.addEventListener('message', function(event) {
                if (event.data && event.data.type === 'ANTIGRAVITY_TTS_SPEAK') {
                    speakManual();
                }
            });

            setTimeout(function() {
                lastSpokenText = extractLatestAssistantText();
                var observer = new MutationObserver(function() {
                    if (!window.AndroidTTS || !window.AndroidTTS.isAutoReadEnabled()) return;
                    if (debounceTimer) clearTimeout(debounceTimer);
                    debounceTimer = setTimeout(checkAutoRead, 2200);
                });
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
                }
            }, 1500);

            window.__antigravitySpeakLatest = speakManual;
        })();
    """.trimIndent()

    // File / Image Picker Handler for WebChromeClient
    var uploadMessageCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (uploadMessageCallback == null) return@rememberLauncherForActivityResult

        val results: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val clipData = data?.clipData
            if (clipData != null && clipData.itemCount > 0) {
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
        } else {
            null
        }

        uploadMessageCallback?.onReceiveValue(results)
        uploadMessageCallback = null
    }

    // Intercept back button to navigate inside webview
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Remote"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLoading) Color(0xFFFF9800) else Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLoading) "กำลังเชื่อมต่อ..." else "Connected (Online)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (isLoading) Color(0xFFFF9800) else Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Auto-Read Toggle Button
                    IconButton(
                        onClick = {
                            isAutoReadEnabled = !isAutoReadEnabled
                            sharedPrefs.edit().putBoolean("auto_read_tts", isAutoReadEnabled).apply()
                            Toast.makeText(
                                context,
                                if (isAutoReadEnabled) "เปิดการอ่านออกเสียงอัตโนมัติ (Auto-Read) แล้ว" else "ปิดการอ่านอัตโนมัติแล้ว",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isAutoReadEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                                contentDescription = "Auto-Read Toggle",
                                tint = if (isAutoReadEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isAutoReadEnabled) "AUTO" else "MANUAL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoReadEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Native TTS Button (Manual Speak / Stop)
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                ttsManager.stop()
                            } else {
                                val triggerJs = """
                                    (function() {
                                        if (typeof window.__antigravitySpeakLatest === 'function') {
                                            window.__antigravitySpeakLatest();
                                        }
                                        var iframes = document.querySelectorAll('iframe');
                                        for (var i = 0; i < iframes.length; i++) {
                                            try {
                                                iframes[i].contentWindow.postMessage({ type: 'ANTIGRAVITY_TTS_SPEAK' }, '*');
                                            } catch(e) {}
                                        }
                                    })()
                                """.trimIndent()
                                webViewInstance?.evaluateJavascript(triggerJs, null)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Read Aloud",
                            tint = if (isSpeaking) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open in Browser",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 AntigravityRemote/1.0"
                            }

                            WebView.setWebContentsDebuggingEnabled(true)

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            // Javascript Interface for Native TTS integration
                            class AndroidTtsBridge(private val isAutoRead: () -> Boolean) {
                                @JavascriptInterface
                                fun speak(text: String) {
                                    android.util.Log.d("TtsManager", "AndroidTtsBridge.speak called (length: ${text.length}): ${text.take(80)}")
                                    (ctx as? Activity)?.runOnUiThread {
                                        ttsManager.speak(text)
                                    }
                                }

                                @JavascriptInterface
                                fun stop() {
                                    android.util.Log.d("TtsManager", "AndroidTtsBridge.stop called")
                                    (ctx as? Activity)?.runOnUiThread {
                                        ttsManager.stop()
                                    }
                                }

                                @JavascriptInterface
                                fun isAutoReadEnabled(): Boolean {
                                    return isAutoRead()
                                }
                            }
                            addJavascriptInterface(AndroidTtsBridge { isAutoReadEnabled }, "AndroidTTS")

                            // Inject cross-origin script into ALL frames (including chat iframe)
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                                try {
                                    WebViewCompat.addDocumentStartJavaScript(
                                        this,
                                        ttsScript,
                                        setOf("*")
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("AntigravityWebScreen", "Failed to add DocumentStartJavaScript", e)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                        pageTitle = title
                                    }
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    uploadMessageCallback?.onReceiveValue(null)
                                    uploadMessageCallback = filePathCallback

                                    val intent = try {
                                        fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "image/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    } catch (e: Exception) {
                                        Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "image/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                        }
                                    }

                                    return try {
                                        fileChooserLauncher.launch(intent)
                                        true
                                    } catch (e: Exception) {
                                        uploadMessageCallback?.onReceiveValue(null)
                                        uploadMessageCallback = null
                                        false
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    view?.saveState(webViewState)
                                    CookieManager.getInstance().flush()
                                    view?.evaluateJavascript(ttsScript, null)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }
                            }

                            if (!webViewState.isEmpty) {
                                restoreState(webViewState)
                            } else {
                                loadUrl(url)
                            }
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating Speech Player Banner when TTS is active
            AnimatedVisibility(
                visible = isSpeaking,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "กำลังอ่านออกเสียงให้ฟัง...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ระบบอ่านออกเสียงผ่านลำโพงมือถือ (TH / EN)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = { ttsManager.stop() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE53935).copy(alpha = 0.15f),
                                contentColor = Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("หยุด", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    webViewInstance?.saveState(webViewState)
                    CookieManager.getInstance().flush()
                    ttsManager.shutdown()
                }
            }
        }
    }
}
