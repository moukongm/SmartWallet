package com.bill.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.Bill
import com.example.business.bill.impl.R
import com.example.business.bill.impl.databinding.BillItemBinding
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class BillListAdapter(
    private val onClick: (Bill) -> Unit
) : ListAdapter<Bill, BillListAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            BillItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: BillItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bill: Bill) {
            binding.billItemTitle.text = bill.note.ifBlank { bill.category }
            binding.billItemCategory.text = bill.category
            binding.billItemTime.text = TIME_FORMAT.format(bill.occurredAt)
            binding.billItemIcon.setImageResource(iconForCategory(bill.category))

            val income = bill.type == BillRecordType.INCOME
            binding.billItemAmount.text = buildString {
                append(if (income) "+" else "-")
                append("¥")
                append(
                    MONEY_FORMAT.format(
                        BigDecimal.valueOf(bill.amountInCents, 2)
                    )
                )
            }
            binding.billItemAmount.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (income) R.color.bill_page_primary
                    else R.color.bill_page_expense
                )
            )
            binding.root.setOnClickListener { onClick(bill) }
        }
    }

    private fun iconForCategory(category: String): Int {
        return when (category) {
            "餐饮" -> R.drawable.bill_page_ic_food
            "交通" -> R.drawable.bill_page_ic_subway
            "工资", "奖金", "理财", "转账" -> R.drawable.bill_page_ic_salary
            else -> R.drawable.bill_page_ic_more
        }
    }

    companion object {
        private val MONEY_FORMAT = DecimalFormat("#,##0.00")
        private val TIME_FORMAT = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
        private val DIFF = object : DiffUtil.ItemCallback<Bill>() {
            override fun areItemsTheSame(oldItem: Bill, newItem: Bill) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Bill, newItem: Bill) =
                oldItem == newItem
        }
    }
}
