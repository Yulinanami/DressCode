package com.example.dresscode.ui.weather

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dresscode.R
import com.example.dresscode.databinding.FragmentCitySelectBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class CitySelectFragment : Fragment(R.layout.fragment_city_select) {

    private var _binding: FragmentCitySelectBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCitySelectBinding.bind(view)
        binding.cityList.layoutManager = LinearLayoutManager(requireContext())
        binding.cityList.adapter = SimpleCityAdapter(
            listOf("上海", "北京", "深圳", "杭州", "广州", "成都", "南京", "武汉")
        ) { city ->
            findNavController().previousBackStackEntry?.savedStateHandle?.set("selected_city", city)
            findNavController().popBackStack()
        }

        binding.btnAddCity.setOnClickListener {
            val city = binding.inputCity.text?.toString()?.trim().orEmpty()
            if (city.isBlank()) {
                Snackbar.make(binding.root, R.string.city_input_empty, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().previousBackStackEntry?.savedStateHandle?.set("selected_city", city)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
