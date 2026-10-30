package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.ReceivedMessage
import sk.odkazprevodica.app.data.ReplyTemplate
import sk.odkazprevodica.app.data.displayPlate
import sk.odkazprevodica.app.data.formatTime
import sk.odkazprevodica.app.ui.components.AppIcons
import sk.odkazprevodica.app.ui.components.BackHeader
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.components.strokeIcon
import sk.odkazprevodica.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    message: ReceivedMessage,
    carAlias: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onReply: (ReplyTemplate) -> Unit,
    onMarkRead: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pendingReply by remember { mutableStateOf<ReplyTemplate?>(null) }
    val replied = message.reply != null

    val replySentText = stringResource(R.string.detail_reply_sent)
    val markedText = stringResource(R.string.detail_marked_read)
    val blockedText = stringResource(R.string.detail_blocked)
    val reportedText = stringResource(R.string.detail_reported)

    fun sendReply(reply: ReplyTemplate) {
        onReply(reply)
        scope.launch { snackbarHostState.showSnackbar(replySentText) }
    }

    Scaffold(
        // Okraje systému rieši MainActivity (safeDrawing) – tu nič navyše.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BackHeader(
                title = stringResource(R.string.detail_title),
                subtitle = "${displayPlate(message.plate)} · $carAlias",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Karta odkazu
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.Surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
                        .background(if (message.template.urgent) AppColors.Urgent else AppColors.Amber),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        strokeIcon(message.template.id, message.template.iconPath),
                        contentDescription = null,
                        tint = AppColors.OnAmber,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Text(stringResource(message.template.fullText), color = AppColors.Text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                Text(
                    stringResource(
                        R.string.detail_meta,
                        formatTime(message.timestamp, stringResource(R.string.today), stringResource(R.string.yesterday)),
                        message.senderPseudonym,
                    ),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp,
                )
            }

            // Odpovede
            val sentReply = message.reply
            if (sentReply != null) {
                Text(
                    stringResource(R.string.detail_your_reply, stringResource(sentReply.label)),
                    color = AppColors.Amber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
            if (!replied) {
                SectionLabel(stringResource(R.string.detail_reply_header), Modifier.padding(top = 26.dp, bottom = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReplyTemplate.entries.forEach { reply ->
                        OutlinedButton(
                            onClick = {
                                if (reply.needsSafetyWarning) pendingReply = reply else sendReply(reply)
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, AppColors.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Text),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Text(stringResource(reply.label), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            // Neutrálne, kým majiteľ neoznačí; po označení sa zvýrazní.
            val read = message.readConfirmed
            OutlinedButton(
                onClick = {
                    if (!read) {
                        onMarkRead()
                        scope.launch { snackbarHostState.showSnackbar(markedText) }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, if (read) AppColors.Amber else AppColors.Outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (read) AppColors.Amber else Color.Transparent,
                    contentColor = if (read) AppColors.OnAmber else AppColors.Text,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(if (message.readConfirmed) R.string.detail_marked_read else R.string.detail_mark_read),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { onBlock(); scope.launch { snackbarHostState.showSnackbar(blockedText) } }) {
                    Text(stringResource(R.string.detail_block), color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.size(16.dp))
                TextButton(
                    onClick = { onReport(); scope.launch { snackbarHostState.showSnackbar(reportedText) } },
                    enabled = !message.reported,
                ) {
                    Text(stringResource(if (message.reported) R.string.detail_reported else R.string.detail_report), color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                stringResource(R.string.detail_retention),
                color = AppColors.TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            )
        }
    }

    // Bezpečnostná výstraha pred odpoveďou „Ďakujem, skontrolujem to“
    pendingReply?.let { reply ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { pendingReply = null },
            sheetState = sheetState,
            containerColor = AppColors.Surface,
        ) {
            SafetySheetContent(
                onConfirm = {
                    pendingReply = null
                    sendReply(reply)
                },
                onCancel = { pendingReply = null },
            )
        }
    }
}

@Composable
private fun SafetySheetContent(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Amber),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Shield, contentDescription = null, tint = AppColors.OnAmber, modifier = Modifier.size(28.dp))
        }
        Text(stringResource(R.string.safety_title), color = AppColors.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(R.string.safety_body), color = AppColors.TextSoft, fontSize = 15.sp, lineHeight = 22.sp)
        listOf(R.string.safety_tip_busy, R.string.safety_tip_alone, R.string.safety_tip_police).forEach { tip ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(AppIcons.Check, contentDescription = null, tint = AppColors.Amber, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(stringResource(tip), color = AppColors.Text, fontSize = 14.sp, lineHeight = 19.sp)
            }
        }
        Button(
            onClick = onConfirm,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Amber, contentColor = AppColors.OnAmber),
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 4.dp),
        ) {
            Text(stringResource(R.string.safety_confirm), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, AppColors.Outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Text),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
