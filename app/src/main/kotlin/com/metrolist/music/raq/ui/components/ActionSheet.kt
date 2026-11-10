package com.metrolist.music.raq.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.raq.ui.Track
import kotlinx.coroutines.launch

/** Satu baris aksi di menu bawah. */
data class SheetAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/** Tombol titik tiga bulat (gaya glass) yang dipakai di daftar lagu. */
@Composable
fun DotsButton(onClick: () -> Unit, size: Int = 38) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(.10f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Filled.MoreHoriz, null, tint = Color.White.copy(.85f)) }
}

/**
 * Menu bawah (bottom sheet) bergaya RaqMusic: header lagu + daftar aksi.
 * [extra] adalah slot opsional di antara header dan daftar aksi (mis. kecepatan putar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    title: String,
    subtitle: String,
    coverUrl: String?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(after: (() -> Unit)? = null) {
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss(); after?.invoke() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Color(0xFF17102F),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(.3f)) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(coverUrl, 56.dp, 12.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(subtitle, color = Color.White.copy(.65f), fontSize = 14.sp, maxLines = 1)
                }
            }
            Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(.10f)))
            extra?.invoke(this)
            actions.forEach { a ->
                Row(
                    Modifier.fillMaxWidth().clickable { close(a.onClick) }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(a.icon, null, tint = Color.White.copy(.9f))
                    Text(a.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 18.dp))
                }
            }
        }
    }
}

private fun trackUrl(t: Track) = "https://music.youtube.com/watch?v=${t.id}"

/** Aksi umum untuk satu lagu: bagikan, salin tautan, buka di YouTube Music. */
fun trackLinkActions(ctx: Context, t: Track): List<SheetAction> = listOf(
    SheetAction(Icons.Outlined.Share, "Share") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${t.title} - ${t.artist}\n${trackUrl(t)}")
        }
        runCatching { ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    },
    SheetAction(Icons.Outlined.Link, "Copy link") {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(t.title, trackUrl(t)))
        Toast.makeText(ctx, "Tautan disalin", Toast.LENGTH_SHORT).show()
    },
    SheetAction(Icons.Outlined.Public, "Open in YouTube Music") {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trackUrl(t))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    },
)
