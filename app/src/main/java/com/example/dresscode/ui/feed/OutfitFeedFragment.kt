package com.example.dresscode.ui.feed

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentOutfitFeedBinding

class OutfitFeedFragment : Fragment(R.layout.fragment_outfit_feed) {

    private var _binding: FragmentOutfitFeedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOutfitFeedBinding.bind(view)
        binding.sectionTitle.text = getString(R.string.tab_outfits)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
