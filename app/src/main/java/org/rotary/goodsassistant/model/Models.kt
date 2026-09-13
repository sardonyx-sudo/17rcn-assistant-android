package org.rotary.goodsassistant.model

import com.google.gson.annotations.SerializedName

data class PhotoItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("originalUrl") val originalUrl: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    var base64Data: String? = null,
    var mimeType: String? = "image/jpeg"
)

data class GoodsItem(
    @SerializedName("row") val row: Int = 0,
    @SerializedName("title") val title: String? = "",
    @SerializedName("category1") val category1: String? = "",
    @SerializedName("category2") val category2: String? = "",
    @SerializedName("quantity") val quantity: String? = "1",
    @SerializedName("condition") val condition: String? = "良好",
    @SerializedName("address") val address: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("photos") val photos: List<PhotoItem> = emptyList(),
    @SerializedName("status") val status: String? = "待刊登"
)

data class GasData(
    @SerializedName("items") val items: List<GoodsItem>? = null
)

data class GasResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: GasData? = null,
    @SerializedName("error") val error: String? = null
)
