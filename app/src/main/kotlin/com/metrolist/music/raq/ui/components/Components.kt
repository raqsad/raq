package com.metrolist.music.raq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.raq.ui.PlayerUi
import com.metrolist.music.raq.ui.theme.*

@Composable
fun Glass(modifier: Modifier = Modifier, radius: Dp = 20.dp, onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val m = modifier.clip(RoundedCornerShape(radius)).background(GlassFill)
    Box(if (onClick != null) m.clickable { onClick() } else m, content = content)
}

@Composable
fun Cover(url: String?, size: Dp, radius: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(Brush.linearGradient(listOf(Accent, Color(0xFF3B1E8F))))) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onSeeAll != null) Text("See all >", color = Color.White.copy(.7f), fontSize = 12.sp, modifier = Modifier.clickable { onSeeAll() })
    }
}

/** Header seksi dengan ikon ungu di depan judul (dipakai di halaman Search). */
@Composable
fun IconSectionHeader(icon: ImageVector, title: String, onSeeAll: (() -> Unit)? = null, seeAllLabel: String = "See all") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Accent.copy(.20f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        }
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        if (onSeeAll != null) Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSeeAll() }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(seeAllLabel, color = Color.White.copy(.7f), fontSize = 14.sp)
            Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun MiniPlayer(
    state: PlayerUi, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier,
    onNext: () -> Unit = {}, onQueue: (() -> Unit)? = null,
) {
    val t = state.track ?: return
    val frac = (state.positionMs.toFloat() / state.durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A1B5C).copy(.70f), Color(0xFF1A1038).copy(.85f))))
            .border(1.dp, Accent.copy(alpha = 0.55f), shape)
            .clickable { onOpen() },
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(t.coverUrl, 48.dp, 12.dp)
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(t.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(t.artist, color = Color.White.copy(.65f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) { Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            if (onQueue != null) IconButton(onClick = onQueue) { Icon(Icons.Filled.QueueMusic, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp).fillMaxWidth(frac).height(2.dp).background(Violet))
    }
}

enum class Tab(val label: String, val icon: ImageVector, val active: Color) {
    Home("Home", Icons.Filled.Home, Violet),
    Search("Search", Icons.Filled.Search, Violet),
    Library("Library", Icons.Filled.LibraryMusic, Violet),
    Profile("Profile", Icons.Filled.Person, Violet),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF07040F).copy(.92f)).navigationBarsPadding().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        Tab.values().forEach { tab ->
            val on = tab == current
            Column(Modifier.clickable { onSelect(tab) }.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(tab.icon, null, tint = if (on) tab.active else Color.White.copy(.55f))
                Text(tab.label, fontSize = 11.sp, color = if (on) tab.active else Color.White.copy(.55f))
                Box(Modifier.padding(top = 3.dp).width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (on) tab.active else Color.Transparent))
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) Accent else GlassFill).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CircleBtn(icon: ImageVector, size: Dp = 46.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White) }
}
