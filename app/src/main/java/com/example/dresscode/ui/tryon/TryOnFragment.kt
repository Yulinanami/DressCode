package com.example.dresscode.ui.tryon

import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentTryOnBinding
import com.example.dresscode.ui.tryon.TryOnFavoriteAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import androidx.recyclerview.widget.LinearLayoutManager

@AndroidEntryPoint
class TryOnFragment : Fragment(R.layout.fragment_try_on) {

    private var _binding: FragmentTryOnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TryOnViewModel by viewModels()
    private val favoriteAdapter by lazy {
        TryOnFavoriteAdapter { item ->
            viewModel.useFavoriteOutfit(item)
        }
    }

    private val pickPortraitImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (_binding == null) return@registerForActivityResult
            if (uri == null) return@registerForActivityResult
            handlePortraitImage(uri)
        }

    private val pickOutfitImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (_binding == null) return@registerForActivityResult
            if (uri == null) return@registerForActivityResult
            handleOutfitImage(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTryOnBinding.bind(view)
        binding.favoritesStrip.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.favoritesStrip.adapter = favoriteAdapter

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.try_on_placeholder, state.status, state.hint)
            binding.progressTryOn.isVisible = state.isSubmitting
            binding.portraitFileName.text = state.selectedPhotoLabel ?: getString(R.string.try_on_portrait_hint)
            state.selectedPhotoBytes?.let { setImageFromBytes(binding.portraitPreview, it) }
            binding.outfitFileName.text = state.selectedOutfitTitle ?: getString(R.string.try_on_outfit_hint)
            state.selectedOutfitBytes?.let { setImageFromBytes(binding.outfitPreview, it) }
            if (state.resultImageBase64 != null) {
                val resultBytes = runCatching {
                    Base64.decode(state.resultImageBase64, Base64.DEFAULT)
                }.getOrNull()
                if (resultBytes != null) {
                    binding.tryonResultPreview.isVisible = true
                    setImageFromBytes(binding.tryonResultPreview, resultBytes)
                } else {
                    binding.tryonResultPreview.isVisible = false
                }
            } else {
                binding.tryonResultPreview.isVisible = false
            }
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectPhoto.setOnClickListener {
            pickPortraitImage.launch("image/*")
        }
        binding.btnSelectOutfit.setOnClickListener {
            pickOutfitImage.launch("image/*")
        }
        binding.btnSubmitTryOn.setOnClickListener { viewModel.submitTryOn() }
        consumePresetOutfit()

        viewModel.favorites.observe(viewLifecycleOwner) { list ->
            favoriteAdapter.submitList(list)
            binding.sectionFavoritesTitle.isVisible = list.isNotEmpty()
            binding.favoritesStrip.isVisible = list.isNotEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.consumePendingRecommendedOutfit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handlePortraitImage(uri: Uri) {
        binding.portraitPreview.setImageURI(uri)
        val fileName = resolveDisplayName(uri)
        val bytes = readBytes(uri)
        if (bytes == null) {
            Snackbar.make(binding.root, R.string.tagging_read_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.portraitFileName.text = fileName
        viewModel.attachPhoto(fileName, bytes, requireContext().contentResolver.getType(uri))
    }

    private fun handleOutfitImage(uri: Uri) {
        binding.outfitPreview.setImageURI(uri)
        val fileName = resolveDisplayName(uri)
        val bytes = readBytes(uri)
        if (bytes == null) {
            Snackbar.make(binding.root, R.string.tagging_read_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.outfitFileName.text = fileName
        viewModel.selectOutfitImage(fileName, bytes, requireContext().contentResolver.getType(uri))
    }

    private fun consumePresetOutfit() {
        val args = arguments
        val title = args?.getString("preset_outfit_title")
        val imageUrl = args?.getString("preset_outfit_image")
        if (!title.isNullOrBlank() && !imageUrl.isNullOrBlank()) {
            viewModel.useRecommendedOutfit(title, imageUrl)
            args.remove("preset_outfit_title")
            args.remove("preset_outfit_image")
        }
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
        return name ?: uri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"
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

    private fun setImageFromBytes(view: ImageView, bytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            view.setImageBitmap(bitmap)
        }
    }
}
