package com.breadler.boxingperformancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed

@Composable
fun HomeScreen(
    onViewPreviousSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "StryKO",
                style = MaterialTheme.typography.headlineLarge,
                color = StrykoRed,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(56.dp))

            HomeActionButton(
                text = "Start a New Session",
                containerColor = StrykoRed,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = StrykoRed,
                        modifier = Modifier.size(28.dp),
                    )
                },
                enabled = false,
                onClick = {},
            )

            Spacer(modifier = Modifier.height(24.dp))

            HomeActionButton(
                text = "View Previous Sessions",
                containerColor = StrykoBlue,
                icon = {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = StrykoBlue,
                        modifier = Modifier.size(28.dp),
                    )
                },
                enabled = true,
                onClick = onViewPreviousSessions,
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = text,
                color = StrykoCard,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = StrykoCard,
                modifier = Modifier.size(48.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    icon()
                }
            }
        }
    }
}
