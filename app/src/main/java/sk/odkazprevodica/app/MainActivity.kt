package sk.odkazprevodica.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import sk.odkazprevodica.app.ui.AppNavigation
import sk.odkazprevodica.app.ui.theme.OdkazTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OdkazTheme {
                AppNavigation()
            }
        }
    }
}
