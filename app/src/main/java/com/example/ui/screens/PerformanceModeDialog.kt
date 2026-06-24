package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Offered once when the launcher detects modest hardware. It clearly explains what visual
 * animations are simplified before the user opts in, then lets them enable performance mode or
 * keep the full 3D Metro animations.
 */
@Composable
fun PerformanceModeDialog(
    isDark: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onEnable: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF1F1F1F) else Color.White)
                .border(
                    width = 2.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "mode performance",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Votre appareil semble modeste. Le mode performance rend le launcher plus fluide en simplifiant les animations des tuiles :",
                    fontSize = 14.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.82f) else Color.DarkGray,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                BulletLine(
                    text = "L'inclinaison 3D des tuiles à l'appui devient un simple zoom.",
                    accentColor = accentColor,
                    isDark = isDark
                )
                BulletLine(
                    text = "Le basculement 3D des tuiles dynamiques devient un fondu.",
                    accentColor = accentColor,
                    isDark = isDark
                )

                Text(
                    text = "Vous pourrez le désactiver à tout moment dans Paramètres › performance.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(accentColor)
                            .clickable { onEnable() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "activer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.5.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                            )
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "garder les animations",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String, accentColor: Color, isDark: Boolean) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "•",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.DarkGray,
            lineHeight = 18.sp
        )
    }
}
