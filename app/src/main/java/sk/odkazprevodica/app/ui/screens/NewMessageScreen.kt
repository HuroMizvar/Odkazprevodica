package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.MessageTemplate
import sk.odkazprevodica.app.data.isValidSkPlate
import sk.odkazprevodica.app.data.normalizePlate
import sk.odkazprevodica.app.ui.components.AppIcons
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.components.strokeIcon
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun NewMessageScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSend: (plate: String, template: MessageTemplate) -> Unit,
) {
    var plateInput by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<MessageTemplate?>(null) }
    val plate = normalizePlate(plateInput)
    val canSend = isValidSkPlate(plate) && selected != null

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackHeader(stringResource(R.string.new_title), onBack = onBack) },
        bottomBar = {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
                Button(
                    onClick = { selected?.let { onSend(plate, it) } },
                    enabled = canSend,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Amber,
                        contentColor = AppColors.OnAmber,
                        disabledContainerColor = AppColors.Surface,
                        disabledContentColor = AppColors.TextMuted,
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(stringResource(R.string.new_send), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    stringResource(R.string.new_privacy_note),
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            SectionLabel(stringResource(R.string.new_plate_label), Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = plateInput,
                onValueChange = { if (normalizePlate(it).length <= 7) plateInput = it.uppercase() },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.new_plate_hint), color = AppColors.TextMuted) },
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
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                // TODO: CameraX + ML Kit Text Recognition – rozpoznanie ŠPZ z fotky.
                TextButton(onClick = { }) {
                    Icon(AppIcons.Camera, contentDescription = null, tint = AppColors.Amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.new_scan), color = AppColors.Amber, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                stringResource(R.string.new_what_happens),
                color = AppColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )
            MessageTemplate.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { t ->
                        TemplateTile(t, isSelected = t == selected, onClick = { selected = t }, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (selected == MessageTemplate.CHILD_OR_ANIMAL) {
                Text(
                    stringResource(R.string.new_emergency_note),
                    color = AppColors.Urgent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TemplateTile(t: MessageTemplate, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconTint = when {
        isSelected -> AppColors.Amber
        t.urgent -> AppColors.Urgent
        else -> AppColors.IconIdle
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) AppColors.AmberSelected else AppColors.Surface,
        border = BorderStroke(1.5.dp, if (isSelected) AppColors.Amber else AppColors.Outline),
        modifier = modifier.height(54.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(strokeIcon(t.id, t.iconPath), contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(stringResource(t.shortLabel), color = AppColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
        }
    }
}
