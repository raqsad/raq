package com.metrolist.music.raq.island

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.metrolist.music.raq.ui.MusicBridge
import com.metrolist.music.raq.ui.components.Cover
import com.metrolist.music.raq.ui.theme.Mint
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max

/**
 * "Dynamic Island" di atas layar (butuh izin "Display over other apps").
 *
 * - Posisi: tepat di area kamera (punch-hole / notch) paling atas layar, bukan di bawah status bar.
 *   Kalau HP tidak punya cutout, dipasang rapat di tepi atas.
 * - Collapsed: pill yang melingkari kamera (cover di kiri, equalizer di kanan).
 * - Tap -> expand: cover, judul, progress, prev / play / next. Menutup sendiri setelah 6 detik.
 * - Mode: Classic (hitam solid) atau Liquid Glass (kaca transparan). Ukuran & posisi sama persis.
 */
class DynamicIslandService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        var bridge: MusicBridge? = null
        /** true = Liquid Glass, false = Classic hitam. Diubah dari Settings, langsung berlaku. */
        var glass by mutableStateOf(false)
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var view: ComposeView? = null
    private lateinit var wm: WindowManager

    // Ukuran pill (dp) & posisi (px), dihitung dari cutout layar.
    private var collapsedW = 168f
    private var collapsedH = 38f
    private var screenWdp = 360f

    override fun onBind(intent: Intent?): IBinder? = null

    /** Jangan dihidupkan lagi otomatis oleh sistem setelah app dibersihkan dari recents. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    /** Cari cutout kamera di tepi atas (punch-hole / notch). */
    private fun topCutout(): Rect? {
        if (Build.VERSION.SDK_INT < 29) return null
        val display = (getSystemService(DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY) ?: return null
        val rects = display.cutout?.boundingRects ?: return null
        val sw = resources.displayMetrics.widthPixels
        return rects.filter { it.top <= 2 && it.width() < sw / 2 }.minByOrNull { abs(it.centerX() - sw / 2) }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        screenWdp = resources.displayMetrics.widthPixels / density
        val cut = topCutout()
        val camW = (cut?.width() ?: 0) / density
        val camH = (cut?.height() ?: 0) / density
        // Tinggi status bar (px) dipakai sebagai patokan kalau HP tidak punya cutout kamera.
        val sbId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val sbPx = if (sbId > 0) resources.getDimensionPixelSize(sbId) else (24 * density).toInt()
        val yPx: Int
        if (cut != null) {
            // Ada kamera: pill melingkari kamera (lebih lebar & tinggi dari lubangnya).
            collapsedH = max(38f, camH + 12f)
            collapsedW = (camW + 2 * 64f).coerceIn(160f, 240f)
            yPx = (cut.exactCenterY() - collapsedH * density / 2f).toInt().coerceAtLeast(0)
        } else {
            // Tanpa cutout: pill menyesuaikan tinggi status bar & ditaruh di tengahnya, jadi tetap rapi di atas.
            collapsedH = (sbPx / density - 6f).coerceIn(30f, 38f)
            collapsedW = 150f
            yPx = ((sbPx - collapsedH * density) / 2f).toInt().coerceAtLeast((3 * density).toInt())
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = yPx
            if (Build.VERSION.SDK_INT >= 30) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandService)
            setContent { Island() }
        }
        wm.addView(view, params)
    }

    private fun Modifier.islandSurface(shape: Shape, glass: Boolean): Modifier =
        if (!glass) clip(shape).background(Color.Black)
        else clip(shape)
            .background(Color.Black.copy(.42f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(.30f), Color.White.copy(.08f), Color.White.copy(.16f))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(.80f), Color.White.copy(.08f), Color.White.copy(.45f))), shape)

    private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

    @Composable
    private fun Bars(playing: Boolean) {
        val tr = rememberInfiniteTransition(label = "bars")
        val a by tr.animateFloat(.35f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
        val b by tr.animateFloat(1f, .3f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "b")
        val c by tr.animateFloat(.5f, .9f, infiniteRepeatable(tween(360), RepeatMode.Reverse), label = "c")
        Row(Modifier.height(18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            listOf(a, b, c, a).forEach { h ->
                Box(Modifier.width(3.dp).fillMaxHeight(if (playing) h else .2f).clip(RoundedCornerShape(2.dp)).background(Mint))
            }
        }
    }

    @Composable
    private fun Island() {
        val b = DynamicIslandService.bridge ?: return
        val st by b.player.collectAsState()
        val t = st.track ?: return
        var expanded by remember { mutableStateOf(false) }
        val radius by animateDpAsState(if (expanded) 38.dp else (collapsedH / 2).dp, label = "radius")
        val expandedW = (screenWdp - 24f).coerceAtMost(380f)

        // Menutup sendiri; timer diulang tiap ada interaksi (ganti lagu / play-pause).
        LaunchedEffect(expanded, t.id, st.isPlaying) { if (expanded) { delay(6000); expanded = false } }

        Box(
            Modifier.animateContentSize(spring(dampingRatio = .75f))
                .islandSurface(RoundedCornerShape(radius), glass)
                .clickable { expanded = !expanded }
        ) {
            if (!expanded) {
                Row(
                    Modifier.width(collapsedW.dp).height(collapsedH.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(t.coverUrl, 26.dp, 13.dp)
                    Spacer(Modifier.weight(1f))
                    Bars(st.isPlaying)
                }
            } else {
                Column(Modifier.width(expandedW.dp).padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(t.coverUrl, 56.dp, 16.dp)
                        Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                            Text(t.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(t.artist, color = Color.White.copy(.65f), fontSize = 13.sp, maxLines = 1)
                        }
                        Bars(st.isPlaying)
                    }
                    Spacer(Modifier.height(12.dp))
                    val frac = (st.positionMs.toFloat() / st.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(.22f))) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Color.White))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(fmt(st.positionMs), color = Color.White.copy(.6f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(fmt(st.durationMs), color = Color.White.copy(.6f), fontSize = 11.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp).clickable { b.previous() })
                        Icon(
                            if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White,
                            modifier = Modifier.padding(horizontal = 26.dp).size(44.dp).clickable { b.togglePlay() },
                        )
                        Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp).clickable { b.next() })
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }
}
