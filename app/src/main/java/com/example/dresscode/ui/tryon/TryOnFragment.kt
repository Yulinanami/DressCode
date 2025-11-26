package com.example.dresscode.ui.tryon

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentTryOnBinding
import com.example.dresscode.model.OutfitPreview
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

@AndroidEntryPoint
class TryOnFragment : Fragment(R.layout.fragment_try_on) {

    private var _binding: FragmentTryOnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TryOnViewModel by viewModels()
    private var cachedFavorites: List<OutfitPreview> = emptyList()

    private val pickOutfitImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (_binding == null) return@registerForActivityResult
            if (uri == null) return@registerForActivityResult
            handleOutfitImage(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTryOnBinding.bind(view)
        viewModel.taggingState.observe(viewLifecycleOwner) { state ->
            binding.taggingStatus.text = state.status
            binding.progressTagging.isVisible = state.isUploading
            binding.btnTagOutfit.isEnabled = !state.isUploading
            binding.taggingFileName.text =
                state.selectedFileName ?: getString(R.string.tagging_preview_placeholder)
            binding.taggingTags.text =
                state.tagsPreview ?: getString(R.string.tagging_tag_placeholder)
            binding.taggingSuggested.isVisible = state.suggestedName != null
            binding.taggingSuggested.text = state.suggestedName?.let {
                getString(R.string.tagging_suggested_name, it)
            }
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }
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
        binding.btnTagOutfit.setOnClickListener {
            pickOutfitImage.launch("image/*")
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

    private fun handleOutfitImage(uri: Uri) {
        binding.taggingPreview.setImageURI(uri)
        val fileName = resolveDisplayName(uri)
        val bytes = readBytes(uri)
        if (bytes == null) {
            Snackbar.make(binding.root, R.string.tagging_read_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        viewModel.uploadOutfitForTags(fileName, bytes)
    }

    private fun resolveDisplayName(uri: Uri): String {
        var name: String? = null
        var cursor: Cursor? = null
        try {
            cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.takeIf { it.moveToFirst() }?.let {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
        return name ?: uri.lastPathSegment ?: "outfit_${System.currentTimeMillis()}.jpg"
    }

    private fun readBytes(uri: Uri): ByteArray? {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } catch (io: IOException) {
            null
        }
    }
}
