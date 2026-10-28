package sk.odkazprevodica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.displayPlate
import sk.odkazprevodica.app.ui.theme.AppColors

/** Malá ŠPZ tabuľka s modrým EÚ pruhom. */
@Composable
fun PlateChip(plate: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.PlateWhite),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(AppColors.PlateBlue))
        Text(
            text = displayPlate(plate),
            modifier = Modifier.padding(horizontal = 9.dp),
            color = AppColors.OnAmber,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
    }
}

/** Veľká ŠPZ tabuľka (obrazovka Nový odkaz). */
@Composable
fun PlateLarge(text: String, modifier: Modifier = Modifier, height: Dp = 60.dp, fontSize: TextUnit = 32.sp) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.PlateWhite),
    ) {
        Box(
            Modifier.width(30.dp).fillMaxHeight().background(AppColors.PlateBlue).padding(bottom = 7.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text("SK", color = AppColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = AppColors.OnAmber,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = AppColors.TextMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
fun BackHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(AppIcons.Back, contentDescription = stringResource(R.string.back), tint = AppColors.Text)
        }
        Column {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = AppColors.Text)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = AppColors.TextMuted)
        }
    }
}

enum class Tab { Home, Sent, Settings }

@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = AppColors.Background, tonalElevation = 0.dp) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AppColors.Amber,
            selectedTextColor = AppColors.Amber,
            unselectedIconColor = AppColors.TextMuted,
            unselectedTextColor = AppColors.TextMuted,
            indicatorColor = AppColors.AmberSelected,
        )
        listOf(
            Triple(Tab.Home, AppIcons.Home, R.string.nav_home),
            Triple(Tab.Sent, AppIcons.Send, R.string.nav_sent),
            Triple(Tab.Settings, AppIcons.Settings, R.string.nav_settings),
        ).forEach { (tab, icon, label) ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(stringResource(label), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = colors,
            )
        }
    }
}
