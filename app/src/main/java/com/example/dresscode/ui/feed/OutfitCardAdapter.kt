package com.example.dresscode.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dresscode.R
import com.example.dresscode.databinding.ItemOutfitCardBinding
import com.example.dresscode.model.OutfitPreview

class OutfitCardAdapter(
    private val onItemClick: (OutfitPreview) -> Unit = {},
    private val onFavoriteClick: (OutfitPreview) -> Unit = {}
) : ListAdapter<OutfitPreview, OutfitCardAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOutfitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
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
