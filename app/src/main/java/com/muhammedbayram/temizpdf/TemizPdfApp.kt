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

        // Background asynchronous initialization - keeps cold start fast and responsive
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PDFBoxResourceLoader.init(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                MobileAds.initialize(this@TemizPdfApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
