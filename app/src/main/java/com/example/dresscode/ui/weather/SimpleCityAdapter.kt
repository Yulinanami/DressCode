package com.example.dresscode.ui.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class SimpleCityAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit = {}
) : RecyclerView.Adapter<SimpleCityAdapter.CityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val card = MaterialCardView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { lp ->
                lp.topMargin = 6
                lp.bottomMargin = 6
            }
            radius = 12f
            strokeWidth = 1
            isClickable = true
            isFocusable = true
        }
        val textView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, card, false) as TextView
        card.addView(textView)
        return CityViewHolder(card, textView)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = items[position]
        holder.label.text = city
        holder.root.setOnClickListener { onClick(city) }
    }

    override fun getItemCount(): Int = items.size

    class CityViewHolder(val root: View, val label: TextView) : RecyclerView.ViewHolder(root)
}
