package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Dojo
import com.example.model.Organisation
import com.example.model.Payment
import com.example.model.Student
import com.example.model.UserProfile
import com.example.ui.theme.DojoCrimson
import com.example.ui.theme.StatusActiveGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    userProfile: UserProfile?,
    organisation: Organisation?,
    dojos: List<Dojo>,
    students: List<Student>,
    payments: List<Payment>,
    onAddDojo: (Dojo) -> Unit,
    onSaveOrganisation: (Organisation) -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    var isAddDojoDialogOpen by remember { mutableStateOf(false) }
    var isLanguagePickerOpen by remember { mutableStateOf(false) }
    var isEditAnnouncementSettingsOpen by remember { mutableStateOf(false) }

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = DojoCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("settings", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            // Profile & Sensei Account Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DojoCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userProfile?.displayName ?: currentUser?.displayName ?: "S").take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName ?: currentUser?.displayName ?: "Sensei",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Role: Founder / Top Sensei Admin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Organisation Profile
            item {
                SectionHeader("Organisation Profile")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(label = "Full Name", value = organisation?.name ?: "Federation")
                        SettingsRow(label = "Short Code", value = organisation?.shortName ?: "N/A")
                        SettingsRow(label = "Style", value = organisation?.martialArtStyle ?: "Shotokan Karate")
                        SettingsRow(label = "Country", value = organisation?.country ?: "India")
                        SettingsRow(label = "Affiliation Line", value = organisation?.affiliation?.ifBlank { "Member of World JKA Karate Association (WJKA)" } ?: "Member of World JKA Karate Association (WJKA)")
                        SettingsRow(label = "Headquarters", value = organisation?.address ?: "")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // WhatsApp Announcement Defaults & Signature
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("WhatsApp Announcement Template")
                    TextButton(onClick = { isEditAnnouncementSettingsOpen = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = DojoCrimson)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Customize", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DojoCrimson)
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(
                            label = "Header Affiliation",
                            value = organisation?.affiliation?.ifBlank { "Member of World JKA Karate Association (WJKA)" } ?: "Member of World JKA Karate Association (WJKA)"
                        )
                        SettingsRow(
                            label = "Greeting Line",
                            value = organisation?.announcementGreeting?.ifBlank { "Dear parents & students," } ?: "Dear parents & students,"
                        )
                        SettingsRow(
                            label = "Default Uniform",
                            value = organisation?.defaultUniform?.ifBlank { "Full Gi with respective belts" } ?: "Full Gi with respective belts"
                        )
                        SettingsRow(
                            label = "Default Items",
                            value = organisation?.defaultItemsToBring?.ifBlank { "Water bottle, Karate diary" } ?: "Water bottle, Karate diary"
                        )
                        SettingsRow(
                            label = "Closing Sign-off",
                            value = organisation?.announcementClosing?.ifBlank { "Thank you," } ?: "Thank you,"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dojos Management
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Established Dojos (${dojos.size})")
                    TextButton(onClick = { isAddDojoDialogOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Branch", fontSize = 13.sp)
                    }
                }
            }

            items(dojos) { dojo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dojo.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("📍 ${dojo.locality}, ${dojo.city} • Monthly: ₹${dojo.monthlyFee.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = StatusActiveGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = dojo.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusActiveGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Language & Localization
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Language & Regional Settings")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLanguagePickerOpen = true },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = DojoCrimson)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("App Language", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${currentLanguage.nativeName} (${currentLanguage.displayName})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Data Export
            item {
                SectionHeader("Data Export & Reports")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Export student rosters and financial summaries in CSV format for Excel/Google Sheets compatibility.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val csv = buildString {
                                        appendLine("Name,Gender,Age,Belt,Dojo,Parent,Mobile,FeePlan,Status")
                                        students.forEach { s ->
                                            val dName = dojos.find { it.id == s.dojoId }?.name ?: ""
                                            appendLine("\"${s.fullName}\",\"${s.gender}\",${s.age},\"${s.currentBelt}\",\"$dName\",\"${s.parentGuardianName}\",\"${s.parentGuardianMobile}\",\"${s.feePlan}\",\"${s.status}\"")
                                        }
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Students CSV", csv))
                                    Toast.makeText(context, "Student roster CSV copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Student CSV", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val csv = buildString {
                                        appendLine("Student,ReceiptNo,Amount,Method,Date,Notes")
                                        payments.forEach { p ->
                                            appendLine("\"${p.studentName}\",\"${p.receiptNumber}\",${p.amount},\"${p.paymentMethod}\",\"${p.paymentDate}\",\"${p.notes}\"")
                                        }
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Fees CSV", csv))
                                    Toast.makeText(context, "Fees ledger CSV copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fees CSV", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sign Out Button
            item {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of DojoFLOW", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add Dojo Dialog
    if (isAddDojoDialogOpen) {
        AddDojoDialog(
            orgId = organisation?.id ?: "",
            onDismiss = { isAddDojoDialogOpen = false },
            onSave = { dojo ->
                onAddDojo(dojo)
                isAddDojoDialogOpen = false
            }
        )
    }

    // Language Picker Dialog
    if (isLanguagePickerOpen) {
        AlertDialog(
            onDismissRequest = { isLanguagePickerOpen = false },
            title = { Text(AppStrings.get("select_language", currentLanguage), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.values().forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(lang)
                                    isLanguagePickerOpen = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == lang,
                                onClick = {
                                    onLanguageChange(lang)
                                    isLanguagePickerOpen = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${lang.nativeName} (${lang.displayName})", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isLanguagePickerOpen = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit Announcement Template Settings Dialog
    if (isEditAnnouncementSettingsOpen && organisation != null) {
        var editAffiliation by remember { mutableStateOf(organisation.affiliation.ifBlank { "Member of World JKA Karate Association (WJKA)" }) }
        var editGreeting by remember { mutableStateOf(organisation.announcementGreeting.ifBlank { "Dear parents & students," }) }
        var editClosing by remember { mutableStateOf(organisation.announcementClosing.ifBlank { "Thank you," }) }
        var editUniform by remember { mutableStateOf(organisation.defaultUniform.ifBlank { "Full Gi with respective belts" }) }
        var editItems by remember { mutableStateOf(organisation.defaultItemsToBring.ifBlank { "Water bottle, Karate diary" }) }

        AlertDialog(
            onDismissRequest = { isEditAnnouncementSettingsOpen = false },
            title = { Text("WhatsApp Template Settings", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editAffiliation,
                        onValueChange = { editAffiliation = it },
                        label = { Text("Affiliation Line (below Org name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editGreeting,
                        onValueChange = { editGreeting = it },
                        label = { Text("Greeting Line") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editUniform,
                        onValueChange = { editUniform = it },
                        label = { Text("Default Uniform") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editItems,
                        onValueChange = { editItems = it },
                        label = { Text("Default Items to Bring") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editClosing,
                        onValueChange = { editClosing = it },
                        label = { Text("Closing Sign-off") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = organisation.copy(
                            affiliation = editAffiliation.trim(),
                            announcementGreeting = editGreeting.trim(),
                            announcementClosing = editClosing.trim(),
                            defaultUniform = editUniform.trim(),
                            defaultItemsToBring = editItems.trim()
                        )
                        onSaveOrganisation(updated)
                        isEditAnnouncementSettingsOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson)
                ) {
                    Text("Save Template Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditAnnouncementSettingsOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDojoDialog(
    orgId: String,
    onDismiss: () -> Unit,
    onSave: (Dojo) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Chennai") }
    var state by remember { mutableStateOf("Tamil Nadu") }
    var monthlyFeeStr by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Establish New Dojo Branch", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Dojo Full Name (e.g. Bengaluru East Dojo) *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_dojo_name"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text("Locality / Area *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = monthlyFeeStr,
                        onValueChange = { monthlyFeeStr = it },
                        label = { Text("Monthly Fee (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || locality.isBlank()) return@Button
                    val dojo = Dojo(
                        orgId = orgId,
                        name = name.trim(),
                        shortName = shortName.trim(),
                        locality = locality.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        monthlyFee = monthlyFeeStr.toDoubleOrNull() ?: 500.0,
                        status = "active"
                    )
                    onSave(dojo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                modifier = Modifier.testTag("save_new_dojo_button")
            ) {
                Text("Create Dojo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
