package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun LauncherAppOpenOverlay(
    appName: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnim = true
    }

    val scale by animateFloatAsState(
        // Minimal scale so the turnstile rotation reads cleanly (was 0.82f, too much).
        targetValue = if (startAnim) 1f else 0.94f,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "overlay_scale"
    )

    val opacity by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.0f,
        animationSpec = tween(350),
        label = "overlay_opacity"
    )

    val rotationAngleY by animateFloatAsState(
        targetValue = if (startAnim) 0f else -78f, // WP turnstile: door-hinge swing
        animationSpec = tween(550, easing = LinearOutSlowInEasing),
        label = "overlay_rot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0.5f) // hinge on the LEFT edge (WP turnstile)
                rotationY = rotationAngleY
                alpha = opacity
                scaleX = scale
                scaleY = scale
                cameraDistance = 16f * density
            }
            .background(accentColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CHARGEMENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 3.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = appName.lowercase(Locale.getDefault()),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraLight,
                color = Color.White,
                lineHeight = 46.sp,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // flying dots progress loader
            FlyingDotsLoader(color = Color.White)
        }
    }
}

@Composable
fun FlyingDotsLoader(color: Color) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "dots")

        (0 until 5).forEach { index ->
            val positionFraction by infiniteTransition.animateFloat(
                initialValue = -0.1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 2200
                        delayMillis = index * 160
                        
                        // WP characteristic movement: start super fast, slow down/bunch in middle, then accelerate away
                        -0.1f at 0 with FastOutSlowInEasing
                        0.45f at 900 with LinearOutSlowInEasing
                        0.55f at 1300 with FastOutLinearInEasing
                        1.1f at 2200 with FastOutLinearInEasing
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_pos_$index"
            )

            Box(
                modifier = Modifier
                    .offset(x = (positionFraction * maxWidth.value).dp)
                    .size(5.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
