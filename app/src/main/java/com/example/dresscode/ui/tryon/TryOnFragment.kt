package com.example.dresscode.ui.tryon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentTryOnBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.example.dresscode.model.OutfitPreview

@AndroidEntryPoint
class TryOnFragment : Fragment(R.layout.fragment_try_on) {

    private var _binding: FragmentTryOnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TryOnViewModel by viewModels()
    private var cachedFavorites: List<OutfitPreview> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTryOnBinding.bind(view)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.try_on_placeholder, state.status, state.hint)
            binding.progressTryOn.isVisible = state.isSubmitting
            val selectionText = buildString {
                append("人像：${state.selectedPhotoLabel ?: "未选择"}\n")
                append("穿搭：${state.selectedOutfitTitle ?: "未选择"}")
                state.resultPreview?.let { append("\n结果：$it") }
            }
            binding.selectionSummary.text = selectionText
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            cachedFavorites = favorites
        }
        binding.btnSelectPhoto.setOnClickListener {
            viewModel.attachPhoto("本地人像示例")
        }
        binding.btnPickFavorite.setOnClickListener {
            if (cachedFavorites.isNotEmpty()) {
                viewModel.useFavorite(cachedFavorites.first())
            } else {
                Snackbar.make(binding.root, R.string.try_on_need_favorite, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.btnSubmitTryOn.setOnClickListener { viewModel.submitTryOn() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
