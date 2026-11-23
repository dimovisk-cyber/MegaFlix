package com.nuvio.tv.megaflix

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.domain.model.AddonStreams
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A title the user liked ("Curtir"). Liked titles fill the Favoritos tab and steer recommendations. */
data class LikedItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val backdrop: String?,
    val genres: List<String>,
    val likedAt: Long
)

/** Local store of liked titles, per device. */
object MegaFlixTaste {
    private const val PREFS = "megaflix_taste"
    private const val KEY = "liked"

    private val state = MutableStateFlow<List<LikedItem>>(emptyList())
    val liked: StateFlow<List<LikedItem>> = state.asStateFlow()

    @Volatile
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            state.value = read(context.applicationContext)
            loaded = true
        }
    }

    fun isLiked(id: String): Boolean = state.value.any { it.id == id }

    fun toggle(context: Context, meta: Meta) {
        toggleItem(
            context,
            LikedItem(
                id = meta.id,
                type = meta.apiType,
                name = meta.name,
                poster = meta.poster,
                backdrop = meta.background,
                genres = meta.genres,
                likedAt = System.currentTimeMillis()
            )
        )
    }

    fun toggleItem(context: Context, item: LikedItem) {
        ensureLoaded(context)
        val current = state.value
        val next = if (current.any { it.id == item.id }) {
            current.filterNot { it.id == item.id }
        } else {
            listOf(item.copy(likedAt = System.currentTimeMillis())) + current
        }
        state.value = next
        write(context.applicationContext, next)
    }

    private fun read(context: Context): List<LikedItem> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            val genres = o.optJSONArray("genres")
            LikedItem(
                id = o.optString("id"),
                type = o.optString("type", "movie"),
                name = o.optString("name"),
                poster = o.optString("poster").takeIf { it.isNotBlank() },
                backdrop = o.optString("backdrop").takeIf { it.isNotBlank() },
                genres = if (genres == null) emptyList() else (0 until genres.length()).map { genres.optString(it) },
                likedAt = o.optLong("likedAt")
            )
        }.filter { it.id.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun write(context: Context, items: List<LikedItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("type", item.type)
                    .put("name", item.name)
                    .put("poster", item.poster ?: "")
                    .put("backdrop", item.backdrop ?: "")
                    .put("genres", JSONArray(item.genres))
                    .put("likedAt", item.likedAt)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}

/** True while [id] is liked; recomposes when the user taps Curtir. */
@Composable
fun rememberIsLiked(id: String): Boolean {
    val context = LocalContext.current
    remember(Unit) { MegaFlixTaste.ensureLoaded(context); true }
    val liked by MegaFlixTaste.liked.collectAsState()
    return liked.any { it.id == id }
}

/** Adds "YouCine" as a second option at the end of the source list of a movie or episode. */
object MegaFlixYouCine {
    const val SOURCE_NAME = "YouCine"

    fun isInstalled(context: Context): Boolean = YouCineLinkActivity.findYouCine(context) != null

    fun append(
        context: Context,
        type: String,
        videoId: String,
        season: Int?,
        episode: Int?,
        groups: List<AddonStreams>
    ): List<AddonStreams> {
        if (type != "movie" && type != "series") return groups
        if (groups.any { it.addonName == SOURCE_NAME }) return groups
        val baseId = when {
            videoId.startsWith("tmdb:") -> "tmdb:" + videoId.removePrefix("tmdb:").substringBefore(':')
            videoId.startsWith("tt") -> videoId.substringBefore(':')
            else -> return groups
        }
        val installed = runCatching { isInstalled(context) }.getOrDefault(false)
        val uri = Uri.Builder()
            .scheme("megaflix")
            .authority("youcine")
            .path("play")
            .appendQueryParameter("type", type)
            .appendQueryParameter("id", baseId)
            .apply {
                if (season != null && episode != null) {
                    appendQueryParameter("s", season.toString())
                    appendQueryParameter("e", episode.toString())
                }
            }
            .build()
            .toString()
        val stream = Stream(
            name = "YouCine",
            title = "Assistir no YouCine",
            description = if (installed) "Dublado e legendado · abre direto no título, em tela cheia"
            else "Toque para instalar o YouCine (vem junto com o MegaFlix)",
            url = null,
            ytId = null,
            infoHash = null,
            fileIdx = null,
            externalUrl = uri,
            behaviorHints = null,
            addonName = SOURCE_NAME,
            addonLogo = null
        )
        return groups + AddonStreams(addonName = SOURCE_NAME, addonLogo = null, streams = listOf(stream))
    }
}
