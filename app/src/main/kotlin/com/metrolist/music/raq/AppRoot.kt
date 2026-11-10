package com.metrolist.music.raq

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.raq.island.DynamicIslandService
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.PlaylistItem
import com.metrolist.music.raq.ui.Track
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.screens.*
import com.metrolist.music.raq.ui.theme.*

/** true = tampilkan UI RaqMusic; false = UI asli Metrolist (restart app untuk kembali). */
object RaqSwitch { var useRaq by mutableStateOf(true) }

private enum class Overlay { None, Player, Lyrics, Queue, Themes, Settings, Playlist }

@Composable
fun AppRoot(bridge: MusicBridge) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("raq_ui", android.content.Context.MODE_PRIVATE) }
    var palette by remember { mutableStateOf(AppPalette.values().getOrElse(prefs.getInt("palette", 0)) { AppPalette.Dynamic }) }
    var name by remember { mutableStateOf(prefs.getString("name", "Raq") ?: "Raq") }
    var tab by remember { mutableStateOf(Tab.values().firstOrNull { it.name == RaqPrefs.of(ctx).getString(RaqPrefs.STARTUP_TAB, "Home") } ?: Tab.Home) }
    var overlay by remember { mutableStateOf(Overlay.None) }
    var playlist by remember { mutableStateOf<PlaylistItem?>(null) }
    var island by remember { mutableStateOf(prefs.getBoolean("island", false) && Settings.canDrawOverlays(ctx)) }
    var glass by remember { mutableStateOf(prefs.getBoolean("island_glass", false)) }
    var dialog by remember { mutableStateOf("") }
    val player by bridge.player.collectAsState()
    val recent by bridge.recentlyPlayed.collectAsState()

    SideEffect { DynamicIslandService.glass = glass }
    // Pulihkan Dynamic Island kalau tadi aktif (service ikut mati saat app dibersihkan dari recents).
    LaunchedEffect(Unit) {
        if (island) {
            DynamicIslandService.bridge = bridge
            runCatching { ctx.startService(Intent(ctx, DynamicIslandService::class.java)) }
        }
    }

    fun toast(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show()
    fun openUrl(u: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun playList(l: List<Track>, i: Int) = bridge.playTracks(l, i)
    fun playMix() { if (recent.isEmpty()) toast("Belum ada lagu. Cari dan suka dulu beberapa lagu.") else bridge.playTracks(recent, 0, shuffle = true) }
    fun openPlaylist(p: PlaylistItem) { playlist = p; overlay = Overlay.Playlist }
    val openMetrolist = { RaqSwitch.useRaq = false }

    BackHandler(enabled = overlay != Overlay.None) {
        overlay = when (overlay) {
            Overlay.Lyrics, Overlay.Queue -> Overlay.Player
            Overlay.Themes -> Overlay.Settings
            else -> Overlay.None
        }
    }

    RaqTheme(palette) {
        when (overlay) {
            Overlay.Player -> PlayerScreen(bridge, { overlay = Overlay.None }, { overlay = Overlay.Lyrics }, { overlay = Overlay.Queue })
            Overlay.Lyrics -> LyricsScreen(bridge) { overlay = Overlay.Player }
            Overlay.Queue -> QueueScreen(bridge) { overlay = Overlay.Player }
            Overlay.Themes -> ThemesScreen(palette, { overlay = Overlay.Settings }) {
                palette = it; prefs.edit().putInt("palette", it.ordinal).apply(); overlay = Overlay.Settings
            }
            Overlay.Playlist -> playlist?.let { p ->
                PlaylistDetailScreen(bridge, p, { overlay = Overlay.None }) { l, i, sh -> bridge.playTracks(l, i, sh) }
            }
            Overlay.Settings -> SettingsScreen(
                bridge = bridge,
                onBack = { overlay = Overlay.None },
                onThemes = { overlay = Overlay.Themes },
                onOpenMetrolist = openMetrolist,
                onAbout = { dialog = "about" },
                onSupport = { openUrl("https://github.com/MetrolistGroup/Metrolist") },
                islandEnabled = island,
                glassEnabled = glass,
                onGlass = { glass = it; prefs.edit().putBoolean("island_glass", it).apply() },
                onIsland = { on ->
                    if (on && !Settings.canDrawOverlays(ctx)) {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        island = on
                        prefs.edit().putBoolean("island", on).apply()
                        DynamicIslandService.bridge = bridge
                        val i = Intent(ctx, DynamicIslandService::class.java)
                        if (on) ctx.startService(i) else ctx.stopService(i)
                    }
                },
            )
            Overlay.None -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Home -> HomeScreen(bridge, { tab = Tab.Search }, ::playList, ::openPlaylist, ::playMix)
                        Tab.Search -> SearchScreen(bridge, ::playList)
                        Tab.Library -> LibraryScreen(bridge, ::openPlaylist, { dialog = "create" }, { tab = Tab.Search }, ::playList)
                        Tab.Profile -> ProfileScreen(
                            name = name,
                            onSettings = { overlay = Overlay.Settings },
                            onPlayMix = ::playMix,
                            onLiked = { openPlaylist(PlaylistItem("LIKED", "Liked Songs", 0, null)) },
                            onOpenMetrolist = openMetrolist,
                            onEdit = { dialog = "edit" },
                            onPremium = { dialog = "premium" },
                            onHelp = { openUrl("https://github.com/MetrolistGroup/Metrolist/issues") },
                            onAbout = { dialog = "about" },
                        )
                    }
                }
                MiniPlayer(player, bridge::togglePlay, { overlay = Overlay.Player }, onNext = bridge::next, onQueue = { overlay = Overlay.Queue })
                BottomNav(tab) { tab = it }
            }
        }

        var text by remember(dialog) { mutableStateOf(if (dialog == "edit") name else "") }
        when (dialog) {
            "create" -> AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Playlist baru") },
                text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Nama playlist") }) },
                confirmButton = { TextButton({ if (text.isNotBlank()) { bridge.createPlaylist(text.trim()); toast("Playlist dibuat"); dialog = "" } }) { Text("Buat") } },
                dismissButton = { TextButton({ dialog = "" }) { Text("Batal") } },
            )
            "edit" -> AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Edit Profile") },
                text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Nama") }) },
                confirmButton = { TextButton({ if (text.isNotBlank()) { name = text.trim(); prefs.edit().putString("name", name).apply(); dialog = "" } }) { Text("Simpan") } },
                dismissButton = { TextButton({ dialog = "" }) { Text("Batal") } },
            )
            "about" -> AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Berst") },
                text = { Text("Developer: Raq\n\nBerst adalah tampilan baru di atas mesin Metrolist (open source, GPL-3.0). Semua fitur pemutaran, lirik, dan library berasal dari Metrolist.") },
                confirmButton = { TextButton({ dialog = "" }) { Text("Tutup") } },
            )
            "premium" -> AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Premium") },
                text = { Text("Berst gratis dan tanpa iklan, jadi tidak ada paket Premium.") },
                confirmButton = { TextButton({ dialog = "" }) { Text("OK") } },
            )
        }
    }
}

/** Titik masuk dari MainActivity: ambil PlayerConnection & database Metrolist, lalu tampilkan UI RaqMusic. */
@Composable
fun RaqAppHost() {
    val pc = com.metrolist.music.LocalPlayerConnection.current
    val db = com.metrolist.music.LocalDatabase.current
    val scope = rememberCoroutineScope()
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    // Service kecil yang mematikan app total saat disapu dari recents.
    LaunchedEffect(Unit) { runCatching { appCtx.startService(Intent(appCtx, TaskCleanupService::class.java)) } }
    // Layar loading tampil minimal ~1,6 detik supaya animasinya sempat terlihat.
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1600); splashDone = true }
    if (pc == null) {
        BerstLoading()
        return
    }
    val bridge = remember(pc) { MetrolistBridge(pc, db, scope) }
    if (splashDone) AppRoot(bridge) else BerstLoading()
}
