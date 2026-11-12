package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.i18n.AppStrings
import com.example.model.AttendanceSession
import com.example.model.ClassSchedule
import com.example.model.Dojo
import com.example.model.Student
import com.example.ui.theme.DojoCrimson
import com.example.ui.theme.StatusActiveGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    currentLanguage: AppLanguage,
    students: List<Student>,
    dojos: List<Dojo>,
    classSchedules: List<ClassSchedule>,
    attendanceSessions: List<AttendanceSession>,
    selectedDojo: Dojo?,
    onSaveAttendance: (AttendanceSession) -> Unit
) {
    val context = LocalContext.current
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var activeDojoId by remember(selectedDojo, dojos) {
        mutableStateOf(selectedDojo?.id ?: dojos.firstOrNull()?.id ?: "")
    }
    var selectedDate by remember { mutableStateOf(todayDate) }
    var selectedClassTitle by remember { mutableStateOf("Regular Training") }

    val enrolledStudents = remember(students, activeDojoId) {
        students.filter { it.dojoId == activeDojoId && it.status == "active" }
    }

    // Set of present student IDs
    val presentStudentIds = remember { mutableStateListOf<String>() }

    // Initialize all present when dojo or students change
    LaunchedEffect(enrolledStudents) {
        presentStudentIds.clear()
        presentStudentIds.addAll(enrolledStudents.map { it.id })
    }

    var isSaving by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = DojoCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("nav_attendance", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = DojoCrimson)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            // Controls Card: Dojo, Date, Class Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val currentDojoName = dojos.find { it.id == activeDojoId }?.name ?: selectedDojo?.name ?: "Active Dojo"
                                Text(
                                    text = currentDojoName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Date: $selectedDate",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${presentStudentIds.size} / ${enrolledStudents.size} Present",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fast Batch Actions: Mark All Present / Mark All Absent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    presentStudentIds.clear()
                                    presentStudentIds.addAll(enrolledStudents.map { it.id })
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mark_all_present_button"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All Present", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    presentStudentIds.clear()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mark_all_absent_button"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All Absent", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Student Roster
            if (enrolledStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No active students enrolled in this dojo", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(enrolledStudents, key = { it.id }) { student ->
                    val isPresent = presentStudentIds.contains(student.id)
                    val beltColor = getBeltColor(student.currentBelt)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPresent) presentStudentIds.remove(student.id)
                                else presentStudentIds.add(student.id)
                            }
                            .testTag("attendance_student_${student.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPresent) MaterialTheme.colorScheme.surface
                                             else Color(0xFFFEE2E2)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPresent) MaterialTheme.colorScheme.outline else Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(beltColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = student.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${student.currentBelt} • ${student.studentDesignation}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Present / Absent Pill Button
                            Surface(
                                color = if (isPresent) StatusActiveGreen else DojoCrimson,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .clickable {
                                        if (isPresent) presentStudentIds.remove(student.id)
                                        else presentStudentIds.add(student.id)
                                    }
                                    .testTag("toggle_attendance_${student.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPresent) "Present" else "Absent",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Save Attendance Button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            val targetOrgId = dojos.find { it.id == activeDojoId }?.orgId ?: ""
                            val session = AttendanceSession(
                                orgId = targetOrgId,
                                dojoId = activeDojoId,
                                date = selectedDate,
                                title = selectedClassTitle,
                                presentStudentIds = presentStudentIds.toList(),
                                totalStudents = enrolledStudents.size
                            )
                            onSaveAttendance(session)
                            isSaving = false
                            Toast.makeText(context, AppStrings.get("attendance_saved", currentLanguage), Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DojoCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_attendance_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${AppStrings.get("save_attendance", currentLanguage)} (${presentStudentIds.size}/${enrolledStudents.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Historical Attendance Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Historical Attendance Sheets") },
            text = {
                val sessionsForDojo = attendanceSessions.filter { it.dojoId == activeDojoId }
                if (sessionsForDojo.isEmpty()) {
                    Text("No past attendance records saved for this dojo yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessionsForDojo) { session ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(session.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("📅 ${session.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "${session.presentStudentIds.size}/${session.totalStudents}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = StatusActiveGreen
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
