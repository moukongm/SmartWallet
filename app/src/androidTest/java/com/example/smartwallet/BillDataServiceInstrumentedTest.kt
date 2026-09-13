package com.example.smartwallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alibaba.android.arouter.launcher.ARouter
import com.example.business.bill.api.BillApiRoutes
import com.example.business.bill.api.BillDataService
import com.example.business.bill.api.BillRecordType
import com.example.business.bill.api.model.BillQuery
import com.example.business.bill.api.model.BillSaveRequest
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillDataServiceInstrumentedTest {

    @Test
    fun billServiceCompletesCrudAndSummary() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        MMKV.initialize(context)
        val loginStorage = MMKV.mmkvWithID(LOGIN_STORAGE_ID)
        val previousLoggedIn = loginStorage.decodeBool(KEY_IS_LOGGED_IN, false)
        val previousUserId = loginStorage.decodeString(KEY_USER_ID)
        val testUserId = "bill-service-test-${System.currentTimeMillis()}"
        loginStorage.encode(KEY_IS_LOGGED_IN, true)
        loginStorage.encode(KEY_USER_ID, testUserId)

        val service = ARouter.getInstance()
            .build(BillApiRoutes.BILL_DATA_SERVICE)
            .navigation() as? BillDataService
        assertNotNull(service)
        service ?: return@runBlocking

        var createdBillId: Long? = null
        val occurredAt = System.currentTimeMillis()
        try {
            val created = service.createBill(
                BillSaveRequest(
                    type = BillRecordType.EXPENSE,
                    amountInCents = 3_500,
                    category = "餐饮",
                    note = "数据服务测试",
                    occurredAt = occurredAt
                )
            )
            createdBillId = created.id
            assertTrue(created.id > 0)
            assertEquals(3_500, created.amountInCents)
            assertEquals(created, service.getBillById(created.id))

            val queried = service.getBills(
                BillQuery(
                    startAt = occurredAt - 1,
                    endAtExclusive = occurredAt + 1,
                    type = BillRecordType.EXPENSE,
                    keyword = "数据服务测试"
                )
            )
            assertTrue(queried.any { it.id == created.id })

            val updated = service.updateBill(
                created.id,
                BillSaveRequest(
                    type = BillRecordType.INCOME,
                    amountInCents = 8_000,
                    category = "工资",
                    note = "更新后的数据服务测试",
                    occurredAt = occurredAt
                )
            )
            assertEquals(BillRecordType.INCOME, updated.type)
            assertEquals(8_000, updated.amountInCents)

            val summary = service.getSummary(
                startAt = occurredAt - 1,
                endAtExclusive = occurredAt + 1
            )
            assertEquals(8_000, summary.incomeInCents)
            assertEquals(0, summary.expenseInCents)
            assertEquals(8_000, summary.balanceInCents)

            assertTrue(service.deleteBill(created.id))
            createdBillId = null
            assertNull(service.getBillById(created.id))
        } finally {
            createdBillId?.let { service.deleteBill(it) }
            loginStorage.encode(KEY_IS_LOGGED_IN, previousLoggedIn)
            if (previousUserId == null) {
                loginStorage.removeValueForKey(KEY_USER_ID)
            } else {
                loginStorage.encode(KEY_USER_ID, previousUserId)
            }
        }
    }

    private companion object {
        const val LOGIN_STORAGE_ID = "smart_wallet_login_state"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_USER_ID = "user_id"
    }
}
