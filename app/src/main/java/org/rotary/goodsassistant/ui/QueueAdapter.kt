package org.rotary.goodsassistant.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.rotary.goodsassistant.databinding.ItemGoodsCardBinding
import org.rotary.goodsassistant.model.GoodsItem

class QueueAdapter(
    private val onPublishClick: (GoodsItem) -> Unit
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
            val catText = if (!item.category2.isNullOrBlank()) {
                "${item.category1 ?: ""} / ${item.category2}"
            } else {
                item.category1 ?: "未分類"
            }
            binding.tvCardCategory.text = catText
            binding.tvCardQuantity.text = "數量：${item.quantity ?: "1"}"
            binding.tvCardTitle.text = item.title ?: "（無品名）"
            binding.tvCardAddress.text = "📍 ${item.address ?: "無指定地址"}"
            binding.tvCardDesc.text = item.description ?: "（無說明）"

            binding.btnPublishThis.setOnClickListener {
                onPublishClick(item)
            }
        }
    }
}
