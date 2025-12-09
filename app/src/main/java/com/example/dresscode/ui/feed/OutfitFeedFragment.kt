package com.example.dresscode.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentOutfitFeedBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.widget.doOnTextChanged
import androidx.core.view.isVisible
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import androidx.paging.LoadState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.inputmethod.EditorInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.dresscode.model.OutfitDetail
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.widget.ImageView
import android.view.ViewGroup
import coil.load
import com.example.dresscode.databinding.DialogOutfitDetailBinding
import com.example.dresscode.BuildConfig

@AndroidEntryPoint
class OutfitFeedFragment : Fragment(R.layout.fragment_outfit_feed) {

    private var _binding: FragmentOutfitFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OutfitFeedViewModel by viewModels()
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
        _binding = FragmentOutfitFeedBinding.bind(view)
        setupList()
        observeUi()
        setupFilters()
        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchSubmitted(binding.searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        binding.searchFilter.setOnClickListener {
            Snackbar.make(binding.root, R.string.filter_placeholder, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnRetry.setOnClickListener { adapter.retry() }
        binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }
        binding.btnClearFilters.setOnClickListener { viewModel.clearFilters() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pagingData.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                binding.progressFeed.isVisible = loadStates.refresh is LoadState.Loading
                binding.swipeRefresh.isRefreshing = loadStates.refresh is LoadState.Loading
                val isError = loadStates.refresh is LoadState.Error
                val isEmpty = loadStates.refresh is LoadState.NotLoading &&
                    loadStates.append.endOfPaginationReached &&
                    adapter.itemCount == 0
                binding.stateEmpty.isVisible = isEmpty
                binding.outfitList.isVisible = !isEmpty && !isError
                binding.sectionSubtitle.isVisible = !isError
                if (isError) {
                    val error = (loadStates.refresh as? LoadState.Error)?.error
                    Snackbar.make(binding.root, error?.message ?: getString(R.string.loading_generic), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.selectedDetail.observe(viewLifecycleOwner) { detail ->
            detail?.let { showDetailSheet(it) }
        }
        viewModel.detailError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupList() {
        binding.outfitList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.outfitList.adapter = adapter
    }

    private fun observeUi() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.outfit_placeholder, state.highlight, state.filters)
            if (binding.searchInput.text.toString() != state.query) {
                binding.searchInput.setText(state.query)
                binding.searchInput.setSelection(state.query.length)
            }
            binding.filterStyle.text = state.selectedStyle ?: getString(R.string.filter_style)
            binding.filterScene.text = state.selectedScene ?: getString(R.string.filter_scene)
            binding.filterWeather.text = state.selectedWeather ?: getString(R.string.filter_weather)
            binding.filterSeason.text = state.selectedSeason ?: getString(R.string.filter_season)
            binding.filterStyle.isChecked = state.selectedStyle != null
            binding.filterScene.isChecked = state.selectedScene != null
            binding.filterWeather.isChecked = state.selectedWeather != null
            binding.filterSeason.isChecked = state.selectedSeason != null
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFilters() {
        binding.filterStyle.setOnClickListener {
            showFilterDialog(
                title = getString(R.string.filter_style),
                options = resources.getStringArray(R.array.filter_options_style).toList(),
            ) { selection -> viewModel.setStyleFilter(selection) }
        }
        binding.filterScene.setOnClickListener {
            showFilterDialog(
                title = getString(R.string.filter_scene),
                options = resources.getStringArray(R.array.filter_options_scene).toList(),
            ) { selection -> viewModel.setSceneFilter(selection) }
        }
        binding.filterWeather.setOnClickListener {
            showFilterDialog(
                title = getString(R.string.filter_weather),
                options = resources.getStringArray(R.array.filter_options_weather).toList(),
            ) { selection -> viewModel.setWeatherFilter(selection) }
        }
        binding.filterSeason.setOnClickListener {
            showFilterDialog(
                title = getString(R.string.filter_season),
                options = resources.getStringArray(R.array.filter_options_season).toList(),
            ) { selection -> viewModel.setSeasonFilter(selection) }
        }
    }

    private fun showFilterDialog(
        title: String,
        options: List<String>,
        onSelected: (String?) -> Unit
    ) {
        val displayOptions = listOf(getString(R.string.filter_all)) + options
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(displayOptions.toTypedArray()) { dialog, which ->
                dialog.dismiss()
                val value = if (which == 0) null else options[which - 1]
                onSelected(value)
            }
            .show()
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
        detail.images.forEach { url ->
            val margin = (8 * resources.displayMetrics.density).toInt()
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
                placeholder(R.color.md_theme_dark_surfaceVariant)
            }
            dialogBinding.imageContainer.addView(imageView)
        }
        dialog.setContentView(dialogBinding.root)
        dialog.setOnDismissListener { viewModel.clearSelectedDetail() }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
