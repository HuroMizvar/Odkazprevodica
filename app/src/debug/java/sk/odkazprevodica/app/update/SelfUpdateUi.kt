package sk.odkazprevodica.app.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.theme.AppColors

/**
 * Aktualizácia z GitHub Releases – IBA testovacia verzia (APK z GitHubu).
 * Verzia pre Google Play má v src/release prázdnu náhradu bez tejto funkcie.
 */
object SelfUpdate {
    suspend fun checkOnStart() = Updater.check()

    @Composable
    fun isAvailable(): Boolean {
        val st by Updater.state.collectAsState()
        return st is Updater.State.Available || st is Updater.State.Ready
    }

    @Composable
    fun SettingsSection() {
        SectionLabel(stringResource(R.string.update_title), Modifier.padding(top = 16.dp, bottom = 8.dp))
        UpdateCard()
    }
}

@Composable
private fun UpdateSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
internal fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by Updater.state.collectAsState()
    // Po návrate z nastavení „Inštalovať neznáme aplikácie“ treba stav prečítať znova.
    var permissionTick by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }

    UpdateSurface {
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

