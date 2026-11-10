package com.metrolist.music.raq.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.metrolist.music.R
import com.metrolist.music.raq.ui.theme.Accent
import com.metrolist.music.raq.ui.theme.Violet

/** Layar loading Berst: karakter animasi memeluk logo B + nama app. */
@Composable
fun BerstLoading(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1B5C), Color(0xFF14102B), Color(0xFF05040C)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        var animated = false
                        if (Build.VERSION.SDK_INT >= 28) {
                            runCatching {
                                val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(ctx.resources, R.raw.berst_loading))
                                setImageDrawable(d)
                                (d as? AnimatedImageDrawable)?.apply { repeatCount = AnimatedImageDrawable.REPEAT_INFINITE; start() }
                                animated = true
                            }
                        }
                        if (!animated) setImageResource(R.drawable.berst_loading_still)
                    }
                },
                modifier = Modifier.width(280.dp).aspectRatio(437f / 389f),
            )
            Spacer(Modifier.height(18.dp))
            Text("Berst", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Loading your music\u2026", color = Color.White.copy(.65f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(22.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(140.dp).height(4.dp),
                color = Violet, trackColor = Accent.copy(.2f),
            )
        }
    }
}
