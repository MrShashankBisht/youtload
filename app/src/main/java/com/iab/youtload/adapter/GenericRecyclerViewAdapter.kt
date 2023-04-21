package com.iab.youtload.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.iab.youtload.adapter.viewHolder.GenericViewHolder

class GenericRecyclerViewAdapter(var genericRecyclerViewAdapterInterface: GenericRecyclerViewAdapterInterface): Adapter<GenericViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
        return genericRecyclerViewAdapterInterface.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        genericRecyclerViewAdapterInterface.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int {
        return genericRecyclerViewAdapterInterface.getItemCount()
    }
}

public interface GenericRecyclerViewAdapterInterface{
    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder
    fun onBindViewHolder(holder: GenericViewHolder, position: Int)
    fun getItemCount(): Int
}