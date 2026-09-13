package com.profile.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.business.bill.api.model.Bill
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileItemFileBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFileAdapter(
    private val onOpen: (Bill) -> Unit,
    private val onRemove: (Bill) -> Unit
) : ListAdapter<Bill, ProfileFileAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ProfileItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ProfileItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bill: Bill) {
            binding.profileFileTitle.text = bill.note.ifBlank { bill.category }
            binding.profileFileTime.text = DATE_FORMAT.format(bill.occurredAt)
            val uri = bill.imageUri
            if (uri.isNullOrBlank()) {
                binding.profileFilePreview.setImageResource(R.drawable.profile_page_ic_folder)
            } else {
                binding.profileFilePreview.setImageURI(Uri.parse(uri))
            }
            binding.root.setOnClickListener { onOpen(bill) }
            binding.profileFileRemoveButton.setOnClickListener { onRemove(bill) }
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        private val DIFF = object : DiffUtil.ItemCallback<Bill>() {
            override fun areItemsTheSame(oldItem: Bill, newItem: Bill) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Bill, newItem: Bill) = oldItem == newItem
        }
    }
}
