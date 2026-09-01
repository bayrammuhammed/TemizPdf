package com.muhammedbayram.temizpdf

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TemizPdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Google Mobile Ads (AdMob) in background
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@TemizPdfApp) {}
        }
    }
}
