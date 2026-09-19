package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DutyStorage
import com.example.model.DutyDefaults
import com.example.model.DutyHistoryRecord
import com.example.model.ShiftItem
import com.example.model.Soldier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DutyStorageTest {

    private lateinit var context: Context
    private lateinit var storage: DutyStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = DutyStorage(context)
        // Clear prefs before each test
        context.getSharedPreferences("unit_duty_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun testLoadData_defaultsOnEmpty() {
        val data = storage.loadData()
        assertEquals(DutyDefaults.DEFAULT_ALARM_TIME, data.alarmTime)
        assertEquals(DutyDefaults.defaultShiftsDay.size, data.shiftsDay.size)
        assertEquals(DutyDefaults.defaultShiftsNight.size, data.shiftsNight.size)
        assertTrue(data.rosterDay.isNotEmpty())
        assertTrue(data.rosterNight.isNotEmpty())
        assertTrue(data.dutyHistory.isEmpty())
    }

    @Test
    fun testSaveAndLoadData_allKeysPersistedCorrectly() {
        val customRosterNight = listOf(Soldier("Đồng chí A", 5))
        val customRosterDay = listOf(Soldier("Đồng chí B", 2))
        val customShiftsDay = listOf(ShiftItem("07:00 - 09:00", "Đồng chí B"))
        val customShiftsNight = listOf(ShiftItem("19:00 - 21:00", "Đồng chí A"))
        val customHistory = listOf(
            DutyHistoryRecord(
                date = "18/09/2026",
                mode = "Gác Ngày",
                shifts = customShiftsDay
            )
        )
        val customAlarm = "17:30"

        storage.saveData(
            rosterNight = customRosterNight,
            rosterDay = customRosterDay,
            shiftsDay = customShiftsDay,
            shiftsNight = customShiftsNight,
            dutyHistory = customHistory,
            alarmTime = customAlarm
        )

        val loaded = storage.loadData()
        assertEquals(customAlarm, loaded.alarmTime)
        assertEquals("Đồng chí A", loaded.rosterNight.first().name)
        assertEquals(5, loaded.rosterNight.first().offDays)
        assertEquals("Đồng chí B", loaded.rosterDay.first().name)
        assertEquals("07:00 - 09:00", loaded.shiftsDay.first().time)
        assertEquals("Đồng chí B", loaded.shiftsDay.first().soldier)
        assertEquals("19:00 - 21:00", loaded.shiftsNight.first().time)
        assertEquals(1, loaded.dutyHistory.size)
        assertEquals("18/09/2026", loaded.dutyHistory.first().date)
    }

    @Test
    fun testClearDataToDefaults_resetsAllKeys() {
        // Save modified data first
        storage.saveData(
            rosterNight = listOf(Soldier("Tạm thời", 10)),
            rosterDay = listOf(Soldier("Tạm thời 2", 12)),
            shiftsDay = listOf(ShiftItem("11:00 - 13:00", "Tạm thời 2")),
            shiftsNight = emptyList(),
            dutyHistory = listOf(DutyHistoryRecord("10/09/2026", "Gác Đêm", emptyList())),
            alarmTime = "05:00"
        )

        // Clear all to defaults
        val resetData = storage.clearDataToDefaults()
        assertEquals(DutyDefaults.DEFAULT_ALARM_TIME, resetData.alarmTime)
        assertEquals(DutyDefaults.defaultShiftsDay.size, resetData.shiftsDay.size)
        assertEquals(DutyDefaults.defaultShiftsNight.size, resetData.shiftsNight.size)
        assertTrue(resetData.dutyHistory.isEmpty())

        // Verify persisted state after reload
        val reloaded = storage.loadData()
        assertEquals(DutyDefaults.DEFAULT_ALARM_TIME, reloaded.alarmTime)
        assertTrue(reloaded.dutyHistory.isEmpty())
    }
}
