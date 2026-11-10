package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.*
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onMic: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).height(56.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Accent.copy(.22f), Color(0xFF2A1B5C).copy(.55f))))
            .border(1.dp, Accent.copy(.40f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 8.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(.10f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Search, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("Search songs, artists...", color = Color.White.copy(.55f), fontSize = 16.sp)
                BasicTextField(
                    value, onChange, enabled = onClick == null, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(.10f))
                    .then(if (onMic != null) Modifier.clickable { onMic() } else Modifier),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Mic, null, tint = Color.White) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    bridge: MusicBridge,
    onSearch: () -> Unit,
    onPlayList: (List<Track>, Int) -> Unit,
    onOpenPlaylist: (PlaylistItem) -> Unit,
    onPlayMix: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val made by bridge.madeForYou.collectAsState()
    val recent by bridge.recentlyPlayed.collectAsState()
    val player by bridge.player.collectAsState()
    val pager = rememberPagerState { 3 }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 22.dp, end = 20.dp, top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Good Morning,", color = Color.White, fontSize = 22.sp)
                    Text("Raq \uD83D\uDC4B", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(GlassFill).border(1.dp, Color.White.copy(.15f), CircleShape)
                        .clickable { android.widget.Toast.makeText(ctx, "Belum ada notifikasi", android.widget.Toast.LENGTH_SHORT).show() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Notifications, null, tint = Color.White)
                    Box(Modifier.align(Alignment.TopEnd).padding(11.dp).size(9.dp).clip(CircleShape).background(Violet))
                }
            }
            SearchBar("", {}, onClick = onSearch)
            Spacer(Modifier.height(18.dp))
            HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp) { i ->
                val p = made.getOrNull(i)
                Box(
                    Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8B3FE8), Color(0xFFB65CE6), Color(0xFF4A1D96))))
                        .clickable { if (p != null) onOpenPlaylist(p) else onPlayMix() },
                ) {
                    Column(Modifier.align(Alignment.CenterStart).padding(24.dp)) {
                        Text(p?.name ?: "Chill Vibes", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(if (p != null) "${p.count} songs" else "Relax \u00B7 Unwind \u00B7 Breathe", color = Color.White.copy(.8f), fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White).clickable { onPlayMix() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) {
                    Box(
                        Modifier.padding(3.dp).size(if (pager.currentPage == it) 8.dp else 7.dp).clip(CircleShape)
                            .background(if (pager.currentPage == it) Color.White else Color.White.copy(.3f))
                    )
                }
            }
            SectionHeader("Made for You") { }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(made) { p ->
                    Box(
                        Modifier.size(width = 118.dp, height = 128.dp).clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6).copy(.75f), Color(0xFF2A1B5C))))
                            .clickable { onOpenPlaylist(p) },
                    ) {
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(p.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${p.count} songs", color = Color.White.copy(.65f), fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SectionHeader("Recently Played") { }
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(GlassFill).padding(vertical = 6.dp),
            ) {
                recent.take(4).forEachIndexed { i, t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlayList(recent, i) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(t.coverUrl, 52.dp, 12.dp)
                        Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                            Text(t.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(t.artist, color = Color.White.copy(.65f), fontSize = 13.sp, maxLines = 1)
                        }
                        if (player.track?.id == t.id) Icon(Icons.Filled.GraphicEq, null, tint = Violet, modifier = Modifier.padding(end = 10.dp))
                        Icon(Icons.Filled.MoreHoriz, null, tint = Color.White.copy(.7f))
                    }
                }
                if (recent.isEmpty()) Text("Belum ada riwayat. Suka atau putar lagu dulu.", color = Color.White.copy(.6f), modifier = Modifier.padding(16.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun LibraryScreen(bridge: MusicBridge, onOpenPlaylist: (PlaylistItem) -> Unit, onCreate: () -> Unit, onSearch: () -> Unit, onPlayList: (List<Track>, Int) -> Unit) {
    val lib by bridge.library.collectAsState()
    val recent by bridge.recentlyPlayed.collectAsState()
    var tab by remember { mutableStateOf("Playlists") }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Your Library", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.clickable { onSearch() })
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.clickable { onCreate() })
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Playlists", "Artists", "Albums", "Songs").forEach { Chip(it, tab == it) { tab = it } }
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.clickable { onCreate() }, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(GlassFill), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, null, tint = Color.White)
                    }
                    Text("Create Playlist", color = Color.White, modifier = Modifier.padding(start = 14.dp), fontWeight = FontWeight.Medium)
                }
            }
            if (tab == "Playlists") items(lib) { p ->
                Row(Modifier.clickable { onOpenPlaylist(p) }, verticalAlignment = Alignment.CenterVertically) {
                    if (p.id == "LIKED") Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Accent), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Favorite, null, tint = Color.White) } else Cover(p.coverUrl, 52.dp, 12.dp)
                    Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Medium)
                        Text("${p.count} songs", color = Color.White.copy(.6f), fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                }
            }
            if (tab == "Songs") itemsIndexed(recent) { i, t ->
                Row(Modifier.clickable { onPlayList(recent, i) }, verticalAlignment = Alignment.CenterVertically) {
                    Cover(t.coverUrl, 52.dp, 12.dp)
                    Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                        Text(t.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(t.artist, color = Color.White.copy(.6f), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            if (tab == "Artists") items(recent.map { it.artist }.distinct()) { a ->
                Text(a, color = Color.White, modifier = Modifier.fillMaxWidth().clickable { onSearch() }.padding(vertical = 10.dp))
            }
            if (tab == "Albums") item { Text("Belum ada album di bagian ini.", color = Color.White.copy(.6f)) }
        }
    }
}

private data class Cat(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val c1: Color, val c2: Color)

private val Cats = listOf(
    Cat("Pop", Icons.Filled.LocalFireDepartment, Color(0xFFEC3B73), Color(0xFFBE1E52)),
    Cat("Hip-Hop", Icons.Filled.Headphones, Color(0xFF6A2FF0), Color(0xFF4A1FB8)),
    Cat("Rock", Icons.Filled.Bolt, Color(0xFF3F5BEA), Color(0xFF2A38B5)),
    Cat("R&B", Icons.Filled.Favorite, Color(0xFFD62A8C), Color(0xFF8E1760)),
    Cat("EDM", Icons.Filled.GraphicEq, Color(0xFF1F8FE8), Color(0xFF1A4FB0)),
    Cat("Indie", Icons.Filled.Album, Color(0xFF14B8A6), Color(0xFF0B6E73)),
)

@Composable
private fun CategoryCard(cat: Cat, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(80.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(cat.c1, cat.c2))).clickable { onClick() },
    ) {
        Icon(cat.icon, null, tint = Color.White.copy(.16f), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 4.dp).size(58.dp))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 14.dp)) {
            Icon(cat.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(cat.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(bridge: MusicBridge, onPlayList: (List<Track>, Int) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val trending by bridge.trending.collectAsState()
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var allCats by remember { mutableStateOf(false) }
    var menuIdx by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(q) {
        if (q.length > 1) { loading = true; kotlinx.coroutines.delay(350); results = runCatching { bridge.search(q) }.getOrDefault(emptyList()); loading = false } else results = emptyList()
    }
    val voice = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { q = it }
    }
    val searching = q.length > 1
    val list = (if (searching) results else trending).take(20)

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(start = 22.dp, end = 20.dp, top = 24.dp, bottom = 22.dp)) {
                Text("Search", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text("Find your favorite songs, artists and more \u2728", color = Color.White.copy(.75f), fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
            }
            SearchBar(q, { q = it }, onMic = {
                runCatching {
                    voice.launch(
                        android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    )
                }.onFailure { android.widget.Toast.makeText(ctx, "Pencarian suara tidak tersedia", android.widget.Toast.LENGTH_SHORT).show() }
            })
            Spacer(Modifier.height(24.dp))
            IconSectionHeader(Icons.Filled.LocalFireDepartment, if (searching) (if (loading) "Mencari\u2026" else "Results") else "Trending Now", onSeeAll = if (searching) null else ({}))
        }
        item {
            if (list.isEmpty()) {
                Text(if (searching && !loading) "Tidak ada hasil." else "", color = Color.White.copy(.6f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            } else Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp)) {
                Column {
                    list.indices.forEach { i ->
                        Box(Modifier.width(32.dp).height(64.dp), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontSize = 17.sp)
                        }
                    }
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(.07f))) {
                    list.forEachIndexed { i, t ->
                        Box(Modifier.fillMaxWidth().height(64.dp).clickable { onPlayList(list, i) }) {
                            Row(Modifier.padding(horizontal = 8.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                Cover(t.coverUrl, 54.dp, 12.dp)
                                Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                                    Text(t.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(t.artist, color = Color.White.copy(.6f), fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                                DotsButton({ menuIdx = i })
                            }
                            if (i < list.lastIndex) Box(Modifier.align(Alignment.BottomCenter).padding(start = 76.dp, end = 10.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(.08f)))
                        }
                    }
                }
            }
        }
        if (!searching) {
            item {
                Spacer(Modifier.height(18.dp))
                IconSectionHeader(Icons.Filled.GridView, "Browse Categories", onSeeAll = { allCats = !allCats }, seeAllLabel = if (allCats) "Show less" else "See all")
                (if (allCats) Cats else Cats.take(3)).chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c -> CategoryCard(c, Modifier.weight(1f)) { q = "${c.name} hits" } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Popular Searches", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
                androidx.compose.foundation.layout.FlowRow(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("The Weeknd", "Billie Eilish", "Taylor Swift", "Drake", "SZA", "Coldplay").forEach { n ->
                        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(GlassFill).clickable { q = n }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(n, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    menuIdx?.let { i ->
        list.getOrNull(i)?.let { t ->
            ActionSheet(
                title = t.title, subtitle = t.artist, coverUrl = t.coverUrl, onDismiss = { menuIdx = null },
                actions = listOf(SheetAction(Icons.Filled.PlayArrow, "Play now") { onPlayList(list, i) }) + trackLinkActions(ctx, t),
            )
        }
    }
}
