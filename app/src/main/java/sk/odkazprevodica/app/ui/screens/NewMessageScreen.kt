package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.MessageTemplate
import sk.odkazprevodica.app.data.SendResult
import sk.odkazprevodica.app.data.displayPlate
import sk.odkazprevodica.app.data.isValidSkPlate
import sk.odkazprevodica.app.data.normalizePlate
import sk.odkazprevodica.app.ui.components.AppIcons
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.PlateTextField
import sk.odkazprevodica.app.ui.components.PrimaryButton
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.components.strokeIcon
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun NewMessageScreen(
    snackbarHostState: SnackbarHostState,
    /** ŠPZ vrátená zo skenera (alebo null). */
    scannedPlate: String?,
    onScannedConsumed: () -> Unit,
    onBack: () -> Unit,
    onScan: () -> Unit,
    /** Vráti výsledok; pri SENT obrazovka zavrie sama seba volajúci. */
    onSend: (plate: String, template: MessageTemplate) -> SendResult,
) {
    var plateInput by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { MessageTemplate.fromId(it) }
    val plate = normalizePlate(plateInput)
    val plateOk = isValidSkPlate(plate)
    val canSend = plateOk && selected != null
    val scope = rememberCoroutineScope()

    val limitPlate = stringResource(R.string.new_limit_plate)
    val limitDay = stringResource(R.string.new_limit_day)

    LaunchedEffect(scannedPlate) {
        if (scannedPlate != null) {
            plateInput = displayPlate(scannedPlate)
            onScannedConsumed()
        }
    }

    Scaffold(
        // Okraje systému rieši MainActivity (safeDrawing) – tu nič navyše.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackHeader(stringResource(R.string.new_title), onBack = onBack) },
        bottomBar = {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.new_send),
                    enabled = canSend,
                    onClick = {
                        val t = selected ?: return@PrimaryButton
                        when (onSend(plate, t)) {
                            SendResult.SENT -> Unit
                            SendResult.LIMIT_PLATE -> scope.launch { snackbarHostState.showSnackbar(limitPlate) }
                            SendResult.LIMIT_DAY -> scope.launch { snackbarHostState.showSnackbar(limitDay) }
                        }
                    },
                )
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
            PlateTextField(plateInput, { plateInput = it }, stringResource(R.string.new_plate_hint))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (plate.length == 7 && !plateOk) {
                    Text(stringResource(R.string.new_invalid_plate), color = AppColors.Urgent, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onScan) {
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
            // Celý text odkazu – presne to, čo uvidí majiteľ.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageTemplate.entries.forEach { t ->
                    TemplateTile(t, isSelected = t == selected, onClick = { selectedId = t.id })
                }
            }
            if (selected == MessageTemplate.CHILD_OR_ANIMAL) {
                Text(
                    stringResource(R.string.new_emergency_note),
                    color = AppColors.Urgent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
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
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(strokeIcon(t.id, t.iconPath), contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Text(
                stringResource(t.fullText),
                color = AppColors.Text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
