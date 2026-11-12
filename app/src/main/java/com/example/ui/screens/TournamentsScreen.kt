package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.Organisation
import com.example.model.Tournament
import com.example.ui.theme.DojoCrimson
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentsScreen(
    currentLanguage: AppLanguage,
    organisation: Organisation?,
    tournaments: List<Tournament>,
    onSaveTournament: (Tournament) -> Unit,
    onDeleteTournament: (String) -> Unit
) {
    val context = LocalContext.current
    var isAddTournamentDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddTournamentDialogOpen = true },
                containerColor = DojoCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_tournament_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tournament")
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = DojoCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tournaments & Championships",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                Text(
                    text = "${tournaments.size} Events Announced",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (tournaments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tournaments announced yet", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { isAddTournamentDialogOpen = true }) {
                                Text("Publish Tournament Notice")
                            }
                        }
                    }
                }
            } else {
                items(tournaments, key = { it.id }) { tournament ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tournament.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Organizer: ${tournament.organizer.ifBlank { organisation?.name ?: "Federation" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = if (tournament.status == "upcoming") Color(0xFFFEF3C7) else Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = tournament.status.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tournament.status == "upcoming") Color(0xFFD97706) else Color(0xFF4B5563),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Date: ${tournament.date}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Venue: ${tournament.venue}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            if (tournament.registrationDeadline.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("⏳ Registration Deadline: ${tournament.registrationDeadline}", fontSize = 12.sp, color = DojoCrimson)
                            }

                            if (tournament.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(tournament.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val notice = "🏆 TOURNAMENT NOTICE - ${organisation?.shortName ?: "DojoFLOW"}\n\nEvent: ${tournament.name}\nDate: ${tournament.date}\nVenue: ${tournament.venue}\nOrganizer: ${tournament.organizer}\nDeadline: ${tournament.registrationDeadline}\n\n${tournament.description}\n\nAll Karateka are encouraged to register and compete!"
                                        val encoded = URLEncoder.encode(notice, "UTF-8")
                                        val uri = Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp Notice", fontSize = 12.sp, color = Color.White)
                                }

                                IconButton(onClick = { onDeleteTournament(tournament.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (isAddTournamentDialogOpen) {
        TournamentFormDialog(
            defaultOrganizer = organisation?.name ?: "Federation",
            onDismiss = { isAddTournamentDialogOpen = false },
            onSave = { tournament ->
                onSaveTournament(tournament.copy(orgId = organisation?.id ?: ""))
                isAddTournamentDialogOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentFormDialog(
    defaultOrganizer: String,
    onDismiss: () -> Unit,
    onSave: (Tournament) -> Unit
) {
    var name by remember { mutableStateOf("All India Inter-Dojo Karate Championship") }
    var date by remember { mutableStateOf("2026-12-15") }
    var venue by remember { mutableStateOf("Jawaharlal Nehru Indoor Stadium, Chennai") }
    var organizer by remember { mutableStateOf(defaultOrganizer) }
    var registrationDeadline by remember { mutableStateOf("2026-12-05") }
    var description by remember { mutableStateOf("Categories: Sub-Junior, Cadet, Junior & Senior. Individual Kata and Kumite divisions with official federation medals.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Tournament", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Championship / Event Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("tournament_name_input"),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Event Date *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = registrationDeadline,
                        onValueChange = { registrationDeadline = it },
                        label = { Text("Deadline") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Arena *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    label = { Text("Organizing Body") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Divisions") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || date.isBlank() || venue.isBlank()) return@Button
                    val tournament = Tournament(
                        name = name.trim(),
                        date = date.trim(),
                        venue = venue.trim(),
                        organizer = organizer.trim(),
                        registrationDeadline = registrationDeadline.trim(),
                        description = description.trim(),
                        status = "upcoming"
                    )
                    onSave(tournament)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                modifier = Modifier.testTag("save_tournament_button")
            ) {
                Text("Publish Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
