package org.rotary.goodsassistant.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.rotary.goodsassistant.R
import org.rotary.goodsassistant.databinding.ItemGoodsCardBinding
import org.rotary.goodsassistant.model.GoodsItem

class QueueAdapter(
    private val onPublishClick: (GoodsItem) -> Unit,
    private val onUnlockClick: (GoodsItem) -> Unit,
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

            val isProcessing = item.status?.trim()?.contains("AI辨識中") == true
            val isLocked = item.status?.trim()?.contains("刊登中") == true

            if (isProcessing) {
                binding.tvCardProcessingBadge.visibility = android.view.View.VISIBLE
                binding.tvCardLockedBadge.visibility = android.view.View.GONE
                binding.btnPublishThis.visibility = android.view.View.GONE
                binding.btnUnlockThis.visibility = android.view.View.GONE
                binding.cardGoods.strokeColor = ContextCompat.getColor(binding.root.context, R.color.badge_processing_text)
                binding.cardGoods.strokeWidth = (2 * binding.root.resources.displayMetrics.density).toInt()
                
                binding.tvCardCategory.text = "AI分析中"
                binding.tvCardTitle.text = "🤖 AI 正在辨識分析中（約需 10~15 秒）..."
                binding.tvCardDesc.text = "雲端 AI 正在識別照片特徵、自動分類與品名，完成後將自動就緒。"
            } else if (isLocked) {
                binding.tvCardProcessingBadge.visibility = android.view.View.GONE
                binding.tvCardLockedBadge.visibility = android.view.View.VISIBLE
                binding.btnPublishThis.visibility = android.view.View.GONE
                binding.btnUnlockThis.visibility = android.view.View.VISIBLE
                binding.cardGoods.strokeColor = ContextCompat.getColor(binding.root.context, R.color.badge_locked_text)
                binding.cardGoods.strokeWidth = (2 * binding.root.resources.displayMetrics.density).toInt()
            } else {
                binding.tvCardProcessingBadge.visibility = android.view.View.GONE
                binding.tvCardLockedBadge.visibility = android.view.View.GONE
                binding.btnPublishThis.visibility = android.view.View.VISIBLE
                binding.btnUnlockThis.visibility = android.view.View.GONE
                binding.cardGoods.strokeColor = ContextCompat.getColor(binding.root.context, R.color.border_color)
                binding.cardGoods.strokeWidth = (1 * binding.root.resources.displayMetrics.density).toInt()
            }

            binding.btnPublishThis.setOnClickListener {
                if (!isProcessing) {
                    onPublishClick(item)
                }
            }
            binding.btnUnlockThis.setOnClickListener {
                onUnlockClick(item)
            }
            binding.root.setOnClickListener {
                if (isLocked) {
                    onUnlockClick(item)
                } else if (isProcessing) {
                    android.widget.Toast.makeText(
                        binding.root.context,
                        binding.root.context.getString(R.string.toast_processing_cannot_publish),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
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
