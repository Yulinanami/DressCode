package com.example.dresscode.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.snackbar.Snackbar
import com.example.dresscode.model.Gender
import com.example.dresscode.model.TaggingModel
import com.example.dresscode.model.TryOnModel

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var suppressGenderCallback = false
    private var suppressTryOnCallback = false
    private var suppressTaggingCallback = false

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

            binding.btnFavorites.setOnClickListener {
                if (auth.isLoggedIn) {
                    findNavController().navigate(R.id.action_profileFragment_to_favoritesFragment)
                } else {
                    Snackbar.make(binding.root, R.string.favorites_need_login, Snackbar.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            }

            val navigateIfGuest: () -> Unit = {
                if (!auth.isLoggedIn) {
                    navigateToLogin()
                }
            }
            binding.sectionTitle.text = auth.displayName ?: getString(R.string.tab_profile)
            binding.sectionTitle.setOnClickListener { navigateIfGuest() }
            binding.sectionSubtitle.setOnClickListener { navigateIfGuest() }
        }

        binding.genderGroup.check(R.id.chip_gender_all)
        binding.genderGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressGenderCallback) return@setOnCheckedStateChangeListener
            val gender = when {
                checkedIds.contains(R.id.chip_gender_female) -> Gender.FEMALE
                checkedIds.contains(R.id.chip_gender_male) -> Gender.MALE
                else -> null
            }
            viewModel.updateDefaultGender(gender)
        }

        viewModel.defaultGender.observe(viewLifecycleOwner) { gender ->
            suppressGenderCallback = true
            val targetId = when (gender) {
                Gender.FEMALE -> R.id.chip_gender_female
                Gender.MALE -> R.id.chip_gender_male
                Gender.UNISEX, null -> R.id.chip_gender_all
            }
            binding.genderGroup.check(targetId)
            binding.genderSummary.text = when (gender) {
                Gender.FEMALE -> getString(R.string.profile_gender_summary_female)
                Gender.MALE -> getString(R.string.profile_gender_summary_male)
                Gender.UNISEX, null -> getString(R.string.profile_gender_summary_all)
            }
            suppressGenderCallback = false
        }

        binding.tryonModelGroup.check(R.id.chip_tryon_model_quality)
        binding.tryonModelGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressTryOnCallback) return@setOnCheckedStateChangeListener
            val model = when {
                checkedIds.contains(R.id.chip_tryon_model_fast) -> TryOnModel.AITRYON
                else -> TryOnModel.AITRYON_PLUS
            }
            viewModel.updateTryOnModel(model)
        }

        viewModel.tryOnModel.observe(viewLifecycleOwner) { model ->
            suppressTryOnCallback = true
            val targetId = when (model) {
                TryOnModel.AITRYON -> R.id.chip_tryon_model_fast
                TryOnModel.AITRYON_PLUS -> R.id.chip_tryon_model_quality
            }
            binding.tryonModelGroup.check(targetId)
            binding.tryonModelSummary.text = when (model) {
                TryOnModel.AITRYON -> getString(R.string.profile_tryon_model_fast_summary)
                TryOnModel.AITRYON_PLUS -> getString(R.string.profile_tryon_model_quality_summary)
            }
            suppressTryOnCallback = false
        }

        binding.taggingModelGroup.check(R.id.chip_tagging_model_quality)
        binding.taggingModelGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressTaggingCallback) return@setOnCheckedStateChangeListener
            val model = when {
                checkedIds.contains(R.id.chip_tagging_model_fast) -> TaggingModel.GEMINI_FLASH_LITE
                else -> TaggingModel.GEMINI_FLASH
            }
            viewModel.updateTaggingModel(model)
        }

        viewModel.taggingModel.observe(viewLifecycleOwner) { model ->
            suppressTaggingCallback = true
            val targetId = when (model) {
                TaggingModel.GEMINI_FLASH_LITE -> R.id.chip_tagging_model_fast
                TaggingModel.GEMINI_FLASH -> R.id.chip_tagging_model_quality
            }
            binding.taggingModelGroup.check(targetId)
            binding.taggingModelSummary.text = when (model) {
                TaggingModel.GEMINI_FLASH_LITE -> getString(R.string.profile_tagging_model_fast_summary)
                TaggingModel.GEMINI_FLASH -> getString(R.string.profile_tagging_model_quality_summary)
            }
            suppressTaggingCallback = false
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
