package com.breadler.boxingperformancetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.breadler.boxingperformancetracker.ui.components.MainActionCard
import com.breadler.boxingperformancetracker.ui.components.StrykoLogo
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.theme.StrykoBackground
import com.breadler.boxingperformancetracker.ui.theme.StrykoBlue
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed
import com.breadler.boxingperformancetracker.ui.theme.StrykoSystemBars

@Composable
fun HomeScreen(
    onStartNewSession: () -> Unit,
    onViewPreviousSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StrykoSystemBars(statusBarColor = StrykoRed, navigationBarColor = StrykoRed)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = StrykoBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StrykoLogo()

            Spacer(modifier = Modifier.height(44.dp))

            MainActionCard(
                title = "Start a New Session",
                backgroundColor = StrykoRed,
                iconTint = StrykoRed,
                iconResId = R.drawable.ic_new_session,
                onClick = onStartNewSession,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(40.dp))

            MainActionCard(
                title = "View Previous Session",
                backgroundColor = StrykoBlue,
                iconTint = StrykoBlue,
                iconResId = R.drawable.ic_previous_session,
                onClick = onViewPreviousSessions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
