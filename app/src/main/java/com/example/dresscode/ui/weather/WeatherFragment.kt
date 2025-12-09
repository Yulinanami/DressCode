package com.example.dresscode.ui.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import com.example.dresscode.R
import com.example.dresscode.BuildConfig
import com.example.dresscode.databinding.FragmentWeatherBinding
import coil.load
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()
    private var loadingDialog: AlertDialog? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            fetchLocationWeather()
        } else {
            Snackbar.make(binding.root, R.string.permission_location_rationale, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWeatherBinding.bind(view)
        observeCitySelection()
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.sectionTitle.text = state.city
            binding.weatherSummary.text = getString(
                R.string.weather_card_title,
                state.city,
                state.temperature
            )
            binding.weatherDetails.text = state.summary
            renderRecommendation(state)
            if (state.isLoading) showLoadingDialog() else hideLoadingDialog()
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnRequestPermission.setOnClickListener {
            attemptAutoLocate()
        }

        binding.btnSelectCity.setOnClickListener {
            findNavController().navigate(R.id.action_weather_to_city_select)
        }
        binding.btnFetchRecommendation.setOnClickListener {
            viewModel.requestRecommendation()
        }

        if (viewModel.shouldAutoRefresh()) {
            attemptAutoLocate()
        }
    }

    private fun attemptAutoLocate() {
        if (hasLocationPermission()) {
            fetchLocationWeather()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun fetchLocationWeather() {
        val location = latestLocation()
        if (location != null) {
            viewModel.refresh(lat = location.latitude, lon = location.longitude)
        } else {
            Snackbar.make(binding.root, R.string.location_unavailable, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = requireContext()
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        return hasCoarse || hasFine
    }

    @SuppressLint("MissingPermission")
    private fun latestLocation(): Location? {
        if (!hasLocationPermission()) return null
        val context = requireContext()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        ).filter { manager.isProviderEnabled(it) }
        providers.forEach { provider ->
            val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) return loc
        }
        return null
    }

    private fun observeCitySelection() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>("selected_city")
            ?.observe(viewLifecycleOwner) { city ->
                viewModel.refresh(city = city)
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("selected_city")
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
            ?.text = getString(R.string.weather_loading)
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

    private fun renderRecommendation(state: com.example.dresscode.model.WeatherUiState) {
        binding.cardRecommendation.isVisible = true
        binding.btnFetchRecommendation.isEnabled = !state.isRecommending
        binding.recommendationLoading.isVisible = state.isRecommending
        val rec = state.recommendation
        binding.recommendationContent.isVisible = rec != null && !state.isRecommending
        binding.recommendationEmpty.isVisible = rec == null && !state.isRecommending
        rec?.let {
            val url = resolveUrl(it.outfit.imageUrl)
            binding.recommendationImage.load(url) {
                crossfade(true)
                placeholder(R.color.md_theme_light_surfaceVariant)
            }
            binding.recommendationTitle.text = it.outfit.title
            binding.recommendationTags.text = it.outfit.tags.joinToString(" · ").ifBlank {
                getString(R.string.filter_all)
            }
            binding.recommendationReason.text = it.reason
        }
    }

    private fun resolveUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http")) {
            url
        } else {
            buildString {
                append(BuildConfig.API_BASE_URL.trimEnd('/'))
                if (!url.startsWith("/")) append("/")
                append(url)
            }
        }
    }
}
