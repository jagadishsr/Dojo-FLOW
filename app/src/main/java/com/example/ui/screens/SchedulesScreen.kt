package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.model.*
import com.example.ui.theme.DojoCrimson
import com.example.ui.theme.StatusActiveGreen
import com.google.firebase.Timestamp
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    currentLanguage: AppLanguage,
    organisation: Organisation?,
    dojos: List<Dojo>,
    selectedDojo: Dojo?,
    classSchedules: List<ClassSchedule>,
    students: List<Student> = emptyList(),
    attendanceSessions: List<AttendanceSession> = emptyList(),
    announcements: List<Announcement> = emptyList(),
    onSaveSchedule: (ClassSchedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSaveAttendance: (AttendanceSession) -> Unit = {},
    onSaveAnnouncement: (Announcement) -> Unit = {},
    onDeleteAnnouncement: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var isCreateDialogOpen by remember { mutableStateOf(false) }
    var scheduleToEdit by remember { mutableStateOf<ClassSchedule?>(null) }
    var announcementSchedule by remember { mutableStateOf<ClassSchedule?>(null) }
    var viewingAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    // Active open class session for inline attendance marking
    var openClassSchedule by remember { mutableStateOf<ClassSchedule?>(null) }
    var openClassDate by remember { mutableStateOf("") }

    val activeDojoId = selectedDojo?.id
    val filteredSchedules = remember(classSchedules, activeDojoId) {
        if (activeDojoId != null) classSchedules.filter { it.dojoId == activeDojoId }
        else classSchedules
    }
    val filteredAnnouncements = remember(announcements, activeDojoId) {
        if (activeDojoId != null) announcements.filter { it.dojoId == activeDojoId }
        else announcements
    }

    // Monthly Calendar State
    val calendar = remember { Calendar.getInstance() }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) } // 0-indexed

    val todayCal = remember { Calendar.getInstance() }
    val todayFormatted = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todayCal.time)
    }
    var selectedDateStr by remember { mutableStateOf(todayFormatted) }

    val monthYearTitle = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // Calendar grid calculations
    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        // We'll use Monday as start: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
        val offset = (firstDayOfWeek + 5) % 7
        Pair(maxDays, offset)
    }

    // Determine day abbreviation for matching recurring schedules (e.g. "Mon", "Tue", etc.)
    val selectedDayOfWeek = remember(selectedDateStr) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(selectedDateStr)
            val cal = Calendar.getInstance().apply { time = date ?: Date() }
            SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.time) // "Mon", "Tue", ...
        } catch (e: Exception) {
            "Mon"
        }
    }

    // Schedules occurring on the selected date
    val schedulesForSelectedDate = remember(filteredSchedules, selectedDateStr, selectedDayOfWeek) {
        filteredSchedules.filter { schedule ->
            val entries = schedule.parsedEntries()
            if (entries.isNotEmpty()) {
                entries.any { entry ->
                    entry.dayOrDate.contains(selectedDayOfWeek, ignoreCase = true) ||
                    entry.dayOrDate.contains(selectedDateStr, ignoreCase = true)
                }
            } else {
                schedule.daysOfWeek.any { it.startsWith(selectedDayOfWeek, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreateDialogOpen = true },
                containerColor = DojoCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_schedule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
        ) {
            // MONTHLY CALENDAR HEADER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Month & Year Selector Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthYearTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 0) {
                                            currentMonth = 11
                                            currentYear--
                                        } else {
                                            currentMonth--
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 11) {
                                            currentMonth = 0
                                            currentYear++
                                        } else {
                                            currentMonth++
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weekday Headers: Mon Tue Wed Thu Fri Sat Sun
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid Days
                        val (maxDays, offset) = daysInMonth
                        val totalCells = offset + maxDays
                        val rows = (totalCells + 6) / 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (r in 0 until rows) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (c in 0 until 7) {
                                        val dayIndex = r * 7 + c - offset + 1
                                        if (dayIndex in 1..maxDays) {
                                            val dayStr = if (dayIndex < 10) "0$dayIndex" else "$dayIndex"
                                            val monthStr = if (currentMonth + 1 < 10) "0${currentMonth + 1}" else "${currentMonth + 1}"
                                            val cellDateStr = "$currentYear-$monthStr-$dayStr"
                                            val isSelected = cellDateStr == selectedDateStr
                                            val isToday = cellDateStr == todayFormatted

                                            // Check if classes exist for this day of week
                                            val dayCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, currentYear)
                                                set(Calendar.MONTH, currentMonth)
                                                set(Calendar.DAY_OF_MONTH, dayIndex)
                                            }
                                            val dayOfWeekAbbr = SimpleDateFormat("EEE", Locale.ENGLISH).format(dayCal.time)
                                            val hasClasses = filteredSchedules.any { sched ->
                                                val entries = sched.parsedEntries()
                                                if (entries.isNotEmpty()) {
                                                    entries.any { it.dayOrDate.contains(dayOfWeekAbbr, ignoreCase = true) }
                                                } else {
                                                    sched.daysOfWeek.any { it.startsWith(dayOfWeekAbbr, ignoreCase = true) }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) DojoCrimson
                                                        else if (isToday) DojoCrimson.copy(alpha = 0.12f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable { selectedDateStr = cellDateStr },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$dayIndex",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (hasClasses) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else DojoCrimson)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // SELECTED DATE SCHEDULE HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sessions for $selectedDateStr ($selectedDayOfWeek)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (schedulesForSelectedDate.isEmpty()) "No classes scheduled for this date"
                                   else "${schedulesForSelectedDate.size} class session(s) found",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { isCreateDialogOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = DojoCrimson)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Session", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DojoCrimson)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // SESSIONS ACCORDION LIST FOR SELECTED DATE
            if (schedulesForSelectedDate.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No training session on this day", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Select another date or add a special/regular schedule", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                items(schedulesForSelectedDate, key = { it.id }) { schedule ->
                    val dojo = dojos.find { it.id == schedule.dojoId }
                    val dojoName = dojo?.name ?: "Main Dojo"
                    val eligibleCount = students.count { (schedule.dojoId.isBlank() || it.dojoId == schedule.dojoId) && it.status == "active" }
                    val entries = schedule.parsedEntries()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(schedule.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("📍 $dojoName • $eligibleCount eligible karatekas", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${schedule.startTime} - ${schedule.endTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (entries.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                entries.forEach { entry ->
                                    Text(
                                        text = "• ${entry.dayOrDate}: ${if (entry.isNoClass) "NO CLASS" else "${entry.sessionTime} - ${entry.activity}"}",
                                        fontSize = 12.sp,
                                        fontWeight = if (entry.isNoClass) FontWeight.Bold else FontWeight.Normal,
                                        color = if (entry.isNoClass) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (schedule.uniform.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🥋 Uniform: ${schedule.uniform}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // MANDATORY PHASE 7 WORKFLOW: OPEN CLASS -> MARK ATTENDANCE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        openClassSchedule = schedule
                                        openClassDate = selectedDateStr
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusActiveGreen),
                                    modifier = Modifier.weight(1.3f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Class & Mark Attendance", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { announcementSchedule = schedule }) {
                                    Icon(Icons.Default.Campaign, contentDescription = "Announcement", tint = DojoCrimson)
                                }

                                IconButton(onClick = { scheduleToEdit = schedule }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(onClick = { onDeleteSchedule(schedule.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // RECENT ANNOUNCEMENT BROADCASTS SECTION
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Announcement History (${filteredAnnouncements.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredAnnouncements.isEmpty()) {
                item {
                    Text(
                        text = "No saved announcements. Use 'WhatsApp Notice' button on any class to broadcast and save.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredAnnouncements.take(2), key = { it.id }) { ann ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                TextButton(onClick = { viewingAnnouncement = ann }) {
                                    Text("View / Re-share", fontSize = 11.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = ann.message.take(120) + if (ann.message.length > 120) "..." else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // MANDATORY PHASE 7: OPEN CLASS DIALOG (SESSION-BASED ATTENDANCE)
    if (openClassSchedule != null) {
        val schedule = openClassSchedule!!
        val dojo = dojos.find { it.id == schedule.dojoId }
        val eligibleStudents = students.filter {
            (schedule.dojoId.isBlank() || it.dojoId == schedule.dojoId) && it.status == "active"
        }

        // Attendance state map: studentId -> isPresent (Boolean)
        val attendanceState = remember(schedule.id, openClassDate) {
            mutableStateMapOf<String, Boolean>().apply {
                eligibleStudents.forEach { put(it.id, true) } // Default all present
            }
        }

        // Absence reasons map: studentId -> reason string
        val absenceReasons = remember(schedule.id, openClassDate) {
            mutableStateMapOf<String, String>()
        }

        val presentCount = attendanceState.count { it.value }
        val absentCount = eligibleStudents.size - presentCount

        AlertDialog(
            onDismissRequest = { openClassSchedule = null },
            title = {
                Column {
                    Text("Class Session Attendance", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "${schedule.title} • Date: $openClassDate",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${eligibleStudents.size} Enrolled • $presentCount Present • $absentCount Absent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Action: Mark All Present / Mark All Absent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                eligibleStudents.forEach { attendanceState[it.id] = true }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("All Present", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                eligibleStudents.forEach { attendanceState[it.id] = false }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("All Absent", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Students List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(eligibleStudents, key = { it.id }) { student ->
                            val isPresent = attendanceState[student.id] ?: true

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${student.currentBelt} • ${student.trainingMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // Present / Absent selector toggle
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isPresent) StatusActiveGreen else Color.Transparent,
                                                border = if (!isPresent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                                modifier = Modifier.clickable { attendanceState[student.id] = true }
                                            ) {
                                                Text(
                                                    text = "P",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPresent) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (!isPresent) Color(0xFFDC2626) else Color.Transparent,
                                                border = if (isPresent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                                modifier = Modifier.clickable { attendanceState[student.id] = false }
                                            ) {
                                                Text(
                                                    text = "A",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isPresent) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // If ABSENT, reveal optional absence-reason input per prompt Phase 7.1
                                    if (!isPresent) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = absenceReasons[student.id] ?: "",
                                            onValueChange = { absenceReasons[student.id] = it },
                                            placeholder = { Text("Absence reason (optional)", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
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
                        val presentIds = attendanceState.filter { it.value }.keys.toList()
                        val session = AttendanceSession(
                            orgId = schedule.orgId,
                            dojoId = schedule.dojoId,
                            classId = schedule.id,
                            date = openClassDate,
                            title = schedule.title,
                            instructorName = organisation?.topSenseiName ?: "Sensei",
                            presentStudentIds = presentIds,
                            totalStudents = eligibleStudents.size
                        )
                        onSaveAttendance(session)
                        Toast.makeText(context, "Attendance saved ($presentCount Present, $absentCount Absent)", Toast.LENGTH_SHORT).show()
                        openClassSchedule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson)
                ) {
                    Text("Save Attendance", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { openClassSchedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CREATE / EDIT SCHEDULE DIALOG
    if (isCreateDialogOpen || scheduleToEdit != null) {
        MultiEntryScheduleFormDialog(
            existingSchedule = scheduleToEdit,
            organisation = organisation,
            dojos = dojos,
            defaultDojoId = selectedDojo?.id ?: dojos.firstOrNull()?.id ?: "",
            onDismiss = {
                isCreateDialogOpen = false
                scheduleToEdit = null
            },
            onSave = { schedule ->
                onSaveSchedule(schedule)
                isCreateDialogOpen = false
                scheduleToEdit = null
            }
        )
    }

    // WHATSAPP ANNOUNCEMENT GENERATOR DIALOG
    if (announcementSchedule != null) {
        val schedule = announcementSchedule!!
        val dojo = dojos.find { it.id == schedule.dojoId } ?: dojos.firstOrNull()
        val orgFullName = organisation?.name ?: "JAPAN FUNAKOSHI SHOTOKAN KARATE INDIA FEDERATION"
        val affiliation = organisation?.affiliation?.ifBlank { "Member of World JKA Karate Association (WJKA)" }
            ?: "Member of World JKA Karate Association (WJKA)"
        val greeting = organisation?.announcementGreeting?.ifBlank { "Dear parents & students," } ?: "Dear parents & students,"
        val closing = organisation?.announcementClosing?.ifBlank { "Thank you," } ?: "Thank you,"
        val dojoName = dojo?.name ?: "Sri Sankara Dojo"

        val entries = schedule.parsedEntries()

        val defaultMessage = buildString {
            appendLine(orgFullName.uppercase())
            appendLine(affiliation)
            appendLine()
            appendLine(greeting)
            appendLine()
            appendLine("The following is the class schedule:")
            appendLine()

            if (entries.isNotEmpty()) {
                entries.forEach { entry ->
                    appendLine("• ${entry.dayOrDate}")
                    if (entry.isNoClass) {
                        appendLine("  ${entry.activity}: NO CLASS")
                    } else {
                        appendLine("  ${entry.sessionTime} - ${entry.activity}")
                    }
                    appendLine()
                }
            } else {
                appendLine("• Days: ${schedule.daysOfWeek.joinToString(", ")}")
                appendLine("  ${schedule.startTime} - ${schedule.endTime}: ${schedule.title}")
                appendLine()
            }

            if (schedule.uniform.isNotBlank()) {
                appendLine("Uniform: ${schedule.uniform}")
            } else {
                appendLine("Uniform: Full Gi with respective belts")
            }

            if (schedule.itemsToBring.isNotBlank()) {
                appendLine("Items to bring: ${schedule.itemsToBring}")
            }

            if (schedule.specialNotice.isNotBlank()) {
                appendLine("Special Notice: ${schedule.specialNotice}")
            }

            appendLine()
            appendLine(closing)
            appendLine()
            appendLine(dojoName)
            appendLine(orgFullName)
        }

        var editableMessage by remember(announcementSchedule) { mutableStateOf(defaultMessage) }
        var isEditMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { announcementSchedule = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WhatsApp Announcement", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(if (isEditMode) "Done Editing" else "Preview & Edit", fontSize = 12.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editableMessage,
                            onValueChange = { editableMessage = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp)
                        )
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(14.dp)) {
                                item {
                                    Text(
                                        text = editableMessage,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Class Announcement", editableMessage))
                            Toast.makeText(context, "Announcement copied to clipboard", Toast.LENGTH_SHORT).show()

                            val effectiveOrgId = schedule.orgId.ifBlank { organisation?.id ?: dojo?.orgId ?: "" }
                            val effectiveDojoId = schedule.dojoId.ifBlank { dojo?.id ?: "" }
                            val newAnnouncement = Announcement(
                                orgId = effectiveOrgId,
                                dojoId = effectiveDojoId,
                                scheduleId = schedule.id,
                                title = schedule.title,
                                message = editableMessage,
                                publishedBy = organisation?.topSenseiName ?: "Sensei",
                                createdAt = Timestamp.now()
                            )
                            onSaveAnnouncement(newAnnouncement)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, editableMessage)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                try {
                                    val encoded = URLEncoder.encode(editableMessage, "UTF-8")
                                    val uri = android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                                    val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    context.startActivity(fallbackIntent)
                                } catch (e2: Exception) {
                                    Toast.makeText(context, "WhatsApp not installed. Text copied.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            val effectiveOrgId = schedule.orgId.ifBlank { organisation?.id ?: dojo?.orgId ?: "" }
                            val effectiveDojoId = schedule.dojoId.ifBlank { dojo?.id ?: "" }
                            val newAnnouncement = Announcement(
                                orgId = effectiveOrgId,
                                dojoId = effectiveDojoId,
                                scheduleId = schedule.id,
                                title = schedule.title,
                                message = editableMessage,
                                publishedBy = organisation?.topSenseiName ?: "Sensei",
                                createdAt = Timestamp.now()
                            )
                            onSaveAnnouncement(newAnnouncement)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { announcementSchedule = null }) {
                    Text("Close")
                }
            }
        )
    }

    // VIEW ANNOUNCEMENT DETAIL DIALOG
    if (viewingAnnouncement != null) {
        val ann = viewingAnnouncement!!
        AlertDialog(
            onDismissRequest = { viewingAnnouncement = null },
            title = { Text(ann.title, fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(14.dp)) {
                        item {
                            Text(
                                text = ann.message,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Class Announcement", ann.message))
                            Toast.makeText(context, "Announcement copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Copy", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val encoded = URLEncoder.encode(ann.message, "UTF-8")
                            val uri = android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("Share WhatsApp", fontSize = 12.sp, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingAnnouncement = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiEntryScheduleFormDialog(
    existingSchedule: ClassSchedule?,
    organisation: Organisation?,
    dojos: List<Dojo>,
    defaultDojoId: String,
    onDismiss: () -> Unit,
    onSave: (ClassSchedule) -> Unit
) {
    var title by remember { mutableStateOf(existingSchedule?.title ?: "Regular Karate Training") }
    var selectedDojoId by remember { mutableStateOf(existingSchedule?.dojoId?.ifBlank { defaultDojoId } ?: defaultDojoId) }
    var instructorName by remember { mutableStateOf(existingSchedule?.instructorName?.ifBlank { organisation?.topSenseiName ?: "Sensei" } ?: "Sensei") }
    var startTime by remember { mutableStateOf(existingSchedule?.startTime ?: "18:00") }
    var endTime by remember { mutableStateOf(existingSchedule?.endTime ?: "19:30") }
    var instructions by remember { mutableStateOf(existingSchedule?.instructions ?: "Please arrive 10 minutes prior to warm-up in full Gi.") }
    var uniform by remember { mutableStateOf(existingSchedule?.uniform ?: "Please come in your Karate uniform.") }
    var itemsToBring by remember { mutableStateOf(existingSchedule?.itemsToBring ?: "") }
    var specialNotice by remember { mutableStateOf(existingSchedule?.specialNotice ?: "") }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            addAll(existingSchedule?.daysOfWeek ?: listOf("Mon", "Wed", "Fri"))
        }
    }

    // Dynamic schedule entries
    val entries = remember {
        mutableStateListOf<ScheduleEntry>().apply {
            val parsed = existingSchedule?.parsedEntries() ?: emptyList()
            if (parsed.isNotEmpty()) {
                addAll(parsed)
            } else {
                add(ScheduleEntry(dayOrDate = "Monday", sessionTime = "6:00 PM - 7:30 PM", activity = "Kihon (Basics) & Kata"))
                add(ScheduleEntry(dayOrDate = "Wednesday", sessionTime = "6:00 PM - 7:30 PM", activity = "Kumite (Sparring) Drills"))
                add(ScheduleEntry(dayOrDate = "Friday", sessionTime = "6:00 PM - 7:30 PM", activity = "Bunkai & Belt Exam Prep"))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingSchedule != null) "Edit Class Timetable" else "Create Class Timetable",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Class / Batch Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_title_input"),
                        singleLine = true
                    )
                }

                if (dojos.size > 1) {
                    item {
                        Text("Dojo Branch", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(dojos) { dojo ->
                                val isSelected = selectedDojoId == dojo.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDojoId = dojo.id },
                                    label = { Text(dojo.shortName.ifBlank { dojo.name }, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Training Days", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        allDays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) DojoCrimson else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.take(2),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = instructorName,
                        onValueChange = { instructorName = it },
                        label = { Text("Instructor / Sensei Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = uniform,
                        onValueChange = { uniform = it },
                        label = { Text("Uniform Requirements") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = itemsToBring,
                        onValueChange = { itemsToBring = it },
                        label = { Text("Items to Bring (Optional)") },
                        placeholder = { Text("e.g. Water bottle, sparring gear") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = specialNotice,
                        onValueChange = { specialNotice = it },
                        label = { Text("Special Notice (Optional)") },
                        placeholder = { Text("e.g. Belt grading on 28th Nov") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session Breakdown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(
                            onClick = {
                                entries.add(ScheduleEntry(dayOrDate = "Date/Day", sessionTime = "$startTime - $endTime", activity = "Training"))
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Slot", fontSize = 11.sp)
                        }
                    }
                }

                items(entries.size) { index ->
                    val entry = entries[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Slot #${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (entries.size > 1) {
                                    IconButton(
                                        onClick = { entries.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = entry.dayOrDate,
                                    onValueChange = { newDay -> entries[index] = entry.copy(dayOrDate = newDay) },
                                    label = { Text("Day/Date", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                OutlinedTextField(
                                    value = entry.sessionTime,
                                    onValueChange = { newTime -> entries[index] = entry.copy(sessionTime = newTime) },
                                    label = { Text("Time", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = entry.activity,
                                onValueChange = { newAct -> entries[index] = entry.copy(activity = newAct) },
                                label = { Text("Activity / Topic", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val serialized = ClassSchedule.serializeEntries(entries.toList())
                    val effectiveOrgId = existingSchedule?.orgId?.ifBlank { organisation?.id ?: "" }
                        ?.ifBlank { dojos.find { it.id == selectedDojoId }?.orgId ?: "" }
                        ?: (organisation?.id ?: dojos.find { it.id == selectedDojoId }?.orgId ?: dojos.firstOrNull()?.orgId ?: "")
                    val effectiveDojoId = selectedDojoId.ifBlank { defaultDojoId }.ifBlank { dojos.firstOrNull()?.id ?: "" }

                    val schedule = (existingSchedule ?: ClassSchedule()).copy(
                        orgId = effectiveOrgId,
                        dojoId = effectiveDojoId,
                        title = title.ifBlank { "Karate Training" },
                        instructorName = instructorName,
                        startTime = startTime,
                        endTime = endTime,
                        daysOfWeek = selectedDays.toList(),
                        instructions = instructions,
                        uniform = uniform,
                        itemsToBring = itemsToBring,
                        specialNotice = specialNotice,
                        entriesJson = serialized
                    )
                    onSave(schedule)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text("Save Timetable", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

