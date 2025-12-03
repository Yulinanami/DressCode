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

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoritesViewModel by viewModels()
    private val adapter by lazy {
        OutfitCardAdapter(
            onItemClick = { preview ->
                Snackbar.make(binding.root, getString(R.string.feed_item_placeholder, preview.title), Snackbar.LENGTH_SHORT).show()
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
    }
}
