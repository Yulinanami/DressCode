package com.example.dresscode.ui.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.dresscode.BuildConfig
import com.example.dresscode.R
import com.example.dresscode.data.repository.TryOnRepository
import com.example.dresscode.databinding.FragmentWeatherBinding
import coil.load
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()
    @Inject lateinit var tryOnRepository: TryOnRepository
    private var loadingDialog: AlertDialog? = null
    private var locationManager: LocationManager? = null
    private var hasDeliveredLocation = false
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { deliverLastKnownOrFail() }
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!hasDeliveredLocation) {
                hasDeliveredLocation = true
                stopLocationUpdates()
                viewModel.refresh(lat = location.latitude, lon = location.longitude)
            }
        }
    }

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

    @SuppressLint("MissingPermission")
    private fun fetchLocationWeather() {
        if (!hasLocationPermission()) return
        viewModel.markLocating()
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) {
            Snackbar.make(binding.root, R.string.location_unavailable, Snackbar.LENGTH_SHORT).show()
            viewModel.onLocationFailed(getString(R.string.location_unavailable))
            return
        }
        stopLocationUpdates()
        locationManager = manager
        hasDeliveredLocation = false
        // 主动请求位置，首个回调即停止
        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(provider, 0L, 0f, locationListener, Looper.getMainLooper())
            }
        }
        // 超时后尝试 lastKnown，再提示
        handler.postDelayed(stopRunnable, 8000L)
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
    private fun deliverLastKnownOrFail() {
        val manager = locationManager ?: return
        if (hasDeliveredLocation) return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        val last = providers.firstNotNullOfOrNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        if (last != null) {
            hasDeliveredLocation = true
            stopLocationUpdates()
            viewModel.refresh(lat = last.latitude, lon = last.longitude)
        } else {
            stopLocationUpdates()
            Snackbar.make(binding.root, R.string.location_unavailable, Snackbar.LENGTH_SHORT).show()
            viewModel.onLocationFailed(getString(R.string.location_unavailable))
        }
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
        stopLocationUpdates()
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

    private fun stopLocationUpdates() {
        handler.removeCallbacks(stopRunnable)
        locationManager?.removeUpdates(locationListener)
        locationManager = null
    }

    private fun renderRecommendation(state: com.example.dresscode.model.WeatherUiState) {
        binding.cardRecommendation.isVisible = true
        binding.btnFetchRecommendation.isEnabled = !state.isRecommending
        binding.recommendationLoading.isVisible = state.isRecommending
        val rec = state.recommendation
        binding.recommendationContent.isVisible = rec != null && !state.isRecommending
        binding.recommendationEmpty.isVisible = rec == null && !state.isRecommending
        if (rec == null) {
            binding.recommendationContent.setOnClickListener(null)
            return
        }
        rec.let {
            val url = resolveUrl(it.outfit.imageUrl)
            if (url == null) {
                binding.recommendationContent.setOnClickListener(null)
                return
            }
            binding.recommendationImage.load(url) {
                crossfade(true)
                placeholder(R.color.md_theme_light_surfaceVariant)
            }
            binding.recommendationTitle.text = it.outfit.title
            binding.recommendationTags.text = it.outfit.tags.joinToString(" · ").ifBlank {
                getString(R.string.filter_all)
            }
            binding.recommendationReason.text = it.reason
            binding.recommendationContent.setOnClickListener { _ ->
                tryOnRepository.setPendingRecommendedOutfit(it.outfit.title, url)
                Snackbar.make(
                    binding.root,
                    getString(R.string.weather_recommend_to_tryon),
                    Snackbar.LENGTH_LONG
                ).show()
            }
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
