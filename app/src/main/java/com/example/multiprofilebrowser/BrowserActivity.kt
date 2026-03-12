package com.example.multiprofilebrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    // ============================================================
    // Variables
    // ============================================================
    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabBar: LinearLayout
    private lateinit var tvProfileName: TextView

    private lateinit var profile: Profile
    private var profileIndex: Int = 0

    // ============================================================
    // onCreate — Activity শুরু হলে এটা চলে
    // ============================================================
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        // Intent থেকে profile index নাও
        profileIndex = intent.getIntExtra("profile_index", 0)
        val profiles = ProfileManager.loadProfiles(this)
        profile = profiles[profileIndex]

        // Views connect করো
        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        tabBar = findViewById(R.id.tabBar)
        tvProfileName = findViewById(R.id.tvProfileName)

        tvProfileName.text = profile.name

        // WebView setup করো
        setupWebView()

        // Buttons setup করো
        setupButtons()

        // Tabs দেখাও
        refreshTabBar()

        // Active tab load করো
        loadActiveTab()
    }

    // ============================================================
    // WebView Setup — সত্যিকারের isolated profile এর জন্য
    // প্রতিটা profile এর আলাদা data directory ব্যবহার করে
    // ============================================================
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(false)
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        // প্রতিটা profile এর জন্য আলাদা storage path
        // এটাই সত্যিকারের isolation তৈরি করে
        val profileDataDir = "${applicationContext.dataDir}/profile_${profile.id}"
        settings.databasePath = "$profileDataDir/databases"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = ProgressBar.VISIBLE
                url?.let {
                    etUrl.setText(it)
                    updateActiveTabUrl(it)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = ProgressBar.GONE
                url?.let {
                    etUrl.setText(it)
                    updateActiveTabUrl(it)
                }
                // Tab title update করো
                view?.title?.let { title ->
                    updateActiveTabTitle(title)
                    refreshTabBar()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    updateActiveTabTitle(it)
                    refreshTabBar()
                }
            }
        }
    }

    // ============================================================
    // Buttons Setup
    // ============================================================
    private fun setupButtons() {
        // Back to profile list
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            saveAndFinish()
        }

        // নতুন tab
        findViewById<ImageButton>(R.id.btnNewTab).setOnClickListener {
            addNewTab()
        }

        // URL bar — Go button
        findViewById<ImageButton>(R.id.btnGo).setOnClickListener {
            loadUrl(etUrl.text.toString())
        }

        // URL bar — keyboard এ Go চাপলে
        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrl(etUrl.text.toString())
                true
            } else false
        }

        // Browser navigation buttons
        findViewById<ImageButton>(R.id.btnWebBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }

        findViewById<ImageButton>(R.id.btnWebForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }

        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            webView.reload()
        }

        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            loadUrl("https://www.google.com")
        }
    }

    // ============================================================
    // Tab Bar — top এ সব tabs দেখাও
    // ============================================================
    private fun refreshTabBar() {
        tabBar.removeAllViews()
        profile.tabs.forEachIndexed { index, tab ->
            val tabView = LayoutInflater.from(this)
                .inflate(R.layout.item_tab, tabBar, false)

            val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
            val btnClose = tabView.findViewById<ImageButton>(R.id.btnCloseTab)

            tabTitle.text = if (tab.title.length > 15) tab.title.substring(0, 15) + "..." else tab.title

            // Active tab highlight করো
            tabView.isSelected = tab.id == profile.activeTabId

            // Tab এ click করলে সেই tab load করো
            tabView.setOnClickListener {
                profile.activeTabId = tab.id
                loadActiveTab()
                refreshTabBar()
            }

            // X button চাপলে tab বন্ধ করো
            btnClose.setOnClickListener {
                closeTab(index)
            }

            tabBar.addView(tabView)
        }
    }

    // ============================================================
    // নতুন Tab যোগ করো
    // ============================================================
    private fun addNewTab() {
        val newTab = Tab(
            id = ProfileManager.generateId(),
            title = "New Tab",
            url = "https://www.google.com"
        )
        profile.tabs.add(newTab)
        profile.activeTabId = newTab.id
        refreshTabBar()
        loadActiveTab()
        saveProfiles()
    }

    // ============================================================
    // Tab বন্ধ করো
    // ============================================================
    private fun closeTab(index: Int) {
        if (profile.tabs.size <= 1) {
            Toast.makeText(this, "কমপক্ষে একটা tab থাকতে হবে", Toast.LENGTH_SHORT).show()
            return
        }
        profile.tabs.removeAt(index)
        // Active tab ঠিক করো
        if (profile.activeTabId == profile.tabs.getOrNull(index)?.id || profile.tabs.none { it.id == profile.activeTabId }) {
            profile.activeTabId = profile.tabs.last().id
        }
        refreshTabBar()
        loadActiveTab()
        saveProfiles()
    }

    // ============================================================
    // Active Tab এর URL load করো WebView এ
    // ============================================================
    private fun loadActiveTab() {
        val activeTab = profile.tabs.find { it.id == profile.activeTabId }
            ?: profile.tabs.firstOrNull()
            ?: return

        profile.activeTabId = activeTab.id
        etUrl.setText(activeTab.url)
        webView.loadUrl(activeTab.url)
    }

    // ============================================================
    // URL load করো — search query হলে Google এ search করো
    // ============================================================
    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(" ") || !input.contains(".") ->
                "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
            else -> "https://$input"
        }
        webView.loadUrl(url)
        // Keyboard বন্ধ করো
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
    }

    // ============================================================
    // Active tab এর URL update করো
    // ============================================================
    private fun updateActiveTabUrl(url: String) {
        profile.tabs.find { it.id == profile.activeTabId }?.url = url
        saveProfiles()
    }

    // ============================================================
    // Active tab এর title update করো
    // ============================================================
    private fun updateActiveTabTitle(title: String) {
        profile.tabs.find { it.id == profile.activeTabId }?.title = title
        saveProfiles()
    }

    // ============================================================
    // Profiles save করো
    // ============================================================
    private fun saveProfiles() {
        val profiles = ProfileManager.loadProfiles(this)
        if (profileIndex < profiles.size) {
            profiles[profileIndex] = profile
            ProfileManager.saveProfiles(this, profiles)
        }
    }

    // ============================================================
    // Save করে back যাও
    // ============================================================
    private fun saveAndFinish() {
        saveProfiles()
        finish()
    }

    // Back button চাপলে browser এ back যাও
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            saveAndFinish()
        }
    }
}