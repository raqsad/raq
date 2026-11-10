package com.metrolist.music.raq

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import com.metrolist.music.playback.MusicService
import com.metrolist.music.raq.island.DynamicIslandService

/**
 * Dijalankan sekali saat app dibuka. Saat app disapu dari recents ("bersihkan RAM"),
 * Android memanggil [onTaskRemoved]; di sini semuanya dimatikan total:
 * Dynamic Island, MusicService (pemutar + notifikasi), lalu proses app dibunuh
 * supaya tidak ada yang tersisa di background.
 */
class TaskCleanupService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching { DynamicIslandService.bridge?.let { if (it.player.value.isPlaying) it.togglePlay() } }
        runCatching { stopService(Intent(this, DynamicIslandService::class.java)) }
        runCatching { stopService(Intent(this, MusicService::class.java)) }
        // Beri jeda singkat supaya Activity & service sempat unbind / menyimpan antrean, lalu matikan proses.
        Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 400)
        super.onTaskRemoved(rootIntent)
    }
}
