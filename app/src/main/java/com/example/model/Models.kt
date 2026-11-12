package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "founder", // founder, shihan, sensei, student, parent
    val orgId: String = "",
    val assignedDojoIds: List<String> = emptyList(),
    val onboardingStatus: String = "completed", // not_started, in_progress, completed
    val onboardingStep: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Organisation(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val shortName: String = "",
    val martialArtStyle: String = "Shotokan Karate",
    val country: String = "India",
    val language: String = "English",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val topSenseiName: String = "",
    val address: String = "",
    val dateOfEstablishment: String = "",
    val logoUrl: String = "",
    val affiliation: String = "", // e.g. "Member of World JKA Karate Association (WJKA)"
    val announcementGreeting: String = "Dear parents & students,",
    val announcementClosing: String = "Thank you,",
    val defaultUniform: String = "Please come in your Karate uniform.",
    val defaultItemsToBring: String = "Kindly bring water bottle & gear where required.",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Dojo(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val shortName: String = "",
    val address: String = "",
    val locality: String = "",
    val city: String = "Chennai",
    val state: String = "Tamil Nadu",
    val country: String = "India",
    val postalCode: String = "",
    val contactNumber: String = "",
    val email: String = "",
    val assignedShihanUids: List<String> = emptyList(),
    val openingDate: String = "",
    val status: String = "active", // active, inactive
    val monthlyFee: Double = 500.0,
    val quarterlyFee: Double = 1400.0,
    val halfYearlyFee: Double = 2700.0,
    val annuallyFee: Double = 5000.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "", // Managing Dojo ID (mandatory for all modes)
    val managingDojoId: String = "", // Explicit alias
    val ownerUid: String = "",
    val fullName: String = "",
    val gender: String = "Male",
    val dateOfBirth: String = "",
    val age: Int = 0,
    val photoUrl: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val parentGuardianName: String = "",
    val parentGuardianMobile: String = "",
    val email: String = "",
    val address: String = "",
    val locality: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val postalCode: String = "",
    val admissionDate: String = "",
    val trainingType: String = "Regular Dojo Training", // Regular Dojo Training, Private Home Training, Online Training
    val trainingMode: String = "DOJO", // DOJO, PRIVATE, ONLINE
    val trainingAddress: String = "",
    val trainingLocation: String = "", // Private training address/locality
    val onlineMeetingInfo: String = "", // Zoom/Google Meet link or platform
    val currentBelt: String = "White Belt",
    val studentDesignation: String = "Karateka", // Shihan, Sempai, Kohai, Karateka
    val emergencyContact: String = "",
    val notes: String = "",
    val feePlan: String = "Monthly", // Monthly, Quarterly, Half-yearly, Annually
    val status: String = "active", // active, inactive
    val leavingDate: String = "",
    val leavingReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceSession(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "",
    val ownerUid: String = "",
    val classId: String = "",
    val date: String = "",
    val title: String = "Regular Class",
    val instructorName: String = "",
    val presentStudentIds: List<String> = emptyList(),
    val totalStudents: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScheduleEntry(
    val dayOrDate: String = "",
    val sessionTime: String = "",
    val activity: String = "",
    val isNoClass: Boolean = false
)

@IgnoreExtraProperties
data class ClassSchedule(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "",
    val ownerUid: String = "",
    val instructorUid: String = "",
    val instructorName: String = "",
    val title: String = "Regular Karate Training",
    val description: String = "",
    val startDate: String = "",
    val startTime: String = "18:00",
    val endTime: String = "19:30",
    val daysOfWeek: List<String> = listOf("Mon", "Wed", "Fri"),
    val recurrenceType: String = "Selected Weekdays", // Weekdays, Weekends, Selected Weekdays, One-time
    val instructions: String = "Please arrive 10 minutes prior to warm-up in full Gi.",
    val entriesJson: String = "", // Serialized multi-entry schedule items
    val uniform: String = "Please come in your Karate uniform.",
    val itemsToBring: String = "",
    val specialNotice: String = "",
    val announcementText: String = "", // Generated WhatsApp announcement
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedEntries(): List<ScheduleEntry> {
        if (entriesJson.isBlank()) return emptyList()
        return try {
            val list = mutableListOf<ScheduleEntry>()
            val array = org.json.JSONArray(entriesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScheduleEntry(
                        dayOrDate = obj.optString("dayOrDate", ""),
                        sessionTime = obj.optString("sessionTime", ""),
                        activity = obj.optString("activity", ""),
                        isNoClass = obj.optBoolean("isNoClass", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun serializeEntries(entries: List<ScheduleEntry>): String {
            val array = org.json.JSONArray()
            for (entry in entries) {
                val obj = org.json.JSONObject()
                obj.put("dayOrDate", entry.dayOrDate)
                obj.put("sessionTime", entry.sessionTime)
                obj.put("activity", entry.activity)
                obj.put("isNoClass", entry.isNoClass)
                array.put(obj)
            }
            return array.toString()
        }
    }
}

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "",
    val scheduleId: String = "",
    val title: String = "",
    val message: String = "",
    val publishedBy: String = "",
    val targetAudience: String = "Parents & Students",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "",
    val ownerUid: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "UPI", // Cash, UPI, Bank Transfer, Other
    val feePeriod: String = "Monthly",
    val receiptNumber: String = "",
    val paymentDate: String = "",
    val notes: String = "",
    val recordedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BeltPromotion(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val dojoId: String = "",
    val ownerUid: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val previousBelt: String = "White Belt",
    val newBelt: String = "Yellow Belt",
    val promotionDate: String = "",
    val examinerName: String = "",
    val comments: String = "",
    val certificateNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val date: String = "",
    val venue: String = "",
    val organizer: String = "",
    val registrationDeadline: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val status: String = "upcoming", // upcoming, completed
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
