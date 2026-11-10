package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun PlaylistDetailScreen(bridge: MusicBridge, item: PlaylistItem, onBack: () -> Unit, onPlayList: (List<Track>, Int, Boolean) -> Unit) {
    var tracks by remember(item.id) { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(item.id) { tracks = runCatching { bridge.playlistTracks(item.id) }.getOrDefault(emptyList()) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(30.dp).clickable { onBack() })
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(item.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${tracks?.size ?: item.count} songs", color = Color.White.copy(.6f), fontSize = 12.sp)
            }
        }
        val list = tracks
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Playlist ini masih kosong", color = Color.White.copy(.6f)) }
        } else {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.clip(CircleShape).background(Color.White).clickable { onPlayList(list, 0, false) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Putar", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.clip(CircleShape).background(GlassFill).clickable { onPlayList(list, 0, true) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Acak", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(list) { i, t ->
                    Row(Modifier.clickable { onPlayList(list, i, false) }, verticalAlignment = Alignment.CenterVertically) {
                        Cover(t.coverUrl, 52.dp, 10.dp)
                        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                            Text(t.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsScreen(bridge: MusicBridge, onBack: () -> Unit) {
    val st by bridge.player.collectAsState()
    var text by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val auto by com.metrolist.music.raq.rememberRaqBool(com.metrolist.music.raq.RaqPrefs.AUTO_LYRICS, true)
    var requested by remember(st.track?.id) { mutableStateOf(false) }
    LaunchedEffect(st.track?.id, auto, requested) {
        text = null; failed = false
        if (!auto && !requested) return@LaunchedEffect
        val r = runCatching { bridge.lyrics() }.getOrNull()
        if (r.isNullOrBlank()) failed = true else text = r
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(30.dp).clickable { onBack() })
            Column(Modifier.padding(start = 12.dp)) {
                Text(st.track?.title ?: "-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(st.track?.artist ?: "", color = Color.White.copy(.6f), fontSize = 13.sp, maxLines = 1)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                !auto && !requested -> Text("Auto Lyrics dimatikan. Tap di sini untuk memuat lirik.", color = Color.White.copy(.7f), modifier = Modifier.padding(top = 60.dp).clickable { requested = true })
                text != null -> Text(text!!, color = Color.White, fontSize = 20.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 40.dp))
                failed -> Text("Lirik untuk lagu ini tidak ditemukan.", color = Color.White.copy(.6f), modifier = Modifier.padding(top = 60.dp))
                else -> CircularProgressIndicator(Modifier.padding(top = 60.dp))
            }
        }
    }
}

@Composable
fun QueueScreen(bridge: MusicBridge, onBack: () -> Unit) {
    val q by bridge.queue.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(30.dp).clickable { onBack() })
            Text("Antrean", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
        }
        if (q.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Antrean kosong", color = Color.White.copy(.6f)) }
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(q) { _, it ->
                Row(Modifier.clickable { bridge.playQueueIndex(it.index) }, verticalAlignment = Alignment.CenterVertically) {
                    Cover(it.coverUrl, 48.dp, 10.dp)
                    Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                        Text(it.title, color = if (it.current) Accent else Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(it.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
                    }
                    if (it.current) Icon(Icons.Filled.GraphicEq, null, tint = Accent)
                }
            }
        }
    }
}
