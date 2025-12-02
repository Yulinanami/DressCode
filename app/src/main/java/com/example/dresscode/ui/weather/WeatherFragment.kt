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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentWeatherBinding
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
            binding.sectionTitle.text = state.title
            binding.sectionSubtitle.text =
                getString(R.string.weather_placeholder, state.city, state.summary, state.temperature)
            if (state.isLoading) showLoadingDialog() else hideLoadingDialog()
            if (state.error != null) {
                Snackbar.make(binding.root, state.error, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnRequestLocation.setOnClickListener {
            attemptAutoLocate()
        }

        binding.btnSelectCity.setOnClickListener {
            findNavController().navigate(R.id.action_weather_to_city_select)
        }

        attemptAutoLocate()
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
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
        return hasCoarse || hasFine
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
}
