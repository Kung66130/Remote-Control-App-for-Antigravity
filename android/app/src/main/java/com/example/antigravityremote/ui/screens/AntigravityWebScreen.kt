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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
                    // Native TTS Button (Speak / Stop)
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                ttsManager.stop()
                            } else {
                                val js = """
                                    (function() {
                                        var textToSpeak = "";

                                        // 1. Check if user highlighted any text
                                        var sel = window.getSelection().toString().trim();
                                        if (sel.length > 0) {
                                            textToSpeak = sel;
                                        } else {
                                            // 2. Query chat message bubbles & markdown elements
                                            var selectors = [
                                                '[data-message-author-role="assistant"]',
                                                '[data-message-author="assistant"]',
                                                '[data-role="model"]',
                                                '[data-role="assistant"]',
                                                '.model-response',
                                                '.assistant-message',
                                                '.prose',
                                                '.markdown-content',
                                                '[role="article"]',
                                                '.chat-bubble',
                                                '[class*="message"]',
                                                '[class*="bubble"]',
                                                '[class*="content"]',
                                                '[class*="markdown"]'
                                            ];

                                            for (var i = 0; i < selectors.length; i++) {
                                                var items = document.querySelectorAll(selectors[i]);
                                                for (var j = items.length - 1; j >= 0; j--) {
                                                    var item = items[j];
                                                    if (!item.closest('header, nav, button, input, textarea, form, [contenteditable="true"]')) {
                                                        var t = item.innerText ? item.innerText.trim() : "";
                                                        if (t.length > 5) {
                                                            textToSpeak = t;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (textToSpeak.length > 0) break;
                                            }

                                            // 3. Fallback: all paragraphs or text containers
                                            if (textToSpeak.length === 0) {
                                                var ps = document.querySelectorAll('p, div');
                                                for (var k = ps.length - 1; k >= 0; k--) {
                                                    var p = ps[k];
                                                    if (!p.closest('header, nav, button, input, textarea, form')) {
                                                        var pt = p.innerText ? p.innerText.trim() : "";
                                                        if (pt.length > 15) {
                                                            textToSpeak = pt;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (textToSpeak.length > 0 && window.AndroidTTS) {
                                            window.AndroidTTS.speak(textToSpeak);
                                        } else if (window.AndroidTTS) {
                                            window.AndroidTTS.speak("ยังไม่พบข้อความในหน้านี้ครับ");
                                        }
                                    })()
                                """.trimIndent()

                                webViewInstance?.evaluateJavascript(js, null)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
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
                            class AndroidTtsBridge {
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
                            }
                            addJavascriptInterface(AndroidTtsBridge(), "AndroidTTS")

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
