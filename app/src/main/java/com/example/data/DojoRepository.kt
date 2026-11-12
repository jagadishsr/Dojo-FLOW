package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DojoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val PREVIEW_USER_ID = DojoPreviewData.PREVIEW_USER_ID
    }

    private val auth = Firebase.auth

    val isPreviewMode: Boolean
        get() = auth.currentUser == null

    // In-memory flows for preview / testing without requiring Google Sign-In in web emulator
    private val _previewUserProfile = MutableStateFlow<UserProfile?>(DojoPreviewData.initialUserProfile)
    private val _previewOrganisations = MutableStateFlow(DojoPreviewData.initialOrganisations)
    private val _previewDojos = MutableStateFlow(DojoPreviewData.initialDojos)
    private val _previewStudents = MutableStateFlow(DojoPreviewData.initialStudents)
    private val _previewAttendanceSessions = MutableStateFlow(DojoPreviewData.initialAttendanceSessions)
    private val _previewClassSchedules = MutableStateFlow(DojoPreviewData.initialClassSchedules)
    private val _previewPayments = MutableStateFlow(DojoPreviewData.initialPayments)
    private val _previewBeltPromotions = MutableStateFlow(DojoPreviewData.initialBeltPromotions)
    private val _previewTournaments = MutableStateFlow(DojoPreviewData.initialTournaments)
    private val _previewAnnouncements = MutableStateFlow(DojoPreviewData.initialAnnouncements)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: if (isPreviewMode) PREVIEW_USER_ID
            else throw IllegalStateException("User must be signed in with Google before accessing DojoFLOW.")
    }

    // --- Safe Document Mappers (prevents numeric/timestamp reflection deserialization crashes) ---

    private fun DocumentSnapshot.toUserProfileSafe(): UserProfile {
        return UserProfile(
            id = id,
            uid = getString("uid") ?: id,
            email = getString("email") ?: "",
            displayName = getString("displayName") ?: "",
            role = getString("role") ?: "founder",
            orgId = getString("orgId") ?: "",
            assignedDojoIds = (get("assignedDojoIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            onboardingStatus = getString("onboardingStatus") ?: "completed",
            onboardingStep = (get("onboardingStep") as? Number)?.toInt() ?: 0,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toOrganisationSafe(): Organisation {
        return Organisation(
            id = id,
            ownerUid = getString("ownerUid") ?: "",
            name = getString("name") ?: "",
            shortName = getString("shortName") ?: "",
            martialArtStyle = getString("martialArtStyle") ?: "Shotokan Karate",
            country = getString("country") ?: "India",
            language = getString("language") ?: "English",
            contactEmail = getString("contactEmail") ?: "",
            contactPhone = getString("contactPhone") ?: "",
            topSenseiName = getString("topSenseiName") ?: "",
            address = getString("address") ?: "",
            dateOfEstablishment = getString("dateOfEstablishment") ?: "",
            logoUrl = getString("logoUrl") ?: "",
            affiliation = getString("affiliation") ?: "",
            announcementGreeting = getString("announcementGreeting") ?: "Dear parents & students,",
            announcementClosing = getString("announcementClosing") ?: "Thank you,",
            defaultUniform = getString("defaultUniform") ?: "Please come in your Karate uniform.",
            defaultItemsToBring = getString("defaultItemsToBring") ?: "Kindly bring water bottle & gear where required.",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toDojoSafe(): Dojo {
        return Dojo(
            id = id,
            orgId = getString("orgId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            name = getString("name") ?: "",
            shortName = getString("shortName") ?: "",
            address = getString("address") ?: "",
            locality = getString("locality") ?: "",
            city = getString("city") ?: "Chennai",
            state = getString("state") ?: "Tamil Nadu",
            country = getString("country") ?: "India",
            postalCode = getString("postalCode") ?: "",
            contactNumber = getString("contactNumber") ?: "",
            email = getString("email") ?: "",
            assignedShihanUids = (get("assignedShihanUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            openingDate = getString("openingDate") ?: "",
            status = getString("status") ?: "active",
            monthlyFee = (get("monthlyFee") as? Number)?.toDouble() ?: 500.0,
            quarterlyFee = (get("quarterlyFee") as? Number)?.toDouble() ?: 1400.0,
            halfYearlyFee = (get("halfYearlyFee") as? Number)?.toDouble() ?: 2700.0,
            annuallyFee = (get("annuallyFee") as? Number)?.toDouble() ?: 5000.0,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toStudentSafe(): Student {
        val storedType = getString("trainingType") ?: "Regular Dojo Training"
        val explicitMode = getString("trainingMode") ?: when {
            storedType.contains("Private", ignoreCase = true) -> "PRIVATE"
            storedType.contains("Online", ignoreCase = true) -> "ONLINE"
            else -> "DOJO"
        }
        val targetDojoId = getString("dojoId") ?: ""
        return Student(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = targetDojoId,
            managingDojoId = getString("managingDojoId") ?: targetDojoId,
            ownerUid = getString("ownerUid") ?: "",
            fullName = getString("fullName") ?: "",
            gender = getString("gender") ?: "Male",
            dateOfBirth = getString("dateOfBirth") ?: "",
            age = (get("age") as? Number)?.toInt() ?: 0,
            photoUrl = getString("photoUrl") ?: "",
            fatherName = getString("fatherName") ?: "",
            motherName = getString("motherName") ?: "",
            parentGuardianName = getString("parentGuardianName") ?: "",
            parentGuardianMobile = getString("parentGuardianMobile") ?: "",
            email = getString("email") ?: "",
            address = getString("address") ?: "",
            locality = getString("locality") ?: "",
            city = getString("city") ?: "",
            state = getString("state") ?: "",
            country = getString("country") ?: "India",
            postalCode = getString("postalCode") ?: "",
            admissionDate = getString("admissionDate") ?: "",
            trainingType = storedType,
            trainingMode = explicitMode,
            trainingAddress = getString("trainingAddress") ?: "",
            trainingLocation = getString("trainingLocation") ?: getString("trainingAddress") ?: "",
            onlineMeetingInfo = getString("onlineMeetingInfo") ?: "",
            currentBelt = getString("currentBelt") ?: "White Belt",
            studentDesignation = getString("studentDesignation") ?: "Karateka",
            emergencyContact = getString("emergencyContact") ?: "",
            notes = getString("notes") ?: "",
            feePlan = getString("feePlan") ?: "Monthly",
            status = getString("status") ?: "active",
            leavingDate = getString("leavingDate") ?: "",
            leavingReason = getString("leavingReason") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toAttendanceSessionSafe(): AttendanceSession {
        return AttendanceSession(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = getString("dojoId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            classId = getString("classId") ?: "",
            date = getString("date") ?: "",
            title = getString("title") ?: "Regular Class",
            instructorName = getString("instructorName") ?: "",
            presentStudentIds = (get("presentStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            totalStudents = (get("totalStudents") as? Number)?.toInt() ?: 0,
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toClassScheduleSafe(): ClassSchedule {
        return ClassSchedule(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = getString("dojoId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            instructorUid = getString("instructorUid") ?: "",
            instructorName = getString("instructorName") ?: "",
            title = getString("title") ?: "Regular Karate Training",
            description = getString("description") ?: "",
            startDate = getString("startDate") ?: "",
            startTime = getString("startTime") ?: "18:00",
            endTime = getString("endTime") ?: "19:30",
            daysOfWeek = (get("daysOfWeek") as? List<*>)?.filterIsInstance<String>() ?: listOf("Mon", "Wed", "Fri"),
            recurrenceType = getString("recurrenceType") ?: "Selected Weekdays",
            instructions = getString("instructions") ?: "",
            entriesJson = getString("entriesJson") ?: "",
            uniform = getString("uniform") ?: "Please come in your Karate uniform.",
            itemsToBring = getString("itemsToBring") ?: "",
            specialNotice = getString("specialNotice") ?: "",
            announcementText = getString("announcementText") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toAnnouncementSafe(): Announcement {
        return Announcement(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = getString("dojoId") ?: "",
            scheduleId = getString("scheduleId") ?: "",
            title = getString("title") ?: "",
            message = getString("message") ?: "",
            publishedBy = getString("publishedBy") ?: "",
            targetAudience = getString("targetAudience") ?: "Parents & Students",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toPaymentSafe(): Payment {
        return Payment(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = getString("dojoId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            studentId = getString("studentId") ?: "",
            studentName = getString("studentName") ?: "",
            amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
            paymentMethod = getString("paymentMethod") ?: "UPI",
            feePeriod = getString("feePeriod") ?: "Monthly",
            receiptNumber = getString("receiptNumber") ?: "",
            paymentDate = getString("paymentDate") ?: "",
            notes = getString("notes") ?: "",
            recordedByUid = getString("recordedByUid") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toBeltPromotionSafe(): BeltPromotion {
        return BeltPromotion(
            id = id,
            orgId = getString("orgId") ?: "",
            dojoId = getString("dojoId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            studentId = getString("studentId") ?: "",
            studentName = getString("studentName") ?: "",
            previousBelt = getString("previousBelt") ?: "White Belt",
            newBelt = getString("newBelt") ?: "Yellow Belt",
            promotionDate = getString("promotionDate") ?: "",
            examinerName = getString("examinerName") ?: "",
            comments = getString("comments") ?: "",
            certificateNumber = getString("certificateNumber") ?: "",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    private fun DocumentSnapshot.toTournamentSafe(): Tournament {
        return Tournament(
            id = id,
            orgId = getString("orgId") ?: "",
            ownerUid = getString("ownerUid") ?: "",
            name = getString("name") ?: "",
            date = getString("date") ?: "",
            venue = getString("venue") ?: "",
            organizer = getString("organizer") ?: "",
            registrationDeadline = getString("registrationDeadline") ?: "",
            description = getString("description") ?: "",
            posterUrl = getString("posterUrl") ?: "",
            status = getString("status") ?: "upcoming",
            createdAt = getTimestamp("createdAt"),
            updatedAt = getTimestamp("updatedAt")
        )
    }

    // --- User Profile ---
    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        if (isPreviewMode) return _previewUserProfile
        return flow {
            val path = "users/$uid"
            emitAll(
                db.collection("users").document(uid)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            try { snapshot.toUserProfileSafe() } catch (e: Exception) { null }
                        } else null
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.GET, path)
                        }
                        emit(null)
                    }
            )
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        if (isPreviewMode) {
            _previewUserProfile.value = profile
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "uid" to uid,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "role" to profile.role,
            "orgId" to profile.orgId,
            "assignedDojoIds" to profile.assignedDojoIds,
            "onboardingStatus" to profile.onboardingStatus,
            "onboardingStep" to profile.onboardingStep,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Organisations ---
    fun observeOrganisations(): Flow<List<Organisation>> {
        if (isPreviewMode) return _previewOrganisations
        return flow {
            val uid = requireUserId()
            val path = "organisations"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toOrganisationSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse org ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveOrganisation(org: Organisation): String {
        if (isPreviewMode) {
            val id = if (org.id.isNotBlank()) org.id else "org_preview_${System.currentTimeMillis()}"
            val updated = org.copy(id = id, ownerUid = PREVIEW_USER_ID)
            _previewOrganisations.value = _previewOrganisations.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        val docRef = if (org.id.isNotBlank()) db.collection("organisations").document(org.id)
                     else db.collection("organisations").document()
        val payload = mutableMapOf<String, Any?>(
            "ownerUid" to uid,
            "name" to org.name,
            "shortName" to org.shortName,
            "martialArtStyle" to org.martialArtStyle,
            "country" to org.country,
            "language" to org.language,
            "contactEmail" to org.contactEmail,
            "contactPhone" to org.contactPhone,
            "topSenseiName" to org.topSenseiName,
            "address" to org.address,
            "dateOfEstablishment" to org.dateOfEstablishment,
            "logoUrl" to org.logoUrl,
            "affiliation" to org.affiliation,
            "announcementGreeting" to org.announcementGreeting,
            "announcementClosing" to org.announcementClosing,
            "defaultUniform" to org.defaultUniform,
            "defaultItemsToBring" to org.defaultItemsToBring,
            "createdAt" to (org.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Dojos ---
    fun observeDojos(): Flow<List<Dojo>> {
        if (isPreviewMode) return _previewDojos
        return flow {
            val uid = requireUserId()
            val path = "dojos"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toDojoSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse dojo ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveDojo(dojo: Dojo): String {
        if (isPreviewMode) {
            val id = if (dojo.id.isNotBlank()) dojo.id else "dojo_preview_${System.currentTimeMillis()}"
            val updated = dojo.copy(id = id, ownerUid = PREVIEW_USER_ID)
            _previewDojos.value = _previewDojos.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        val docRef = if (dojo.id.isNotBlank()) db.collection("dojos").document(dojo.id)
                     else db.collection("dojos").document()
        val payload = mapOf(
            "orgId" to dojo.orgId,
            "ownerUid" to uid,
            "name" to dojo.name,
            "shortName" to dojo.shortName,
            "address" to dojo.address,
            "locality" to dojo.locality,
            "city" to dojo.city,
            "state" to dojo.state,
            "country" to dojo.country,
            "postalCode" to dojo.postalCode,
            "contactNumber" to dojo.contactNumber,
            "email" to dojo.email,
            "assignedShihanUids" to dojo.assignedShihanUids,
            "openingDate" to dojo.openingDate,
            "status" to dojo.status,
            "monthlyFee" to dojo.monthlyFee,
            "quarterlyFee" to dojo.quarterlyFee,
            "halfYearlyFee" to dojo.halfYearlyFee,
            "annuallyFee" to dojo.annuallyFee,
            "createdAt" to (dojo.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDojo(dojoId: String) {
        if (isPreviewMode) {
            _previewDojos.value = _previewDojos.value.filter { it.id != dojoId }
            return
        }
        val docRef = db.collection("dojos").document(dojoId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Auto-repair / ensure default org & dojo if user is authenticated but collections are empty
    suspend fun ensureDefaultSetup(defaultSenseiName: String, email: String): Pair<String, String> {
        if (isPreviewMode) {
            return Pair(DojoPreviewData.PREVIEW_ORG_ID, DojoPreviewData.PREVIEW_DOJO_1_ID)
        }
        val uid = requireUserId()
        // Check existing orgs
        val orgQuery = db.collection("organisations")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()

        val orgId = if (!orgQuery.isEmpty) {
            orgQuery.documents.first().id
        } else {
            val newOrg = Organisation(
                ownerUid = uid,
                name = "Japan Funakoshi Shotokan Karate India Federation",
                shortName = "JFSKIF",
                martialArtStyle = "Shotokan Karate",
                country = "India",
                language = "English",
                topSenseiName = defaultSenseiName,
                address = "Chennai, Tamil Nadu",
                contactEmail = email,
                affiliation = "Member of World JKA Karate Association (WJKA)"
            )
            saveOrganisation(newOrg)
        }

        // Check existing dojos
        val dojoQuery = db.collection("dojos")
            .whereEqualTo("ownerUid", uid)
            .get()
            .await()

        val dojoId = if (!dojoQuery.isEmpty) {
            dojoQuery.documents.first().id
        } else {
            val newDojo = Dojo(
                orgId = orgId,
                ownerUid = uid,
                name = "Sri Sankara Dojo",
                shortName = "Main",
                locality = "T. Nagar",
                city = "Chennai",
                state = "Tamil Nadu",
                country = "India",
                monthlyFee = 500.0,
                quarterlyFee = 1400.0,
                status = "active"
            )
            saveDojo(newDojo)
        }

        // Save UserProfile if missing
        val profileDoc = db.collection("users").document(uid).get().await()
        if (!profileDoc.exists()) {
            val profile = UserProfile(
                uid = uid,
                email = email,
                displayName = defaultSenseiName,
                role = "founder",
                orgId = orgId,
                assignedDojoIds = listOf(dojoId)
            )
            saveUserProfile(profile)
        }

        return Pair(orgId, dojoId)
    }

    // --- Students ---
    fun observeStudents(): Flow<List<Student>> {
        if (isPreviewMode) return _previewStudents
        return flow {
            val uid = requireUserId()
            val path = "students"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toStudentSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse student ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveStudent(student: Student): String {
        if (isPreviewMode) {
            val id = if (student.id.isNotBlank()) student.id else "student_preview_${System.currentTimeMillis()}"
            val effectiveDojoId = student.dojoId.ifBlank { student.managingDojoId.ifBlank { DojoPreviewData.PREVIEW_DOJO_1_ID } }
            val updated = student.copy(
                id = id,
                orgId = student.orgId.ifBlank { DojoPreviewData.PREVIEW_ORG_ID },
                dojoId = effectiveDojoId,
                managingDojoId = effectiveDojoId,
                ownerUid = PREVIEW_USER_ID
            )
            _previewStudents.value = _previewStudents.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        require(student.orgId.isNotBlank()) { "Organisation ID is required to save a student." }
        val effectiveDojoId = student.dojoId.ifBlank { student.managingDojoId }
        require(effectiveDojoId.isNotBlank()) { "Managing Dojo branch ID is required to save a student." }
        require(student.fullName.isNotBlank()) { "Student full name is required." }

        val docRef = if (student.id.isNotBlank()) db.collection("students").document(student.id)
                     else db.collection("students").document()
        val payload = mapOf(
            "orgId" to student.orgId,
            "dojoId" to effectiveDojoId,
            "managingDojoId" to effectiveDojoId,
            "ownerUid" to uid,
            "fullName" to student.fullName,
            "gender" to student.gender,
            "dateOfBirth" to student.dateOfBirth,
            "age" to student.age,
            "photoUrl" to student.photoUrl,
            "fatherName" to student.fatherName,
            "motherName" to student.motherName,
            "parentGuardianName" to student.parentGuardianName,
            "parentGuardianMobile" to student.parentGuardianMobile,
            "email" to student.email,
            "address" to student.address,
            "locality" to student.locality,
            "city" to student.city,
            "state" to student.state,
            "country" to student.country,
            "postalCode" to student.postalCode,
            "admissionDate" to student.admissionDate,
            "trainingType" to student.trainingType,
            "trainingMode" to student.trainingMode,
            "trainingAddress" to student.trainingAddress,
            "trainingLocation" to (student.trainingLocation.ifBlank { student.trainingAddress }),
            "onlineMeetingInfo" to student.onlineMeetingInfo,
            "currentBelt" to student.currentBelt,
            "studentDesignation" to student.studentDesignation,
            "emergencyContact" to student.emergencyContact,
            "notes" to student.notes,
            "feePlan" to student.feePlan,
            "status" to student.status,
            "leavingDate" to student.leavingDate,
            "leavingReason" to student.leavingReason,
            "createdAt" to (student.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun archiveStudent(studentId: String, reason: String) {
        if (isPreviewMode) {
            _previewStudents.value = _previewStudents.value.map {
                if (it.id == studentId) it.copy(status = "inactive", leavingReason = reason) else it
            }
            return
        }
        val docRef = db.collection("students").document(studentId)
        try {
            docRef.update(
                mapOf(
                    "status" to "inactive",
                    "leavingReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun restoreStudent(studentId: String) {
        if (isPreviewMode) {
            _previewStudents.value = _previewStudents.value.map {
                if (it.id == studentId) it.copy(status = "active", leavingReason = "") else it
            }
            return
        }
        val docRef = db.collection("students").document(studentId)
        try {
            docRef.update(
                mapOf(
                    "status" to "active",
                    "leavingReason" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStudent(studentId: String) {
        if (isPreviewMode) {
            _previewStudents.value = _previewStudents.value.filter { it.id != studentId }
            return
        }
        val docRef = db.collection("students").document(studentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Attendance Sessions ---
    fun observeAttendanceSessions(): Flow<List<AttendanceSession>> {
        if (isPreviewMode) return _previewAttendanceSessions
        return flow {
            val uid = requireUserId()
            val path = "attendance_sessions"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toAttendanceSessionSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse attendance ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveAttendanceSession(session: AttendanceSession): String {
        if (isPreviewMode) {
            val id = if (session.id.isNotBlank()) session.id else "session_preview_${System.currentTimeMillis()}"
            val effectiveOrg = session.orgId.ifBlank { DojoPreviewData.PREVIEW_ORG_ID }
            val effectiveDojo = session.dojoId.ifBlank { DojoPreviewData.PREVIEW_DOJO_1_ID }
            val updated = session.copy(
                id = id,
                orgId = effectiveOrg,
                dojoId = effectiveDojo,
                ownerUid = PREVIEW_USER_ID
            )
            _previewAttendanceSessions.value = _previewAttendanceSessions.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        var effectiveOrgId = session.orgId
        var effectiveDojoId = session.dojoId

        if (effectiveDojoId.isBlank()) {
            val dojoQuery = db.collection("dojos").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!dojoQuery.isEmpty) {
                val doc = dojoQuery.documents.first()
                effectiveDojoId = doc.id
                if (effectiveOrgId.isBlank()) effectiveOrgId = doc.getString("orgId") ?: ""
            }
        }
        if (effectiveOrgId.isBlank()) {
            val orgQuery = db.collection("organisations").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!orgQuery.isEmpty) effectiveOrgId = orgQuery.documents.first().id
        }

        require(effectiveOrgId.isNotBlank()) { "Organisation ID is required." }
        require(effectiveDojoId.isNotBlank()) { "Dojo ID is required." }

        val docRef = if (session.id.isNotBlank()) db.collection("attendance_sessions").document(session.id)
                     else db.collection("attendance_sessions").document()
        val payload = mapOf(
            "orgId" to effectiveOrgId,
            "dojoId" to effectiveDojoId,
            "ownerUid" to uid,
            "classId" to session.classId,
            "date" to session.date,
            "title" to session.title,
            "instructorName" to session.instructorName,
            "presentStudentIds" to session.presentStudentIds,
            "totalStudents" to session.totalStudents,
            "createdAt" to (session.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Class Schedules ---
    fun observeClassSchedules(): Flow<List<ClassSchedule>> {
        if (isPreviewMode) return _previewClassSchedules
        return flow {
            val uid = requireUserId()
            val path = "class_schedules"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toClassScheduleSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse schedule ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveClassSchedule(schedule: ClassSchedule): String {
        if (isPreviewMode) {
            val id = if (schedule.id.isNotBlank()) schedule.id else "sched_preview_${System.currentTimeMillis()}"
            val effectiveOrg = schedule.orgId.ifBlank { DojoPreviewData.PREVIEW_ORG_ID }
            val effectiveDojo = schedule.dojoId.ifBlank { DojoPreviewData.PREVIEW_DOJO_1_ID }
            val updated = schedule.copy(
                id = id,
                orgId = effectiveOrg,
                dojoId = effectiveDojo,
                ownerUid = PREVIEW_USER_ID
            )
            _previewClassSchedules.value = _previewClassSchedules.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        var effectiveOrgId = schedule.orgId
        var effectiveDojoId = schedule.dojoId

        if (effectiveDojoId.isBlank()) {
            val dojoQuery = db.collection("dojos").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!dojoQuery.isEmpty) {
                val doc = dojoQuery.documents.first()
                effectiveDojoId = doc.id
                if (effectiveOrgId.isBlank()) {
                    effectiveOrgId = doc.getString("orgId") ?: ""
                }
            }
        }

        if (effectiveOrgId.isBlank()) {
            val orgQuery = db.collection("organisations").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!orgQuery.isEmpty) {
                effectiveOrgId = orgQuery.documents.first().id
            }
        }

        require(effectiveOrgId.isNotBlank()) { "Organisation ID is required." }
        require(effectiveDojoId.isNotBlank()) { "Dojo ID is required." }

        val docRef = if (schedule.id.isNotBlank()) db.collection("class_schedules").document(schedule.id)
                     else db.collection("class_schedules").document()
        val payload = mapOf(
            "orgId" to effectiveOrgId,
            "dojoId" to effectiveDojoId,
            "ownerUid" to uid,
            "instructorUid" to schedule.instructorUid,
            "instructorName" to schedule.instructorName,
            "title" to schedule.title,
            "description" to schedule.description,
            "startDate" to schedule.startDate,
            "startTime" to schedule.startTime,
            "endTime" to schedule.endTime,
            "daysOfWeek" to schedule.daysOfWeek,
            "recurrenceType" to schedule.recurrenceType,
            "instructions" to schedule.instructions,
            "entriesJson" to schedule.entriesJson,
            "uniform" to schedule.uniform,
            "itemsToBring" to schedule.itemsToBring,
            "specialNotice" to schedule.specialNotice,
            "announcementText" to schedule.announcementText,
            "createdAt" to (schedule.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClassSchedule(scheduleId: String) {
        if (isPreviewMode) {
            _previewClassSchedules.value = _previewClassSchedules.value.filter { it.id != scheduleId }
            return
        }
        val docRef = db.collection("class_schedules").document(scheduleId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Announcements ---
    fun observeAnnouncements(): Flow<List<Announcement>> {
        if (isPreviewMode) return _previewAnnouncements
        return flow {
            val uid = requireUserId()
            val path = "announcements"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toAnnouncementSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse announcement ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveAnnouncement(announcement: Announcement): String {
        if (isPreviewMode) {
            val id = if (announcement.id.isNotBlank()) announcement.id else "ann_preview_${System.currentTimeMillis()}"
            val effectiveOrg = announcement.orgId.ifBlank { DojoPreviewData.PREVIEW_ORG_ID }
            val effectiveDojo = announcement.dojoId.ifBlank { DojoPreviewData.PREVIEW_DOJO_1_ID }
            val updated = announcement.copy(
                id = id,
                orgId = effectiveOrg,
                dojoId = effectiveDojo
            )
            _previewAnnouncements.value = _previewAnnouncements.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        var effectiveOrgId = announcement.orgId
        var effectiveDojoId = announcement.dojoId

        if (effectiveDojoId.isBlank()) {
            val dojoQuery = db.collection("dojos").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!dojoQuery.isEmpty) {
                val doc = dojoQuery.documents.first()
                effectiveDojoId = doc.id
                if (effectiveOrgId.isBlank()) effectiveOrgId = doc.getString("orgId") ?: ""
            }
        }
        if (effectiveOrgId.isBlank()) {
            val orgQuery = db.collection("organisations").whereEqualTo("ownerUid", uid).limit(1).get().await()
            if (!orgQuery.isEmpty) effectiveOrgId = orgQuery.documents.first().id
        }

        require(effectiveOrgId.isNotBlank()) { "Organisation ID is required." }
        require(effectiveDojoId.isNotBlank()) { "Dojo ID is required." }
        require(announcement.message.isNotBlank()) { "Announcement message is required." }

        val docRef = if (announcement.id.isNotBlank()) db.collection("announcements").document(announcement.id)
                     else db.collection("announcements").document()
        val payload = mapOf(
            "orgId" to effectiveOrgId,
            "dojoId" to effectiveDojoId,
            "scheduleId" to announcement.scheduleId,
            "ownerUid" to uid,
            "title" to announcement.title,
            "message" to announcement.message,
            "publishedBy" to announcement.publishedBy,
            "targetAudience" to announcement.targetAudience,
            "createdAt" to (announcement.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        if (isPreviewMode) {
            _previewAnnouncements.value = _previewAnnouncements.value.filter { it.id != announcementId }
            return
        }
        val docRef = db.collection("announcements").document(announcementId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Payments ---
    fun observePayments(): Flow<List<Payment>> {
        if (isPreviewMode) return _previewPayments
        return flow {
            val uid = requireUserId()
            val path = "payments"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toPaymentSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse payment ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun savePayment(payment: Payment): String {
        if (isPreviewMode) {
            val id = if (payment.id.isNotBlank()) payment.id else "pay_preview_${System.currentTimeMillis()}"
            val updated = payment.copy(id = id, ownerUid = PREVIEW_USER_ID)
            _previewPayments.value = _previewPayments.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        require(payment.orgId.isNotBlank()) { "Organisation ID is required." }
        require(payment.dojoId.isNotBlank()) { "Dojo ID is required." }
        require(payment.studentId.isNotBlank()) { "Student ID is required." }

        val docRef = if (payment.id.isNotBlank()) db.collection("payments").document(payment.id)
                     else db.collection("payments").document()
        val payload = mapOf(
            "orgId" to payment.orgId,
            "dojoId" to payment.dojoId,
            "ownerUid" to uid,
            "studentId" to payment.studentId,
            "studentName" to payment.studentName,
            "amount" to payment.amount,
            "paymentMethod" to payment.paymentMethod,
            "feePeriod" to payment.feePeriod,
            "receiptNumber" to payment.receiptNumber,
            "paymentDate" to payment.paymentDate,
            "notes" to payment.notes,
            "recordedByUid" to uid,
            "createdAt" to (payment.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Belt Promotions ---
    fun observeBeltPromotions(): Flow<List<BeltPromotion>> {
        if (isPreviewMode) return _previewBeltPromotions
        return flow {
            val uid = requireUserId()
            val path = "belt_promotions"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toBeltPromotionSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse promo ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveBeltPromotion(promotion: BeltPromotion): String {
        if (isPreviewMode) {
            val id = if (promotion.id.isNotBlank()) promotion.id else "promo_preview_${System.currentTimeMillis()}"
            val updated = promotion.copy(id = id, ownerUid = PREVIEW_USER_ID)
            _previewBeltPromotions.value = _previewBeltPromotions.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        require(promotion.orgId.isNotBlank()) { "Organisation ID is required." }
        require(promotion.dojoId.isNotBlank()) { "Dojo ID is required." }
        require(promotion.studentId.isNotBlank()) { "Student ID is required." }

        val docRef = if (promotion.id.isNotBlank()) db.collection("belt_promotions").document(promotion.id)
                     else db.collection("belt_promotions").document()
        val payload = mapOf(
            "orgId" to promotion.orgId,
            "dojoId" to promotion.dojoId,
            "ownerUid" to uid,
            "studentId" to promotion.studentId,
            "studentName" to promotion.studentName,
            "previousBelt" to promotion.previousBelt,
            "newBelt" to promotion.newBelt,
            "promotionDate" to promotion.promotionDate,
            "examinerName" to promotion.examinerName,
            "comments" to promotion.comments,
            "certificateNumber" to promotion.certificateNumber,
            "createdAt" to (promotion.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Tournaments ---
    fun observeTournaments(): Flow<List<Tournament>> {
        if (isPreviewMode) return _previewTournaments
        return flow {
            val uid = requireUserId()
            val path = "tournaments"
            emitAll(
                db.collection(path)
                    .whereEqualTo("ownerUid", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            try { doc.toTournamentSafe() } catch (e: Exception) {
                                Log.w("DojoRepo", "Failed to parse tournament ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val exceptionToLog = firestoreError ?: (error as? Exception)
                        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                        }
                        emit(emptyList())
                    }
            )
        }
    }

    suspend fun saveTournament(tournament: Tournament): String {
        if (isPreviewMode) {
            val id = if (tournament.id.isNotBlank()) tournament.id else "tourney_preview_${System.currentTimeMillis()}"
            val updated = tournament.copy(id = id, ownerUid = PREVIEW_USER_ID)
            _previewTournaments.value = _previewTournaments.value.filter { it.id != id } + updated
            return id
        }
        val uid = requireUserId()
        require(tournament.orgId.isNotBlank()) { "Organisation ID is required." }

        val docRef = if (tournament.id.isNotBlank()) db.collection("tournaments").document(tournament.id)
                     else db.collection("tournaments").document()
        val payload = mapOf(
            "orgId" to tournament.orgId,
            "ownerUid" to uid,
            "name" to tournament.name,
            "date" to tournament.date,
            "venue" to tournament.venue,
            "organizer" to tournament.organizer,
            "registrationDeadline" to tournament.registrationDeadline,
            "description" to tournament.description,
            "posterUrl" to tournament.posterUrl,
            "status" to tournament.status,
            "createdAt" to (tournament.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTournament(tournamentId: String) {
        if (isPreviewMode) {
            _previewTournaments.value = _previewTournaments.value.filter { it.id != tournamentId }
            return
        }
        val docRef = db.collection("tournaments").document(tournamentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
