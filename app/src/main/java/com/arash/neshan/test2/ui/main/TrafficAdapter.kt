package com.arash.neshan.test2.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arash.neshan.test2.R
import com.arash.neshan.test2.databinding.ItemTrafficBinding

class TrafficAdapter(private val trafficList: ArrayList<String>): RecyclerView.Adapter<TrafficAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemTrafficBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = trafficList[position]

        holder.binding.apply {
            val context = root.context
            if (position == 0) {
                tvLoginPerson.setText(R.string.login_roll_call)
                tvLoginPerson.setTextColor(ContextCompat.getColor(context, R.color.colorGreen))
                tvLogoutPerson.setText(R.string.logout_roll_call)
                tvLogoutPerson.setTextColor(ContextCompat.getColor(context, R.color.colorRed))
            }
            if (list == context.getString(R.string.empty_time)) {
                tvLoginPerson.setTextColor(ContextCompat.getColor(context, R.color.gray))
                tvLogoutPerson.setTextColor(ContextCompat.getColor(context, R.color.gray))
                tvLoginPerson.text = list
                tvLogoutPerson.text = list
            }
        }

    }

    override fun getItemCount(): Int = trafficList.size

    inner class ViewHolder(val binding: ItemTrafficBinding): RecyclerView.ViewHolder(binding.root)

}
