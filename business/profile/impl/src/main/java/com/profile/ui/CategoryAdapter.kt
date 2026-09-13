package com.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.business.bill.api.CategoryDataService
import com.example.business.bill.api.model.BillCategoryInfo
import com.example.business.profile.impl.R
import com.example.business.profile.impl.databinding.ProfileItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (BillCategoryInfo) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private val items = mutableListOf<BillCategoryInfo>()
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryViewHolder {
        val binding = ProfileItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(categories: List<BillCategoryInfo>) {
        items.clear()
        items.addAll(categories)
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val category = items.removeAt(fromPosition)
        items.add(toPosition, category)
        notifyItemMoved(fromPosition,toPosition)
    }
    fun currentIds(): List<Long> {
        return items.map(BillCategoryInfo::id)
    }
    inner class CategoryViewHolder(
        private val binding: ProfileItemCategoryBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(category: BillCategoryInfo) {
            binding.profileCategoryItemName.text = category.name
            binding.profileCategoryItemIcon.setImageResource(
                category.iconKey.toDrawable()
            )
            binding.profileCategoryItemEdit.setOnClickListener {
                onEdit(category)
            }
        }
    }

    private fun String.toDrawable(): Int {
        return when (this) {
            CategoryDataService.CategoryIconKey.FOOD ->
                R.drawable.profile_category_ic_food
            CategoryDataService.CategoryIconKey.TRAFFIC ->
                R.drawable.profile_category_ic_traffic
            CategoryDataService.CategoryIconKey.SHOPPING ->
                R.drawable.profile_category_ic_shopping
            CategoryDataService.CategoryIconKey.ENTERTAINMENT ->
                R.drawable.profile_category_ic_entertainment
            CategoryDataService.CategoryIconKey.HOUSING ->
                R.drawable.profile_category_ic_housing
            CategoryDataService.CategoryIconKey.MEDICAL ->
                R.drawable.profile_category_ic_medical
            CategoryDataService.CategoryIconKey.STUDY ->
                R.drawable.profile_category_ic_study
            else ->
                R.drawable.profile_category_ic_more
        }
    }
}