package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DutyStorage
import com.example.model.DutyHistoryRecord
import com.example.model.ShiftItem
import com.example.model.Soldier
import com.example.util.DutyNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DutyTab(val title: String) {
    DAY("Gác Ngày"),
    NIGHT("Gác Đêm"),
    ROSTER_NIGHT("QS Gác Đêm"),
    ALARM("⏰ Hẹn Giờ"),
    PREVIEW("🔮 Xem Trước"),
    HISTORY("📜 Lịch Sử")
}

data class InAppToast(
    val title: String,
    val message: String,
    val id: Long = System.currentTimeMillis()
)

data class PreviewDayCard(
    val title: String,
    val dateStr: String,
    val modeText: String,
    val shifts: List<ShiftItem>
)

data class DutyUiState(
    val currentTab: DutyTab = DutyTab.DAY,
    val isEditing: Boolean = false,
    val shiftsDay: List<ShiftItem> = emptyList(),
    val shiftsNight: List<ShiftItem> = emptyList(),
    val rosterDay: List<Soldier> = emptyList(),
    val rosterNight: List<Soldier> = emptyList(),
    val dutyHistory: List<DutyHistoryRecord> = emptyList(),
    val alarmTime: String = "16:00",
    val previewMode: String = "day", // "day" or "night"
    val previewDaysLimit: Int = 3, // 1..5
    val previewCards: List<PreviewDayCard> = emptyList(),
    val toast: InAppToast? = null,
    val showClearDialog: Boolean = false,
    val showAddHistoryDialog: Boolean = false,
    val transferCode: String = ""
)

class DutyViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = DutyStorage(application)
    private val _uiState = MutableStateFlow(DutyUiState())
    val uiState: StateFlow<DutyUiState> = _uiState.asStateFlow()

    private var lastAutoAssignDate: String = ""
    private var last6AMProcessedDate: String = ""

    init {
        loadData()
        startPeriodicAlarmChecker()
    }

    /**
     * Correctly retrieves all persisted keys on page load as required.
     */
    fun loadData() {
        val data = storage.loadData()
        lastAutoAssignDate = data.lastAutoAssignDate
        last6AMProcessedDate = data.last6AMProcessedDate

        _uiState.update { current ->
            current.copy(
                shiftsDay = data.shiftsDay,
                shiftsNight = data.shiftsNight,
                rosterDay = data.rosterDay,
                rosterNight = data.rosterNight,
                dutyHistory = data.dutyHistory,
                alarmTime = data.alarmTime
            )
        }
        check6AMReset()
        regeneratePreview()
    }

    private fun persistCurrentState() {
        val s = _uiState.value
        storage.saveData(
            rosterNight = s.rosterNight,
            rosterDay = s.rosterDay,
            shiftsDay = s.shiftsDay,
            shiftsNight = s.shiftsNight,
            dutyHistory = s.dutyHistory,
            alarmTime = s.alarmTime,
            lastAutoAssignDate = lastAutoAssignDate,
            last6AMProcessedDate = last6AMProcessedDate
        )
    }

    /**
     * Clear button action: resets all stored data to defaults with toast notification.
     */
    fun clearDataToDefaults() {
        val freshData = storage.clearDataToDefaults()
        lastAutoAssignDate = ""
        last6AMProcessedDate = ""
        _uiState.update { current ->
            current.copy(
                shiftsDay = freshData.shiftsDay,
                shiftsNight = freshData.shiftsNight,
                rosterDay = freshData.rosterDay,
                rosterNight = freshData.rosterNight,
                dutyHistory = freshData.dutyHistory,
                alarmTime = freshData.alarmTime,
                isEditing = false,
                showClearDialog = false,
                transferCode = ""
            )
        }
        regeneratePreview()
        showToast("🗑️ ĐÃ ĐẶT LẠI MẶC ĐỊNH", "Đã xóa toàn bộ dữ liệu và khôi phục cài đặt gốc!")
    }

    fun setClearDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showClearDialog = visible) }
    }

    fun setAddHistoryDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddHistoryDialog = visible) }
    }

    fun switchTab(tab: DutyTab) {
        _uiState.update { it.copy(currentTab = tab, isEditing = false) }
        if (tab == DutyTab.PREVIEW) {
            regeneratePreview()
        }
    }

    fun toggleEdit() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    // --- Shifts editing ---

    fun updateShiftTime(isDay: Boolean, index: Int, newTime: String) {
        _uiState.update { s ->
            if (isDay) {
                val list = s.shiftsDay.toMutableList()
                if (index in list.indices) {
                    list[index] = list[index].copy(time = newTime)
                }
                s.copy(shiftsDay = list)
            } else {
                val list = s.shiftsNight.toMutableList()
                if (index in list.indices) {
                    list[index] = list[index].copy(time = newTime)
                }
                s.copy(shiftsNight = list)
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    fun updateShiftSoldier(isDay: Boolean, index: Int, soldierName: String) {
        _uiState.update { s ->
            if (isDay) {
                val list = s.shiftsDay.toMutableList()
                if (index in list.indices) {
                    list[index] = list[index].copy(soldier = soldierName)
                }
                s.copy(shiftsDay = list)
            } else {
                val list = s.shiftsNight.toMutableList()
                if (index in list.indices) {
                    list[index] = list[index].copy(soldier = soldierName)
                }
                s.copy(shiftsNight = list)
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    fun addShift(isDay: Boolean) {
        _uiState.update { s ->
            if (isDay) {
                s.copy(shiftsDay = s.shiftsDay + ShiftItem("00:00 - 00:00", ""))
            } else {
                s.copy(shiftsNight = s.shiftsNight + ShiftItem("00:00 - 00:00", ""))
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    fun deleteShift(isDay: Boolean, index: Int) {
        _uiState.update { s ->
            if (isDay) {
                val list = s.shiftsDay.toMutableList()
                if (index in list.indices) list.removeAt(index)
                s.copy(shiftsDay = list)
            } else {
                val list = s.shiftsNight.toMutableList()
                if (index in list.indices) list.removeAt(index)
                s.copy(shiftsNight = list)
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    // --- Roster editing ---

    fun addSoldier(isDay: Boolean, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { s ->
            if (isDay) {
                s.copy(rosterDay = s.rosterDay + Soldier(trimmed, offDays = 3))
            } else {
                s.copy(rosterNight = s.rosterNight + Soldier(trimmed, offDays = 3))
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    fun removeSoldier(isDay: Boolean, index: Int) {
        _uiState.update { s ->
            if (isDay) {
                val list = s.rosterDay.toMutableList()
                if (index in list.indices) list.removeAt(index)
                s.copy(rosterDay = list)
            } else {
                val list = s.rosterNight.toMutableList()
                if (index in list.indices) list.removeAt(index)
                s.copy(rosterNight = list)
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    fun adjustOffDays(isDay: Boolean, index: Int, delta: Int) {
        _uiState.update { s ->
            if (isDay) {
                val list = s.rosterDay.toMutableList()
                if (index in list.indices) {
                    val cur = list[index]
                    list[index] = cur.copy(offDays = (cur.offDays + delta).coerceAtLeast(0))
                }
                s.copy(rosterDay = list)
            } else {
                val list = s.rosterNight.toMutableList()
                if (index in list.indices) {
                    val cur = list[index]
                    list[index] = cur.copy(offDays = (cur.offDays + delta).coerceAtLeast(0))
                }
                s.copy(rosterNight = list)
            }
        }
        persistCurrentState()
        regeneratePreview()
    }

    // --- Smart auto-rotation ---

    fun autoAssignCurrentMode(isDay: Boolean) {
        val s = _uiState.value
        val modeText = if (isDay) "Gác Ngày" else "Gác Đêm"
        val roster = if (isDay) s.rosterDay else s.rosterNight
        val shifts = if (isDay) s.shiftsDay else s.shiftsNight

        val assignedShifts = computeSmartAssignment(roster, shifts, s.dutyHistory, modeText)

        _uiState.update { cur ->
            if (isDay) cur.copy(shiftsDay = assignedShifts)
            else cur.copy(shiftsNight = assignedShifts)
        }
        persistCurrentState()
        regeneratePreview()
        showToast("🔄 ĐÃ XOAY CA", "Đã cập nhật thứ tự phân ca tự động!")
    }

    private fun computeSmartAssignment(
        roster: List<Soldier>,
        shifts: List<ShiftItem>,
        history: List<DutyHistoryRecord>,
        modeText: String
    ): List<ShiftItem> {
        if (roster.isEmpty() || shifts.isEmpty()) return shifts
        val numShifts = shifts.size
        val lastShiftMap = mutableMapOf<String, Int>()

        // Scan history from newest to oldest
        for (h in history) {
            if (h.mode == modeText) {
                h.shifts.forEachIndexed { idx, shift ->
                    if (shift.soldier.isNotEmpty() && !lastShiftMap.containsKey(shift.soldier)) {
                        lastShiftMap[shift.soldier] = idx
                    }
                }
            }
        }

        val sortedRoster = roster.sortedWith { a, b ->
            val daysCompare = b.offDays.compareTo(a.offDays)
            if (daysCompare != 0) {
                daysCompare
            } else {
                val lastA = lastShiftMap[a.name] ?: -1
                val lastB = lastShiftMap[b.name] ?: -1
                lastA.compareTo(lastB)
            }
        }

        val resultShifts = shifts.map { it.copy(soldier = "") }.toMutableList()
        val maxToAssign = minOf(sortedRoster.size, numShifts)
        val candidateList = sortedRoster.take(maxToAssign)

        candidateList.forEach { soldierObj ->
            val name = soldierObj.name
            val lastIdx = lastShiftMap[name]
            val targetIdx = if (lastIdx != null && lastIdx >= 0) (lastIdx + 1) % numShifts else 0

            if (resultShifts[targetIdx].soldier.isEmpty()) {
                resultShifts[targetIdx] = resultShifts[targetIdx].copy(soldier = name)
            } else {
                for (offset in 1 until numShifts) {
                    val posIdx = (targetIdx + offset) % numShifts
                    if (resultShifts[posIdx].soldier.isEmpty()) {
                        resultShifts[posIdx] = resultShifts[posIdx].copy(soldier = name)
                        break
                    }
                    val negIdx = (targetIdx - offset + numShifts) % numShifts
                    if (resultShifts[negIdx].soldier.isEmpty()) {
                        resultShifts[negIdx] = resultShifts[negIdx].copy(soldier = name)
                        break
                    }
                }
            }
        }
        return resultShifts
    }

    // --- Save Today's Duty ---

    fun saveTodayDuty(isDay: Boolean, showAlert: Boolean = true) {
        val s = _uiState.value
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val modeText = if (isDay) "Gác Ngày" else "Gác Đêm"
        val currentShifts = if (isDay) s.shiftsDay else s.shiftsNight

        val historyList = s.dutyHistory.toMutableList()
        val existingIndex = historyList.indexOfFirst { it.date == todayStr && it.mode == modeText }

        val newRecord = DutyHistoryRecord(
            date = todayStr,
            mode = modeText,
            shifts = currentShifts.map { it.copy() }
        )

        if (existingIndex >= 0) {
            historyList[existingIndex] = newRecord
        } else {
            historyList.add(0, newRecord)
        }

        val trimmedHistory = if (historyList.size > 7) historyList.take(7) else historyList
        _uiState.update { it.copy(dutyHistory = trimmedHistory) }
        persistCurrentState()
        regeneratePreview()

        if (showAlert) {
            showToast("💾 ĐÃ LƯU", "Đã lưu lịch cắt gác hôm nay vào lịch sử!")
        }
    }

    // --- Save Manual History ---

    fun saveManualHistory(dateStr: String, modeText: String, assignedShifts: List<ShiftItem>) {
        val historyList = _uiState.value.dutyHistory.toMutableList()
        val existingIndex = historyList.indexOfFirst { it.date == dateStr && it.mode == modeText }
        val newRecord = DutyHistoryRecord(
            date = dateStr,
            mode = modeText,
            shifts = assignedShifts
        )

        if (existingIndex >= 0) {
            historyList[existingIndex] = newRecord
        } else {
            historyList.add(0, newRecord)
        }

        val trimmed = if (historyList.size > 7) historyList.take(7) else historyList
        _uiState.update { it.copy(dutyHistory = trimmed, showAddHistoryDialog = false) }
        persistCurrentState()
        regeneratePreview()
        showToast("✅ ĐÃ THÊM LỊCH SỬ", "Đã lưu dữ liệu ngày $dateStr")
    }

    // --- Alarm & Periodic check ---

    fun saveAlarmTime(newTime: String) {
        _uiState.update { it.copy(alarmTime = newTime) }
        persistCurrentState()
        showToast("⏰ ĐÃ LƯU HẸN GIỜ", "Cắt gác tự động vào lúc $newTime hằng ngày.")
    }

    private fun startPeriodicAlarmChecker() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Check every 10 seconds
                check6AMReset()
                checkAlarmTick()
            }
        }
    }

    private fun check6AMReset() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)

        if (currentHour >= 6 && last6AMProcessedDate != todayStr) {
            now.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)

            val guardedDay = mutableSetOf<String>()
            val guardedNight = mutableSetOf<String>()

            _uiState.value.dutyHistory.forEach { h ->
                if (h.date == todayStr || h.date == yesterdayStr) {
                    if (h.mode == "Gác Ngày") {
                        h.shifts.forEach { if (it.soldier.isNotEmpty()) guardedDay.add(it.soldier) }
                    } else if (h.mode == "Gác Đêm") {
                        h.shifts.forEach { if (it.soldier.isNotEmpty()) guardedNight.add(it.soldier) }
                    }
                }
            }

            _uiState.update { s ->
                val newRosterDay = s.rosterDay.map { soldier ->
                    if (guardedDay.contains(soldier.name)) soldier.copy(offDays = 0)
                    else soldier.copy(offDays = soldier.offDays + 1)
                }
                val newRosterNight = s.rosterNight.map { soldier ->
                    if (guardedNight.contains(soldier.name)) soldier.copy(offDays = 0)
                    else soldier.copy(offDays = soldier.offDays + 1)
                }
                s.copy(rosterDay = newRosterDay, rosterNight = newRosterNight)
            }

            last6AMProcessedDate = todayStr
            persistCurrentState()
        }
    }

    private fun checkAlarmTick() {
        val now = Calendar.getInstance()
        val currentHM = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)

        if (currentHM == _uiState.value.alarmTime && lastAutoAssignDate != todayStr) {
            lastAutoAssignDate = todayStr
            autoAssignCurrentMode(isDay = true)
            autoAssignCurrentMode(isDay = false)
            saveTodayDuty(isDay = true, showAlert = false)
            saveTodayDuty(isDay = false, showAlert = false)

            DutyNotificationHelper.sendSystemNotification(
                getApplication(),
                "📢 TỰ ĐỘNG CẮT GÁC",
                "Hệ thống đã tự động xoay ca gác hôm nay!"
            )
            showToast("📢 TỰ ĐỘNG CẮT GÁC", "Hệ thống đã tự động xoay ca gác hôm nay!")
        }
    }

    fun triggerTestNotification(context: Context) {
        DutyNotificationHelper.sendSystemNotification(
            context,
            "🔔 THỬ THÔNG BÁO",
            "Hệ thống thông báo cắt gác đang hoạt động rất tốt!"
        )
        showToast("🔔 THỬ THÔNG BÁO", "Chuông, rung và thông báo hệ thống đã phát!")
    }

    // --- Data Export & Import ---

    fun updateTransferCode(code: String) {
        _uiState.update { it.copy(transferCode = code) }
    }

    fun exportDataToClipboard(context: Context) {
        val currentData = storage.loadData().copy(
            rosterNight = _uiState.value.rosterNight,
            rosterDay = _uiState.value.rosterDay,
            shiftsDay = _uiState.value.shiftsDay,
            shiftsNight = _uiState.value.shiftsNight,
            dutyHistory = _uiState.value.dutyHistory,
            alarmTime = _uiState.value.alarmTime
        )
        val jsonStr = storage.exportToJson(currentData)
        _uiState.update { it.copy(transferCode = jsonStr) }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mã Dữ Liệu Cắt Gác", jsonStr)
        clipboard.setPrimaryClip(clip)

        showToast("📋 ĐÃ COPY MÃ", "Đã chép mã dữ liệu vào bộ nhớ tạm!")
    }

    fun importDataFromInput(jsonInput: String) {
        val trimmed = jsonInput.trim()
        if (trimmed.isEmpty()) {
            showToast("⚠️ LỖI", "Vui lòng dán mã dữ liệu!")
            return
        }
        val imported = storage.importFromJson(trimmed)
        if (imported != null) {
            _uiState.update { current ->
                current.copy(
                    shiftsDay = imported.shiftsDay,
                    shiftsNight = imported.shiftsNight,
                    rosterDay = imported.rosterDay,
                    rosterNight = imported.rosterNight,
                    dutyHistory = imported.dutyHistory,
                    alarmTime = imported.alarmTime,
                    transferCode = ""
                )
            }
            regeneratePreview()
            showToast("📥 NHẬP THÀNH CÔNG", "Khôi phục dữ liệu hoàn tất!")
        } else {
            showToast("⚠️ LỖI", "Mã dữ liệu không hợp lệ!")
        }
    }

    // --- Preview N Days (1 to 5) ---

    fun switchPreviewMode(mode: String) {
        _uiState.update { it.copy(previewMode = mode) }
        regeneratePreview()
    }

    fun changePreviewDays(delta: Int) {
        val current = _uiState.value.previewDaysLimit
        val next = (current + delta).coerceIn(1, 5)
        if (next != current) {
            _uiState.update { it.copy(previewDaysLimit = next) }
            regeneratePreview()
        }
    }

    private fun regeneratePreview() {
        val s = _uiState.value
        val isDay = s.previewMode == "day"
        val modeText = if (isDay) "Gác Ngày" else "Gác Đêm"
        val baseRoster = if (isDay) s.rosterDay else s.rosterNight
        val baseShifts = if (isDay) s.shiftsDay else s.shiftsNight

        var simRoster = baseRoster.map { it.copy() }
        val simHistory = s.dutyHistory.map { it.copy(shifts = it.shifts.map { sh -> sh.copy() }) }.toMutableList()

        val cards = mutableListOf<PreviewDayCard>()
        val calendar = Calendar.getInstance()

        for (d in 0 until s.previewDaysLimit) {
            val simCal = calendar.clone() as Calendar
            simCal.add(Calendar.DAY_OF_YEAR, d)
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(simCal.time)

            val predictedShifts = computeSmartAssignment(simRoster, baseShifts, simHistory, modeText)
            val dayTitle = when (d) {
                0 -> "HÔM NAY"
                1 -> "NGÀY MAI"
                else -> "NGÀY THỨ ${d + 1}"
            }

            cards.add(
                PreviewDayCard(
                    title = dayTitle,
                    dateStr = dateStr,
                    modeText = modeText,
                    shifts = predictedShifts
                )
            )

            // Update simulation history and offDays for next day calculation
            val guardedSet = predictedShifts.filter { it.soldier.isNotEmpty() }.map { it.soldier }.toSet()
            simHistory.add(0, DutyHistoryRecord(dateStr, modeText, predictedShifts))
            simRoster = simRoster.map { soldier ->
                if (guardedSet.contains(soldier.name)) soldier.copy(offDays = 0)
                else soldier.copy(offDays = soldier.offDays + 1)
            }
        }

        _uiState.update { it.copy(previewCards = cards) }
    }

    // --- In-App Toast helper ---

    fun showToast(title: String, message: String) {
        val toast = InAppToast(title, message)
        _uiState.update { it.copy(toast = toast) }
        viewModelScope.launch {
            delay(3500)
            _uiState.update { if (it.toast?.id == toast.id) it.copy(toast = null) else it }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }
}
