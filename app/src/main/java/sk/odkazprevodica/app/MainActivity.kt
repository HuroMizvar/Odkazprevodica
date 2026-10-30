package sk.odkazprevodica.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import sk.odkazprevodica.app.data.Repository
import sk.odkazprevodica.app.push.Notifications
import sk.odkazprevodica.app.ui.AppNavigation
import sk.odkazprevodica.app.ui.theme.AppColors
import sk.odkazprevodica.app.ui.theme.OdkazTheme

class MainActivity : ComponentActivity() {

    /** ŠPZ z ťuknutia na notifikáciu – otvorí schránku auta. */
    private val openPlate = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        Notifications.createChannel(this)
        // Tmavé pozadie → svetlé ikony v stavovom a navigačnom riadku.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        handleIntent(intent)
        setContent {
            OdkazTheme {
                // Obsah nikdy nezasahuje pod stavový riadok, výrez kamery ani spodné tlačidlá.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(AppColors.Background)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    AppNavigation(
                        openPlate = openPlate.value,
                        onPlateOpened = { openPlate.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(Notifications.EXTRA_PLATE)?.let { openPlate.value = it }
    }
}
