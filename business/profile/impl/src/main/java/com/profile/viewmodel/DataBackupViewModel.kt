package com.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSaveRequest
import com.example.common.base.BaseViewModel
import org.json.JSONArray
import org.json.JSONObject

class DataBackupViewModel(
    private val billService: BillDataService
) : BaseViewModel() {

    suspend fun createBackup(): String {
        val items = JSONArray()
        billService.getBills(BillQuery()).forEach { bill ->
            items.put(JSONObject().apply {
                put("type", bill.type.value)
                put("amountInCents", bill.amountInCents)
                put("category", bill.category)
                put("note", bill.note)
                put("occurredAt", bill.occurredAt)
                put("imageUri", bill.imageUri ?: JSONObject.NULL)
            })
        }
        return JSONObject()
            .put("version", 1)
            .put("bills", items)
            .toString(2)
    }

    suspend fun restoreBackup(content: String): Int {
        val items = JSONObject(content).getJSONArray("bills")
        repeat(items.length()) { index ->
            val item = items.getJSONObject(index)
            billService.createBill(
                BillSaveRequest(
                    type = BillRecordType.fromValue(item.getString("type")),
                    amountInCents = item.getLong("amountInCents"),
                    category = item.getString("category"),
                    note = item.optString("note"),
                    occurredAt = item.getLong("occurredAt"),
                    imageUri = item.optString("imageUri")
                        .takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return items.length()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DataBackupViewModel::class.java))
            val service = ARouter.getInstance()
                .build(BillApiRoutes.BILL_DATA_SERVICE)
                .navigation() as? BillDataService
            requireNotNull(service) {
                "无法获取 BillDataService，请检查 bill:impl 是否已打包"
            }
            return DataBackupViewModel(service) as T
        }
    }
}
