package com.example.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DojoRepository
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Dojo
import com.example.ui.components.KaratePunchEmblem
import com.example.ui.screens.*
import com.example.ui.theme.DojoCrimson
import com.example.ui.theme.DojoCrimsonDark
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    currentUserId: String,
    repository: DojoRepository,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Real-time Firestore Observation
    val userProfile by repository.observeUserProfile(currentUserId).collectAsStateWithLifecycle(initialValue = null)
    val organisations by repository.observeOrganisations().collectAsStateWithLifecycle(initialValue = emptyList())
    val dojos by repository.observeDojos().collectAsStateWithLifecycle(initialValue = emptyList())
    val students by repository.observeStudents().collectAsStateWithLifecycle(initialValue = emptyList())
    val attendanceSessions by repository.observeAttendanceSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val classSchedules by repository.observeClassSchedules().collectAsStateWithLifecycle(initialValue = emptyList())
    val payments by repository.observePayments().collectAsStateWithLifecycle(initialValue = emptyList())
    val beltPromotions by repository.observeBeltPromotions().collectAsStateWithLifecycle(initialValue = emptyList())
    val tournaments by repository.observeTournaments().collectAsStateWithLifecycle(initialValue = emptyList())
    val announcements by repository.observeAnnouncements().collectAsStateWithLifecycle(initialValue = emptyList())

    val organisation = organisations.firstOrNull()

    // Role Evaluation according to Master Specification:
    // - TOP SENSEI ADMIN: role in ['founder', 'top_sensei'] -> Full administration of all dojos, instructors, fees, reports
    // - SHIHAN: role == 'shihan' -> Access to assigned dojo(s), with dojo-level student oversight
    // - REGULAR SENSEI: role == 'sensei' -> Class management and attendance only
    val userRole = userProfile?.role ?: "founder"
    val isTopSensei = userRole == "founder" || userRole == "top_sensei"
    val isShihan = userRole == "shihan"
    val isRegularSensei = userRole == "sensei"

    // Dojo scoping based on assigned role (Top Sensei has access to all; Shihan & Sensei only assigned)
    val visibleDojos = remember(dojos, isTopSensei, userProfile?.assignedDojoIds, isShihan, isRegularSensei) {
        if (isTopSensei) {
            dojos
        } else {
            val assignedIds = userProfile?.assignedDojoIds ?: emptyList()
            val filtered = dojos.filter { assignedIds.contains(it.id) }
            if (filtered.isNotEmpty()) filtered
            else dojos.take(1)
        }
    }

    // Dedicated Loading state to avoid the flash of onboarding screen on login
    var isInitialDataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile, organisations) {
        if (userProfile != null || organisations.isNotEmpty()) {
            isInitialDataLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        // Fallback safety timeout so brand new users are not indefinitely blocked
        delay(1200)
        isInitialDataLoaded = true
    }

    // Full-screen loading splash while initial user/org data loads
    if (!isInitialDataLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(DojoCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    KaratePunchEmblem(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "DOJOFLOW",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Loading Academy Workspace...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = DojoCrimson,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
        return
    }

    // Onboarding Gate: persistent check on onboardingStatus and existing organisations
    val isCompleted = (userProfile?.onboardingStatus == "completed") || organisations.isNotEmpty()
    if (!isCompleted) {
        OnboardingScreen(
            repository = repository,
            currentLanguage = currentLanguage,
            initialStep = userProfile?.onboardingStep ?: 0,
            onLanguageChange = onLanguageChange,
            onOnboardingComplete = {
                // Flow will automatically pick up newly created organisation & dojo
            }
        )
        return
    }

    // Auto-heal if organisation exists but dojos list is empty
    LaunchedEffect(organisation, visibleDojos) {
        if (organisation != null && visibleDojos.isEmpty()) {
            val user = Firebase.auth.currentUser
            try {
                repository.ensureDefaultSetup(
                    defaultSenseiName = user?.displayName ?: "Sensei",
                    email = user?.email ?: ""
                )
            } catch (e: Exception) {
                Log.w("MainAppScreen", "Auto-setup healing failed: ${e.message}")
            }
        }
    }

    // Active selected dojo management: default to first available dojo
    var selectedDojoId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(visibleDojos) {
        if (selectedDojoId == null || visibleDojos.none { it.id == selectedDojoId }) {
            selectedDojoId = visibleDojos.firstOrNull()?.id
        }
    }
    val selectedDojo = visibleDojos.find { it.id == selectedDojoId } ?: visibleDojos.firstOrNull()

    // Navigation Tab (Exact 5 permanent bottom destinations):
    // 0: STUDENTS
    // 1: SCHEDULE
    // 2: HOME (Center raised 62px button with KaratePunchEmblem)
    // 3: EVENTS
    // 4: STATS (Integrated fees, attendance, belts, reports)
    var currentTab by remember { mutableStateOf(2) } // Default to 2: HOME
    var isSettingsOpen by remember { mutableStateOf(false) }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var isAddDojoDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Persistent Full-Width Red Gradient Header (#971923 to #C9212B)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                DojoCrimsonDark,   // #971923
                                DojoCrimson        // #C9212B
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Martial Arts Crest Logo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        KaratePunchEmblem(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center: UPPERCASE Org Name + "POWERED BY DOJOFLOW"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = (organisation?.name?.ifBlank { "JAPAN FUNAKOSHI SHOTOKAN KARATE INDIA FEDERATION (JFSKIF)" } ?: "DOJOFLOW FEDERATION").uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "POWERED BY DOJOFLOW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                    }

                    // Right: Settings Cog & Profile Avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTopSensei || isShihan) {
                            IconButton(
                                onClick = {
                                    isSettingsOpen = !isSettingsOpen
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                if (isTopSensei || isShihan) isSettingsOpen = true
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userProfile?.displayName?.take(1)?.uppercase() ?: "S"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Mandatory 5-Item Navigation with Center Raised Button:
            // 0: STUDENTS | 1: SCHEDULE | 2: HOME (Center raised, NO LABEL) | 3: EVENTS | 4: STATS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // 0: STUDENTS
                        BottomNavItem(
                            icon = Icons.Default.People,
                            label = "STUDENTS",
                            isSelected = !isSettingsOpen && currentTab == 0,
                            onClick = {
                                isSettingsOpen = false
                                currentTab = 0
                            },
                            modifier = Modifier.weight(1f).testTag("nav_tab_students")
                        )

                        // 1: SCHEDULE
                        BottomNavItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "SCHEDULE",
                            isSelected = !isSettingsOpen && currentTab == 1,
                            onClick = {
                                isSettingsOpen = false
                                currentTab = 1
                            },
                            modifier = Modifier.weight(1f).testTag("nav_tab_schedule")
                        )

                        // Spacer for center raised button (no text label)
                        Spacer(modifier = Modifier.weight(1.1f))

                        // 3: EVENTS
                        BottomNavItem(
                            icon = Icons.Default.EmojiEvents,
                            label = "EVENTS",
                            isSelected = !isSettingsOpen && currentTab == 3,
                            onClick = {
                                isSettingsOpen = false
                                currentTab = 3
                            },
                            modifier = Modifier.weight(1f).testTag("nav_tab_events")
                        )

                        // 4: STATS (Fees & Analytics)
                        BottomNavItem(
                            icon = Icons.Default.QueryStats,
                            label = "STATS",
                            isSelected = !isSettingsOpen && currentTab == 4,
                            onClick = {
                                isSettingsOpen = false
                                currentTab = 4
                            },
                            modifier = Modifier.weight(1f).testTag("nav_tab_stats")
                        )
                    }
                }

                // Center Raised Red Button: HOME (2) with white KaratePunchEmblem - STRICTLY NO TEXT LABEL
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(DojoCrimsonDark, DojoCrimson)
                            )
                        )
                        .border(3.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable {
                            isSettingsOpen = false
                            currentTab = 2
                        }
                        .testTag("nav_tab_home_raised"),
                    contentAlignment = Alignment.Center
                ) {
                    KaratePunchEmblem(
                        color = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    ) { padding ->
        // Central Independently Scrollable Viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isSettingsOpen) {
                BackHandler { isSettingsOpen = false }
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isSettingsOpen = false }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DojoCrimson
                                )
                            }
                            Text(
                                text = "Back to Workspace",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    SettingsScreen(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        userProfile = userProfile,
                        organisation = organisation,
                        dojos = visibleDojos,
                        students = students,
                        payments = payments,
                        onAddDojo = { dojo ->
                            coroutineScope.launch {
                                try {
                                    repository.saveDojo(dojo)
                                    snackbarHostState.showSnackbar("Dojo branch established")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to save dojo")
                                }
                            }
                        },
                        onSaveOrganisation = { org ->
                            coroutineScope.launch {
                                try {
                                    repository.saveOrganisation(org)
                                    snackbarHostState.showSnackbar("Organisation settings updated")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to update org")
                                }
                            }
                        },
                        onSignOut = onSignOut
                    )
                }
            } else {
                when (currentTab) {
                    // 0: STUDENTS
                    0 -> StudentsScreen(
                        currentLanguage = currentLanguage,
                        students = students,
                        dojos = visibleDojos,
                        selectedDojo = selectedDojo,
                        attendanceSessions = attendanceSessions,
                        payments = payments,
                        beltPromotions = beltPromotions,
                        userProfile = userProfile,
                        isTopSensei = isTopSensei,
                        isShihan = isShihan,
                        isRegularSensei = isRegularSensei,
                        showAddStudentDialogInitially = showAddStudentDialog,
                        onAddStudentDismissed = { showAddStudentDialog = false },
                        onSaveStudent = { student ->
                            coroutineScope.launch {
                                try {
                                    repository.saveStudent(student)
                                    snackbarHostState.showSnackbar("Karateka profile saved successfully")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to save student")
                                }
                            }
                        },
                        onArchiveStudent = { studentId, reason ->
                            coroutineScope.launch {
                                try {
                                    repository.archiveStudent(studentId, reason)
                                    snackbarHostState.showSnackbar("Student record archived")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to archive student")
                                }
                            }
                        },
                        onRestoreStudent = { student ->
                            coroutineScope.launch {
                                try {
                                    repository.restoreStudent(student.id)
                                    snackbarHostState.showSnackbar("Student record restored")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to restore student")
                                }
                            }
                        },
                        onDeleteStudent = { studentId ->
                            coroutineScope.launch {
                                try {
                                    repository.deleteStudent(studentId)
                                    snackbarHostState.showSnackbar("Student record deleted")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to delete student")
                                }
                            }
                        },
                        onNavigateToSchedule = {
                            currentTab = 1
                        }
                    )

                    // 1: SCHEDULE (With integrated month calendar & open class attendance check-in)
                    1 -> SchedulesScreen(
                        currentLanguage = currentLanguage,
                        organisation = organisation,
                        dojos = visibleDojos,
                        selectedDojo = selectedDojo,
                        classSchedules = classSchedules,
                        announcements = announcements,
                        students = students,
                        onSaveSchedule = { schedule ->
                            coroutineScope.launch {
                                try {
                                    repository.saveClassSchedule(schedule)
                                    snackbarHostState.showSnackbar("Class schedule updated")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to save schedule")
                                }
                            }
                        },
                        onDeleteSchedule = { scheduleId ->
                            coroutineScope.launch {
                                try {
                                    repository.deleteClassSchedule(scheduleId)
                                    snackbarHostState.showSnackbar("Schedule removed")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to delete schedule")
                                }
                            }
                        },
                        onSaveAnnouncement = { announcement ->
                            coroutineScope.launch {
                                try {
                                    repository.saveAnnouncement(announcement)
                                } catch (e: Exception) {
                                    Log.w("MainAppScreen", "Failed to save announcement: ${e.message}")
                                }
                            }
                        },
                        onDeleteAnnouncement = { announcementId ->
                            coroutineScope.launch {
                                try {
                                    repository.deleteAnnouncement(announcementId)
                                    snackbarHostState.showSnackbar("Announcement removed from history")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to delete announcement")
                                }
                            }
                        },
                        onSaveAttendance = { session ->
                            coroutineScope.launch {
                                try {
                                    repository.saveAttendanceSession(session)
                                    snackbarHostState.showSnackbar("Attendance sheet saved for ${session.title}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to save attendance")
                                }
                            }
                        }
                    )

                    // 2: HOME (Executive Sensei Dashboard with exclusive Dojo Selection)
                    2 -> DashboardScreen(
                        currentLanguage = currentLanguage,
                        organisation = organisation,
                        selectedDojo = selectedDojo,
                        dojos = visibleDojos,
                        students = students,
                        attendanceSessions = attendanceSessions,
                        classSchedules = classSchedules,
                        payments = payments,
                        tournaments = tournaments,
                        onSelectDojo = { dojo -> selectedDojoId = dojo.id },
                        onAddDojoClick = { isAddDojoDialogOpen = true },
                        isTopSensei = isTopSensei,
                        isShihan = isShihan,
                        isRegularSensei = isRegularSensei,
                        onNavigateToStudents = { currentTab = 0 },
                        onNavigateToAttendance = { currentTab = 1 },
                        onNavigateToFees = { currentTab = 4 },
                        onNavigateToSchedules = { currentTab = 1 },
                        onNavigateToBelts = { currentTab = 4 },
                        onNavigateToTournaments = { currentTab = 3 },
                        onAddStudentClick = {
                            currentTab = 0
                            showAddStudentDialog = true
                        }
                    )

                    // 3: EVENTS (Tournaments, Seminars, Exams)
                    3 -> TournamentsScreen(
                        currentLanguage = currentLanguage,
                        organisation = organisation,
                        tournaments = tournaments,
                        onSaveTournament = { tournament ->
                            coroutineScope.launch {
                                try {
                                    repository.saveTournament(tournament)
                                    snackbarHostState.showSnackbar("Event published successfully")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to publish tournament")
                                }
                            }
                        },
                        onDeleteTournament = { tournamentId ->
                            coroutineScope.launch {
                                try {
                                    repository.deleteTournament(tournamentId)
                                    snackbarHostState.showSnackbar("Event deleted")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to delete tournament")
                                }
                            }
                        }
                    )

                    // 4: STATS (Integrated Fees & Ledger, Attendance Stats, Belt Progression, Reports)
                    4 -> StatsScreen(
                        currentLanguage = currentLanguage,
                        organisation = organisation,
                        dojos = visibleDojos,
                        students = students,
                        payments = payments,
                        attendanceSessions = attendanceSessions,
                        beltPromotions = beltPromotions,
                        selectedDojo = selectedDojo,
                        onRecordPayment = { payment ->
                            coroutineScope.launch {
                                try {
                                    repository.savePayment(payment)
                                    snackbarHostState.showSnackbar("Fee receipt created: ₹${payment.amount.toInt()}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to record payment")
                                }
                            }
                        },
                        onRecordPromotion = { promotion ->
                            coroutineScope.launch {
                                try {
                                    repository.saveBeltPromotion(promotion)
                                    snackbarHostState.showSnackbar("Belt promotion recorded for ${promotion.studentName}")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to record promotion")
                                }
                            }
                        },
                        onUpdateStudentBelt = { studentId, newBelt ->
                            coroutineScope.launch {
                                try {
                                    val student = students.find { it.id == studentId }
                                    if (student != null) {
                                        repository.saveStudent(student.copy(currentBelt = newBelt))
                                    }
                                } catch (e: Exception) {
                                    Log.w("MainAppScreen", "Failed to update student belt", e)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Dojo Dialog when user clicks [ + ADD DOJO ]
    if (isAddDojoDialogOpen) {
        AddDojoDialog(
            orgId = organisation?.id ?: "",
            onDismiss = { isAddDojoDialogOpen = false },
            onSave = { dojo ->
                coroutineScope.launch {
                    try {
                        val targetOrgId = organisation?.id ?: ""
                        repository.saveDojo(dojo.copy(orgId = targetOrgId))
                        snackbarHostState.showSnackbar("New dojo branch established")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.localizedMessage ?: "Failed to create dojo branch")
                    }
                }
                isAddDojoDialogOpen = false
            }
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) DojoCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DojoCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            letterSpacing = 0.5.sp
        )
    }
}
