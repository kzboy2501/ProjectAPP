package com.example.model

data class ShiftItem(
    val time: String,
    val soldier: String = ""
)

data class Soldier(
    val name: String,
    val offDays: Int = 3
)

data class DutyHistoryRecord(
    val date: String,
    val mode: String, // "Gác Ngày" or "Gác Đêm"
    val shifts: List<ShiftItem>
)

object DutyDefaults {
    val defaultShiftsDay = listOf(
        ShiftItem("06:00 - 08:00", ""),
        ShiftItem("08:00 - 10:00", ""),
        ShiftItem("10:00 - 12:00", ""),
        ShiftItem("12:00 - 14:00", "")
    )

    val defaultShiftsNight = listOf(
        ShiftItem("18:00 - 20:00", ""),
        ShiftItem("20:00 - 22:00", ""),
        ShiftItem("22:00 - 00:00", ""),
        ShiftItem("00:00 - 02:00", ""),
        ShiftItem("02:00 - 04:00", ""),
        ShiftItem("04:00 - 06:00", "")
    )

    val defaultRosterDay = listOf(
        Soldier("Nguyễn Văn An", 3),
        Soldier("Trần Đình Bình", 3),
        Soldier("Lê Hoàng Cường", 2),
        Soldier("Phạm Quốc Dũng", 4),
        Soldier("Võ Minh Đức", 1)
    )

    val defaultRosterNight = listOf(
        Soldier("Vũ Minh Hải", 4),
        Soldier("Hoàng Văn Tuấn", 3),
        Soldier("Đỗ Quốc Toàn", 3),
        Soldier("Bùi Trọng Nam", 2),
        Soldier("Ngô Thành Đạt", 3),
        Soldier("Đặng Văn Lâm", 4),
        Soldier("Phan Anh Vũ", 1),
        Soldier("Trịnh Tiến Dũng", 2)
    )

    const val DEFAULT_ALARM_TIME = "16:00"
}
