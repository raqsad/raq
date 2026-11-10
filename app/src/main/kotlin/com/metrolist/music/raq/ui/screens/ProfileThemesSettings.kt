package com.metrolist.music.raq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.components.*
import com.metrolist.music.raq.ui.theme.*

@Composable
fun ThemesScreen(selected: AppPalette, onBack: () -> Unit, onApply: (AppPalette) -> Unit) {
    var pick by remember { mutableStateOf(selected) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ChevronLeft, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterStart).clickable { onBack() })
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Themes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Choose your favorite theme", color = Color.White.copy(.7f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(AppPalette.values().toList()) { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(p.top, p.mid, p.bottom)))
                            .then(if (p == pick) Modifier.border(2.dp, Mint, RoundedCornerShape(16.dp)) else Modifier)
                            .clickable { pick = p },
                        contentAlignment = Alignment.BottomEnd,
                    ) { if (p == pick) Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.padding(8.dp)) }
                    Text(p.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF5B5BD6), AccentSoft))).clickable { onApply(pick) },
            contentAlignment = Alignment.Center,
        ) { Text("Apply Theme", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun ProfileScreen(
    name: String,
    onSettings: () -> Unit,
    onPlayMix: () -> Unit,
    onLiked: () -> Unit,
    onOpenMetrolist: () -> Unit,
    onEdit: () -> Unit,
    onPremium: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Cover(null, 96.dp, 48.dp)
        Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp).clickable { onEdit() })
        Text("Developer: Raq", color = Color.White.copy(.6f), fontSize = 13.sp)
        Glass(Modifier.padding(top = 10.dp), radius = 20.dp, onClick = onPremium) {
            Text("\uD83D\uDC51 Premium", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("248" to "Playlists", "12.4K" to "Followers", "120" to "Following").forEach { (n, l) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(l, color = Color.White.copy(.6f), fontSize = 12.sp)
                }
            }
        }
        Glass(Modifier.fillMaxWidth(), radius = 20.dp, onClick = onPlayMix) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Accent))
                Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                    Text("Your Mood", color = Color.White, fontWeight = FontWeight.Medium)
                    Text("Perfect Playlist for your vibe", color = Color.White.copy(.6f), fontSize = 11.sp)
                }
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        listOf(
            Triple(Icons.Outlined.Timeline, "Your Activity", onOpenMetrolist),
            Triple(Icons.Outlined.Download, "Downloads", onOpenMetrolist),
            Triple(Icons.Outlined.FavoriteBorder, "Liked Songs", onLiked),
            Triple(Icons.Outlined.Settings, "Settings", onSettings),
            Triple(Icons.Outlined.HelpOutline, "Help & Support", onHelp),
            Triple(Icons.Outlined.Info, "About Berst", onAbout),
        ).forEach { (ic, t, action) ->
                Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ic, null, tint = Color.White)
                    Text(t, color = Color.White, modifier = Modifier.padding(start = 14.dp).weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.5f))
                }
            }
        Spacer(Modifier.weight(1f))
        Glass(Modifier.fillMaxWidth().height(44.dp), radius = 22.dp, onClick = onOpenMetrolist) {
            Text("Akun & Log Out", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}
