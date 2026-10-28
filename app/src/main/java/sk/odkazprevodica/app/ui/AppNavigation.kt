package sk.odkazprevodica.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import sk.odkazprevodica.app.R
import sk.odkazprevodica.app.data.FakeRepository
import sk.odkazprevodica.app.ui.screens.HomeScreen
import sk.odkazprevodica.app.ui.screens.MessageDetailScreen
import sk.odkazprevodica.app.ui.screens.NewMessageScreen

private object Routes {
    const val HOME = "home"
    const val NEW = "new"
    const val MESSAGE = "message/{id}"
    fun message(id: String) = "message/$id"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sentText = stringResource(R.string.new_sent_confirmation)
    val repo = FakeRepository

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                pseudonym = repo.myPseudonym,
                cars = repo.cars,
                snackbarHostState = snackbar,
                onSendClick = { nav.navigate(Routes.NEW) },
                onCarClick = { car -> repo.latestMessageFor(car.plate)?.let { nav.navigate(Routes.message(it.id)) } },
                onAddCarClick = { /* TODO: pridanie auta + pozvánka pre ďalšieho sledovateľa */ },
                onTabSelect = { /* TODO: Odoslané, Nastavenia */ },
            )
        }
        composable(Routes.NEW) {
            NewMessageScreen(
                snackbarHostState = snackbar,
                onBack = { nav.popBackStack() },
                onSend = { plate, template ->
                    repo.sendMessage(plate, template)
                    nav.popBackStack()
                    // Rovnaké potvrdenie vždy – odosielateľ nevie, či ŠPZ má konto.
                    scope.launch { snackbar.showSnackbar(sentText) }
                },
            )
        }
        composable(Routes.MESSAGE) { entry ->
            val id = entry.arguments?.getString("id")
            val message = id?.let { repo.message(it) }
            if (message == null) {
                LaunchedEffect(Unit) { nav.popBackStack() }
            } else {
                MessageDetailScreen(
                    message = message,
                    snackbarHostState = snackbar,
                    onBack = { nav.popBackStack() },
                    onReply = { repo.sendReply(message.id, it) },
                    onMarkRead = { repo.markAsRead(message.id) },
                    onBlock = { repo.blockSender(message.id) },
                    onReport = { repo.report(message.id) },
                )
            }
        }
    }
}
