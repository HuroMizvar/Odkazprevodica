package sk.odkazprevodica.app.update

import androidx.compose.runtime.Composable

/**
 * Verzia pre Google Play: bez aktualizácie mimo Play (pravidlá Google Play).
 * Aktualizácie doručuje Obchod Play.
 */
object SelfUpdate {
    suspend fun checkOnStart() {}

    @Composable
    fun isAvailable(): Boolean = false

    @Composable
    fun SettingsSection() {}
}
