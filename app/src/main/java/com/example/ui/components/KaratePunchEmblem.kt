package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Karate Martial Arts Punching Emblem vector illustration
 * Renders a stylized karateka delivering a dynamic reverse punch (Gyaku-Zuki).
 */
@Composable
fun KaratePunchEmblem(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Head (circular silhouette)
        drawCircle(
            color = color,
            radius = w * 0.12f,
            center = Offset(w * 0.36f, h * 0.22f)
        )

        // Martial arts headband tie (hachimaki tail trailing backward)
        val headbandPath = Path().apply {
            moveTo(w * 0.28f, h * 0.20f)
            quadraticTo(w * 0.16f, h * 0.17f, w * 0.12f, h * 0.24f)
            lineTo(w * 0.15f, h * 0.28f)
            quadraticTo(w * 0.22f, h * 0.23f, w * 0.30f, h * 0.24f)
            close()
        }
        drawPath(headbandPath, color = color, style = Fill)

        // Torso / Gi jacket (forward leaning powerful stance)
        val torsoPath = Path().apply {
            moveTo(w * 0.36f, h * 0.32f) // neck
            lineTo(w * 0.52f, h * 0.40f) // right shoulder
            lineTo(w * 0.46f, h * 0.62f) // waist right
            lineTo(w * 0.30f, h * 0.62f) // waist left
            lineTo(w * 0.24f, h * 0.38f) // left shoulder
            close()
        }
        drawPath(torsoPath, color = color, style = Fill)

        // Punching arm (extended right fist delivering punch forward)
        val punchArmPath = Path().apply {
            moveTo(w * 0.50f, h * 0.40f) // shoulder
            lineTo(w * 0.72f, h * 0.39f) // forearm
            lineTo(w * 0.88f, h * 0.39f) // extended fist
        }
        drawPath(
            punchArmPath,
            color = color,
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Clenched Fist at end of punch
        drawCircle(
            color = color,
            radius = w * 0.08f,
            center = Offset(w * 0.88f, h * 0.39f)
        )

        // Retracted Chambering Arm (Hikite fist at ribcage)
        val chamberArmPath = Path().apply {
            moveTo(w * 0.26f, h * 0.40f)
            lineTo(w * 0.18f, h * 0.48f)
            lineTo(w * 0.26f, h * 0.54f)
        }
        drawPath(
            chamberArmPath,
            color = color,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Black belt knot / Obi hanging at waist
        val beltPath = Path().apply {
            moveTo(w * 0.28f, h * 0.62f)
            lineTo(w * 0.48f, h * 0.62f)
        }
        drawPath(
            beltPath,
            color = color,
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        )
        // Belt knot tails
        val beltTails = Path().apply {
            moveTo(w * 0.37f, h * 0.64f)
            lineTo(w * 0.33f, h * 0.76f)
            moveTo(w * 0.39f, h * 0.64f)
            lineTo(w * 0.42f, h * 0.78f)
        }
        drawPath(
            beltTails,
            color = color,
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )

        // Deep front stance legs (Zenkutsu-Dachi)
        // Front knee bent forward
        val frontLegPath = Path().apply {
            moveTo(w * 0.44f, h * 0.62f)
            lineTo(w * 0.60f, h * 0.76f)
            lineTo(w * 0.62f, h * 0.94f)
        }
        drawPath(
            frontLegPath,
            color = color,
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Back leg locked and straight driving power into punch
        val backLegPath = Path().apply {
            moveTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.78f)
            lineTo(w * 0.10f, h * 0.92f)
        }
        drawPath(
            backLegPath,
            color = color,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
