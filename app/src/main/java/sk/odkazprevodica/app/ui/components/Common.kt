package sk.odkazprevodica.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
    NavigationBar(containerColor = AppColors.Background, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
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

/** Riadok odkazu v zozname (schránka auta, odoslané). */
@Composable
fun MessageRow(
    template: sk.odkazprevodica.app.data.MessageTemplate,
    meta: String,
    unread: Boolean,
    onClick: (() -> Unit)?,
    leading: (@Composable () -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (template.urgent) AppColors.Urgent else AppColors.AmberSelected),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    strokeIcon(template.id, template.iconPath),
                    contentDescription = null,
                    tint = if (template.urgent) AppColors.OnAmber else AppColors.Amber,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                leading?.invoke()
                Text(stringResource(template.fullText), color = AppColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(meta, color = AppColors.TextMuted, fontSize = 13.sp)
            }
            if (unread) Box(Modifier.size(10.dp).clip(CircleShape).background(AppColors.Amber))
        }
    }
    val shape = RoundedCornerShape(18.dp)
    val border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Outline)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = AppColors.Surface, border = border, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        Surface(shape = shape, color = AppColors.Surface, border = border, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        titleContentColor = AppColors.Text,
        textContentColor = AppColors.TextSoft,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = AppColors.Urgent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = AppColors.Text) }
        },
    )
}

/** Textové pole v štýle aplikácie. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = AppColors.TextMuted) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Surface,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            focusedBorderColor = AppColors.Amber,
            unfocusedBorderColor = AppColors.Outline,
            cursorColor = AppColors.Amber,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Pole na zadanie ŠPZ – biela tabuľka, veľké písmená, max. 7 znakov. */
@Composable
fun PlateTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (sk.odkazprevodica.app.data.normalizePlate(it).length <= 7) onValueChange(it.uppercase()) },
        singleLine = true,
        placeholder = { Text(placeholder, color = AppColors.TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppColors.PlateWhite,
            unfocusedContainerColor = AppColors.PlateWhite,
            focusedTextColor = AppColors.OnAmber,
            unfocusedTextColor = AppColors.OnAmber,
            focusedBorderColor = AppColors.Amber,
            unfocusedBorderColor = AppColors.PlateWhite,
            cursorColor = AppColors.OnAmber,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Amber,
            contentColor = AppColors.OnAmber,
            disabledContainerColor = AppColors.Surface,
            disabledContentColor = AppColors.TextMuted,
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
