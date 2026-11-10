package com.metrolist.music.raq.ui

import kotlinx.coroutines.flow.StateFlow

data class Track(val id: String, val title: String, val artist: String, val coverUrl: String?)
data class PlaylistItem(val id: String, val name: String, val count: Int, val coverUrl: String?)
data class QueueItem(val index: Int, val title: String, val artist: String, val coverUrl: String?, val current: Boolean)
data class PlayerUi(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 1,
    val liked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
)

/**
 * JEMBATAN ke Metrolist. Implementasikan pakai kode Metrolist:
 *  player        -> PlayerConnection (ExoPlayer / MediaSession)
 *  search        -> YouTube.search(...) dari modul :innertube
 *  home/library  -> YouTube.home() / MusicDatabase (Room)
 *  lyrics        -> LyricsHelper (LrcLib / KuGou)
 */
interface MusicBridge {
    val player: StateFlow<PlayerUi>
    val madeForYou: StateFlow<List<PlaylistItem>>
    val recentlyPlayed: StateFlow<List<Track>>
    val library: StateFlow<List<PlaylistItem>>
    val trending: StateFlow<List<Track>>
    val queue: StateFlow<List<QueueItem>>
    val queueTitle: StateFlow<String?>
    suspend fun search(query: String): List<Track>
    suspend fun playlistTracks(id: String): List<Track>
    suspend fun lyrics(): String?
    fun createPlaylist(name: String)
    fun play(track: Track)
    fun playTracks(tracks: List<Track>, start: Int = 0, shuffle: Boolean = false)
    fun playQueueIndex(index: Int)
    fun togglePlay()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun toggleLike()
    fun toggleShuffle()
    fun toggleRepeat()
    val sleepTimerEndsAt: StateFlow<Long?>
    fun setSleepTimer(minutes: Int?)
    fun audioSessionId(): Int
    fun clearHistory()
    fun playbackSpeed(): Float
    fun setPlaybackSpeed(speed: Float)
}
