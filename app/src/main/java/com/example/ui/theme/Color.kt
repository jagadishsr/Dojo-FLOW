package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// DojoFLOW Stitch Authoritative Brand Palette (Sprint 03)
val DojoCrimson = Color(0xFFC9212B)         // Primary Red
val DojoCrimsonDark = Color(0xFF971923)     // Deep Red / Header Gradient Start
val DojoCrimsonLight = Color(0xFFE11D48)

val DojoWarmCanvas = Color(0xFFF7F6F3)      // Master warm ground canvas
val DojoCardSurface = Color(0xFFFFFFFF)     // Clean white container / card
val DojoCardHover = Color(0xFFF1F3F5)
val DojoBorder = Color(0xFFE6E6E8)          // Fine hairline border
val DojoBorderSubtle = Color(0xFFF0F0F2)

val DojoTextPrimary = Color(0xFF20232B)     // High-contrast primary ink
val DojoTextSecondary = Color(0xFF777D87)   // Metadata, subtitles, captions
val DojoTextMuted = Color(0xFF9CA3AF)

// Semantic State Tokens
val DojoSuccess = Color(0xFF1E9B61)         // Present, paid, cleared
val DojoWarning = Color(0xFFE9A52A)         // Pending dues, grading candidates
val DojoError = Color(0xFFD94141)           // Absent, overdue

// Backward-compatible aliases
val DojoBlack = Color(0xFF0F1115)
val DojoDarkSurface = Color(0xFF171A21)
val DojoDarkSurfaceVariant = Color(0xFF21252F)
val DojoLightBackground = DojoWarmCanvas
val DojoLightSurface = DojoCardSurface
val DojoLightSurfaceVariant = Color(0xFFF1F3F5)
val DojoLightBorder = DojoBorder

val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFF9CA3AF)
val TextPrimaryLight = DojoTextPrimary
val TextSecondaryLight = DojoTextSecondary

val StatusActiveGreen = DojoSuccess
val StatusInactiveGray = DojoTextSecondary
val StatusWarningOrange = DojoWarning

// Martial Arts Belt Colors (Shotokan / Traditional Kyu-Dan)
val BeltWhite = Color(0xFFF3F4F6)
val BeltYellow = Color(0xFFEAB308)
val BeltOrange = Color(0xFFF97316)
val BeltGreen = Color(0xFF22C55E)
val BeltBlue = Color(0xFF2563EB)
val BeltPurple = Color(0xFF9333EA)
val BeltBrown = Color(0xFF78350F)
val BeltBlack = Color(0xFF18181B)
val BeltRed = DojoCrimson
