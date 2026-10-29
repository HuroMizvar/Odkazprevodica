package sk.odkazprevodica.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.AddCarResult
import sk.odkazprevodica.app.data.Repository
import sk.odkazprevodica.app.data.SendResult
import sk.odkazprevodica.app.ui.components.Tab
import sk.odkazprevodica.app.ui.screens.AddCarScreen
import sk.odkazprevodica.app.ui.screens.CarInboxScreen
import sk.odkazprevodica.app.ui.screens.HomeScreen
import sk.odkazprevodica.app.ui.screens.MessageDetailScreen
import sk.odkazprevodica.app.ui.screens.NewMessageScreen
import sk.odkazprevodica.app.ui.screens.ScannerScreen
import sk.odkazprevodica.app.ui.screens.SentScreen
import sk.odkazprevodica.app.ui.screens.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SENT = "sent"
    const val SETTINGS = "settings"
    const val NEW = "new"
    const val SCAN = "scan"
    const val ADD_CAR = "add_car"
    const val CAR = "car/{plate}"
    const val MESSAGE = "message/{id}"
    fun car(plate: String) = "car/$plate"
    fun message(id: String) = "message/$id"
}

private const val KEY_SCANNED = "scanned_plate"

private fun NavHostController.goToTab(tab: Tab) {
    val route = when (tab) {
        Tab.Home -> Routes.HOME
        Tab.Sent -> Routes.SENT
        Tab.Settings -> Routes.SETTINGS
    }
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val data by Repository.state.collectAsState()

    val sentText = stringResource(R.string.new_sent_confirmation)
    val addedText = stringResource(R.string.add_done)
    val removedText = stringResource(R.string.car_removed)
    fun toast(text: String) = scope.launch { snackbar.showSnackbar(text) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                data = data,
                snackbarHostState = snackbar,
                onSendClick = { nav.navigate(Routes.NEW) },
                onCarClick = { car -> nav.navigate(Routes.car(car.plate)) },
                onAddCarClick = { nav.navigate(Routes.ADD_CAR) },
                onTabSelect = { nav.goToTab(it) },
            )
        }
        composable(Routes.SENT) {
            SentScreen(sent = data.sent, onTabSelect = { nav.goToTab(it) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                data = data,
                snackbarHostState = snackbar,
                onTabSelect = { nav.goToTab(it) },
                onUnblock = { Repository.unblock(it) },
                onWipe = { Repository.wipeAll() },
            )
        }
        composable(Routes.NEW) { entry ->
            val scanned by entry.savedStateHandle.getStateFlow<String?>(KEY_SCANNED, null).collectAsState()
            NewMessageScreen(
                snackbarHostState = snackbar,
                scannedPlate = scanned,
                onScannedConsumed = { entry.savedStateHandle.set<String?>(KEY_SCANNED, null) },
                onBack = { nav.popBackStack() },
                onScan = { nav.navigate(Routes.SCAN) },
                onSend = { plate, template ->
                    val result = Repository.sendMessage(plate, template)
                    if (result == SendResult.SENT) {
                        nav.popBackStack()
                        // Rovnaké potvrdenie vždy – odosielateľ nevie, či ŠPZ má konto.
                        toast(sentText)
                    }
                    result
                },
            )
        }
        composable(Routes.SCAN) {
            ScannerScreen(
                onBack = { nav.popBackStack() },
                onPlate = { plate ->
                    nav.previousBackStackEntry?.savedStateHandle?.set(KEY_SCANNED, plate)
                    nav.popBackStack()
                },
            )
        }
        composable(Routes.ADD_CAR) {
            AddCarScreen(
                snackbarHostState = snackbar,
                onBack = { nav.popBackStack() },
                onAdd = { plate, alias ->
                    val result = Repository.addCar(plate, alias)
                    if (result == AddCarResult.ADDED) {
                        nav.popBackStack()
                        toast(addedText)
                    }
                    result
                },
            )
        }
        composable(Routes.CAR) { entry ->
            val plate = entry.arguments?.getString("plate").orEmpty()
            val car = data.car(plate)
            if (car == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                CarInboxScreen(
                    car = car,
                    messages = data.inboxFor(plate),
                    snackbarHostState = snackbar,
                    onBack = { nav.popBackStack() },
                    onMessageClick = { nav.navigate(Routes.message(it.id)) },
                    onRename = { Repository.renameCar(plate, it) },
                    onRemove = {
                        Repository.removeCar(plate)
                        toast(removedText)
                    },
                )
            }
        }
        composable(Routes.MESSAGE) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val message = data.visibleInbox.firstOrNull { it.id == id }
            if (message == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                LaunchedEffect(id) { Repository.markSeen(id) }
                MessageDetailScreen(
                    message = message,
                    carAlias = data.car(message.plate)?.alias.orEmpty(),
                    snackbarHostState = snackbar,
                    onBack = { nav.popBackStack() },
                    onReply = { Repository.reply(id, it) },
                    onMarkRead = { Repository.confirmRead(id) },
                    onBlock = { Repository.blockSenderOf(id) },
                    onReport = { Repository.report(id) },
                )
            }
        }
    }
}
