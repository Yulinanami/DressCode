package com.example.dresscode.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.paging.PagingDataAdapter
import coil.load
import com.example.dresscode.R
import com.example.dresscode.databinding.ItemOutfitCardBinding
import com.example.dresscode.model.OutfitPreview

class OutfitCardAdapter(
    private val onItemClick: (OutfitPreview) -> Unit = {},
    private val onFavoriteClick: (OutfitPreview) -> Unit = {}
) : PagingDataAdapter<OutfitPreview, OutfitCardAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOutfitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    class ViewHolder(
        private val binding: ItemOutfitCardBinding,
        private val onItemClick: (OutfitPreview) -> Unit,
        private val onFavoriteClick: (OutfitPreview) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OutfitPreview) {
            binding.outfitTitle.text = item.title
            binding.outfitTags.text = item.tags.joinToString(" · ")
            binding.btnFavorite.text = if (item.isFavorite) {
                binding.root.context.getString(R.string.action_favorited)
            } else {
                binding.root.context.getString(R.string.action_favorite)
            }
            val tint = if (item.isFavorite) {
                ContextCompat.getColor(binding.root.context, R.color.md_theme_light_primary)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.md_theme_light_onSurfaceVariant)
            }
            binding.btnFavorite.setTextColor(tint)
            binding.outfitImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.color.md_theme_light_surfaceVariant)
            }
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnFavorite.setOnClickListener { onFavoriteClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<OutfitPreview>() {
        override fun areItemsTheSame(oldItem: OutfitPreview, newItem: OutfitPreview): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: OutfitPreview, newItem: OutfitPreview): Boolean =
            oldItem == newItem
    }
}
