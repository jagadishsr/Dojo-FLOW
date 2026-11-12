package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DojoRepository
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.Dojo
import com.example.model.Organisation
import com.example.model.UserProfile
import com.example.ui.theme.DojoCrimson
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

data class PendingDojo(
    val name: String,
    val shortName: String,
    val address: String,
    val locality: String,
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String,
    val monthlyFee: Double = 500.0,
    val quarterlyFee: Double = 1400.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    repository: DojoRepository,
    currentLanguage: AppLanguage,
    initialStep: Int = 0,
    onLanguageChange: (AppLanguage) -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // Onboarding Wizard Steps
    // 0: Welcome & Academy Name
    // 1: Head Instructor
    // 2: First Dojo Name
    // 3: First Dojo Location
    // 4: Add More Dojos Question (Yes/No loop)
    // 5: Next Dojo Name & Location (if user chooses Yes)
    var currentStep by remember(initialStep) { mutableStateOf(initialStep.coerceIn(0, 5)) }

    // Organization data
    var orgName by remember { mutableStateOf("") }
    var orgShortName by remember { mutableStateOf("") }
    var martialArtStyle by remember { mutableStateOf("Shotokan Karate") }
    var topSenseiName by remember {
        mutableStateOf(currentUser?.displayName?.ifBlank { "Shihan Jagadish" } ?: "Shihan Jagadish")
    }

    // First Dojo data
    var firstDojoName by remember { mutableStateOf("") }
    var firstDojoShortName by remember { mutableStateOf("") }
    var firstDojoAddress by remember { mutableStateOf("") }
    var firstDojoLocality by remember { mutableStateOf("") }
    var firstDojoCity by remember { mutableStateOf("Chennai") }
    var firstDojoState by remember { mutableStateOf("Tamil Nadu") }
    var firstDojoCountry by remember { mutableStateOf("India") }
    var firstDojoPostalCode by remember { mutableStateOf("") }

    // List of additional established dojos if user adds more
    val establishedDojos = remember { mutableStateListOf<PendingDojo>() }

    // Additional dojo temporary fields
    var nextDojoName by remember { mutableStateOf("") }
    var nextDojoShortName by remember { mutableStateOf("") }
    var nextDojoAddress by remember { mutableStateOf("") }
    var nextDojoLocality by remember { mutableStateOf("") }
    var nextDojoCity by remember { mutableStateOf("Chennai") }
    var nextDojoState by remember { mutableStateOf("Tamil Nadu") }
    var nextDojoCountry by remember { mutableStateOf("India") }
    var nextDojoPostalCode by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var langDropdownExpanded by remember { mutableStateOf(false) }

    // Country selection options
    val countries = listOf("India", "Japan", "United States", "United Kingdom", "Australia", "Singapore", "Malaysia", "UAE", "Germany", "Canada")

    fun finalizeOnboarding() {
        val uid = currentUser?.uid ?: return
        if (orgName.isBlank() || firstDojoName.isBlank()) {
            errorMessage = "Please enter academy name and first dojo name"
            return
        }
        isSubmitting = true
        errorMessage = null

        coroutineScope.launch {
            try {
                // 1. Save Organisation
                val org = Organisation(
                    ownerUid = uid,
                    name = orgName.trim(),
                    shortName = orgShortName.ifBlank { orgName.take(6).uppercase() }.trim(),
                    martialArtStyle = martialArtStyle.trim(),
                    country = firstDojoCountry.trim(),
                    language = currentLanguage.displayName,
                    topSenseiName = topSenseiName.trim(),
                    address = "$firstDojoCity, $firstDojoState, $firstDojoCountry",
                    contactEmail = currentUser.email ?: ""
                )
                val orgId = repository.saveOrganisation(org)

                // 2. Save First Dojo
                val dojo1 = Dojo(
                    orgId = orgId,
                    ownerUid = uid,
                    name = firstDojoName.trim(),
                    shortName = firstDojoShortName.ifBlank { firstDojoName.take(8) }.trim(),
                    address = firstDojoAddress.trim(),
                    locality = firstDojoLocality.trim(),
                    city = firstDojoCity.trim(),
                    state = firstDojoState.trim(),
                    country = firstDojoCountry.trim(),
                    postalCode = firstDojoPostalCode.trim(),
                    monthlyFee = 500.0,
                    quarterlyFee = 1400.0,
                    status = "active"
                )
                val firstDojoId = repository.saveDojo(dojo1)
                val allDojoIds = mutableListOf(firstDojoId)

                // 3. Save any additional dojos
                for (extra in establishedDojos) {
                    val extraDojo = Dojo(
                        orgId = orgId,
                        ownerUid = uid,
                        name = extra.name.trim(),
                        shortName = extra.shortName.ifBlank { extra.name.take(8) }.trim(),
                        address = extra.address.trim(),
                        locality = extra.locality.trim(),
                        city = extra.city.trim(),
                        state = extra.state.trim(),
                        country = extra.country.trim(),
                        postalCode = extra.postalCode.trim(),
                        monthlyFee = extra.monthlyFee,
                        quarterlyFee = extra.quarterlyFee,
                        status = "active"
                    )
                    val extraId = repository.saveDojo(extraDojo)
                    allDojoIds.add(extraId)
                }

                // 4. Save User Profile as Founder / Top Sensei
                val userProfile = UserProfile(
                    uid = uid,
                    email = currentUser.email ?: "",
                    displayName = topSenseiName.trim(),
                    role = "founder",
                    orgId = orgId,
                    assignedDojoIds = allDojoIds,
                    onboardingStatus = "completed"
                )
                repository.saveUserProfile(userProfile)

                isSubmitting = false
                onOnboardingComplete()
            } catch (e: Exception) {
                isSubmitting = false
                errorMessage = e.localizedMessage ?: "Failed to save academy setup"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DojoCrimson),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥋", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DojoFLOW",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep > 0 && !isSubmitting) {
                        IconButton(onClick = {
                            errorMessage = null
                            if (currentStep == 5) {
                                currentStep = 4
                            } else {
                                currentStep--
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { langDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(currentLanguage.nativeName, fontSize = 13.sp)
                        }
                        DropdownMenu(
                            expanded = langDropdownExpanded,
                            onDismissRequest = { langDropdownExpanded = false }
                        ) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.nativeName} (${lang.displayName})") },
                                    onClick = {
                                        onLanguageChange(lang)
                                        langDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Step Progress Indicator
            val totalSteps = 5
            val currentProgress = ((currentStep.coerceAtMost(4) + 1).toFloat() / totalSteps.toFloat())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP ${currentStep.coerceAtMost(4) + 1} OF $totalSteps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = DojoCrimson
                )
                if (establishedDojos.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "+${establishedDojos.size} extra dojo(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = DojoCrimson,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Animated Conversational Steps
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "OnboardingWizardAnimation"
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    when (step) {
                        // SCREEN 1: WELCOME & ACADEMY NAME
                        0 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "WELCOME TO DOJOFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "What is the name of your Martial Arts Academy?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Let's set up your Martial Arts Academy. This will only take a few moments.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = orgName,
                                onValueChange = { orgName = it },
                                label = { Text("Full Academy / Organisation Name *") },
                                placeholder = { Text("e.g. Japan Funakoshi Shotokan Karate India Federation") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("org_name_input"),
                                singleLine = false,
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = orgShortName,
                                    onValueChange = { orgShortName = it },
                                    label = { Text("Short Code (Optional)") },
                                    placeholder = { Text("e.g. JFSKIF") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = martialArtStyle,
                                    onValueChange = { martialArtStyle = it },
                                    label = { Text("Martial Art Style") },
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // SCREEN 2: HEAD INSTRUCTOR (SENSEI)
                        1 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "LEADERSHIP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Who is the Head Instructor (Sensei)?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This name will appear on official certificates, notifications and announcements.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = topSenseiName,
                                onValueChange = { topSenseiName = it },
                                label = { Text("Full Name of Head Instructor / Top Sensei *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DojoCrimson) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("top_sensei_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // SCREEN 3: FIRST DOJO NAME
                        2 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "PRIMARY LOCATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Let's establish your first Dojo.",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "What is the name of your Dojo?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = firstDojoName,
                                onValueChange = { firstDojoName = it },
                                label = { Text("Dojo Name *") },
                                placeholder = { Text("e.g. Sri Sankara Dojo") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("first_dojo_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = firstDojoShortName,
                                onValueChange = { firstDojoShortName = it },
                                label = { Text("Branch Identifier (Optional)") },
                                placeholder = { Text("e.g. Central / HQ") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // SCREEN 4: FIRST DOJO LOCATION
                        3 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "DOJO ADDRESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Where is this Dojo located?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter physical training address details for $firstDojoName.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = firstDojoAddress,
                                onValueChange = { firstDojoAddress = it },
                                label = { Text("Address / Building / Hall Name") },
                                placeholder = { Text("e.g. 14, Main Road, Near Community Hall") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = firstDojoLocality,
                                    onValueChange = { firstDojoLocality = it },
                                    label = { Text("Locality / Area *") },
                                    placeholder = { Text("e.g. Pammal") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = firstDojoCity,
                                    onValueChange = { firstDojoCity = it },
                                    label = { Text("City *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = firstDojoState,
                                    onValueChange = { firstDojoState = it },
                                    label = { Text("State / Province") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = firstDojoCountry,
                                    onValueChange = { firstDojoCountry = it },
                                    label = { Text("Country") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = firstDojoPostalCode,
                                onValueChange = { firstDojoPostalCode = it },
                                label = { Text("Postal / PIN Code") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // SCREEN 5: ADD MORE DOJOS QUESTION?
                        4 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "MULTI-DOJO NETWORK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Would you like to add another Dojo?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "If you manage multiple branches or training centres, add them now or anytime later from Settings.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Summary card of already established dojos
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Established Dojo(s):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("1. $firstDojoName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(${firstDojoLocality.ifBlank { firstDojoCity }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    establishedDojos.forEachIndexed { index, dojo ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${index + 2}. ${dojo.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${dojo.locality.ifBlank { dojo.city }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Reset next dojo fields and proceed to step 5
                                        nextDojoName = ""
                                        nextDojoShortName = ""
                                        nextDojoAddress = ""
                                        nextDojoLocality = ""
                                        nextDojoCity = firstDojoCity
                                        nextDojoState = firstDojoState
                                        nextDojoCountry = firstDojoCountry
                                        nextDojoPostalCode = ""
                                        currentStep = 5
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Yes, Add Another", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { finalizeOnboarding() },
                                    enabled = !isSubmitting,
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(52.dp)
                                        .testTag("complete_onboarding_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("No, Finish Setup", fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    }
                                }
                            }
                        }

                        // SCREEN 5: EXTRA DOJO ENTRY
                        5 -> {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ADDITIONAL DOJO #${establishedDojos.size + 2}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DojoCrimson,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Enter details for the next Dojo branch",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 30.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = nextDojoName,
                                onValueChange = { nextDojoName = it },
                                label = { Text("Dojo Name *") },
                                placeholder = { Text("e.g. Pallavaram Dojo") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = nextDojoLocality,
                                    onValueChange = { nextDojoLocality = it },
                                    label = { Text("Locality / Area *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = nextDojoCity,
                                    onValueChange = { nextDojoCity = it },
                                    label = { Text("City *") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = nextDojoAddress,
                                onValueChange = { nextDojoAddress = it },
                                label = { Text("Address / Building / Hall") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (nextDojoName.isBlank()) {
                                        errorMessage = "Please enter the dojo name"
                                        return@Button
                                    }
                                    establishedDojos.add(
                                        PendingDojo(
                                            name = nextDojoName.trim(),
                                            shortName = nextDojoShortName.ifBlank { nextDojoName.take(8) }.trim(),
                                            address = nextDojoAddress.trim(),
                                            locality = nextDojoLocality.trim(),
                                            city = nextDojoCity.trim(),
                                            state = nextDojoState.trim(),
                                            country = nextDojoCountry.trim(),
                                            postalCode = nextDojoPostalCode.trim()
                                        )
                                    )
                                    errorMessage = null
                                    // Return to question step 4 to ask if they want yet another dojo
                                    currentStep = 4
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Dojo & Continue", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFF991B1B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Bottom Continue Button for Steps 0, 1, 2, 3
            if (currentStep < 4) {
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        when (currentStep) {
                            0 -> {
                                if (orgName.isBlank()) {
                                    errorMessage = "Please enter your Martial Arts Academy name"
                                    return@Button
                                }
                                currentStep = 1
                            }
                            1 -> {
                                if (topSenseiName.isBlank()) {
                                    errorMessage = "Please enter Head Instructor's name"
                                    return@Button
                                }
                                currentStep = 2
                            }
                            2 -> {
                                if (firstDojoName.isBlank()) {
                                    errorMessage = "Please enter your first Dojo name"
                                    return@Button
                                }
                                currentStep = 3
                            }
                            3 -> {
                                if (firstDojoLocality.isBlank() && firstDojoCity.isBlank()) {
                                    errorMessage = "Please enter the locality or city for this dojo"
                                    return@Button
                                }
                                currentStep = 4
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DojoCrimson),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
