package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed

@OptIn(ExperimentalTextApi::class)
private val PlayfairDisplayBlack = FontFamily(
    Font(
        resId = R.font.playfair_display,
        weight = FontWeight.Black,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(900)),
    ),
)

@Composable
fun StrykoLogo(
    modifier: Modifier = Modifier,
) {
    Text(
        text = "StryKO",
        color = StrykoRed,
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        fontFamily = PlayfairDisplayBlack,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
