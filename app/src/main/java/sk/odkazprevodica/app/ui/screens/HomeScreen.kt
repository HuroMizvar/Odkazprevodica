package sk.odkazprevodica.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.AppData
import sk.odkazprevodica.app.data.Car
import sk.odkazprevodica.app.data.formatTime
import sk.odkazprevodica.app.data.MAX_CARS
import sk.odkazprevodica.app.ui.components.AppIcons
import sk.odkazprevodica.app.ui.components.BottomBar
import sk.odkazprevodica.app.ui.components.PlateChip
import sk.odkazprevodica.app.ui.components.SectionLabel
import sk.odkazprevodica.app.ui.components.Tab
import sk.odkazprevodica.app.ui.theme.AppColors

@Composable
fun HomeScreen(
    data: AppData,
    snackbarHostState: SnackbarHostState,
    onSendClick: () -> Unit,
    onCarClick: (Car) -> Unit,
    onAddCarClick: () -> Unit,
    onTabSelect: (Tab) -> Unit,
) {
    val cars = data.cars
    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomBar(selected = Tab.Home, onSelect = onTabSelect) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Hlavička
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.Amber),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Message, contentDescription = null, tint = AppColors.OnAmber, modifier = Modifier.size(20.dp))
                }
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                    color = AppColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Surface(
                    onClick = { onTabSelect(Tab.Settings) },
                    shape = CircleShape,
                    color = AppColors.Surface,
                    border = BorderStroke(1.dp, AppColors.Outline),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("#" + data.pseudonym.takeLast(4), color = AppColors.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Hlavná akcia
            Surface(
                onClick = onSendClick,
                shape = RoundedCornerShape(24.dp),
                color = AppColors.Amber,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x1F121417)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Camera, contentDescription = null, tint = AppColors.OnAmber, modifier = Modifier.size(28.dp))
                    }
                    Text(stringResource(R.string.home_send_title), color = AppColors.OnAmber, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.home_send_subtitle), color = AppColors.OnAmberSoft, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Moje autá
            Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp)) {
                SectionLabel(stringResource(R.string.home_my_cars), Modifier.weight(1f))
                Text(stringResource(R.string.home_cars_count, cars.size, MAX_CARS), color = AppColors.TextMuted, fontSize = 13.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cars.forEach { car ->
                    val last = data.inboxFor(car.plate).firstOrNull()?.let {
                        formatTime(it.timestamp, stringResource(R.string.today), stringResource(R.string.yesterday))
                    }
                    CarRow(car, data.unreadFor(car.plate), last, onClick = { onCarClick(car) })
                }
                OutlinedButton(
                    onClick = onAddCarClick,
                    enabled = cars.size < MAX_CARS,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, AppColors.OutlineDashed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextMuted),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.home_add_car), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CarRow(car: Car, unreadCount: Int, lastMessageLabel: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlateChip(car.plate)
            Column(Modifier.weight(1f)) {
                Text(car.alias, color = AppColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    lastMessageLabel?.let { stringResource(R.string.home_last_message, it) }
                        ?: stringResource(R.string.home_no_new),
                    color = AppColors.TextMuted,
                    fontSize = 13.sp,
                )
            }
            if (unreadCount > 0) {
                Box(
                    Modifier.defaultMinSize(26.dp, 26.dp).clip(CircleShape).background(AppColors.Amber).padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(unreadCount.toString(), color = AppColors.OnAmber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
