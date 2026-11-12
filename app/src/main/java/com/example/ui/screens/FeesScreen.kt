package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import com.example.model.Dojo
import com.example.model.Payment
import com.example.model.Student
import com.example.ui.theme.DojoCrimson
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusWarningOrange
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeesScreen(
    currentLanguage: AppLanguage,
    students: List<Student>,
    dojos: List<Dojo>,
    payments: List<Payment>,
    selectedDojo: Dojo?,
    onRecordPayment: (Payment) -> Unit
) {
    val context = LocalContext.current
    var isRecordPaymentDialogOpen by remember { mutableStateOf(false) }
    var selectedStudentForPayment by remember { mutableStateOf<Student?>(null) }
    var viewingReceipt by remember { mutableStateOf<Payment?>(null) }
    var feeFilter by remember { mutableStateOf("all") } // "all", "pending", "history"

    val activeDojoId = selectedDojo?.id
    val filteredStudents = remember(students, activeDojoId) {
        if (activeDojoId != null) students.filter { it.dojoId == activeDojoId && it.status == "active" }
        else students.filter { it.status == "active" }
    }

    val filteredPayments = remember(payments, activeDojoId) {
        if (activeDojoId != null) payments.filter { it.dojoId == activeDojoId }
        else payments
    }

    val totalCollected = filteredPayments.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isRecordPaymentDialogOpen = true },
                containerColor = DojoCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("record_payment_fab")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = "Record Payment")
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
            // Collections Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Fee Collection Ledger",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${totalCollected.toInt()}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusActiveGreen
                            )
                            Surface(
                                color = StatusActiveGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${filteredPayments.size} Transactions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusActiveGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filter Chips (Students Ledger vs Payment Transactions)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = feeFilter == "all",
                                onClick = { feeFilter = "all" },
                                label = { Text("Student Ledgers") }
                            )
                            FilterChip(
                                selected = feeFilter == "history",
                                onClick = { feeFilter = "history" },
                                label = { Text("Receipt History (${filteredPayments.size})") }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (feeFilter == "all") {
                item {
                    Text(
                        text = "Student Fee Status & WhatsApp Reminders",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredStudents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No students found to display fee ledgers.")
                            }
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.id }) { student ->
                        val studentPayments = filteredPayments.filter { it.studentId == student.id }
                        val lastPayment = studentPayments.maxByOrNull { it.paymentDate }
                        val dojoName = dojos.find { it.id == student.dojoId }?.name ?: "Dojo"

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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = student.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Plan: ${student.feePlan} • Dojo: $dojoName",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = if (lastPayment != null) StatusActiveGreen.copy(alpha = 0.15f) else StatusWarningOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (lastPayment != null) "Paid ₹${lastPayment.amount.toInt()}" else "Pending",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (lastPayment != null) StatusActiveGreen else StatusWarningOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (lastPayment != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Last Payment: ${lastPayment.paymentDate} (${lastPayment.paymentMethod})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            selectedStudentForPayment = student
                                            isRecordPaymentDialogOpen = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Fee", fontSize = 12.sp)
                                    }

                                    if (student.parentGuardianMobile.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                val message = "Dear Parent, this is a friendly reminder that the training fee for ${student.fullName} at $dojoName is due. Kindly arrange payment. Thank you - DojoFLOW"
                                                val encoded = URLEncoder.encode(message, "UTF-8")
                                                val phone = student.parentGuardianMobile.replace("+", "").replace(" ", "").trim()
                                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encoded")
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            modifier = Modifier.weight(1.3f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp Reminder", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // Receipt History View
                item {
                    Text(
                        text = "Receipts & Transaction History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredPayments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No payment receipts recorded yet.")
                            }
                        }
                    }
                } else {
                    items(filteredPayments) { payment ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = payment.studentName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Rcpt #${payment.receiptNumber} • ${payment.paymentDate} • ${payment.paymentMethod}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${payment.amount.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = StatusActiveGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(onClick = { viewingReceipt = payment }) {
                                        Icon(Icons.Default.Receipt, contentDescription = "View Receipt", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Record Payment Dialog
    if (isRecordPaymentDialogOpen) {
        RecordPaymentDialog(
            students = filteredStudents,
            preselectedStudent = selectedStudentForPayment,
            onDismiss = {
                isRecordPaymentDialogOpen = false
                selectedStudentForPayment = null
            },
            onSave = { payment ->
                onRecordPayment(payment)
                isRecordPaymentDialogOpen = false
                selectedStudentForPayment = null
                viewingReceipt = payment
            }
        )
    }

    // Receipt Dialog
    if (viewingReceipt != null) {
        ReceiptPreviewDialog(
            payment = viewingReceipt!!,
            onDismiss = { viewingReceipt = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    students: List<Student>,
    preselectedStudent: Student?,
    onDismiss: () -> Unit,
    onSave: (Payment) -> Unit
) {
    var studentId by remember { mutableStateOf(preselectedStudent?.id ?: students.firstOrNull()?.id ?: "") }
    var amountStr by remember { mutableStateOf("500") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var feePeriod by remember { mutableStateOf(preselectedStudent?.feePlan ?: "Monthly") }
    var receiptNumber by remember { mutableStateOf("RCP-${System.currentTimeMillis() % 100000}") }
    var paymentDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var notes by remember { mutableStateOf("") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    val selectedStudent = students.find { it.id == studentId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Fee Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Student Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedStudent?.fullName ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student") },
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
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹) *") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    singleLine = true
                )

                // Payment Method Dropdown
                Box {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = {
                            IconButton(onClick = { methodDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = methodDropdownExpanded,
                        onDismissRequest = { methodDropdownExpanded = false }
                    ) {
                        listOf("UPI", "Cash", "Bank Transfer", "Cheque", "Other").forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    methodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = receiptNumber,
                    onValueChange = { receiptNumber = it },
                    label = { Text("Receipt Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Transaction Ref (e.g. GPay ref)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (studentId.isBlank() || amount <= 0) return@Button

                    val payment = Payment(
                        orgId = selectedStudent?.orgId ?: "",
                        dojoId = selectedStudent?.dojoId ?: "",
                        studentId = studentId,
                        studentName = selectedStudent?.fullName ?: "",
                        amount = amount,
                        paymentMethod = paymentMethod,
                        feePeriod = feePeriod,
                        receiptNumber = receiptNumber,
                        paymentDate = paymentDate,
                        notes = notes.trim()
                    )
                    onSave(payment)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                modifier = Modifier.testTag("save_payment_confirm_button")
            ) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReceiptPreviewDialog(
    payment: Payment,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment Receipt", fontWeight = FontWeight.Bold) },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StatusActiveGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusActiveGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "₹${payment.amount.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Payment Received",
                        fontSize = 12.sp,
                        color = StatusActiveGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(14.dp))

                    ReceiptRow(label = "Student", value = payment.studentName)
                    ReceiptRow(label = "Receipt #", value = payment.receiptNumber)
                    ReceiptRow(label = "Method", value = payment.paymentMethod)
                    ReceiptRow(label = "Date", value = payment.paymentDate)
                    if (payment.notes.isNotBlank()) {
                        ReceiptRow(label = "Reference", value = payment.notes)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = "🥋 DojoFLOW Payment Receipt\n\nStudent: ${payment.studentName}\nReceipt #: ${payment.receiptNumber}\nAmount: ₹${payment.amount.toInt()}\nDate: ${payment.paymentDate}\nMethod: ${payment.paymentMethod}\nStatus: Confirmed Paid\n\nThank you for training with us!"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Receipt"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
