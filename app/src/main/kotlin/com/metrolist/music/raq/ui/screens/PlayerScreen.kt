package com.metrolist.music.raq.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

/** Warna dominan cover (dipakai buat background Now Playing seperti di desain). */
private suspend fun dominantColor(url: String?): Color? = withContext(Dispatchers.IO) {
    if (url == null) return@withContext null
    runCatching {
        val conn = java.net.URL(url).openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
        val bmp = android.graphics.BitmapFactory.decodeStream(conn.getInputStream()) ?: return@runCatching null
        val s = android.graphics.Bitmap.createScaledBitmap(bmp, 24, 24, true)
        var r = 0.0; var g = 0.0; var b = 0.0; var w = 0.0
        val hsv = FloatArray(3)
        for (x in 0 until 24) for (y in 0 until 24) {
            val c = s.getPixel(x, y)
            android.graphics.Color.colorToHSV(c, hsv)
            val wt = (hsv[1] * hsv[2]).toDouble() + 0.02
            r += android.graphics.Color.red(c) * wt
            g += android.graphics.Color.green(c) * wt
            b += android.graphics.Color.blue(c) * wt
            w += wt
        }
        Color((r / w).toInt().coerceIn(0, 255), (g / w).toInt().coerceIn(0, 255), (b / w).toInt().coerceIn(0, 255))
    }.getOrNull()
}

@Composable
private fun GlassCircle(size: Int = 44, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(.14f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(bridge: MusicBridge, onClose: () -> Unit, onLyrics: () -> Unit, onQueue: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val st by bridge.player.collectAsState()
    val source by bridge.queueTitle.collectAsState()
    val t = st.track
    var menu by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(Accent) }
    LaunchedEffect(t?.coverUrl) { dominantColor(t?.coverUrl)?.let { base = it } }
    val top by animateColorAsState(lerp(base, Color.Black, 0.10f))
    val mid by animateColorAsState(lerp(base, Color.Black, 0.65f))

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, mid, Color(0xFF0A0706))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Playing from", color = Color.White.copy(.65f), fontSize = 12.sp)
                    Box(
                        Modifier.padding(top = 4.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(.14f))
                            .clickable { onQueue() }.padding(horizontal = 16.dp, vertical = 6.dp),
                    ) { Text(source ?: "Queue", color = Color.White, fontSize = 15.sp, maxLines = 1) }
                }
                GlassCircle(onClick = { menu = true }) { Icon(Icons.Filled.MoreHoriz, null, tint = Color.White) }
            }
            Spacer(Modifier.height(24.dp))
            Cover(
                t?.coverUrl, 320.dp, 30.dp,
                Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, Color.White.copy(.25f), RoundedCornerShape(30.dp)),
            )
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t?.title ?: "-", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(t?.artist ?: "", color = Color.White.copy(.65f), fontSize = 18.sp, maxLines = 1)
                }
                GlassCircle(46, onClick = { bridge.toggleLike() }) {
                    Icon(if (st.liked) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = if (st.liked) Color(0xFFFFD54F) else Color.White)
                }
                Spacer(Modifier.width(10.dp))
                GlassCircle(46, onClick = { menu = true }) { Icon(Icons.Filled.MoreHoriz, null, tint = Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            Slider(
                value = (st.positionMs.toFloat() / st.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f),
                onValueChange = { bridge.seekTo((it * st.durationMs).toLong()) },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(.25f)),
            )
            Row {
                Text(fmt(st.positionMs), color = Color.White.copy(.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(fmt(st.durationMs), color = Color.White.copy(.7f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shuffle, null, tint = if (st.shuffle) Violet else Color.White, modifier = Modifier.size(28.dp).clickable { bridge.toggleShuffle() })
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { bridge.previous() })
                Box(Modifier.size(78.dp).clip(CircleShape).background(Color.White).clickable { bridge.togglePlay() }, contentAlignment = Alignment.Center) {
                    Icon(if (st.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                }
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { bridge.next() })
                Icon(Icons.Filled.Repeat, null, tint = if (st.repeat) Violet else Color.White, modifier = Modifier.size(28.dp).clickable { bridge.toggleRepeat() })
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.White.copy(.10f)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircle(46, onClick = {
                    runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Icon(Icons.Outlined.Cast, null, tint = Color.White) }
                GlassCircle(46, onClick = onQueue) { Icon(Icons.Filled.QueueMusic, null, tint = Color.White) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Color.White.copy(.10f)).clickable { onLyrics() }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircle(46) { Icon(Icons.Filled.MusicNote, null, tint = Color.White) }
                Text("Lyrics", color = Color.White, fontSize = 17.sp, modifier = Modifier.padding(start = 14.dp).weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.padding(end = 12.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (menu && t != null) {
        var speed by remember { mutableStateOf(bridge.playbackSpeed()) }
        ActionSheet(
            title = t.title, subtitle = t.artist, coverUrl = t.coverUrl, onDismiss = { menu = false },
            extra = {
                Text("Playback speed", color = Color.White.copy(.65f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                Row(Modifier.padding(horizontal = 24.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { v ->
                        Chip("${v}x".replace(".0x", "x"), kotlin.math.abs(speed - v) < 0.01f) { speed = v; bridge.setPlaybackSpeed(v) }
                    }
                }
            },
            actions = listOf(
                SheetAction(if (st.liked) Icons.Filled.Star else Icons.Outlined.StarBorder, if (st.liked) "Remove from liked" else "Add to liked") { bridge.toggleLike() },
                SheetAction(Icons.Filled.MusicNote, "Lyrics") { onLyrics() },
                SheetAction(Icons.Filled.QueueMusic, "Queue") { onQueue() },
            ) + trackLinkActions(ctx, t),
        )
    }
}
