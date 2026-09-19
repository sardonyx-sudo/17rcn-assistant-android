package org.rotary.goodsassistant.model

import com.google.gson.annotations.SerializedName

data class PhotoItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fileId") val fileId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("originalUrl") val originalUrl: String? = null,
    @SerializedName("downloadUrl") val downloadUrl: String? = null,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    var base64Data: String? = null,
    var mimeType: String? = "image/jpeg"
)

data class GoodsItem(
    @SerializedName("row") val row: Int = 0,
    @SerializedName("timestamp") val timestamp: String? = "",
    @SerializedName("status") val status: String? = "待刊登",
    @SerializedName("uploader") val uploader: String? = "",
    @SerializedName("title") val title: String? = "",
    @SerializedName("category1") val category1: Any? = null,
    @SerializedName("category1_name") val category1Name: String? = null,
    @SerializedName("category2") val category2: Any? = null,
    @SerializedName("category2_name") val category2Name: String? = null,
    @SerializedName("quantity") val quantity: Any? = "1",
    @SerializedName("price") val price: Any? = "0",
    @SerializedName("condition") val condition: String? = "used",
    @SerializedName("address") val address: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("photos") val photos: List<PhotoItem> = emptyList()
) {
    val category1Str: String
        get() = category1?.toString() ?: ""

    val category2Str: String
        get() = category2?.toString() ?: ""

    val quantityStr: String
        get() = quantity?.toString() ?: "1"

    val priceStr: String
        get() = price?.toString() ?: "0"
}

data class GasData(
    @SerializedName("items") val items: List<GoodsItem>? = null
)

data class GasResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("items") val items: List<GoodsItem>? = null,
    @SerializedName("processingItems") val processingItems: List<GoodsItem>? = null,
    @SerializedName("processingCount") val processingCount: Int = 0,
    @SerializedName("data") val data: GasData? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("failedCount") val failedCount: Int = 0
)

data class GasImageResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("base64") val base64: String? = null,
    @SerializedName("filename") val filename: String? = null,
    @SerializedName("error") val error: String? = null
)
