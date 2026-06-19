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

@Composable
fun MetroDefaultLauncherDialog(
    isDark: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "lanceur par défaut",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Voulez-vous définir ce launcher d'inspiration Windows Phone 8.1 comme votre écran d'accueil par défaut ?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color.White.copy(alpha = 0.82f) else Color.DarkGray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "définir" button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(accentColor)
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "définir",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // "plus tard" button
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
                            text = "plus tard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}
