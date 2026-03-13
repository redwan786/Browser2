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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabBar: LinearLayout
    private lateinit var tvTabCount: TextView
    private lateinit var btnProfileIcon: TextView

    private lateinit var profile: Profile
    private var profileIndex: Int = 0

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        profileIndex = intent.getIntExtra("profile_index", 0)
        val profiles = ProfileManager.loadProfiles(this)
        profile = profiles[profileIndex]

        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progressBar = findViewById(R.id.progressBar)
        tabBar = findViewById(R.id.tabBar)
        tvTabCount = findViewById(R.id.tvTabCount)
        btnProfileIcon = findViewById(R.id.btnProfileIcon)

        // Profile icon এ প্রথম অক্ষর দেখাও
        btnProfileIcon.text = profile.name.first().toString().uppercase()

        setupWebView()
        setupButtons()
        refreshTabBar()
        loadActiveTab()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = ProgressBar.VISIBLE
                url?.let { etUrl.setText(it); updateActiveTabUrl(it) }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = ProgressBar.GONE
                url?.let { etUrl.setText(it); updateActiveTabUrl(it) }
                view?.title?.let { updateActiveTabTitle(it); refreshTabBar() }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { updateActiveTabTitle(it); refreshTabBar() }
            }
        }
    }

    private fun setupButtons() {
        // Back to profiles
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { saveAndFinish() }

        // New tab
        findViewById<ImageButton>(R.id.btnNewTab).setOnClickListener { addNewTab() }

        // URL go
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener { webView.reload() }

        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { loadUrl(etUrl.text.toString()); true }
            else false
        }

        // Navigation
        findViewById<ImageButton>(R.id.btnWebBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btnWebForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btnHome).setOnClickListener {
            loadUrl("https://www.google.com")
        }

        // Tab count button — tab list দেখাও
        findViewById<FrameLayout>(R.id.btnTabCount).setOnClickListener {
            Toast.makeText(this, "${profile.tabs.size} tabs open", Toast.LENGTH_SHORT).show()
        }

        // 3 dot menu
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("New Tab")
            popup.menu.add("Reload")
            popup.menu.add("Back to Profiles")
            popup.setOnMenuItemClickListener {
                when (it.title) {
                    "New Tab" -> addNewTab()
                    "Reload" -> webView.reload()
                    "Back to Profiles" -> saveAndFinish()
                }
                true
            }
            popup.show()
        }
    }

    private fun refreshTabBar() {
        tabBar.removeAllViews()
        profile.tabs.forEachIndexed { index, tab ->
            val tabView = LayoutInflater.from(this).inflate(R.layout.item_tab, tabBar, false)
            val tabTitle = tabView.findViewById<TextView>(R.id.tabTitle)
            val btnClose = tabView.findViewById<ImageButton>(R.id.btnCloseTab)

            tabTitle.text = if (tab.title.length > 12) tab.title.substring(0, 12) + "…" else tab.title
            tabView.isSelected = tab.id == profile.activeTabId
            tabView.alpha = if (tab.id == profile.activeTabId) 1f else 0.6f

            tabView.setOnClickListener {
                profile.activeTabId = tab.id
                loadActiveTab()
                refreshTabBar()
            }
            btnClose.setOnClickListener { closeTab(index) }
            tabBar.addView(tabView)
        }
        // Tab count update
        tvTabCount.text = profile.tabs.size.toString()
    }

    private fun addNewTab() {
        val t = Tab(ProfileManager.generateId(), "New Tab", "https://www.google.com")
        profile.tabs.add(t)
        profile.activeTabId = t.id
        refreshTabBar()
        loadActiveTab()
        saveProfiles()
    }

    private fun closeTab(index: Int) {
        if (profile.tabs.size <= 1) {
            Toast.makeText(this, "কমপক্ষে একটা tab থাকতে হবে", Toast.LENGTH_SHORT).show()
            return
        }
        profile.tabs.removeAt(index)
        if (profile.tabs.none { it.id == profile.activeTabId })
            profile.activeTabId = profile.tabs.last().id
        refreshTabBar()
        loadActiveTab()
        saveProfiles()
    }

    private fun loadActiveTab() {
        val tab = profile.tabs.find { it.id == profile.activeTabId }
            ?: profile.tabs.firstOrNull() ?: return
        profile.activeTabId = tab.id
        etUrl.setText(tab.url)
        webView.loadUrl(tab.url)
    }

    private fun loadUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(" ") || !input.contains(".") ->
                "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
            else -> "https://$input"
        }
        webView.loadUrl(url)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
    }

    private fun updateActiveTabUrl(url: String) {
        profile.tabs.find { it.id == profile.activeTabId }?.url = url
        saveProfiles()
    }

    private fun updateActiveTabTitle(title: String) {
        profile.tabs.find { it.id == profile.activeTabId }?.title = title
        saveProfiles()
    }

    private fun saveProfiles() {
        val profiles = ProfileManager.loadProfiles(this)
        if (profileIndex < profiles.size) {
            profiles[profileIndex] = profile
            ProfileManager.saveProfiles(this, profiles)
        }
    }

    private fun saveAndFinish() { saveProfiles(); finish() }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else saveAndFinish()
    }
}