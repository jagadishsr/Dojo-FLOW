package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.model.*
import com.example.ui.theme.DojoCrimson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    currentLanguage: AppLanguage,
    organisation: Organisation?,
    dojos: List<Dojo>,
    students: List<Student>,
    payments: List<Payment>,
    attendanceSessions: List<AttendanceSession>,
    beltPromotions: List<BeltPromotion>,
    selectedDojo: Dojo?,
    onRecordPayment: (Payment) -> Unit,
    onRecordPromotion: (BeltPromotion) -> Unit,
    onUpdateStudentBelt: (studentId: String, newBelt: String) -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) } // 0: Financials & Fees, 1: Attendance Analytics, 2: Belt Progression, 3: Reports & Export

    val activeDojoId = selectedDojo?.id
    val scopedStudents = remember(students, activeDojoId) {
        if (activeDojoId != null) students.filter { it.dojoId == activeDojoId }
        else students
    }
    val activeStudents = remember(scopedStudents) { scopedStudents.filter { it.status == "active" } }

    val scopedPayments = remember(payments, activeDojoId) {
        if (activeDojoId != null) payments.filter { it.dojoId == activeDojoId }
        else payments
    }
    val totalRevenue = remember(scopedPayments) { scopedPayments.sumOf { it.amount } }

    val scopedAttendance = remember(attendanceSessions, activeDojoId) {
        if (activeDojoId != null) attendanceSessions.filter { it.dojoId == activeDojoId }
        else attendanceSessions
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High level stats sub-tabs
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp,
            divider = { Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Fees & Ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Attendance Stats", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("Belt Progression", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text("Reports & Export", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (activeSubTab) {
            0 -> {
                FeesScreen(
                    currentLanguage = currentLanguage,
                    students = students,
                    dojos = dojos,
                    payments = payments,
                    selectedDojo = selectedDojo,
                    onRecordPayment = onRecordPayment
                )
            }

            1 -> {
                AttendanceAnalyticsTab(
                    students = scopedStudents,
                    attendanceSessions = scopedAttendance,
                    selectedDojo = selectedDojo
                )
            }

            2 -> {
                BeltsScreen(
                    currentLanguage = currentLanguage,
                    students = students,
                    dojos = dojos,
                    beltPromotions = beltPromotions,
                    selectedDojo = selectedDojo,
                    onRecordPromotion = onRecordPromotion,
                    onUpdateStudentBelt = onUpdateStudentBelt
                )
            }

            3 -> {
                ReportsAndExportTab(
                    organisation = organisation,
                    selectedDojo = selectedDojo,
                    dojos = dojos,
                    students = scopedStudents,
                    payments = scopedPayments,
                    attendanceSessions = scopedAttendance,
                    onShareReport = { reportText ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share DojoFLOW Report"))
                    }
                )
            }
        }
    }
}

@Composable
private fun AttendanceAnalyticsTab(
    students: List<Student>,
    attendanceSessions: List<AttendanceSession>,
    selectedDojo: Dojo?
) {
    val totalSessions = attendanceSessions.size
    val totalRecordedAttendances = attendanceSessions.sumOf { it.presentStudentIds.size }
    val totalStudentsMarked = attendanceSessions.sumOf { it.totalStudents }
    val totalAbsences = maxOf(0, totalStudentsMarked - totalRecordedAttendances)
    val grandTotalMarked = if (totalStudentsMarked > 0) totalStudentsMarked else totalRecordedAttendances
    val overallAttendanceRate = if (grandTotalMarked > 0) {
        (totalRecordedAttendances.toFloat() / grandTotalMarked * 100f).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attendance Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dojo: ${selectedDojo?.name ?: "Active Dojo"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(
                            title = "Rate",
                            value = "$overallAttendanceRate%",
                            color = if (overallAttendanceRate >= 75) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                        MetricBox(
                            title = "Sessions",
                            value = "$totalSessions",
                            color = MaterialTheme.colorScheme.primary
                        )
                        MetricBox(
                            title = "Present",
                            value = "$totalRecordedAttendances",
                            color = Color(0xFF16A34A)
                        )
                        MetricBox(
                            title = "Absent",
                            value = "$totalAbsences",
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Training Sessions",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (attendanceSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No attendance sessions logged yet.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Open a class from Schedule to mark attendance.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(attendanceSessions.sortedByDescending { it.date }) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = session.title.ifBlank { "Karate Session" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Date: ${session.date} • Sensei: ${session.instructorName.ifBlank { "Instructor" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "${session.presentStudentIds.size} Present",
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            val absentCount = maxOf(0, session.totalStudents - session.presentStudentIds.size)
                            if (absentCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = "$absentCount Absent",
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportsAndExportTab(
    organisation: Organisation?,
    selectedDojo: Dojo?,
    dojos: List<Dojo>,
    students: List<Student>,
    payments: List<Payment>,
    attendanceSessions: List<AttendanceSession>,
    onShareReport: (String) -> Unit
) {
    val totalRevenue = payments.sumOf { it.amount }
    val activeCount = students.count { it.status == "active" }
    val dojoCount = students.count { it.trainingMode == "DOJO" }
    val privateCount = students.count { it.trainingMode == "PRIVATE" }
    val onlineCount = students.count { it.trainingMode == "ONLINE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Executive Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = organisation?.name ?: "DojoFLOW Martial Arts Academy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ReportStatRow("Active Students", "$activeCount")
                    ReportStatRow("Dojo Training", "$dojoCount")
                    ReportStatRow("Private Home Training", "$privateCount")
                    ReportStatRow("Online Training", "$onlineCount")
                    ReportStatRow("Total Fee Collection", "₹${totalRevenue.toInt()}")
                    ReportStatRow("Attendance Sessions", "${attendanceSessions.size}")
                    ReportStatRow("Active Branches", "${dojos.size}")
                }
            }
        }

        item {
            Text(
                text = "Export & Share",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Button(
                onClick = {
                    val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())
                    val text = buildString {
                        appendLine("🥋 ${organisation?.name ?: "DOJOFLOW ACADEMY"} – EXECUTIVE REPORT")
                        appendLine("Generated on: $dateStr")
                        appendLine("Head Instructor: ${organisation?.topSenseiName ?: "Sensei"}")
                        appendLine("Martial Art Style: ${organisation?.martialArtStyle ?: "Martial Arts"}")
                        appendLine("----------------------------------------")
                        appendLine("• Total Active Students: $activeCount")
                        appendLine("   - Dojo Training: $dojoCount")
                        appendLine("   - Private Training: $privateCount")
                        appendLine("   - Online Training: $onlineCount")
                        appendLine("• Total Fee Receipts: ${payments.size} (₹${totalRevenue.toInt()})")
                        appendLine("• Attendance Sessions: ${attendanceSessions.size}")
                        appendLine("• Dojos: ${dojos.joinToString { it.name }}")
                        appendLine("----------------------------------------")
                        appendLine("Powered by DojoFLOW Ecosystem")
                    }
                    onShareReport(text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_summary_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Executive Academy Report", fontWeight = FontWeight.Bold)
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val csv = buildString {
                        appendLine("Name,ID,Belt,TrainingMode,Dojo,Phone,Status")
                        students.forEach { s ->
                            appendLine("\"${s.fullName}\",\"${s.id}\",\"${s.currentBelt}\",\"${s.trainingMode}\",\"${s.dojoId}\",\"${s.parentGuardianMobile}\",\"${s.status}\"")
                        }
                    }
                    onShareReport(csv)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_students_csv_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Students Roster (CSV Format)", fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    val csv = buildString {
                        appendLine("ReceiptNumber,PaymentDate,StudentName,Amount,PaymentMethod,Period")
                        payments.forEach { p ->
                            appendLine("\"${p.receiptNumber}\",\"${p.paymentDate}\",\"${p.studentName}\",\"${p.amount}\",\"${p.paymentMethod}\",\"${p.feePeriod}\"")
                        }
                    }
                    onShareReport(csv)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_fees_csv_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Fee Transactions Ledger (CSV Format)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReportStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
