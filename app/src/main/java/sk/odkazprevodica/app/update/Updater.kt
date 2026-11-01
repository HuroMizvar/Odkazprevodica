package sk.odkazprevodica.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import sk.odkazprevodica.app.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aktualizácia mimo Google Play: kontroluje GitHub Releases repozitára
 * a stiahne + nainštaluje novšiu APK. Číslo verzie = číslo zostavenia
 * na GitHube (tag „build-N“ ↔ versionCode N).
 */
object Updater {

    private const val RELEASES_URL = "https://api.github.com/repos/HuroMizvar/odkazprevodica/releases?per_page=10"

    data class Release(val build: Int, val name: String, val apkUrl: String)

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        data class Downloading(val release: Release, val progress: Float) : State
        data class Ready(val release: Release, val file: File) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    val currentBuild: Int get() = BuildConfig.VERSION_CODE

    /** Zistí, či je na GitHube novšia verzia. */
    suspend fun check() {
        if (_state.value is State.Downloading) return
        _state.value = State.Checking
        _state.value = runCatching { fetchLatest() }.fold(
            onSuccess = { r -> if (r != null && r.build > currentBuild) State.Available(r) else State.UpToDate },
            onFailure = { State.Error("Nepodarilo sa overiť aktualizáciu. Skontroluj internet.") },
        )
    }

    private suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            (0 until arr.length()).map { arr.getJSONObject(it) }.mapNotNull { rel ->
                val build = rel.optString("tag_name").removePrefix("build-").toIntOrNull() ?: return@mapNotNull null
                val assets = rel.optJSONArray("assets") ?: return@mapNotNull null
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@mapNotNull null
                Release(build, rel.optString("name"), apk.getString("browser_download_url"))
            }.maxByOrNull { it.build }
        } finally {
            conn.disconnect()
        }
    }

    /** Stiahne APK do priečinka aplikácie (nepotrebuje povolenie na úložisko). */
    suspend fun download(context: Context, release: Release) {
        _state.value = State.Downloading(release, 0f)
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "odkaz-pre-vodica.apk")
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                }
                try {
                    val total = conn.contentLengthLong.takeIf { it > 0 }
                    conn.inputStream.use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var lastReported = 0f
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total != null) {
                                    val p = done.toFloat() / total
                                    if (p - lastReported >= 0.02f) {
                                        lastReported = p
                                        _state.value = State.Downloading(release, p)
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
        _state.value = result.fold(
            onSuccess = { State.Ready(release, file) },
            onFailure = { State.Error("Sťahovanie zlyhalo. Skús to znova.") },
        )
    }

    /** Smie aplikácia otvoriť inštalátor? (Android 8+: „Inštalovať neznáme aplikácie“) */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Otvorí systémové nastavenie, kde používateľ povolí inštaláciu z tejto aplikácie. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Otvorí systémový inštalátor. Údaje v aplikácii zostanú (rovnaký podpis). */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
