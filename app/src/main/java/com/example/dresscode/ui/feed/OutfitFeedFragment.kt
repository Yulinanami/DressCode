package com.example.dresscode.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentOutfitFeedBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OutfitFeedFragment : Fragment(R.layout.fragment_outfit_feed) {

    private var _binding: FragmentOutfitFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OutfitFeedViewModel by viewModels()
    private val adapter by lazy { OutfitCardAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOutfitFeedBinding.bind(view)
        setupList()
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.outfit_placeholder, state.highlight, state.filters)
        }
        viewModel.featured.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }
    }

    private fun setupList() {
        binding.outfitList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.outfitList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
