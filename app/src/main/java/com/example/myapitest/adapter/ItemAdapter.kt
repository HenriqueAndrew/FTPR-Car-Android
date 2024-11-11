package com.example.myapitest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.Car
import com.example.myapitest.ui.CircleTransform
import com.example.myapitest.ui.loadUrl
import com.squareup.picasso.Picasso

class ItemAdapter (private val Cars: List<Car>, private val itemClickListener: (Car) -> Unit): RecyclerView.Adapter<ItemAdapter.ViewHolder>(){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val imageView: ImageView = view.findViewById(R.id.image)
        val modelTextView: TextView = view.findViewById(R.id.model)
        val yearTextView: TextView = view.findViewById(R.id.year)
        val licenceTextView: TextView = view.findViewById(R.id.license)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = Cars[position]

        holder.itemView.setOnClickListener {
            itemClickListener.invoke(car)
        }

        holder.modelTextView.text = car.name
        holder.yearTextView.text = car.year
        holder.licenceTextView.text = car.licence
        holder.imageView.loadUrl(car.imageUrl)
    }

    override fun getItemCount(): Int = Cars.size

}
