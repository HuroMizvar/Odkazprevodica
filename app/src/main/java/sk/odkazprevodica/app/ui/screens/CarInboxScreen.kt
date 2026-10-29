package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.Car
import sk.odkazprevodica.app.data.ReceivedMessage
import sk.odkazprevodica.app.data.displayPlate
import sk.odkazprevodica.app.data.formatTime
import sk.odkazprevodica.app.ui.components.AppTextField
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.ConfirmDialog
import sk.odkazprevodica.app.ui.components.MessageRow
import sk.odkazprevodica.app.ui.theme.AppColors

/** Schránka jedného auta: prijaté odkazy + premenovanie / odobratie. */
@Composable
fun CarInboxScreen(
    car: Car,
    messages: List<ReceivedMessage>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onMessageClick: (ReceivedMessage) -> Unit,
    onRename: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val today = stringResource(R.string.today)
    val yesterday = stringResource(R.string.yesterday)

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackHeader(displayPlate(car.plate), subtitle = car.alias, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallOutlined(stringResource(R.string.car_rename), Modifier.weight(1f)) { renaming = true }
                    SmallOutlined(stringResource(R.string.car_remove), Modifier.weight(1f)) { removing = true }
                }
            }
            if (messages.isEmpty()) {
                item {
                    Text(stringResource(R.string.car_inbox_empty), color = AppColors.Text, fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
                    Text(stringResource(R.string.car_test_hint), color = AppColors.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            items(messages, key = { it.id }) { m ->
                MessageRow(
                    template = m.template,
                    meta = formatTime(m.timestamp, today, yesterday) + " · " + m.senderPseudonym,
                    unread = !m.seen,
                    onClick = { onMessageClick(m) },
                )
            }
            item { Text("", modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(car.alias) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            containerColor = AppColors.Surface,
            titleContentColor = AppColors.Text,
            title = { Text(stringResource(R.string.car_rename), fontWeight = FontWeight.ExtraBold) },
            text = { AppTextField(name, { if (it.length <= 30) name = it }, stringResource(R.string.add_alias_hint)) },
            confirmButton = {
                TextButton(onClick = { onRename(name); renaming = false }) {
                    Text(stringResource(R.string.save), color = AppColors.Amber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel), color = AppColors.Text) }
            },
        )
    }
    if (removing) {
        ConfirmDialog(
            title = stringResource(R.string.car_remove_title),
            text = stringResource(R.string.car_remove_text, displayPlate(car.plate)),
            confirmLabel = stringResource(R.string.car_remove_confirm),
            onConfirm = { removing = false; onRemove() },
            onDismiss = { removing = false },
        )
    }
}

@Composable
private fun SmallOutlined(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, AppColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Text),
        modifier = modifier,
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}
