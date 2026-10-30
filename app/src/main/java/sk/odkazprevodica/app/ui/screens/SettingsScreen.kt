package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.BuildConfig
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.AppData
import sk.odkazprevodica.app.ui.components.AppIcons
import sk.odkazprevodica.app.ui.components.BottomBar
import sk.odkazprevodica.app.ui.components.ConfirmDialog
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.components.Tab
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun SettingsScreen(
    data: AppData,
    snackbarHostState: SnackbarHostState,
    onTabSelect: (Tab) -> Unit,
    onUnblock: (String) -> Unit,
    onWipe: () -> Unit,
) {
    var wiping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val wipedText = stringResource(R.string.settings_wiped)

    Scaffold(
        // Okraje systému rieši MainActivity (safeDrawing) – tu nič navyše.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomBar(selected = Tab.Settings, onSelect = onTabSelect) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(
                stringResource(R.string.settings_title),
                color = AppColors.Text,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            // Identita
            SectionLabel(stringResource(R.string.settings_identity), Modifier.padding(top = 16.dp, bottom = 8.dp))
            Card {
                Text(data.pseudonym, color = AppColors.Amber, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(if (data.online) R.string.settings_identity_note_anon else R.string.settings_identity_note), color = AppColors.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }

            // Blokovaní
            SectionLabel(stringResource(R.string.settings_blocked), Modifier.padding(top = 24.dp, bottom = 8.dp))
            Card {
                if (data.blocked.isEmpty()) {
                    Text(stringResource(R.string.settings_blocked_empty), color = AppColors.TextMuted, fontSize = 14.sp)
                }
                data.blocked.forEach { (key, name) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(name.ifEmpty { key }, color = AppColors.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onUnblock(key) }) {
                            Text(stringResource(R.string.settings_unblock), color = AppColors.Amber, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Súkromie
            SectionLabel(stringResource(R.string.settings_privacy), Modifier.padding(top = 24.dp, bottom = 8.dp))
            Card {
                listOf(
                    R.string.settings_privacy_1, R.string.settings_privacy_2,
                    R.string.settings_privacy_3, R.string.settings_privacy_4,
                ).forEach { res ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(AppIcons.Check, contentDescription = null, tint = AppColors.Amber, modifier = Modifier.size(18.dp))
                        Text(stringResource(res), color = AppColors.Text, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = { wiping = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AppColors.Urgent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Urgent),
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp),
            ) { Text(stringResource(R.string.settings_wipe), fontWeight = FontWeight.Bold) }

            Text(
                stringResource(
                    R.string.settings_version,
                    BuildConfig.VERSION_NAME,
                    stringResource(if (data.online) R.string.mode_online else R.string.mode_local),
                ),
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
        }
    }

    if (wiping) {
        ConfirmDialog(
            title = stringResource(R.string.settings_wipe_title),
            text = stringResource(R.string.settings_wipe_text),
            confirmLabel = stringResource(R.string.settings_wipe_confirm),
            onConfirm = {
                wiping = false
                onWipe()
                scope.launch { snackbarHostState.showSnackbar(wipedText) }
            },
            onDismiss = { wiping = false },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
