package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.AddCarResult
import sk.odkazprevodica.app.data.isValidSkPlate
import sk.odkazprevodica.app.data.normalizePlate
import sk.odkazprevodica.app.ui.components.AppTextField
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.PlateTextField
import sk.odkazprevodica.app.ui.components.PrimaryButton
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun AddCarScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    /** Pri ADDED zavrie obrazovku volajúci. */
    onAdd: (plate: String, alias: String) -> AddCarResult,
) {
    var plateInput by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    val plateOk = isValidSkPlate(normalizePlate(plateInput))
    val scope = rememberCoroutineScope()

    val errDuplicate = stringResource(R.string.add_err_duplicate)
    val errLimit = stringResource(R.string.add_err_limit)
    val errInvalid = stringResource(R.string.new_invalid_plate)

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackHeader(stringResource(R.string.add_title), onBack = onBack) },
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.add_submit),
                enabled = plateOk && consent,
                onClick = {
                    val msg = when (onAdd(plateInput, alias)) {
                        AddCarResult.ADDED -> null
                        AddCarResult.DUPLICATE -> errDuplicate
                        AddCarResult.LIMIT -> errLimit
                        AddCarResult.INVALID -> errInvalid
                    }
                    if (msg != null) scope.launch { snackbarHostState.showSnackbar(msg) }
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            SectionLabel(stringResource(R.string.add_plate), Modifier.padding(bottom = 8.dp))
            PlateTextField(plateInput, { plateInput = it }, stringResource(R.string.new_plate_hint))

            SectionLabel(stringResource(R.string.add_alias), Modifier.padding(top = 24.dp, bottom = 8.dp))
            AppTextField(alias, { if (it.length <= 30) alias = it }, stringResource(R.string.add_alias_hint))

            Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { consent = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Amber,
                        checkmarkColor = AppColors.OnAmber,
                        uncheckedColor = AppColors.TextMuted,
                    ),
                )
                Text(stringResource(R.string.add_consent), color = AppColors.Text, fontSize = 15.sp)
            }
            Text(
                stringResource(R.string.add_info),
                color = AppColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
