package com.example.dresscode.ui.weather

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentWeatherBinding

class WeatherFragment : Fragment(R.layout.fragment_weather) {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWeatherBinding.bind(view)
        binding.sectionTitle.text = getString(R.string.tab_weather)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
