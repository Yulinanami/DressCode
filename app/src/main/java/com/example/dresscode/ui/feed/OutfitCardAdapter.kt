package com.example.dresscode.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dresscode.databinding.ItemOutfitCardBinding
import com.example.dresscode.model.OutfitPreview

class OutfitCardAdapter : ListAdapter<OutfitPreview, OutfitCardAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOutfitCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemOutfitCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OutfitPreview) {
            binding.outfitTitle.text = item.title
            binding.outfitTags.text = item.tags.joinToString(" · ")
        }
    }

    private object Diff : DiffUtil.ItemCallback<OutfitPreview>() {
        override fun areItemsTheSame(oldItem: OutfitPreview, newItem: OutfitPreview): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: OutfitPreview, newItem: OutfitPreview): Boolean =
            oldItem == newItem
    }
}
