package com.breadler.boxingperformancetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breadler.boxingperformancetracker.ui.theme.StrykoCard

// Small pill button with an icon on either side of the label
@Composable
fun SmallRoundButton(
    text: String,
    background: Color,
    iconResId: Int,
    onClick: () -> Unit,
    iconAtEnd: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val icon = @Composable {
                Icon(painter = painterResource(iconResId), contentDescription = null, tint = StrykoCard, modifier = Modifier.size(18.dp))
            }
            if (!iconAtEnd) icon()
            Text(text = text, color = StrykoCard, fontWeight = FontWeight.ExtraBold)
            if (iconAtEnd) icon()
        }
    }
}
