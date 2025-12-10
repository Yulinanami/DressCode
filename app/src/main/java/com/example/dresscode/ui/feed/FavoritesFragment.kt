package com.example.dresscode.ui.feed

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentFavoritesBinding
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.dresscode.model.OutfitDetail
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.dresscode.databinding.DialogOutfitDetailBinding
import android.view.LayoutInflater
import android.widget.ImageView
import android.view.ViewGroup
import coil.load
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import com.example.dresscode.BuildConfig

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private val adapter by lazy {
        OutfitCardAdapter(
            onItemClick = { preview ->
                viewModel.loadOutfitDetail(preview.id)
            },
            onFavoriteClick = { preview -> viewModel.toggleFavorite(preview.id) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoritesBinding.bind(view)
        setupList()
        binding.toolbarFavorites.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnRetry.setOnClickListener { viewModel.refreshFavorites() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favorites.collectLatest { items ->
                adapter.submitData(viewLifecycleOwner.lifecycle, items)
            }
        }
        viewModel.detail.observe(viewLifecycleOwner) { detail ->
            detail?.let { showDetailSheet(it) }
        }
        viewModel.toast.observe(viewLifecycleOwner) { message ->
            message?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.progressFavorites.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.stateEmpty.isVisible = state.isEmpty
        }
        viewModel.refreshFavorites()
    }

    private fun setupList() {
        binding.favoritesList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.favoritesList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        adapter.submitData(lifecycle, androidx.paging.PagingData.empty())
    }

    private fun showDetailSheet(detail: OutfitDetail) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogOutfitDetailBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.title.text = detail.title
        dialogBinding.tags.text = if (detail.tags.isEmpty()) {
            getString(R.string.filter_all)
        } else {
            detail.tags.joinToString(" · ")
        }
        dialogBinding.imageContainer.removeAllViews()
        val margin = (8 * resources.displayMetrics.density).toInt()
        val placeholder = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.md_theme_light_surfaceVariant))
        detail.images.forEach { url ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = margin }
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            imageView.load(resolveUrl(url)) {
                crossfade(true)
                placeholder(placeholder)
                error(placeholder)
                allowHardware(false)
            }
            dialogBinding.imageContainer.addView(imageView)
        }
        dialog.setContentView(dialogBinding.root)
        dialog.setOnDismissListener { viewModel.clearDetail() }
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            val behavior = dialog.behavior
            behavior.skipCollapsed = true
            behavior.isDraggable = true
            dialogBinding.scrollContainer.post {
                behavior.peekHeight = dialogBinding.root.height
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                dialogBinding.scrollContainer.scrollTo(0, 0)
            }
        }
        dialog.show()
    }

    private fun resolveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http")) url else {
            buildString {
                append(BuildConfig.API_BASE_URL.trimEnd('/'))
                if (!url.startsWith("/")) append("/")
                append(url)
            }
        }
    }
}
