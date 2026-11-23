package com.pixelquest.app.tune.model

import kotlinx.serialization.Serializable

@Serializable
data class TuneItem(
    val id: String,
    val name: String,
    val description: String,
    val categories: List<String>,
    val objectKey: String,
    val previewObjectKey: String,
    val mimeType: String,
    val bytes: Long,
    val durationMillis: Long,
    val sha256: String,
    val previewUrl: String? = null,
    val downloadUrl: String? = null
)

@Serializable
data class TuneCatalogResponse(
    val version: Int = 1,
    val tunes: List<TuneItem> = emptyList()
)

@Serializable
data class TuneDownloadResponse(
    val tune: TuneItem
)

enum class TuneDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}
