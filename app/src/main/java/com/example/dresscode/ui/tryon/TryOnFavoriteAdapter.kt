package com.example.dresscode.ui.tryon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.dresscode.BuildConfig
import com.example.dresscode.databinding.ItemTryOnFavoriteBinding
import com.example.dresscode.model.OutfitPreview

class TryOnFavoriteAdapter(
    private val onClick: (OutfitPreview) -> Unit = {}
) : RecyclerView.Adapter<TryOnFavoriteAdapter.ViewHolder>() {

    private val items = mutableListOf<OutfitPreview>()

    fun submitList(data: List<OutfitPreview>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTryOnFavoriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemTryOnFavoriteBinding,
        private val onClick: (OutfitPreview) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OutfitPreview) {
            binding.title.text = item.title
            binding.tags.text = item.tags.joinToString(" · ")
            val resolvedUrl = resolveUrl(item.imageUrl)
            binding.cover.load(resolvedUrl) {
                crossfade(true)
            }
            binding.root.setOnClickListener { onClick(item) }
        }

        private fun resolveUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            return if (url.startsWith("http")) {
                url
            } else {
                buildString {
                    append(BuildConfig.API_BASE_URL.trimEnd('/'))
                    if (!url.startsWith("/")) append("/")
                    append(url)
                }
            }
        }
    }
}
