package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonTextPrimary
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.MonoFontFamily

@Composable
fun AutonstyleBrandMark(
    modifier: Modifier = Modifier,
    darkBackground: Boolean = false,
    subtitle: String = "BUSINESS MANAGER"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.autonstyle_icon_fg_1794699894066),
            contentDescription = "Autonstyle Logo",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, AutonLime, RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AUTON",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = if (darkBackground) AutonWhite else AutonNavy
                )
                Text(
                    text = "STYLE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = AutonRed
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .background(AutonLime, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (darkBackground) AutonWhite.copy(alpha = 0.78f) else AutonTextSecondary
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    iconContainerColor: Color,
    modifier: Modifier = Modifier,
    highlightDark: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val container = if (highlightDark) AutonNavy else AutonWhite
    val titleColor = if (highlightDark) AutonWhite.copy(alpha = 0.78f) else AutonTextSecondary
    val valueColor = if (highlightDark) AutonLime else AutonTextPrimary
    val subColor = if (highlightDark) AutonWhite.copy(alpha = 0.68f) else AutonTextSecondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, if (highlightDark) AutonNavy else AutonBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlightDark) 3.dp else 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AutonNavy
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AutonTextSecondary
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AutonNavy,
                    contentColor = AutonWhite
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = AutonTextSecondary)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AutonWhite,
            unfocusedContainerColor = AutonWhite,
            focusedBorderColor = AutonNavy,
            unfocusedBorderColor = AutonBorder
        )
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AutonCanvas),
        border = BorderStroke(1.dp, AutonBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AutonWhite)
                    .border(1.dp, AutonBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AutonNavy,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AutonNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AutonTextSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = AutonRed, contentColor = AutonWhite)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
