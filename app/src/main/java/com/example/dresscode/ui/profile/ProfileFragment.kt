package com.example.dresscode.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.profile_placeholder, state.subtitle, state.notes)
        }

        viewModel.authState.observe(viewLifecycleOwner) { auth ->
            binding.btnAuthAction.text =
                if (auth.isLoggedIn) getString(R.string.btn_logout) else getString(R.string.btn_go_login)
            binding.btnAuthAction.setOnClickListener {
                if (auth.isLoggedIn) {
                    viewModel.logout()
                } else {
                    navigateToLogin()
                }
            }

            val navigateIfGuest: () -> Unit = {
                if (!auth.isLoggedIn) {
                    navigateToLogin()
                }
            }
            binding.sectionTitle.setOnClickListener { navigateIfGuest() }
            binding.sectionSubtitle.setOnClickListener { navigateIfGuest() }
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
