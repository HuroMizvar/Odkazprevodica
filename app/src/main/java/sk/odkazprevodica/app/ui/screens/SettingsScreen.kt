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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import sk.odkazprevodica.app.update.Updater
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

            // Aktualizácia
            SectionLabel(stringResource(R.string.update_title), Modifier.padding(top = 16.dp, bottom = 8.dp))
            UpdateCard()

            // Identita
            SectionLabel(stringResource(R.string.settings_identity), Modifier.padding(top = 24.dp, bottom = 8.dp))
            Card {
                Text(data.pseudonym.ifEmpty { "—" }, color = AppColors.Amber, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                if (data.online && data.serverStatus.isNotEmpty()) {
                    Text("Server: " + data.serverStatus, color = AppColors.TextSoft, fontSize = 13.sp, lineHeight = 18.sp)
                }
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
private fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by Updater.state.collectAsState()
    // Po návrate z nastavení „Inštalovať neznáme aplikácie“ treba stav prečítať znova.
    var permissionTick by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }

    Card {
        when (val st = state) {
            Updater.State.Idle, Updater.State.UpToDate -> {
                Text(
                    stringResource(if (st == Updater.State.UpToDate) R.string.update_up_to_date else R.string.update_current, Updater.currentBuild),
                    color = AppColors.Text, fontSize = 15.sp,
                )
                UpdateButton(stringResource(R.string.update_check), filled = false) { scope.launch { Updater.check() } }
            }
            Updater.State.Checking -> {
                Text(stringResource(R.string.update_checking), color = AppColors.TextMuted, fontSize = 15.sp)
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Amber, trackColor = AppColors.Outline)
            }
            is Updater.State.Available -> {
                Text(
                    stringResource(R.string.update_available, st.release.build, Updater.currentBuild),
                    color = AppColors.Amber, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                )
                UpdateButton(stringResource(R.string.update_download), filled = true) {
                    scope.launch { Updater.download(context, st.release) }
                }
            }
            is Updater.State.Downloading -> {
                Text(stringResource(R.string.update_downloading, (st.progress * 100).toInt()), color = AppColors.Text, fontSize = 15.sp)
                LinearProgressIndicator(
                    progress = { st.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Amber,
                    trackColor = AppColors.Outline,
                )
            }
            is Updater.State.Ready -> {
                val allowed = remember(permissionTick) { Updater.canInstall(context) }
                if (allowed) {
                    Text(stringResource(R.string.update_ready), color = AppColors.Text, fontSize = 15.sp)
                    UpdateButton(stringResource(R.string.update_install), filled = true) { Updater.install(context, st.file) }
                } else {
                    Text(stringResource(R.string.update_permission), color = AppColors.Text, fontSize = 14.sp, lineHeight = 19.sp)
                    UpdateButton(stringResource(R.string.update_allow), filled = true) { Updater.openInstallPermissionSettings(context) }
                }
            }
            is Updater.State.Error -> {
                Text(st.message, color = AppColors.Urgent, fontSize = 14.sp)
                UpdateButton(stringResource(R.string.update_check), filled = false) { scope.launch { Updater.check() } }
            }
        }
    }
}

@Composable
private fun UpdateButton(text: String, filled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (filled) AppColors.Amber else AppColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (filled) AppColors.Amber else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (filled) AppColors.OnAmber else AppColors.Text,
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
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
