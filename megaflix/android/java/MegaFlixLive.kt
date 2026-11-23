package com.nuvio.tv.megaflix

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.domain.model.enabledAddons
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.domain.repository.CatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val LiveOrange = Color(0xFFFF8000)
private val LiveRed = Color(0xFFE50914)
private val LiveText = Color(0xFFE5E5E5)
private val LiveMuted = Color(0xFF8C8C8C)
private val LiveCard = Color(0xFF1A1A1A)

// ================================================================ models

data class MfxChannel(
    val id: String,
    val type: String,
    val name: String,
    val logo: String?,
    val genres: List<String>,
    val addonBaseUrl: String,
    val catalogName: String
)

data class MfxGame(
    val id: String,
    val leagueName: String,
    val hints: List<String>,
    val home: String,
    val away: String,
    val homeLogo: String?,
    val awayLogo: String?,
    val homeScore: String,
    val awayScore: String,
    val state: String,          // pre | in | post
    val detail: String,         // 45'+5', Intervalo, Encerrado
    val startMs: Long,
    val broadcasts: List<String>
) {
    val title: String get() = "$home x $away"
    val isLive: Boolean get() = state == "in"
    val timeLabel: String
        get() = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getDefault() }.format(Date(startMs))
    val badge: String
        get() = when (state) {
            "in" -> "AO VIVO $detail".trim()
            "post" -> "ENCERRADO"
            else -> "HOJE $timeLabel"
        }
}

/** Channel groups, in order; a channel goes to the first group its name/genre/catalog matches. */
private val CHANNEL_GROUPS = listOf(
    "Abertos" to Regex("(?i).*(abertos|globo|record|sbt|band(?! ?sports)|redetv|cultura).*"),
    "Esportes" to Regex("(?i).*(esporte|sportv|espn|premiere|dazn|ppv|nba|campeonato|futsal|combate|ufc|tnt sports|band ?sports|xsports|caze).*"),
    "Filmes e séries" to Regex("(?i).*(telecine|hbo|max|paramount|apple|disney|filme|s[eé]rie|star|universal|warner|amc|fx|axn|sony|space|tnt|megapix|cinemax).*"),
    "Infantil" to Regex("(?i).*(infantil|kids|desenho|crian|cartoon|nick|gloob|discovery kids).*"),
    "Notícias" to Regex("(?i).*(not[ií]cia|news|jornal|cnn|globonews|bandnews).*"),
    "Documentários" to Regex("(?i).*(document|discovery|history|national|animal planet|nat geo).*"),
    "Variedades e reality" to Regex("(?i).*(variedade|reality|bbb|a fazenda|gnt|multishow|viva|tlc|home).*"),
    "Religiosos" to Regex("(?i).*(religi|gospel|cat[oó]lic|can[cç][aã]o nova|aparecida).*")
)

private const val SPORTS_GROUP = "Esportes"

private fun MfxChannel.group(): String {
    val hay = (listOf(name, catalogName) + genres).joinToString(" ")
    return CHANNEL_GROUPS.firstOrNull { it.second.matches(hay) }?.first ?: "Outros canais"
}

private fun norm(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        .lowercase(Locale.ROOT).replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

// ================================================================ data: addons channels + ESPN scoreboard

@HiltViewModel
class MegaFlixLiveViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val channelState = MutableStateFlow<List<MfxChannel>>(emptyList())
    val channels: StateFlow<List<MfxChannel>> = channelState.asStateFlow()

    private val channelsLoading = MutableStateFlow(true)
    val loadingChannels: StateFlow<Boolean> = channelsLoading.asStateFlow()

    private val gameState = MutableStateFlow<List<MfxGame>?>(null)
    val games: StateFlow<List<MfxGame>?> = gameState.asStateFlow()

    private var sportsStarted = false

    init {
        viewModelScope.launch { loadChannels() }
    }

    private suspend fun loadChannels() {
        channelsLoading.value = true
        val addons = runCatching { addonRepository.getInstalledAddons().first().enabledAddons() }.getOrDefault(emptyList())
        val jobs = addons.flatMap { addon ->
            addon.catalogs
                .filter { (it.apiType == "tv" || it.apiType == "channel") && it.extraRequired.isEmpty() }
                .take(12)
                .map { catalog -> addon to catalog }
        }.take(30)
        val result = withContext(Dispatchers.IO) {
            jobs.map { (addon, catalog) ->
                async {
                    val row = withTimeoutOrNull(20_000) {
                        catalogRepository.getCatalog(
                            addonBaseUrl = addon.baseUrl,
                            addonId = addon.id,
                            addonName = addon.displayName,
                            catalogId = catalog.id,
                            catalogName = catalog.name,
                            type = catalog.apiType
                        ).first { it !is NetworkResult.Loading }
                    }
                    val data = (row as? NetworkResult.Success)?.data ?: return@async emptyList<MfxChannel>()
                    data.items.map { item ->
                        MfxChannel(
                            id = item.id,
                            type = item.rawType,
                            name = item.name,
                            logo = item.logo ?: item.poster,
                            genres = item.genres,
                            addonBaseUrl = addon.baseUrl,
                            catalogName = catalog.name
                        )
                    }
                }
            }.awaitAll().flatten()
        }
        channelState.value = result.distinctBy { norm(it.name) }
        channelsLoading.value = false
    }

    /** Loads today's games and refreshes the scores every minute while the screen is open. */
    fun startSports() {
        if (sportsStarted) return
        sportsStarted = true
        viewModelScope.launch {
            while (true) {
                val list = withContext(Dispatchers.IO) { runCatching { Scoreboard.today() }.getOrNull() }
                if (list != null) gameState.value = list
                delay(60_000)
            }
        }
    }

    fun channelsFor(game: MfxGame): List<MfxChannel> {
        val terms = (game.broadcasts.map { it.replace(Regex("(?i)\\s*(brasil|brazil|hd)$"), "").trim() } + game.hints)
            .map { norm(it) }.filter { it.length >= 2 }.distinct()
        val sports = channelState.value.filter { it.group() == SPORTS_GROUP }
        val matched = sports.filter { ch -> terms.any { t -> norm(ch.name).contains(t) } }
        return (matched + sports).distinctBy { it.id }.take(30)
    }
}

/** Today's games from ESPN's public scoreboard (no key). */
object Scoreboard {
    /** sport, league slug, display name, Brazilian broadcasters to look for */
    private val LEAGUES = listOf(
        listOf("soccer", "bra.1", "Brasileirão Série A", "Premiere|SporTV|Globo|Record|Prime|Caze"),
        listOf("soccer", "bra.2", "Brasileirão Série B", "Premiere|SporTV|Band|ESPN|Disney"),
        listOf("soccer", "bra.copa_do_brazil", "Copa do Brasil", "Premiere|SporTV|Globo|Prime"),
        listOf("soccer", "conmebol.libertadores", "Libertadores", "ESPN|Globo|Paramount|SporTV|Disney"),
        listOf("soccer", "conmebol.sudamericana", "Sul-Americana", "ESPN|Paramount|SBT|Disney"),
        listOf("soccer", "uefa.champions", "Champions League", "TNT|Max|SBT|Prime"),
        listOf("soccer", "uefa.europa", "Liga Europa", "ESPN|Disney"),
        listOf("soccer", "eng.1", "Premier League", "ESPN|Disney"),
        listOf("soccer", "esp.1", "La Liga", "ESPN|Disney"),
        listOf("soccer", "ita.1", "Série A Italiana", "ESPN|Disney"),
        listOf("soccer", "ger.1", "Bundesliga", "SporTV|OneFootball"),
        listOf("soccer", "fra.1", "Ligue 1", "DAZN|Caze"),
        listOf("soccer", "por.1", "Liga Portugal", "ESPN|Disney"),
        listOf("soccer", "arg.1", "Campeonato Argentino", "ESPN|Disney"),
        listOf("basketball", "nba", "NBA", "NBA|ESPN|Prime"),
        listOf("football", "nfl", "NFL", "ESPN|Disney")
    )

    suspend fun today(): List<MfxGame> = kotlinx.coroutines.coroutineScope {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = TimeZone.getDefault() }.format(Date())
        LEAGUES.map { lg ->
            async(Dispatchers.IO) {
                val url = "https://site.api.espn.com/apis/site/v2/sports/${lg[0]}/${lg[1]}/scoreboard?dates=$date&lang=pt&region=br"
                runCatching { parse(get(url), lg) }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten().sortedBy { it.startMs }
    }

    private fun parse(json: JSONObject, lg: List<String>): List<MfxGame> {
        val events = json.optJSONArray("events") ?: return emptyList()
        val out = ArrayList<MfxGame>()
        for (i in 0 until events.length()) {
            runCatching {
                val e = events.getJSONObject(i)
                val comp = e.getJSONArray("competitions").getJSONObject(0)
                var home: String? = null; var away: String? = null
                var homeLogo: String? = null; var awayLogo: String? = null
                var homeScore = "0"; var awayScore = "0"
                val teams = comp.getJSONArray("competitors")
                for (t in 0 until teams.length()) {
                    val ct = teams.getJSONObject(t)
                    val team = ct.optJSONObject("team")
                    val name = team?.optString("shortDisplayName")?.takeIf { it.isNotBlank() }
                        ?: team?.optString("displayName") ?: "?"
                    val logo = team?.optString("logo")?.takeIf { it.isNotBlank() }
                    val score = ct.optString("score", "0")
                    if (ct.optString("homeAway") == "home") { home = name; homeLogo = logo; homeScore = score }
                    else { away = name; awayLogo = logo; awayScore = score }
                }
                val status = e.optJSONObject("status")
                val type = status?.optJSONObject("type")
                val state = type?.optString("state", "pre") ?: "pre"
                val broadcasts = ArrayList<String>()
                comp.optJSONArray("broadcasts")?.let { bc ->
                    for (b in 0 until bc.length()) {
                        val names = bc.getJSONObject(b).optJSONArray("names") ?: continue
                        for (n in 0 until names.length()) broadcasts += names.optString(n)
                    }
                }
                if (home != null && away != null) {
                    out += MfxGame(
                        id = e.optString("id"),
                        leagueName = lg[2],
                        hints = lg[3].split("|"),
                        home = home!!, away = away!!,
                        homeLogo = homeLogo, awayLogo = awayLogo,
                        homeScore = homeScore, awayScore = awayScore,
                        state = state,
                        detail = translate(type?.optString("shortDetail").orEmpty(), status?.optString("displayClock").orEmpty(), state),
                        startMs = parseDate(e.optString("date")),
                        broadcasts = broadcasts.distinct()
                    )
                }
            }
        }
        return out
    }

    private fun parseDate(d: String): Long {
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")) {
            runCatching {
                return SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(d)!!.time
            }
        }
        return 0L
    }

    private fun translate(shortDetail: String, clock: String, state: String): String {
        val s = shortDetail.lowercase(Locale.US)
        return when {
            state == "post" -> "Encerrado"
            state == "pre" -> ""
            s.contains("half") -> "Intervalo"
            s.contains("penal") -> "Pênaltis"
            s.contains("et") && s.contains("'") -> "Prorrogação $clock"
            clock.isNotBlank() && clock != "0'" -> clock
            else -> ""
        }
    }

    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        try {
            return c.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            c.disconnect()
        }
    }
}

// ================================================================ YouCine sections

private fun openYouCineSection(context: Context, name: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("megaflix://youcine/section?name=$name"))
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// ================================================================ shared UI

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MfxCta(text: String, onClick: () -> Unit, primary: Boolean = true) {
    Button(
        onClick = onClick,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) LiveOrange else Color(0xB36D6D6E),
            contentColor = if (primary) Color.Black else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun LiveTitle(text: String) {
    Text(
        text = text,
        color = LiveText,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun Billboard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    0f to Color.Black, 0.55f to Color(0xFF1A0C00), 1f to Color(0xFF6B3000)
                )
            )
            .padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 28.dp)
    ) { content() }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelCard(channel: MfxChannel, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(modifier = Modifier.width(200.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(112.dp),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(containerColor = LiveCard, focusedContainerColor = Color(0xFF2B1A08)),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                if (!channel.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        Text(
            text = channel.name,
            color = LiveText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ChannelRow(title: String, channels: List<MfxChannel>, onOpen: (MfxChannel) -> Unit) {
    Column {
        LiveTitle(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
        ) {
            items(channels, key = { it.addonBaseUrl + "|" + it.id }) { ch -> ChannelCard(ch) { onOpen(ch) } }
        }
    }
}

// ================================================================ Canais TV

@Composable
fun MegaFlixChannelsScreen(
    onOpenChannel: (id: String, type: String, addonBaseUrl: String) -> Unit,
    viewModel: MegaFlixLiveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsState()
    val loading by viewModel.loadingChannels.collectAsState()
    val hasYouCine = remember { MegaFlixYouCine.isInstalled(context) }
    val groups = remember(channels) {
        val byGroup = channels.groupBy { it.group() }
        (CHANNEL_GROUPS.map { it.first } + "Outros canais").mapNotNull { g -> byGroup[g]?.let { g to it } }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "bill") {
            Billboard {
                Column {
                    Text("AO VIVO", color = LiveOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Canais de TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 44.sp)
                    Text(
                        text = if (hasYouCine) "Addons  •  YouCine" else "Canais dos seus addons",
                        color = LiveText, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Escolha um canal: ele abre na página do canal, é só apertar Assistir.",
                        color = LiveMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                    )
                    if (hasYouCine) MfxCta("Canais do YouCine", { openYouCineSection(context, "aovivo") })
                }
            }
        }
        if (channels.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = if (loading) "Carregando canais…"
                    else "Nenhum addon de canais instalado. Em Configurações > Addons, adicione um addon de TV (ex.: FrostStream).",
                    color = LiveMuted, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }
        groups.forEach { (name, list) ->
            item(key = "g:$name") {
                Box(Modifier.padding(start = 48.dp)) {
                    ChannelRow(name, list) { ch -> onOpenChannel(ch.id, ch.type, ch.addonBaseUrl) }
                }
            }
        }
    }
}

// ================================================================ Esportes (jogos do dia com placar)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GameCard(game: MfxGame, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.width(300.dp).height(170.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = LiveCard, focusedContainerColor = Color(0xFF2B1A08)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.leagueName,
                    color = LiveMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = game.badge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(if (game.isLive) LiveRed else Color(0xFF333333), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Crest(game.homeLogo, 56)
                Text(
                    text = if (game.state == "pre") "x" else "${game.homeScore}  x  ${game.awayScore}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
                Crest(game.awayLogo, 56)
            }
            Text(
                text = game.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Crest(url: String?, size: Int) {
    Box(modifier = Modifier.size(size.dp)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GameRow(title: String, games: List<MfxGame>, onPick: (MfxGame) -> Unit) {
    Column {
        LiveTitle(title)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
        ) {
            items(games, key = { it.id }) { g -> GameCard(g) { onPick(g) } }
        }
    }
}

@Composable
fun MegaFlixSportsScreen(
    onOpenChannel: (id: String, type: String, addonBaseUrl: String) -> Unit,
    viewModel: MegaFlixLiveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.startSports() }
    val games by viewModel.games.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val hasYouCine = remember { MegaFlixYouCine.isInstalled(context) }
    var picked by remember { mutableStateOf<MfxGame?>(null) }

    val all = games.orEmpty()
    val live = all.filter { it.state == "in" }
    val next = all.filter { it.state == "pre" }
    val done = all.filter { it.state == "post" }
    val featured = live.firstOrNull() ?: next.firstOrNull()
    val sportsChannels = remember(channels) { channels.filter { it.group() == SPORTS_GROUP } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item(key = "bill") {
            Billboard {
                Column {
                    if (featured != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Crest(featured.homeLogo, 84)
                            Text("x", color = Color.White, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            Crest(featured.awayLogo, 84)
                        }
                        Text(
                            text = if (featured.isLive) "AO VIVO  ${featured.detail}".trim() else "HOJE ${featured.timeLabel}",
                            color = if (featured.isLive) Color(0xFFFF4040) else LiveOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                        Text(
                            text = featured.title + if (featured.state == "pre") "" else "   ${featured.homeScore}  x  ${featured.awayScore}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 46.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(featured.leagueName, color = LiveText, fontSize = 19.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = if (featured.broadcasts.isEmpty()) "Escolha o canal e assista em tela cheia."
                            else "Transmissão: " + featured.broadcasts.joinToString(", "),
                            color = LiveMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        MfxCta("▶  Onde assistir", { picked = featured })
                    } else {
                        Text("ESPORTES", color = LiveOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Jogos de hoje", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 44.sp)
                        Text(
                            text = when {
                                games == null -> "Carregando os jogos…"
                                all.isEmpty() -> "Nenhum jogo hoje nos campeonatos acompanhados. Veja os canais de esporte abaixo."
                                else -> "Os jogos de hoje já terminaram. Veja os canais de esporte abaixo."
                            },
                            color = LiveMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        if (hasYouCine) MfxCta("Esportes no YouCine", { openYouCineSection(context, "esportes") })
                    }
                }
            }
        }
        if (live.isNotEmpty()) item(key = "live") { Box(Modifier.padding(start = 48.dp)) { GameRow("Ao vivo agora", live) { g -> picked = g } } }
        if (next.isNotEmpty()) item(key = "next") { Box(Modifier.padding(start = 48.dp)) { GameRow("Mais tarde hoje", next) { g -> picked = g } } }
        if (done.isNotEmpty()) item(key = "done") { Box(Modifier.padding(start = 48.dp)) { GameRow("Encerrados hoje", done) { g -> picked = g } } }
        if (sportsChannels.isNotEmpty()) {
            item(key = "channels") {
                Box(Modifier.padding(start = 48.dp)) {
                    ChannelRow("Canais de esporte", sportsChannels) { ch -> onOpenChannel(ch.id, ch.type, ch.addonBaseUrl) }
                }
            }
        }
    }

    picked?.let { game ->
        WhereToWatchDialog(
            game = game,
            channels = remember(game, channels) { viewModel.channelsFor(game) },
            hasYouCine = hasYouCine,
            onDismiss = { picked = null },
            onYouCine = { picked = null; openYouCineSection(context, "esportes") },
            onChannel = { ch -> picked = null; onOpenChannel(ch.id, ch.type, ch.addonBaseUrl) }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WhereToWatchDialog(
    game: MfxGame,
    channels: List<MfxChannel>,
    hasYouCine: Boolean,
    onDismiss: () -> Unit,
    onYouCine: () -> Unit,
    onChannel: (MfxChannel) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .background(Color(0xFF141414), RoundedCornerShape(8.dp))
                .padding(28.dp)
        ) {
            Text(game.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                text = listOfNotNull(game.leagueName, game.badge, game.broadcasts.takeIf { it.isNotEmpty() }?.joinToString(", "))
                    .joinToString("  •  "),
                color = LiveOrange, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasYouCine) {
                    item(key = "yc") { DialogOption("Esportes no YouCine", "Abre a área de esportes do YouCine", onYouCine) }
                }
                items(channels, key = { it.addonBaseUrl + "|" + it.id }) { ch ->
                    DialogOption(ch.name, ch.catalogName) { onChannel(ch) }
                }
                if (!hasYouCine && channels.isEmpty()) {
                    item(key = "none") {
                        Text(
                            "Nenhum canal de esporte encontrado. Adicione um addon de TV em Configurações > Addons.",
                            color = LiveMuted, fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DialogOption(title: String, subtitle: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF222222),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 13.sp, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
