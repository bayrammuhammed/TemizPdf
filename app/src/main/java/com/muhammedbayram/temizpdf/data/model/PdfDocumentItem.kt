package com.muhammedbayram.temizpdf.data.model

import android.net.Uri

data class PdfDocumentItem(
    val uri: String,
    val name: String,
    val pageCount: Int = 0,
    val sizeFormatted: String = "",
    val lastOpened: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null
)
