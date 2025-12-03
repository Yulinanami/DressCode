package com.example.dresscode.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentLoginBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.appcompat.app.AlertDialog

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var loadingDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.btnLogin.setOnClickListener {
            viewModel.submit(
                binding.inputEmail.text?.toString().orEmpty(),
                binding.inputPassword.text?.toString().orEmpty(),
                binding.inputPasswordConfirm.text?.toString(),
                binding.inputDisplayName.text?.toString().orEmpty()
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
            binding.fieldPasswordConfirm.isVisible = state.mode == AuthMode.REGISTER
            binding.fieldDisplayName.isVisible = state.mode == AuthMode.REGISTER
            binding.fieldPassword.helperText = if (state.mode == AuthMode.REGISTER) {
                getString(R.string.password_hint)
            } else {
                null
            }
            binding.fieldEmail.isEnabled = !state.isLoading
            binding.fieldPassword.isEnabled = !state.isLoading
            binding.fieldPasswordConfirm.isEnabled = !state.isLoading
            binding.fieldDisplayName.isEnabled = !state.isLoading
            if (state.isLoading) showLoadingDialog() else hideLoadingDialog()
            if (state.error != null) {
                binding.fieldPassword.error = null
                binding.fieldEmail.error = null
                binding.fieldPasswordConfirm.error = null
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            } else {
                binding.fieldEmail.error = null
                binding.fieldPassword.error = null
                binding.fieldPasswordConfirm.error = null
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
        hideLoadingDialog()
        _binding = null
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        view.findViewById<android.widget.TextView>(R.id.text_loading)
            ?.text = getString(R.string.auth_loading)
        loadingDialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(false)
            .create().also { dialog ->
                dialog.show()
            }
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}
