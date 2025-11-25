package com.example.dresscode.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.btnLogin.setOnClickListener {
            viewModel.submit(
                binding.inputEmail.text?.toString().orEmpty(),
                binding.inputPassword.text?.toString().orEmpty()
            )
        }

        binding.btnToggle.setOnClickListener {
            viewModel.toggleMode()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnLogin.text = if (state.mode == AuthMode.LOGIN) {
                getString(R.string.btn_login_submit)
            } else {
                getString(R.string.btn_register_submit)
            }
            binding.btnToggle.text = if (state.mode == AuthMode.LOGIN) {
                getString(R.string.action_go_register)
            } else {
                getString(R.string.action_go_login)
            }
            binding.subtitle.text = if (state.mode == AuthMode.LOGIN) {
                getString(R.string.welcome_subtitle)
            } else {
                getString(R.string.register_subtitle)
            }
            binding.fieldPassword.helperText = if (state.mode == AuthMode.REGISTER) {
                getString(R.string.password_hint)
            } else {
                null
            }
            binding.progress.isVisible = state.isLoading
            binding.fieldEmail.isEnabled = !state.isLoading
            binding.fieldPassword.isEnabled = !state.isLoading
            if (state.error != null) {
                binding.fieldPassword.error = null
                binding.fieldEmail.error = null
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            } else {
                binding.fieldEmail.error = null
                binding.fieldPassword.error = null
            }
            if (state.info != null) {
                Snackbar.make(binding.root, state.info, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.authState.observe(viewLifecycleOwner) { auth ->
            if (auth.isLoggedIn) {
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
