package com.example.samsunggearcontrollerbleandroiddriver.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.samsunggearcontrollerbleandroiddriver.databinding.ItemDeviceBinding
import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerDevice

/**
 * Adapter for displaying controller devices in a RecyclerView.
 */
class DeviceAdapter(private val onConnectClick: (ControllerDevice) -> Unit) :
    ListAdapter<ControllerDevice, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(device: ControllerDevice) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceAddress.text = device.address
            
            binding.btnConnect.setOnClickListener {
                onConnectClick(device)
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient RecyclerView updates.
     */
    class DeviceDiffCallback : DiffUtil.ItemCallback<ControllerDevice>() {
        override fun areItemsTheSame(oldItem: ControllerDevice, newItem: ControllerDevice): Boolean {
            return oldItem.address == newItem.address
        }
        
        override fun areContentsTheSame(oldItem: ControllerDevice, newItem: ControllerDevice): Boolean {
            return oldItem == newItem
        }
    }
}
