package com.metrolist.music.raq

import androidx.media3.common.Player
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.raq.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Menyambungkan UI RaqMusic ke mesin Metrolist.
 * Dipakai di MainActivity (di dalam CompositionLocalProvider yang sudah ada):
 *   val pc = LocalPlayerConnection.current ; val db = LocalDatabase.current
 *   val bridge = remember(pc) { MetrolistBridge(pc!!, db, scope) }
 *   AppRoot(bridge)
 */
class MetrolistBridge(
    private val pc: PlayerConnection,
    private val db: MusicDatabase,
    private val scope: CoroutineScope,
) : MusicBridge {

    private val position = MutableStateFlow(0L)

    init {
        scope.launch {
            while (isActive) {
                runCatching { position.value = pc.player.currentPosition }
                delay(500)
            }
        }
    }

    override val player: StateFlow<PlayerUi> = combine(
        pc.mediaMetadata, pc.isPlaying, position, pc.shuffleModeEnabled, pc.repeatMode,
    ) { meta, playing, pos, shuffle, repeat ->
        PlayerUi(
            track = meta?.let { Track(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnailUrl) },
            isPlaying = playing,
            positionMs = pos,
            durationMs = ((meta?.duration ?: 1) * 1000L).coerceAtLeast(1),
            liked = meta?.liked ?: false,
            shuffle = shuffle,
            repeat = repeat != Player.REPEAT_MODE_OFF,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlayerUi())

    override val library: StateFlow<List<PlaylistItem>> =
        combine(db.playlistsByCreateDateAsc(), db.likedSongsCount()) { list, liked ->
            listOf(PlaylistItem("LIKED", "Liked Songs", liked, null)) +
                list.map { PlaylistItem(it.playlist.id, it.playlist.name, it.songCount, it.thumbnails.firstOrNull()) }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val queueTitle: StateFlow<String?> = pc.queueTitle

    override val queue: StateFlow<List<QueueItem>> =
        combine(pc.queueWindows, pc.currentWindowIndex) { wins, cur ->
            wins.mapIndexed { i, w ->
                val m = w.mediaItem.mediaMetadata
                QueueItem(i, m.title?.toString() ?: "-", m.artist?.toString() ?: "", m.artworkUri?.toString(), i == cur)
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val madeForYou: StateFlow<List<PlaylistItem>> = library

    override val recentlyPlayed: StateFlow<List<Track>> = run {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate().atStartOfDay()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        db.historyEvents(now.plusDays(1), today, today.minusDays(1), monday, monday.minusDays(7)).map { events ->
            events.map { it.song.toMediaMetadata() }.distinctBy { it.id }.take(20).map { m ->
                Track(m.id, m.title, m.artists.joinToString { a -> a.name }, m.thumbnailUrl)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _sleepEnd = MutableStateFlow<Long?>(null)
    override val sleepTimerEndsAt: StateFlow<Long?> = _sleepEnd
    private var sleepJob: Job? = null

    override fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes == null) { _sleepEnd.value = null; return }
        val ms = minutes * 60_000L
        _sleepEnd.value = System.currentTimeMillis() + ms
        sleepJob = scope.launch {
            delay(ms)
            withContext(Dispatchers.Main) { pc.player.pause() }
            _sleepEnd.value = null
        }
    }

    override fun audioSessionId(): Int = runCatching { pc.player.audioSessionId }.getOrDefault(0)

    override fun clearHistory() { db.query { clearListenHistory() } }

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    override val trending: StateFlow<List<Track>> = _trending

    init {
        scope.launch(Dispatchers.IO) {
            _trending.value = runCatching { searchInternal("top hits") }.getOrDefault(emptyList()).take(5)
        }
    }

    private suspend fun searchInternal(q: String): List<Track> =
        YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items
            ?.filterIsInstance<SongItem>()
            ?.map { Track(it.id, it.title, it.artists.joinToString { a -> a.name }, it.thumbnail) }
            ?: emptyList()

    override suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) { searchInternal(query) }

    override fun play(track: Track) = playTracks(listOf(track))

    override fun playTracks(tracks: List<Track>, start: Int, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        val items = tracks.map { t ->
            MediaMetadata(
                id = t.id,
                title = t.title,
                artists = listOf(MediaMetadata.Artist(null, t.artist)),
                duration = -1,
                thumbnailUrl = t.coverUrl,
            ).toMediaItem()
        }
        scope.launch(Dispatchers.Main) {
            pc.playQueue(ListQueue(title = tracks[start.coerceIn(0, tracks.lastIndex)].title, items = items, startIndex = start.coerceIn(0, tracks.lastIndex)))
            if (shuffle) pc.player.shuffleModeEnabled = true
        }
    }

    override fun playQueueIndex(index: Int) {
        scope.launch(Dispatchers.Main) { runCatching { pc.player.seekToDefaultPosition(index); pc.player.play() } }
    }

    override suspend fun playlistTracks(id: String): List<Track> = withContext(Dispatchers.IO) {
        val songs = if (id == "LIKED") db.likedSongsByCreateDateAsc().first()
        else db.playlistSongs(id).first().map { it.song }
        songs.map {
            val m = it.toMediaMetadata()
            Track(m.id, m.title, m.artists.joinToString { a -> a.name }, m.thumbnailUrl)
        }
    }

    private fun cleanLyrics(raw: String): String =
        raw.replace(Regex("\\[\\d+:\\d+(\\.\\d+)?]"), "").replace(Regex("<\\d+:\\d+(\\.\\d+)?>"), "")
            .lines().filterNot { it.startsWith("[ar:") || it.startsWith("[ti:") || it.startsWith("[al:") || it.startsWith("[by:") }
            .joinToString("\n") { it.trim() }.trim()

    override suspend fun lyrics(): String? = withContext(Dispatchers.IO) {
        val meta = pc.mediaMetadata.value ?: return@withContext null
        val cached = db.lyrics(meta.id).first()?.lyrics
        if (!cached.isNullOrBlank() && cached != LyricsEntity.LYRICS_NOT_FOUND) return@withContext cleanLyrics(cached)
        runCatching { pc.service.lyricsHelper.getLyrics(meta).lyrics }.getOrNull()
            ?.takeIf { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND }
            ?.let { cleanLyrics(it) }
    }

    override fun createPlaylist(name: String) {
        db.query { insert(PlaylistEntity(name = name)) }
    }

    override fun togglePlay() = pc.togglePlayPause()
    override fun next() = pc.seekToNext()
    override fun previous() = pc.seekToPrevious()
    override fun seekTo(ms: Long) = pc.seekTo(ms)
    override fun toggleLike() = pc.toggleLike()
    override fun playbackSpeed(): Float = pc.player.playbackParameters.speed
    override fun setPlaybackSpeed(speed: Float) { pc.player.setPlaybackSpeed(speed) }
    override fun toggleShuffle() { pc.player.shuffleModeEnabled = !pc.player.shuffleModeEnabled }
    override fun toggleRepeat() {
        pc.player.repeatMode =
            if (pc.player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }
}
