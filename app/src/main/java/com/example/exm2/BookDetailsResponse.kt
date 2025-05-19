package com.example.exm2

import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class BookDetailsResponse(
    val title: String?,
    @SerializedName("description") val description: @RawValue Any? // Description can be String or JsonObject
) : Parcelable {
    fun getDescriptionString(): String? {
        return when (description) {
            is String -> description
            is Map<*, *> -> (description as? Map<String, String>)?.get("value")
            else -> null
        }
    }
}