package com.example.dresscode.ui.tryon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentTryOnBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TryOnFragment : Fragment(R.layout.fragment_try_on) {

    private var _binding: FragmentTryOnBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TryOnViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTryOnBinding.bind(view)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.try_on_placeholder, state.status, state.hint)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
