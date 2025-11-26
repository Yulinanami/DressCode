package com.example.dresscode.ui.weather

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
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
            Snackbar.make(binding.root, R.string.permission_location_rationale, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnSelectCity.setOnClickListener {
            findNavController().navigate(R.id.action_weather_to_city_select)
        }

        viewModel.refresh()
    }

    private fun observeCitySelection() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>("selected_city")
            ?.observe(viewLifecycleOwner) { city ->
                viewModel.refresh(city)
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("selected_city")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
