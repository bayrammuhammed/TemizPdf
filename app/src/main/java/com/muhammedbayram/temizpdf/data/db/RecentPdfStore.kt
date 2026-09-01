package com.muhammedbayram.temizpdf.data.db

import android.content.Context
import android.content.SharedPreferences
import com.muhammedbayram.temizpdf.data.model.PdfDocumentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class RecentPdfStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("temiz_pdf_prefs", Context.MODE_PRIVATE)
    private val _recentPdfs = MutableStateFlow<List<PdfDocumentItem>>(emptyList())
    val recentPdfs: StateFlow<List<PdfDocumentItem>> = _recentPdfs.asStateFlow()

    init {
        loadRecents()
    }

    private fun loadRecents() {
        val jsonString = prefs.getString("recent_list", "[]") ?: "[]"
        val list = mutableListOf<PdfDocumentItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PdfDocumentItem(
                        uri = obj.getString("uri"),
                        name = obj.getString("name"),
                        pageCount = obj.optInt("pageCount", 0),
                        sizeFormatted = obj.optString("sizeFormatted", ""),
                        lastOpened = obj.optLong("lastOpened", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        thumbnailPath = obj.optString("thumbnailPath", null)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _recentPdfs.value = list.sortedByDescending { it.lastOpened }
    }

    fun addOrUpdate(item: PdfDocumentItem) {
        val current = _recentPdfs.value.toMutableList()
        current.removeAll { it.uri == item.uri }
        current.add(0, item.copy(lastOpened = System.currentTimeMillis()))
        saveList(current)
    }

    fun toggleFavorite(uri: String) {
        val current = _recentPdfs.value.map {
            if (it.uri == uri) it.copy(isFavorite = !it.isFavorite) else it
        }
        saveList(current)
    }

    fun remove(uri: String) {
        val current = _recentPdfs.value.filterNot { it.uri == uri }
        saveList(current)
    }

    fun clearAll() {
        saveList(emptyList())
    }

    private fun saveList(list: List<PdfDocumentItem>) {
        val array = JSONArray()
        list.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("uri", item.uri)
                put("name", item.name)
                put("pageCount", item.pageCount)
                put("sizeFormatted", item.sizeFormatted)
                put("lastOpened", item.lastOpened)
                put("isFavorite", item.isFavorite)
                put("thumbnailPath", item.thumbnailPath ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("recent_list", array.toString()).apply()
        _recentPdfs.value = list
    }
}
