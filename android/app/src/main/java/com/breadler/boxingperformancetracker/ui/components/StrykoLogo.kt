package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.breadler.boxingperformancetracker.R
import com.breadler.boxingperformancetracker.ui.theme.StrykoRed

// Bold display font used for the "StryKO" wordmark
@OptIn(ExperimentalTextApi::class)
private val PlayfairDisplayBlack = FontFamily(
    Font(
        resId = R.font.playfair_display,
        weight = FontWeight.Black,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(900)),
    ),
)

// App logo mark + wordmark
@Composable
fun StrykoLogo(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_stryko),
            contentDescription = null,
            modifier = Modifier.width(54.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "StryKO",
            color = StrykoRed,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            fontFamily = PlayfairDisplayBlack,
            textAlign = TextAlign.Center,
        )
    }
}
