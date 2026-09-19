package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DutyDefaults
import com.example.model.DutyHistoryRecord
import com.example.model.ShiftItem
import com.example.model.Soldier
import org.json.JSONArray
import org.json.JSONObject

data class DutyStoredData(
    val rosterNight: List<Soldier>,
    val rosterDay: List<Soldier>,
    val shiftsDay: List<ShiftItem>,
    val shiftsNight: List<ShiftItem>,
    val dutyHistory: List<DutyHistoryRecord>,
    val alarmTime: String,
    val lastAutoAssignDate: String,
    val last6AMProcessedDate: String
)

class DutyStorage(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("unit_duty_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_ROSTER_NIGHT = "rosterNight"
        const val KEY_ROSTER_DAY = "rosterDay"
        const val KEY_SHIFTS_DAY = "shiftsDay"
        const val KEY_SHIFTS_NIGHT = "shiftsNight"
        const val KEY_DUTY_HISTORY = "dutyHistory"
        const val KEY_ALARM_TIME = "alarmTime"
        const val KEY_LAST_AUTO_ASSIGN_DATE = "lastAutoAssignDate"
        const val KEY_LAST_6AM_PROCESSED_DATE = "last6AMProcessedDate"
    }

    /**
     * Correctly retrieves all persisted keys on page load as required.
     */
    fun loadData(): DutyStoredData {
        val rosterNight = parseSoldierList(prefs.getString(KEY_ROSTER_NIGHT, null))
            ?: DutyDefaults.defaultRosterNight
        val rosterDay = parseSoldierList(prefs.getString(KEY_ROSTER_DAY, null))
            ?: DutyDefaults.defaultRosterDay
        val shiftsDay = parseShiftList(prefs.getString(KEY_SHIFTS_DAY, null))
            ?: DutyDefaults.defaultShiftsDay
        val shiftsNight = parseShiftList(prefs.getString(KEY_SHIFTS_NIGHT, null))
            ?: DutyDefaults.defaultShiftsNight
        val dutyHistory = parseHistoryList(prefs.getString(KEY_DUTY_HISTORY, null))
            ?: emptyList()
        val alarmTime = prefs.getString(KEY_ALARM_TIME, DutyDefaults.DEFAULT_ALARM_TIME)
            ?: DutyDefaults.DEFAULT_ALARM_TIME
        val lastAutoAssignDate = prefs.getString(KEY_LAST_AUTO_ASSIGN_DATE, "") ?: ""
        val last6AMProcessedDate = prefs.getString(KEY_LAST_6AM_PROCESSED_DATE, "") ?: ""

        return DutyStoredData(
            rosterNight = rosterNight,
            rosterDay = rosterDay,
            shiftsDay = shiftsDay,
            shiftsNight = shiftsNight,
            dutyHistory = dutyHistory,
            alarmTime = alarmTime,
            lastAutoAssignDate = lastAutoAssignDate,
            last6AMProcessedDate = last6AMProcessedDate
        )
    }

    /**
     * Persists all data structures to local storage under the exact keys.
     */
    fun saveData(
        rosterNight: List<Soldier>,
        rosterDay: List<Soldier>,
        shiftsDay: List<ShiftItem>,
        shiftsNight: List<ShiftItem>,
        dutyHistory: List<DutyHistoryRecord>,
        alarmTime: String,
        lastAutoAssignDate: String = "",
        last6AMProcessedDate: String = ""
    ) {
        val trimmedHistory = if (dutyHistory.size > 7) dutyHistory.take(7) else dutyHistory
        prefs.edit().apply {
            putString(KEY_ROSTER_NIGHT, serializeSoldierList(rosterNight))
            putString(KEY_ROSTER_DAY, serializeSoldierList(rosterDay))
            putString(KEY_SHIFTS_DAY, serializeShiftList(shiftsDay))
            putString(KEY_SHIFTS_NIGHT, serializeShiftList(shiftsNight))
            putString(KEY_DUTY_HISTORY, serializeHistoryList(trimmedHistory))
            putString(KEY_ALARM_TIME, alarmTime)
            if (lastAutoAssignDate.isNotEmpty()) {
                putString(KEY_LAST_AUTO_ASSIGN_DATE, lastAutoAssignDate)
            }
            if (last6AMProcessedDate.isNotEmpty()) {
                putString(KEY_LAST_6AM_PROCESSED_DATE, last6AMProcessedDate)
            }
            apply()
        }
    }

    /**
     * Reset all stored data to defaults, clearing user modifications.
     */
    fun clearDataToDefaults(): DutyStoredData {
        prefs.edit().clear().apply()
        // Save fresh defaults immediately so they are initialized
        saveData(
            rosterNight = DutyDefaults.defaultRosterNight,
            rosterDay = DutyDefaults.defaultRosterDay,
            shiftsDay = DutyDefaults.defaultShiftsDay,
            shiftsNight = DutyDefaults.defaultShiftsNight,
            dutyHistory = emptyList(),
            alarmTime = DutyDefaults.DEFAULT_ALARM_TIME,
            lastAutoAssignDate = "",
            last6AMProcessedDate = ""
        )
        return loadData()
    }

    fun exportToJson(data: DutyStoredData): String {
        val json = JSONObject()
        val rNightArray = JSONArray()
        data.rosterNight.forEach { s ->
            rNightArray.put(JSONObject().apply {
                put("name", s.name)
                put("offDays", s.offDays)
            })
        }
        val rDayArray = JSONArray()
        data.rosterDay.forEach { s ->
            rDayArray.put(JSONObject().apply {
                put("name", s.name)
                put("offDays", s.offDays)
            })
        }
        val sDayArray = JSONArray()
        data.shiftsDay.forEach { s ->
            sDayArray.put(JSONObject().apply {
                put("time", s.time)
                put("soldier", s.soldier)
            })
        }
        val sNightArray = JSONArray()
        data.shiftsNight.forEach { s ->
            sNightArray.put(JSONObject().apply {
                put("time", s.time)
                put("soldier", s.soldier)
            })
        }
        val histArray = JSONArray()
        data.dutyHistory.take(7).forEach { h ->
            val hObj = JSONObject().apply {
                put("date", h.date)
                put("mode", h.mode)
                val shifts = JSONArray()
                h.shifts.forEach { s ->
                    shifts.put(JSONObject().apply {
                        put("time", s.time)
                        put("soldier", s.soldier)
                    })
                }
                put("shifts", shifts)
            }
            histArray.put(hObj)
        }

        json.put(KEY_ROSTER_NIGHT, rNightArray)
        json.put(KEY_ROSTER_DAY, rDayArray)
        json.put(KEY_SHIFTS_DAY, sDayArray)
        json.put(KEY_SHIFTS_NIGHT, sNightArray)
        json.put("historyData", histArray)
        json.put(KEY_DUTY_HISTORY, histArray)
        json.put(KEY_ALARM_TIME, data.alarmTime)

        return json.toString(2)
    }

    fun importFromJson(jsonStr: String): DutyStoredData? {
        return try {
            val json = JSONObject(jsonStr)
            val rosterNight = if (json.has(KEY_ROSTER_NIGHT)) {
                parseSoldierArray(json.getJSONArray(KEY_ROSTER_NIGHT))
            } else DutyDefaults.defaultRosterNight

            val rosterDay = if (json.has(KEY_ROSTER_DAY)) {
                parseSoldierArray(json.getJSONArray(KEY_ROSTER_DAY))
            } else DutyDefaults.defaultRosterDay

            val shiftsDay = if (json.has(KEY_SHIFTS_DAY)) {
                parseShiftArray(json.getJSONArray(KEY_SHIFTS_DAY))
            } else DutyDefaults.defaultShiftsDay

            val shiftsNight = if (json.has(KEY_SHIFTS_NIGHT)) {
                parseShiftArray(json.getJSONArray(KEY_SHIFTS_NIGHT))
            } else DutyDefaults.defaultShiftsNight

            val historyArray = when {
                json.has(KEY_DUTY_HISTORY) -> json.getJSONArray(KEY_DUTY_HISTORY)
                json.has("historyData") -> json.getJSONArray("historyData")
                else -> null
            }
            val dutyHistory = if (historyArray != null) parseHistoryArray(historyArray) else emptyList()
            val alarmTime = if (json.has(KEY_ALARM_TIME)) json.getString(KEY_ALARM_TIME) else DutyDefaults.DEFAULT_ALARM_TIME

            val trimmedHistory = if (dutyHistory.size > 7) dutyHistory.take(7) else dutyHistory

            saveData(
                rosterNight = rosterNight,
                rosterDay = rosterDay,
                shiftsDay = shiftsDay,
                shiftsNight = shiftsNight,
                dutyHistory = trimmedHistory,
                alarmTime = alarmTime
            )
            loadData()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- JSON Serialization Helpers ---

    private fun serializeSoldierList(list: List<Soldier>): String {
        val array = JSONArray()
        list.forEach { s ->
            array.put(JSONObject().apply {
                put("name", s.name)
                put("offDays", s.offDays)
            })
        }
        return array.toString()
    }

    private fun parseSoldierList(raw: String?): List<Soldier>? {
        if (raw.isNullOrBlank()) return null
        return try {
            parseSoldierArray(JSONArray(raw))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSoldierArray(array: JSONArray): List<Soldier> {
        val list = mutableListOf<Soldier>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "").trim()
            val offDays = obj.optInt("offDays", 3)
            if (name.isNotEmpty()) {
                list.add(Soldier(name, offDays))
            }
        }
        return list
    }

    private fun serializeShiftList(list: List<ShiftItem>): String {
        val array = JSONArray()
        list.forEach { s ->
            array.put(JSONObject().apply {
                put("time", s.time)
                put("soldier", s.soldier)
            })
        }
        return array.toString()
    }

    private fun parseShiftList(raw: String?): List<ShiftItem>? {
        if (raw.isNullOrBlank()) return null
        return try {
            parseShiftArray(JSONArray(raw))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseShiftArray(array: JSONArray): List<ShiftItem> {
        val list = mutableListOf<ShiftItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val time = obj.optString("time", "00:00 - 00:00")
            val soldier = obj.optString("soldier", "")
            list.add(ShiftItem(time, soldier))
        }
        return list
    }

    private fun serializeHistoryList(list: List<DutyHistoryRecord>): String {
        val array = JSONArray()
        list.forEach { h ->
            val obj = JSONObject().apply {
                put("date", h.date)
                put("mode", h.mode)
                val sArray = JSONArray()
                h.shifts.forEach { s ->
                    sArray.put(JSONObject().apply {
                        put("time", s.time)
                        put("soldier", s.soldier)
                    })
                }
                put("shifts", sArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseHistoryList(raw: String?): List<DutyHistoryRecord>? {
        if (raw.isNullOrBlank()) return null
        return try {
            parseHistoryArray(JSONArray(raw))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHistoryArray(array: JSONArray): List<DutyHistoryRecord> {
        val list = mutableListOf<DutyHistoryRecord>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val date = obj.optString("date", "")
            val mode = obj.optString("mode", "Gác Ngày")
            val shifts = if (obj.has("shifts")) parseShiftArray(obj.getJSONArray("shifts")) else emptyList()
            if (date.isNotEmpty()) {
                list.add(DutyHistoryRecord(date, mode, shifts))
            }
        }
        return list
    }
}
