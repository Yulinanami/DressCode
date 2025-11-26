package com.example.dresscode.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentOutfitFeedBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.widget.doOnTextChanged
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class OutfitFeedFragment : Fragment(R.layout.fragment_outfit_feed) {

    private var _binding: FragmentOutfitFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OutfitFeedViewModel by viewModels()
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
        _binding = FragmentOutfitFeedBinding.bind(view)
        setupList()
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.outfit_placeholder, state.highlight, state.filters)
            binding.progressFeed.isVisible = state.isLoading
            if (binding.searchInput.text.toString() != state.query) {
                binding.searchInput.setText(state.query)
                binding.searchInput.setSelection(state.query.length)
            }
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }
        viewModel.featured.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
        setupFilters()
        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
        binding.searchFilter.setOnClickListener {
            Snackbar.make(binding.root, R.string.filter_placeholder, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupList() {
        binding.outfitList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.outfitList.adapter = adapter
    }

    private fun setupFilters() {
        binding.filterGender.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleGenderFilter(isChecked)
        }
        binding.filterStyle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTag(getString(R.string.filter_style), isChecked)
        }
        binding.filterScene.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTag(getString(R.string.filter_scene), isChecked)
        }
        binding.filterWeather.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTag(getString(R.string.filter_weather), isChecked)
        }
        binding.filterSeason.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleTag(getString(R.string.filter_season), isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
