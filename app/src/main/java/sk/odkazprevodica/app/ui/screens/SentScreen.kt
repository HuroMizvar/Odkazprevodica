package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.SentMessage
import sk.odkazprevodica.app.data.formatTime
import sk.odkazprevodica.app.ui.components.BottomBar
import sk.odkazprevodica.app.ui.components.MessageRow
import sk.odkazprevodica.app.ui.components.PlateChip
import sk.odkazprevodica.app.ui.components.Tab
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun SentScreen(sent: List<SentMessage>, onTabSelect: (Tab) -> Unit) {
    val today = stringResource(R.string.today)
    val yesterday = stringResource(R.string.yesterday)
    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = { BottomBar(selected = Tab.Sent, onSelect = onTabSelect) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.sent_title),
                    color = AppColors.Text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.sent_note),
                    color = AppColors.TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
            }
            if (sent.isEmpty()) {
                item { Text(stringResource(R.string.sent_empty), color = AppColors.Text, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp)) }
            }
            items(sent.sortedByDescending { it.timestamp }, key = { it.id }) { m ->
                MessageRow(
                    template = m.template,
                    meta = formatTime(m.timestamp, today, yesterday),
                    unread = false,
                    onClick = null,
                    leading = { PlateChip(m.plate) },
                )
            }
        }
    }
}
