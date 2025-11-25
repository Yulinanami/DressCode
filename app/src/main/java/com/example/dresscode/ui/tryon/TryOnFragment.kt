package com.example.dresscode.ui.tryon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentTryOnBinding

class TryOnFragment : Fragment(R.layout.fragment_try_on) {

    private var _binding: FragmentTryOnBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTryOnBinding.bind(view)
        binding.sectionTitle.text = getString(R.string.tab_try_on)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
