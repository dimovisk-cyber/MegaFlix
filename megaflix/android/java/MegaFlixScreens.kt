package com.nuvio.tv.megaflix

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.WatchProgress
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ---------------------------------------------------------------- tokens (Netflix DS, orange accent)

private val MfxOrange = Color(0xFFFF8000)
private val MfxText = Color(0xFFE5E5E5)
private val MfxMuted = Color(0xFF808080)
private val MfxCard = Color(0xFF1A1A1A)

/** Minimal line icons used by the MegaFlix tabs and buttons. */
object MegaFlixIcons {
    val Favorites: ImageVector get() = Icons.Outlined.FavoriteBorder
    val History: ImageVector get() = Icons.Filled.History
    val Like: ImageVector get() = Icons.Outlined.ThumbUp
    val Liked: ImageVector get() = Icons.Filled.ThumbUp
    val Channels: ImageVector get() = Icons.Outlined.LiveTv
    val Sports: ImageVector get() = Icons.Outlined.SportsSoccer
}

object MegaFlixRoutes {
    const val FAVORITES = "megaflix_favorites"
    const val HISTORY = "megaflix_history"
    const val CHANNELS = "megaflix_channels"
    const val SPORTS = "megaflix_sports"
}

data class MfxPoster(
    val id: String,
    val type: String,
    val name: String,
    val image: String?,
    val subtitle: String? = null,
    val progress: Float? = null
)

// ---------------------------------------------------------------- shared pieces

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MegaFlixPosterCard(item: MfxPoster, onClick: () -> Unit, landscape: Boolean = false) {
    val shape = RoundedCornerShape(4.dp)
    Column(modifier = Modifier.width(if (landscape) 272.dp else 150.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (landscape) 16f / 9f else 2f / 3f),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MfxCard,
                focusedContainerColor = MfxCard
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = shape)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!item.image.isNullOrBlank()) {
                    AsyncImage(
                        model = item.image,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = item.name,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp)
                    )
                }
                val progress = item.progress
                if (progress != null && progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0x66FFFFFF))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(4.dp)
                            .background(MfxOrange)
                    )
                }
            }
        }
        Text(
            text = item.name,
            color = MfxText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
        item.subtitle?.let {
            Text(
                text = it,
                color = MfxMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MfxRow(
    title: String,
    posters: List<MfxPoster>,
    landscape: Boolean = false,
    onOpen: (String, String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = MfxText,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
        ) {
            items(posters, key = { it.id + "|" + (it.subtitle ?: "") }) { poster ->
                MegaFlixPosterCard(item = poster, landscape = landscape, onClick = { onOpen(poster.id, poster.type) })
            }
        }
    }
}

@Composable
private fun MfxHeader(title: String, subtitle: String?) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
        if (subtitle != null) {
            Text(text = subtitle, color = MfxMuted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MfxEmpty(text: String) {
    Text(
        text = text,
        color = MfxMuted,
        fontSize = 17.sp,
        modifier = Modifier.padding(vertical = 24.dp)
    )
}

// ---------------------------------------------------------------- Favoritos

@Composable
fun MegaFlixFavoritesScreen(onOpen: (String, String) -> Unit) {
    val context = LocalContext.current
    remember(Unit) { MegaFlixTaste.ensureLoaded(context); true }
    val liked by MegaFlixTaste.liked.collectAsState()
    var rows by remember { mutableStateOf<List<Pair<String, List<MfxPoster>>>>(emptyList()) }
    val seedKey = liked.take(MegaFlixRecommender.SEEDS).joinToString { it.id }
    LaunchedEffect(seedKey) {
        rows = if (liked.isEmpty()) emptyList()
        else withContext(Dispatchers.IO) { runCatching { MegaFlixRecommender.rows(liked) }.getOrDefault(emptyList()) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "header") {
            MfxHeader("Favoritos", "Títulos que você curtiu e sugestões parecidas")
        }
        if (liked.isEmpty()) {
            item(key = "empty") {
                MfxEmpty("Abra um filme ou série e toque em Curtir. Seus favoritos aparecem aqui e o MegaFlix passa a sugerir títulos parecidos.")
            }
        } else {
            item(key = "liked") {
                MfxRow(
                    title = "Seus favoritos",
                    posters = liked.map { MfxPoster(it.id, it.type, it.name, it.poster) },
                    onOpen = onOpen
                )
            }
        }
        rows.forEach { (title, recs) ->
            item(key = "rec:$title") { MfxRow(title = title, posters = recs, onOpen = onOpen) }
        }
    }
}

/** "Algorithm": TMDB recommendations of the latest liked titles, ranked by how many likes point to them. */
object MegaFlixRecommender {
    const val SEEDS = 5
    private const val IMG = "https://image.tmdb.org/t/p/w342"

    fun rows(liked: List<LikedItem>): List<Pair<String, List<MfxPoster>>> {
        val key = com.nuvio.tv.BuildConfig.TMDB_API_KEY
        if (key.isBlank()) return emptyList()
        val likedIds = liked.map { it.id }.toSet()
        val perSeed = mutableListOf<Pair<LikedItem, List<MfxPoster>>>()
        val score = linkedMapOf<String, Pair<MfxPoster, Double>>()
        liked.take(SEEDS).forEachIndexed { index, seed ->
            val recs = recommendationsFor(seed, key).filter { it.id !in likedIds }
            if (recs.isEmpty()) return@forEachIndexed
            perSeed += seed to recs
            val seedWeight = 1.0 / (1 + index * 0.35)             // newer likes count more
            recs.forEachIndexed { rank, poster ->
                val gain = seedWeight * (1.0 - rank / 40.0)
                val previous = score[poster.id]
                score[poster.id] = poster to ((previous?.second ?: 0.0) + gain)
            }
        }
        val out = mutableListOf<Pair<String, List<MfxPoster>>>()
        val top = score.values.sortedByDescending { it.second }.map { it.first }.take(24)
        if (top.isNotEmpty()) out += "Recomendados para você" to top
        perSeed.forEach { (seed, recs) -> out += "Porque você curtiu ${seed.name}" to recs.take(20) }
        return out
    }

    private fun recommendationsFor(seed: LikedItem, key: String): List<MfxPoster> {
        val tv = seed.type == "series" || seed.type == "tv"
        val tmdb = tmdbId(seed.id, tv, key) ?: return emptyList()
        val kind = if (tmdb.second) "tv" else "movie"
        val json = get("https://api.themoviedb.org/3/$kind/${tmdb.first}/recommendations?language=pt-BR&page=1&api_key=$key")
            ?: return emptyList()
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val r = results.optJSONObject(i) ?: return@mapNotNull null
            val id = r.optInt("id", 0).takeIf { it > 0 } ?: return@mapNotNull null
            val isTv = r.optString("media_type", kind) == "tv"
            val name = r.optString(if (isTv) "name" else "title").ifBlank { r.optString("original_title", r.optString("original_name")) }
            val poster = r.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }?.let { IMG + it }
            MfxPoster(
                id = "tmdb:$id",
                type = if (isTv) "series" else "movie",
                name = name,
                image = poster
            )
        }
    }

    /** (tmdbId, isTv) for an IMDb or tmdb: id. */
    private fun tmdbId(id: String, tv: Boolean, key: String): Pair<Int, Boolean>? {
        if (id.startsWith("tmdb:")) {
            return id.removePrefix("tmdb:").substringBefore(':').toIntOrNull()?.let { it to tv }
        }
        if (!id.startsWith("tt")) return null
        val json = get("https://api.themoviedb.org/3/find/${id.substringBefore(':')}?external_source=imdb_id&api_key=$key")
            ?: return null
        val primary = json.optJSONArray(if (tv) "tv_results" else "movie_results")
        primary?.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 }?.let { return it to tv }
        val other = json.optJSONArray(if (tv) "movie_results" else "tv_results")
        return other?.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 }?.let { it to !tv }
    }

    private fun get(url: String): JSONObject? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        try {
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

// ---------------------------------------------------------------- Assistidos

@HiltViewModel
class MegaFlixHistoryViewModel @Inject constructor(
    repository: WatchProgressRepository
) : ViewModel() {
    val items: StateFlow<List<WatchProgress>> = repository.allProgress
        .map { all -> all.sortedByDescending { it.lastWatched }.distinctBy { it.contentId }.take(80) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun MegaFlixHistoryScreen(
    onOpen: (String, String) -> Unit,
    viewModel: MegaFlixHistoryViewModel = hiltViewModel()
) {
    val history by viewModel.items.collectAsState()
    val inProgress = history.filter { it.isInProgress() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "header") {
            MfxHeader("Assistidos", "Continue de onde parou ou reveja o que viu recentemente")
        }
        if (history.isEmpty()) {
            item(key = "empty") { MfxEmpty("Nada por aqui ainda. O que você assistir aparece nesta aba.") }
        }
        if (inProgress.isNotEmpty()) {
            item(key = "continue") {
                MfxRow(
                    title = "Continuar assistindo",
                    posters = inProgress.map { it.toPoster(landscape = true) },
                    landscape = true,
                    onOpen = onOpen
                )
            }
        }
        if (history.isNotEmpty()) {
            item(key = "recent") {
                MfxRow(
                    title = "Assistidos recentemente",
                    posters = history.map { it.toPoster(landscape = false) },
                    onOpen = onOpen
                )
            }
        }
    }
}

private fun WatchProgress.toPoster(landscape: Boolean): MfxPoster {
    val label = if (season != null && episode != null) {
        "T$season E$episode" + (episodeTitle?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
    } else null
    val remainingMin = (remainingTime / 60_000L).toInt()
    val subtitle = when {
        isInProgress() && remainingMin > 0 -> listOfNotNull(label, "faltam $remainingMin min").joinToString(" · ")
        else -> label
    }
    return MfxPoster(
        id = contentId,
        type = contentType,
        name = name,
        image = if (landscape) (backdrop ?: poster) else (poster ?: backdrop),
        subtitle = subtitle,
        progress = if (isInProgress()) progressPercentage else null
    )
}
