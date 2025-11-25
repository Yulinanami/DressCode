package com.example.dresscode.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        binding.sectionTitle.text = getString(R.string.tab_profile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
