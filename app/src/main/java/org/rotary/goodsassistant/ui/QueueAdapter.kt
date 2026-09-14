package org.rotary.goodsassistant.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.rotary.goodsassistant.databinding.ItemGoodsCardBinding
import org.rotary.goodsassistant.model.GoodsItem

class QueueAdapter(
    private val onPublishClick: (GoodsItem) -> Unit,
    private val onReprocessClick: (GoodsItem) -> Unit,
    private val onDeleteClick: (GoodsItem) -> Unit
) : RecyclerView.Adapter<QueueAdapter.ViewHolder>() {

    private val items = mutableListOf<GoodsItem>()

    fun submitList(newItems: List<GoodsItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoodsCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemGoodsCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GoodsItem) {
            val cat1 = item.category1Name?.ifBlank { null } ?: item.category1Str
            val cat2 = item.category2Name?.ifBlank { null } ?: item.category2Str
            val catText = if (cat2.isNotBlank()) {
                "$cat1 / $cat2"
            } else {
                cat1.ifBlank { "未分類" }
            }
            binding.tvCardCategory.text = catText
            binding.tvCardQuantity.text = "數量：${item.quantityStr}"
            binding.tvCardTitle.text = item.title?.ifBlank { null } ?: "（無品名）"
            binding.tvCardAddress.text = if (!item.address.isNullOrBlank()) "📍 ${item.address}" else "📍 無指定地址"
            binding.tvCardDesc.text = if (!item.description.isNullOrBlank()) item.description else "（無說明）"

            binding.btnPublishThis.setOnClickListener {
                onPublishClick(item)
            }
            binding.btnReprocessThis.setOnClickListener {
                onReprocessClick(item)
            }
            binding.btnDeleteThis.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}
