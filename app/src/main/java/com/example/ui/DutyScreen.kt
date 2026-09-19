package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ShiftItem
import com.example.model.Soldier
import com.example.ui.theme.MilitaryAccent
import com.example.ui.theme.MilitaryBg
import com.example.ui.theme.MilitaryBorder
import com.example.ui.theme.MilitaryCard
import com.example.ui.theme.MilitaryDanger
import com.example.ui.theme.MilitaryLightGreen
import com.example.ui.theme.MilitaryPrimary
import com.example.ui.theme.MilitaryPrimaryDark
import com.example.ui.theme.MilitarySuccessBg
import com.example.ui.theme.MilitarySuccessText
import com.example.ui.theme.MilitaryText
import com.example.ui.theme.MilitaryWarnBg
import com.example.ui.theme.MilitaryWarnText
import com.example.viewmodel.DutyTab
import com.example.viewmodel.DutyUiState
import com.example.viewmodel.DutyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DutyScreen(viewModel: DutyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.showToast(
                "✅ ĐÃ BẬT THÔNG BÁO",
                "Ứng dụng đã được cấp quyền thông báo hệ thống!"
            )
        } else {
            viewModel.showToast(
                "🔔 ĐÃ KÍCH HOẠT",
                "Đang sử dụng chế độ chuông & rung nội bộ app."
            )
        }
    }

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MilitaryBg,
        topBar = {
            DutyHeader()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Navigation Tabs
                DutyTabsBar(
                    currentTab = uiState.currentTab,
                    onTabSelected = { viewModel.switchTab(it) }
                )

                // Scrollable Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when (uiState.currentTab) {
                        DutyTab.DAY -> DayDutyPanel(viewModel, uiState)
                        DutyTab.NIGHT -> NightDutyPanel(viewModel, uiState)
                        DutyTab.ROSTER_NIGHT -> NightRosterPanel(viewModel, uiState)
                        DutyTab.ALARM -> AlarmAndDataPanel(
                            viewModel = viewModel,
                            uiState = uiState,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.showToast(
                                        "🔔 ĐÃ KÍCH HOẠT",
                                        "Đã bật chế độ thông báo Chuông + Rung nội bộ app!"
                                    )
                                }
                            }
                        )
                        DutyTab.PREVIEW -> PreviewPanel(viewModel, uiState)
                        DutyTab.HISTORY -> HistoryPanel(viewModel, uiState)
                    }
                }
            }

            // In-App Toast Overlay
            AnimatedVisibility(
                visible = uiState.toast != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                uiState.toast?.let { toast ->
                    InAppToastView(
                        title = toast.title,
                        message = toast.message,
                        onDismiss = { viewModel.dismissToast() }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Clear All Data
    if (uiState.showClearDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setClearDialogVisible(false) },
            title = {
                Text(
                    text = "Xác nhận đặt lại mặc định?",
                    fontWeight = FontWeight.Bold,
                    color = MilitaryPrimaryDark
                )
            },
            text = {
                Text(
                    text = "Hành động này sẽ xóa toàn bộ danh sách quân số gác ngày/đêm, các khung giờ ca gác đã chỉnh sửa và toàn bộ lịch sử. Hệ thống sẽ được khôi phục về cấu hình ban đầu. Bạn có chắc chắn không?",
                    fontSize = 14.sp,
                    color = MilitaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearDataToDefaults() },
                    colors = ButtonDefaults.buttonColors(containerColor = MilitaryDanger),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Xác nhận xóa", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.setClearDialogVisible(false) },
                    modifier = Modifier.testTag("cancel_clear_button")
                ) {
                    Text("Hủy", color = MilitaryPrimary)
                }
            },
            containerColor = MilitaryCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Modal Dialog for Adding Past History Record
    if (uiState.showAddHistoryDialog) {
        AddHistoryDialog(
            uiState = uiState,
            onDismiss = { viewModel.setAddHistoryDialogVisible(false) },
            onSave = { dateStr, mode, shifts ->
                viewModel.saveManualHistory(dateStr, mode, shifts)
            }
        )
    }
}

// ==========================================
// HEADER & TOP BAR
// ==========================================

@Composable
fun DutyHeader() {
    Surface(
        color = MilitaryPrimary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield Emblem",
                tint = MilitaryLightGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "QUẢN LÝ CẮT GÁC ĐƠN VỊ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ==========================================
// NAVIGATION TABS
// ==========================================

@Composable
fun DutyTabsBar(
    currentTab: DutyTab,
    onTabSelected: (DutyTab) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MilitaryPrimaryDark)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DutyTab.values().forEach { tab ->
            val isActive = currentTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) MilitaryAccent else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    color = if (isActive) Color.White else MilitaryLightGreen,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// PANEL 1: GÁC NGÀY
// ==========================================

@Composable
fun DayDutyPanel(viewModel: DutyViewModel, uiState: DutyUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Table Card
        DutyTableCard(
            title = "Lịch Gác Ngày",
            isDay = true,
            shifts = uiState.shiftsDay,
            roster = uiState.rosterDay,
            isEditing = uiState.isEditing,
            onToggleEdit = { viewModel.toggleEdit() },
            onAutoAssign = { viewModel.autoAssignCurrentMode(isDay = true) },
            onUpdateTime = { idx, time -> viewModel.updateShiftTime(isDay = true, index = idx, newTime = time) },
            onUpdateSoldier = { idx, name -> viewModel.updateShiftSoldier(isDay = true, index = idx, soldierName = name) },
            onAddShift = { viewModel.addShift(isDay = true) },
            onDeleteShift = { idx -> viewModel.deleteShift(isDay = true, index = idx) },
            onSaveToday = { viewModel.saveTodayDuty(isDay = true) }
        )

        // Day Roster Card
        RosterCard(
            title = "Quân Số Gác Ngày",
            isDay = true,
            roster = uiState.rosterDay,
            onAddSoldier = { name -> viewModel.addSoldier(isDay = true, name = name) },
            onRemoveSoldier = { idx -> viewModel.removeSoldier(isDay = true, index = idx) },
            onAdjustOffDays = { idx, delta -> viewModel.adjustOffDays(isDay = true, index = idx, delta = delta) }
        )
    }
}

// ==========================================
// PANEL 2: GÁC ĐÊM
// ==========================================

@Composable
fun NightDutyPanel(viewModel: DutyViewModel, uiState: DutyUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DutyTableCard(
            title = "Lịch Gác Đêm",
            isDay = false,
            shifts = uiState.shiftsNight,
            roster = uiState.rosterNight,
            isEditing = uiState.isEditing,
            onToggleEdit = { viewModel.toggleEdit() },
            onAutoAssign = { viewModel.autoAssignCurrentMode(isDay = false) },
            onUpdateTime = { idx, time -> viewModel.updateShiftTime(isDay = false, index = idx, newTime = time) },
            onUpdateSoldier = { idx, name -> viewModel.updateShiftSoldier(isDay = false, index = idx, soldierName = name) },
            onAddShift = { viewModel.addShift(isDay = false) },
            onDeleteShift = { idx -> viewModel.deleteShift(isDay = false, index = idx) },
            onSaveToday = { viewModel.saveTodayDuty(isDay = false) }
        )
    }
}

// ==========================================
// PANEL 3: QS GÁC ĐÊM
// ==========================================

@Composable
fun NightRosterPanel(viewModel: DutyViewModel, uiState: DutyUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        RosterCard(
            title = "Danh Sách Đồng Chí Gác Đêm",
            isDay = false,
            roster = uiState.rosterNight,
            onAddSoldier = { name -> viewModel.addSoldier(isDay = false, name = name) },
            onRemoveSoldier = { idx -> viewModel.removeSoldier(isDay = false, index = idx) },
            onAdjustOffDays = { idx, delta -> viewModel.adjustOffDays(isDay = false, index = idx, delta = delta) }
        )
    }
}

// ==========================================
// PANEL 4: HẸN GIỜ & DỮ LIỆU
// ==========================================

@Composable
fun AlarmAndDataPanel(
    viewModel: DutyViewModel,
    uiState: DutyUiState,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var inputAlarmTime by remember(uiState.alarmTime) { mutableStateOf(uiState.alarmTime) }
    var transferInput by remember(uiState.transferCode) { mutableStateOf(uiState.transferCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Alarm & Permissions
        Card(
            colors = CardDefaults.cardColors(containerColor = MilitaryCard),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "⏰ Tự Động Cắt Gác Hằng Ngày",
                    fontWeight = FontWeight.Bold,
                    color = MilitaryPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Status badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hasNotificationPermission) MilitarySuccessBg else MilitaryWarnBg)
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasNotificationPermission)
                            "✓ Đã cấp quyền thông báo hệ thống"
                        else
                            "⚠️ Dùng thông báo chuông & rung nội bộ app",
                        color = if (hasNotificationPermission) MilitarySuccessText else MilitaryWarnText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_permission_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔔 Cấp Quyền Thông Báo Hệ Thống Direct", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { viewModel.triggerTestNotification(context) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF495057)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_notification_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔊 Thử Chuông & Thông Báo", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Giờ tự động cắt ca hằng ngày:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MilitaryText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputAlarmTime,
                        onValueChange = { inputAlarmTime = it },
                        placeholder = { Text("16:00") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alarm_time_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MilitaryPrimary,
                            unfocusedBorderColor = MilitaryBorder
                        )
                    )
                    Button(
                        onClick = { viewModel.saveAlarmTime(inputAlarmTime) },
                        colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("save_alarm_button")
                    ) {
                        Text("Lưu Giờ", fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 2: Data Transfer (Copy & Import)
        Card(
            colors = CardDefaults.cardColors(containerColor = MilitaryCard),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📋 Sao Chép & Nhập Dữ Liệu",
                    fontWeight = FontWeight.Bold,
                    color = MilitaryPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Button(
                    onClick = { viewModel.exportDataToClipboard(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_data_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Mã Dữ Liệu", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = transferInput,
                    onValueChange = {
                        transferInput = it
                        viewModel.updateTransferCode(it)
                    },
                    placeholder = { Text("Dán mã dữ liệu JSON vào đây để khôi phục...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("transfer_code_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MilitaryPrimary,
                        unfocusedBorderColor = MilitaryBorder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.importDataFromInput(transferInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_data_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nhập Dữ Liệu", fontSize = 13.sp)
                }
            }
        }

        // Section 3: Reset / Clear to defaults (REQUIRED)
        Card(
            colors = CardDefaults.cardColors(containerColor = MilitaryCard),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "⚙️ Quản Lý Bộ Nhớ & Đặt Lại",
                    fontWeight = FontWeight.Bold,
                    color = MilitaryPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Khôi phục dữ liệu ca gác, quân số và lịch sử về trạng thái mặc định ban đầu.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Button(
                    onClick = { viewModel.setClearDialogVisible(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = MilitaryDanger),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_data_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🗑️ Xóa Tất Cả Dữ Liệu (Reset về mặc định)", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// PANEL 5: XEM TRƯỚC LỊCH GÁC
// ==========================================

@Composable
fun PreviewPanel(viewModel: DutyViewModel, uiState: DutyUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MilitaryCard),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mode toggle buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.switchPreviewMode("day") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.previewMode == "day") MilitaryPrimary else Color(0xFF6C757D)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("preview_mode_day")
                        ) {
                            Text("Gác Ngày", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.switchPreviewMode("night") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.previewMode == "night") MilitaryPrimary else Color(0xFF6C757D)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("preview_mode_night")
                        ) {
                            Text("Gác Đêm", fontSize = 12.sp)
                        }
                    }

                    // Stepper: Xem X ngày
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Xem: ",
                            fontSize = 12.sp,
                            color = MilitaryText
                        )
                        Text(
                            text = "${uiState.previewDaysLimit} ngày",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MilitaryPrimaryDark
                        )
                        IconButton(
                            onClick = { viewModel.changePreviewDays(1) },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MilitaryPrimary)
                                .testTag("preview_days_plus")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tăng", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { viewModel.changePreviewDays(-1) },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6C757D))
                                .testTag("preview_days_minus")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Preview Cards for each simulated day
        uiState.previewCards.forEachIndexed { index, card ->
            val isToday = index == 0
            Card(
                colors = CardDefaults.cardColors(containerColor = MilitaryCard),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isToday) MilitaryPrimary else MilitaryBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📅 ${card.title} (${card.dateStr})",
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MilitaryPrimary else MilitaryPrimaryDark,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE2E8F0))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = card.modeText,
                                fontSize = 11.sp,
                                color = Color(0xFF4A5568),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Shift Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MilitaryBorder, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE9ECEF))
                                .padding(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Ca Gác",
                                fontWeight = FontWeight.SemiBold,
                                color = MilitaryPrimaryDark,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.4f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Dự Kiến",
                                fontWeight = FontWeight.SemiBold,
                                color = MilitaryPrimaryDark,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Rows
                        card.shifts.forEachIndexed { sIdx, shift ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (sIdx % 2 == 0) Color.White else Color(0xFFFBFBFB))
                                    .padding(vertical = 7.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = shift.time,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(0.4f),
                                    textAlign = TextAlign.Center,
                                    color = MilitaryText
                                )
                                Text(
                                    text = shift.soldier.ifEmpty { "(Trống)" },
                                    fontSize = 12.sp,
                                    fontWeight = if (shift.soldier.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (shift.soldier.isNotEmpty()) MilitaryText else Color(0xFFAAAAAA),
                                    modifier = Modifier.weight(0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (sIdx < card.shifts.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MilitaryBorder)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PANEL 6: LỊCH SỬ GÁC
// ==========================================

@Composable
fun HistoryPanel(viewModel: DutyViewModel, uiState: DutyUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MilitaryCard),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lịch Sử Cắt Gác (Tối đa 7 ngày)",
                        fontWeight = FontWeight.Bold,
                        color = MilitaryPrimary,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { viewModel.setAddHistoryDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("open_add_history_modal")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm", fontSize = 12.sp)
                    }
                }
            }
        }

        if (uiState.dutyHistory.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MilitaryCard),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có lịch sử lưu.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            uiState.dutyHistory.forEach { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MilitaryCard),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📅 Ngày ${record.date}",
                                fontWeight = FontWeight.Bold,
                                color = MilitaryPrimaryDark,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = record.mode,
                                    fontSize = 11.sp,
                                    color = Color(0xFF4A5568),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MilitaryBorder, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            record.shifts.forEachIndexed { idx, shift ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 0) Color.White else Color(0xFFF9F9F9))
                                        .padding(vertical = 6.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shift.time,
                                        fontSize = 12.sp,
                                        color = Color(0xFF495057)
                                    )
                                    Text(
                                        text = shift.soldier.ifEmpty { "(Trống)" },
                                        fontSize = 12.sp,
                                        fontWeight = if (shift.soldier.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (shift.soldier.isNotEmpty()) MilitaryText else Color(0xFFAAAAAA)
                                    )
                                }
                                if (idx < record.shifts.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MilitaryBorder)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REUSABLE COMPONENTS
// ==========================================

@Composable
fun DutyTableCard(
    title: String,
    isDay: Boolean,
    shifts: List<ShiftItem>,
    roster: List<Soldier>,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onAutoAssign: () -> Unit,
    onUpdateTime: (Int, String) -> Unit,
    onUpdateSoldier: (Int, String) -> Unit,
    onAddShift: () -> Unit,
    onDeleteShift: (Int) -> Unit,
    onSaveToday: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MilitaryCard),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MilitaryPrimary,
                    fontSize = 15.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onToggleEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag(if (isDay) "day_edit_toggle" else "night_edit_toggle")
                    ) {
                        Text(if (isEditing) "✔ Xong" else "✏️ Sửa", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onAutoAssign,
                        colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag(if (isDay) "day_auto_assign" else "night_auto_assign")
                    ) {
                        Text("🔄 Xoay ca", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shifts Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MilitaryBorder, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE9ECEF))
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ca Gác",
                        fontWeight = FontWeight.SemiBold,
                        color = MilitaryPrimaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(0.4f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Người Gác",
                        fontWeight = FontWeight.SemiBold,
                        color = MilitaryPrimaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(if (isEditing) 0.5f else 0.6f),
                        textAlign = TextAlign.Center
                    )
                    if (isEditing) {
                        Text(
                            text = "Xóa",
                            fontWeight = FontWeight.SemiBold,
                            color = MilitaryPrimaryDark,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(0.15f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Table Rows
                shifts.forEachIndexed { index, shift ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.White else Color(0xFFFAFAFA))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time column
                        if (isEditing) {
                            var editTime by remember(shift.time) { mutableStateOf(shift.time) }
                            OutlinedTextField(
                                value = editTime,
                                onValueChange = {
                                    editTime = it
                                    onUpdateTime(index, it)
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .testTag("shift_time_input_$index"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MilitaryPrimary,
                                    unfocusedBorderColor = MilitaryBorder
                                )
                            )
                        } else {
                            Text(
                                text = shift.time,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.4f),
                                textAlign = TextAlign.Center,
                                color = MilitaryText
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Soldier column
                        if (isEditing) {
                            var expanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .border(1.dp, MilitaryBorder, RoundedCornerShape(4.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { expanded = true }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("select_soldier_dropdown_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shift.soldier.ifEmpty { "-- Chọn --" },
                                    fontSize = 12.sp,
                                    color = if (shift.soldier.isNotEmpty()) MilitaryText else Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("-- Trống --", fontSize = 13.sp) },
                                        onClick = {
                                            onUpdateSoldier(index, "")
                                            expanded = false
                                        }
                                    )
                                    roster.forEach { soldier ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "${soldier.name} (Nghỉ ${soldier.offDays}d)",
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                onUpdateSoldier(index, soldier.name)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = shift.soldier.ifEmpty { "(Trống)" },
                                fontSize = 12.sp,
                                fontWeight = if (shift.soldier.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (shift.soldier.isNotEmpty()) MilitaryText else Color(0xFFAAAAAA),
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Delete button when editing
                        if (isEditing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onDeleteShift(index) },
                                modifier = Modifier
                                    .weight(0.15f)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MilitaryDanger)
                                    .testTag("delete_shift_$index")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa ca",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (index < shifts.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MilitaryBorder)
                        )
                    }
                }
            }

            // Edit tools: Add Shift button
            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddShift,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (isDay) "day_add_shift_button" else "night_add_shift_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Thêm khung giờ gác", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Today Button
            Button(
                onClick = onSaveToday,
                colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isDay) "day_save_today_button" else "night_save_today_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("💾 Lưu bảng gác hôm nay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RosterCard(
    title: String,
    isDay: Boolean,
    roster: List<Soldier>,
    onAddSoldier: (String) -> Unit,
    onRemoveSoldier: (Int) -> Unit,
    onAdjustOffDays: (Int, Int) -> Unit
) {
    var newSoldierName by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MilitaryCard),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MilitaryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MilitaryPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Add Soldier Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newSoldierName,
                    onValueChange = { newSoldierName = it },
                    placeholder = { Text("Tên đồng chí...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(if (isDay) "new_day_soldier_input" else "new_night_soldier_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MilitaryPrimary,
                        unfocusedBorderColor = MilitaryBorder
                    )
                )

                Button(
                    onClick = {
                        if (newSoldierName.isNotBlank()) {
                            onAddSoldier(newSoldierName)
                            newSoldierName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag(if (isDay) "add_day_soldier_button" else "add_night_soldier_button")
                ) {
                    Text("Thêm", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Soldiers List
            if (roster.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDay) "Chưa có quân số gác ngày" else "Chưa có quân số gác đêm",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MilitaryBorder, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    roster.forEachIndexed { index, soldier ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Name & OffDays tag & Stepper
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = soldier.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MilitaryText
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Nghỉ: ${soldier.offDays}d",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4A5568)
                                    )
                                }
                                // + / - buttons
                                IconButton(
                                    onClick = { onAdjustOffDays(index, 1) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF6C757D))
                                        .testTag("offdays_plus_${if (isDay) "day" else "night"}_$index")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tăng", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = { onAdjustOffDays(index, -1) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF6C757D))
                                        .testTag("offdays_minus_${if (isDay) "day" else "night"}_$index")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }

                            // Right: Delete button
                            Button(
                                onClick = { onRemoveSoldier(index) },
                                colors = ButtonDefaults.buttonColors(containerColor = MilitaryDanger),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("remove_soldier_${if (isDay) "day" else "night"}_$index")
                            ) {
                                Text("Xóa", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        if (index < roster.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MilitaryBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// IN-APP TOAST VIEW
// ==========================================

@Composable
fun InAppToastView(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MilitaryPrimaryDark,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                color = MilitaryLightGreen,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// MODAL: ADD PAST HISTORY DIALOG
// ==========================================

@Composable
fun AddHistoryDialog(
    uiState: DutyUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, List<ShiftItem>) -> Unit
) {
    val yesterday = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }

    var dateStr by remember { mutableStateOf(yesterday) }
    var mode by remember { mutableStateOf("Gác Ngày") }
    val isDay = mode == "Gác Ngày"
    val baseShifts = if (isDay) uiState.shiftsDay else uiState.shiftsNight
    val roster = if (isDay) uiState.rosterDay else uiState.rosterNight

    var currentAssignments by remember(mode) {
        mutableStateOf(baseShifts.map { it.copy(soldier = "") })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm Lịch Sử Gác Ngày Trước",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MilitaryPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date input
                Text("Chọn Ngày (dd/MM/yyyy):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hist_date_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MilitaryPrimary,
                        unfocusedBorderColor = MilitaryBorder
                    )
                )

                // Mode picker
                Text("Loại Ca Gác:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { mode = "Gác Ngày" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDay) MilitaryPrimary else Color(0xFF6C757D)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gác Ngày", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { mode = "Gác Đêm" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDay) MilitaryPrimary else Color(0xFF6C757D)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gác Đêm", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Phân ca chi tiết:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                // List of shifts to select soldiers
                currentAssignments.forEachIndexed { index, shift ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${shift.time}:",
                            fontSize = 12.sp,
                            modifier = Modifier.weight(0.45f)
                        )

                        var dropdownOpen by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .border(1.dp, MilitaryBorder, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { dropdownOpen = true }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shift.soldier.ifEmpty { "-- Chọn đồng chí --" },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            DropdownMenu(
                                expanded = dropdownOpen,
                                onDismissRequest = { dropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("-- Trống --") },
                                    onClick = {
                                        val list = currentAssignments.toMutableList()
                                        list[index] = list[index].copy(soldier = "")
                                        currentAssignments = list
                                        dropdownOpen = false
                                    }
                                )
                                roster.forEach { sol ->
                                    DropdownMenuItem(
                                        text = { Text(sol.name) },
                                        onClick = {
                                            val list = currentAssignments.toMutableList()
                                            list[index] = list[index].copy(soldier = sol.name)
                                            currentAssignments = list
                                            dropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dateStr.isNotBlank()) {
                        onSave(dateStr, mode, currentAssignments)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MilitaryPrimary),
                modifier = Modifier.testTag("save_manual_history_button")
            ) {
                Text("Lưu Lịch Sử", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_manual_history_button")
            ) {
                Text("Hủy", color = Color(0xFF6C757D))
            }
        },
        containerColor = MilitaryCard,
        shape = RoundedCornerShape(12.dp)
    )
}
