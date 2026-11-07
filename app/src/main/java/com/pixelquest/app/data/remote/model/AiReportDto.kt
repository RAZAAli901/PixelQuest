package com.pixelquest.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiReportDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("insight_content") val insightContent: String,
    @SerialName("reason") val reason: String,
)
