package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.model.BeltPromotion
import com.example.model.Dojo
import com.example.model.Student
import com.example.ui.theme.DojoCrimson
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeltsScreen(
    currentLanguage: AppLanguage,
    students: List<Student>,
    dojos: List<Dojo>,
    beltPromotions: List<BeltPromotion>,
    selectedDojo: Dojo?,
    onRecordPromotion: (BeltPromotion) -> Unit,
    onUpdateStudentBelt: (studentId: String, newBelt: String) -> Unit
) {
    var isPromoteDialogOpen by remember { mutableStateOf(false) }
    var selectedStudentForPromotion by remember { mutableStateOf<Student?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Grading Eligibility & Belts, 1: History Log

    val activeDojoId = selectedDojo?.id
    val filteredStudents = remember(students, activeDojoId) {
        if (activeDojoId != null) students.filter { it.dojoId == activeDojoId && it.status == "active" }
        else students.filter { it.status == "active" }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isPromoteDialogOpen = true },
                containerColor = DojoCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("record_promotion_fab")
            ) {
                Icon(Icons.Default.MilitaryTech, contentDescription = "Promote Belt")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
        ) {
            // Visual Karate Belt Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Standard Kyu / Dan Rank Progression",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BeltOptions) { (belt, color) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = belt.replace(" Belt", "").replace(" (1st Dan)", ""),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tab Switcher (Students Eligibility vs Promotion History)
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Students (${filteredStudents.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Promotion History (${beltPromotions.size})", fontWeight = FontWeight.Bold) }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (activeTab == 0) {
                // Students and Belt Rank list
                if (filteredStudents.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No students available for belt grading.")
                            }
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.id }) { student ->
                        val beltColor = getBeltColor(student.currentBelt)
                        val dojoName = dojos.find { it.id == student.dojoId }?.name ?: "Dojo"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(beltColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = student.fullName.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (beltColor == Color(0xFF18181B)) Color.White else Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${student.currentBelt} • $dojoName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        selectedStudentForPromotion = student
                                        isPromoteDialogOpen = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Promote", fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Historical Promotions
                if (beltPromotions.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No belt promotions recorded yet.")
                            }
                        }
                    }
                } else {
                    items(beltPromotions) { promo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(promo.studentName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("📅 ${promo.promotionDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = promo.previousBelt,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = promo.newBelt,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DojoCrimson
                                    )
                                }

                                if (promo.examinerName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Examiner: ${promo.examinerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Belt Promotion Dialog
    if (isPromoteDialogOpen) {
        RecordPromotionDialog(
            students = filteredStudents,
            preselectedStudent = selectedStudentForPromotion,
            onDismiss = {
                isPromoteDialogOpen = false
                selectedStudentForPromotion = null
            },
            onSave = { promo ->
                onRecordPromotion(promo)
                onUpdateStudentBelt(promo.studentId, promo.newBelt)
                isPromoteDialogOpen = false
                selectedStudentForPromotion = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPromotionDialog(
    students: List<Student>,
    preselectedStudent: Student?,
    onDismiss: () -> Unit,
    onSave: (BeltPromotion) -> Unit
) {
    var studentId by remember { mutableStateOf(preselectedStudent?.id ?: students.firstOrNull()?.id ?: "") }
    val selectedStudent = students.find { it.id == studentId }

    var previousBelt by remember(selectedStudent) { mutableStateOf(selectedStudent?.currentBelt ?: "White Belt") }
    var newBelt by remember { mutableStateOf("Yellow Belt") }
    var promotionDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var examinerName by remember { mutableStateOf("Shihan Jagadish") }
    var comments by remember { mutableStateOf("Excellent execution of Kata and Kumite technique.") }
    var certificateNumber by remember { mutableStateOf("DAN-${System.currentTimeMillis() % 100000}") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var newBeltDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Award Belt Promotion", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Student selector
                Box {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Karateka") },
                        trailingIcon = {
                            IconButton(onClick = { studentDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.fullName} (${s.currentBelt})") },
                                onClick = {
                                    studentId = s.id
                                    previousBelt = s.currentBelt
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = previousBelt,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Previous Belt") },
                    modifier = Modifier.fillMaxWidth()
                )

                // New Belt Selector
                Box {
                    OutlinedTextField(
                        value = newBelt,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New Belt Awarded *") },
                        trailingIcon = {
                            IconButton(onClick = { newBeltDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = newBeltDropdownExpanded,
                        onDismissRequest = { newBeltDropdownExpanded = false }
                    ) {
                        BeltOptions.forEach { (belt, _) ->
                            DropdownMenuItem(
                                text = { Text(belt) },
                                onClick = {
                                    newBelt = belt
                                    newBeltDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = examinerName,
                    onValueChange = { examinerName = it },
                    label = { Text("Chief Examiner / Sensei") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = certificateNumber,
                    onValueChange = { certificateNumber = it },
                    label = { Text("Certificate Serial #") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Examiner Comments & Performance") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (studentId.isBlank()) return@Button
                    val promotion = BeltPromotion(
                        orgId = selectedStudent?.orgId ?: "",
                        dojoId = selectedStudent?.dojoId ?: "",
                        studentId = studentId,
                        studentName = selectedStudent?.fullName ?: "",
                        previousBelt = previousBelt,
                        newBelt = newBelt,
                        promotionDate = promotionDate,
                        examinerName = examinerName.trim(),
                        comments = comments.trim(),
                        certificateNumber = certificateNumber.trim()
                    )
                    onSave(promotion)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                modifier = Modifier.testTag("confirm_promotion_button")
            ) {
                Text("Confirm Promotion")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
