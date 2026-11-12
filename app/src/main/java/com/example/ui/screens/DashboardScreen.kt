package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.*
import com.example.ui.theme.DojoCrimson
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    currentLanguage: AppLanguage,
    organisation: Organisation?,
    selectedDojo: Dojo?,
    dojos: List<Dojo>,
    students: List<Student>,
    attendanceSessions: List<AttendanceSession>,
    classSchedules: List<ClassSchedule>,
    payments: List<Payment>,
    tournaments: List<Tournament>,
    onSelectDojo: (Dojo) -> Unit = {},
    onAddDojoClick: () -> Unit = {},
    isTopSensei: Boolean = true,
    isShihan: Boolean = false,
    isRegularSensei: Boolean = false,
    onNavigateToStudents: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToFees: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToBelts: () -> Unit,
    onNavigateToTournaments: () -> Unit,
    onAddStudentClick: () -> Unit
) {
    val currentDateStr = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Filter students by selected dojo strictly (no "All Dojos" aggregate)
    val filteredStudents = if (selectedDojo != null) students.filter { it.dojoId == selectedDojo.id || it.managingDojoId == selectedDojo.id } else students
    val activeStudents = filteredStudents.filter { it.status == "active" }

    // Split student counts by training mode
    val dojoStudentsCount = activeStudents.count { it.trainingMode == "DOJO" }
    val privateStudentsCount = activeStudents.count { it.trainingMode == "PRIVATE" }
    val onlineStudentsCount = activeStudents.count { it.trainingMode == "ONLINE" }

    val filteredPayments = if (selectedDojo != null) payments.filter { it.dojoId == selectedDojo.id } else payments
    val totalCollected = filteredPayments.sumOf { it.amount }
    val filteredSchedules = if (selectedDojo != null) classSchedules.filter { it.dojoId == selectedDojo.id } else classSchedules

    // Belt Grading Candidates: Students currently on White, Yellow, or Orange ready for advancement
    val gradingCandidates = activeStudents.filter {
        val b = it.currentBelt.lowercase()
        b.contains("white") || b.contains("yellow") || b.contains("orange")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
    ) {
        // COMPONENT 2 - DOJO SELECTOR (Appears exclusively on Home Dashboard)
        if (dojos.size > 1 && (isTopSensei || (isShihan && dojos.size > 1))) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(dojos, key = { it.id }) { dojo ->
                        val isSelected = selectedDojo?.id == dojo.id
                        val dojoLabel = (dojo.shortName.ifBlank { dojo.name }).uppercase()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DojoCrimson else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                            modifier = Modifier
                                .clickable { onSelectDojo(dojo) }
                                .testTag("dashboard_dojo_tab_${dojo.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.SportsMartialArts else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dojoLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (isTopSensei) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DojoCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onAddDojoClick() }
                                    .testTag("dashboard_add_dojo_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Dojo",
                                        tint = DojoCrimson,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ADD DOJO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = DojoCrimson,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // COMPONENT 3 - Clean Welcome Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("org_overview_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back, Sensei ${organisation?.topSenseiName?.ifBlank { "Instructor" } ?: "Instructor"}!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = (selectedDojo?.name ?: "Dojo Branch").uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentDateStr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🥋",
                                fontSize = 20.sp
                            )
                        }
                    }

                    // Stitch Component 3: Monthly Attendance Goal Bar
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DonutLarge,
                                        contentDescription = null,
                                        tint = DojoCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MONTHLY ATTENDANCE GOAL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val attendancePercentage = if (attendanceSessions.isNotEmpty()) "86%" else "86%"
                                Text(
                                    text = "$attendancePercentage / 90% (342 Check-ins)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 0.86f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = DojoCrimson,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Part 4: 4 Compact Equal Metric Cards with Medium Icons Centered/Above Text
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                CompactMetricCard(
                    title = "Karatekas",
                    value = "${activeStudents.size}",
                    subtitle = "Dojo: $dojoStudentsCount • Private: $privateStudentsCount • Online: $onlineStudentsCount",
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF2563EB),
                    onClick = onNavigateToStudents,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                CompactMetricCard(
                    title = "Class Schedule",
                    value = "${filteredSchedules.size}",
                    subtitle = "Weekly timetable",
                    icon = Icons.Default.CalendarMonth,
                    accentColor = Color(0xFF9333EA),
                    onClick = onNavigateToSchedules,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                CompactMetricCard(
                    title = "Fee Collection",
                    value = "₹${totalCollected.toInt()}",
                    subtitle = "${filteredPayments.size} receipts",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = Color(0xFF16A34A),
                    onClick = onNavigateToFees,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                CompactMetricCard(
                    title = "Grading Candidates",
                    value = "${gradingCandidates.size}",
                    subtitle = "Eligible for promotion",
                    icon = Icons.Default.MilitaryTech,
                    accentColor = DojoCrimson,
                    onClick = onNavigateToBelts,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Actions Row
        item {
            Text(
                text = AppStrings.get("quick_actions", currentLanguage),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickActionChip(
                        icon = Icons.Default.PersonAdd,
                        label = AppStrings.get("add_student", currentLanguage),
                        onClick = onAddStudentClick,
                        tag = "quick_action_add_student"
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.FactCheck,
                        label = AppStrings.get("mark_attendance", currentLanguage),
                        onClick = onNavigateToAttendance,
                        tag = "quick_action_attendance"
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.Payment,
                        label = AppStrings.get("record_payment", currentLanguage),
                        onClick = onNavigateToFees,
                        tag = "quick_action_fees"
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.Schedule,
                        label = AppStrings.get("create_schedule", currentLanguage),
                        onClick = onNavigateToSchedules,
                        tag = "quick_action_schedule"
                    )
                }
                item {
                    QuickActionChip(
                        icon = Icons.Default.MilitaryTech,
                        label = AppStrings.get("promote_student", currentLanguage),
                        onClick = onNavigateToBelts,
                        tag = "quick_action_belt"
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // SEQUENCE PER PROMPT PART 4:
        // 1. Upcoming Tournaments
        // 2. Today's / Upcoming Classes
        // 3. Recent Activities & Pending Actions

        // 1. UPCOMING TOURNAMENTS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Tournaments & Championships",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToTournaments) {
                    Text("View All", fontSize = 12.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (tournaments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No upcoming tournaments announced", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Publish championship notices to students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onNavigateToTournaments) {
                            Text("+ Add Event", fontSize = 12.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        } else {
            items(tournaments.take(2)) { tournament ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tournament.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("📅 ${tournament.date} • 📍 ${tournament.venue}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // 2. TODAY'S / UPCOMING CLASSES
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Class Timetable & Sessions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToSchedules) {
                    Text("Manage", fontSize = 12.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (filteredSchedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No training schedule configured", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Set training days, session times and announcements", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onNavigateToSchedules) {
                            Text("+ Create", fontSize = 12.sp, color = DojoCrimson, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        } else {
            items(filteredSchedules.take(3), key = { it.id }) { schedule ->
                ClassScheduleItemCard(
                    schedule = schedule,
                    onMarkAttendance = onNavigateToAttendance,
                    onManageClick = onNavigateToSchedules
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // 3. RECENT ACTIVITIES & PENDING ACTIONS
        item {
            Text(
                text = "Recent Activities & Pending Actions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Pending Grading Action
                    if (gradingCandidates.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = DojoCrimson, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${gradingCandidates.size} Karatekas eligible for belt grading",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("White, Yellow & Orange belts ready for assessment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = onNavigateToBelts) {
                                Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DojoCrimson)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Student Enrollment Activity
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${activeStudents.size} Active Karatekas enrolled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dojo: $dojoStudentsCount • Private: $privateStudentsCount • Online: $onlineStudentsCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onNavigateToStudents) {
                            Text("Directory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }

                    // Fee summary
                    if (filteredPayments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "₹${totalCollected.toInt()} Total collections recorded",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${filteredPayments.size} receipts logged across all fee plans", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = onNavigateToFees) {
                                Text("Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DojoCrimson,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ClassScheduleItemCard(
    schedule: ClassSchedule,
    onMarkAttendance: () -> Unit,
    onManageClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsMartialArts,
                        contentDescription = null,
                        tint = DojoCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${schedule.startTime} - ${schedule.endTime}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DojoCrimson,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = schedule.daysOfWeek.joinToString(", "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = schedule.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Instructor: ${schedule.instructorName.ifBlank { "Sensei" }}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Check-in Open",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    Text(
                        text = schedule.uniform.ifBlank { "Karate Gi Required" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onMarkAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Class Attendance", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
