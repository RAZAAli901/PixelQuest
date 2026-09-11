package com.pixelquest.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("reported_profile_id") val reportedProfileId: String,
    @SerialName("reason") val reason: String
)
