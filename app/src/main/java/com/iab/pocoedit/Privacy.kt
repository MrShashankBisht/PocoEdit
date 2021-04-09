package com.iab.pocoedit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.iab.pocoedit.view.MainActivity

class Privacy : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        if (supportActionBar != null) {
            if (MainActivity.isSupportActionBarEnabled) {
                supportActionBar!!.show()
            } else {
                supportActionBar!!.hide()
            }
        }
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/"+"privacy.html")
    }
}