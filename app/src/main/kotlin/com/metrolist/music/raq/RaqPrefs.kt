package com.metrolist.music.raq

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Penyimpanan pengaturan khusus UI RaqMusic (SharedPreferences "raq_ui"). */
object RaqPrefs {
    const val FILE = "raq_ui"
    const val AUTO_LYRICS = "auto_lyrics"
    const val STARTUP_TAB = "startup_tab"

    fun of(ctx: Context): SharedPreferences = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

private class PersistedState<T>(initial: T, private val save: (T) -> Unit) : MutableState<T> {
    private val state = mutableStateOf(initial)
    override var value: T
        get() = state.value
        set(v) {
            state.value = v
            save(v)
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

@Composable
fun rememberRaqBool(key: String, default: Boolean): MutableState<Boolean> {
    val p = RaqPrefs.of(LocalContext.current)
    return remember(key) { PersistedState(p.getBoolean(key, default)) { p.edit().putBoolean(key, it).apply() } }
}

@Composable
fun rememberRaqString(key: String, default: String): MutableState<String> {
    val p = RaqPrefs.of(LocalContext.current)
    return remember(key) { PersistedState(p.getString(key, default) ?: default) { p.edit().putString(key, it).apply() } }
}
