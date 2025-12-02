package com.example.dresscode.ui.weather

import android.Manifest
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
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentWeatherBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()

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
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.weather_placeholder, state.city, state.summary, state.temperature)
            binding.progressWeather.isVisible = state.isLoading
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnRequestLocation.setOnClickListener {
            requestLocationPermission()
        }

        binding.btnSelectCity.setOnClickListener {
            findNavController().navigate(R.id.action_weather_to_city_select)
        }

        viewModel.refresh()
    }

    private fun requestLocationPermission() {
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        if (hasCoarse || hasFine) {
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

    private fun latestLocation(): Location? {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        _binding = null
    }
}
