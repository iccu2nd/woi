package com.komikta.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.komikta.app.data.remote.ApiEnvelope
import com.komikta.app.data.remote.KomiktaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF080809)
private val Panel = Color(0xFF171719)
private val Panel2 = Color(0xFF202023)
private val Line = Color(0xFF303034)
private val White = Color(0xFFF2F2F4)
private val Muted = Color(0xFFA1A1A8)
private val Accent = Color(0xFFA0A0A5)
private val Gold = Color(0xFFD8B778)
private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)
private val KomiktaTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Poppins),
        displayMedium = t.displayMedium.copy(fontFamily = Poppins),
        displaySmall = t.displaySmall.copy(fontFamily = Poppins),
        headlineLarge = t.headlineLarge.copy(fontFamily = Poppins),
        headlineMedium = t.headlineMedium.copy(fontFamily = Poppins),
        headlineSmall = t.headlineSmall.copy(fontFamily = Poppins),
        titleLarge = t.titleLarge.copy(fontFamily = Poppins),
        titleMedium = t.titleMedium.copy(fontFamily = Poppins),
        titleSmall = t.titleSmall.copy(fontFamily = Poppins),
        bodyLarge = t.bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = t.bodyMedium.copy(fontFamily = Poppins),
        bodySmall = t.bodySmall.copy(fontFamily = Poppins),
        labelLarge = t.labelLarge.copy(fontFamily = Poppins),
        labelMedium = t.labelMedium.copy(fontFamily = Poppins),
        labelSmall = t.labelSmall.copy(fontFamily = Poppins)
    )
}
private val api: KomiktaApi by lazy { KomiktaApi.create(BuildConfig.API_BASE_URL) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KomiktaApp() }
    }
}

private fun JsonElement?.str(vararg keys: String): String {
    if (this == null || !isJsonObject) return ""
    for (key in keys) {
        val v = asJsonObject.get(key) ?: continue
        if (!v.isJsonNull && v.isJsonPrimitive) return v.asString
    }
    return ""
}
private fun JsonElement?.arr(vararg keys: String): List<JsonElement> {
    if (this == null || !isJsonObject) return emptyList()
    for (key in keys) {
        val v = asJsonObject.get(key)
        if (v != null && v.isJsonArray) return v.asJsonArray.toList()
    }
    return emptyList()
}
private fun JsonElement?.textArray(vararg keys: String): List<String> {
    if (this == null || !isJsonObject) return emptyList()
    for (key in keys) {
        val v = asJsonObject.get(key) ?: continue
        if (v.isJsonArray) return v.asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
    }
    return emptyList()
}
private fun JsonElement?.obj(): JsonObject? = if (this != null && isJsonObject) asJsonObject else null
private fun JsonElement?.cover(): String = str("poster", "cover", "thumbnail", "image")
private fun JsonElement?.url(): String = str("url", "link", "slug")
private fun JsonElement?.title(): String = str("title", "name")
private fun imageUrl(source: String): String {
    if (source.isBlank()) return ""
    val normalized = if (source.startsWith("//")) "https:$source" else source
    // Gunakan proxy backend agar sumber gambar yang memerlukan Referer tetap bisa dimuat.
    return if (normalized.startsWith("https://") && !normalized.startsWith(BuildConfig.API_BASE_URL)) {
        BuildConfig.API_BASE_URL + "api/img?url=" + Uri.encode(normalized)
    } else normalized
}

@Composable
private fun KomiktaApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val context = LocalContext.current
    val saved = remember {
        mutableStateListOf<String>().apply {
            addAll(context.getSharedPreferences("komikta_local", android.content.Context.MODE_PRIVATE).getStringSet("saved_comics", emptySet()) ?: emptySet())
        }
    }
    LaunchedEffect(saved.toList()) {
        context.getSharedPreferences("komikta_local", android.content.Context.MODE_PRIVATE)
            .edit().putStringSet("saved_comics", saved.toSet()).apply()
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, onPrimary = Ink, background = Ink, surface = Panel, onSurface = White, secondary = Muted), typography = KomiktaTypography) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                if (route in listOf("home", "explore", "library", "profile")) {
                    NavigationBar(containerColor = Color(0xF2171719), tonalElevation = 0.dp, modifier = Modifier.height(68.dp)) {
                        val tabs = listOf(Triple("home", "Home", Icons.Default.Home), Triple("explore", "Explore", Icons.Default.Search), Triple("library", "Library", Icons.Default.BookmarkBorder), Triple("profile", "Profile", Icons.Default.PersonOutline))
                        tabs.forEach { (key, label, icon) ->
                            NavigationBarItem(selected = route == key, onClick = { nav.navigate(key) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(icon, null, modifier = Modifier.size(21.dp)) }, label = { Text(label, fontSize = 10.sp, fontWeight = if (route == key) FontWeight.SemiBold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = White, selectedTextColor = White, indicatorColor = Color(0xFF343438), unselectedIconColor = Muted, unselectedTextColor = Muted))
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(onExplore = { nav.navigate("explore") }, onOpen = { nav.navigate("detail/${Uri.encode(it)}") }, saved = saved) }
                composable("explore") { ExploreScreen(onOpen = { nav.navigate("detail/${Uri.encode(it)}") }) }
                composable("library") { LibraryScreen(saved = saved, onOpen = { nav.navigate("detail/${Uri.encode(it)}") }, onExplore = { nav.navigate("explore") }) }
                composable("profile") { ProfileScreen() }
                composable("detail/{sourceUrl}", arguments = listOf(navArgument("sourceUrl") { type = NavType.StringType })) { back ->
                    DetailScreen(sourceUrl = Uri.decode(back.arguments?.getString("sourceUrl").orEmpty()), saved = saved,
                        onBack = { nav.popBackStack() }, onRead = { nav.navigate("reader/${Uri.encode(it)}") })
                }
                composable("reader/{chapterUrl}", arguments = listOf(navArgument("chapterUrl") { type = NavType.StringType })) { back ->
                    ReaderScreen(chapterUrl = Uri.decode(back.arguments?.getString("chapterUrl").orEmpty()), onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun AppHeader(subtitle: String? = null, onSearch: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Image(
                painter = painterResource(id = R.drawable.komikta_wordmark),
                contentDescription = "Komikta",
                modifier = Modifier.width(128.dp).height(48.dp),
                contentScale = ContentScale.Fit
            )
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        if (onSearch != null) Surface(shape = CircleShape, color = Panel2, modifier = Modifier.size(42.dp).clickable { onSearch() }) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Search, "Cari komik", tint = White) }
        }
    }
}

@Composable
private fun HomeScreen(onExplore: () -> Unit, onOpen: (String) -> Unit, saved: List<String>) {
    var feed by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var updates by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val pair = withContext(Dispatchers.IO) { api.home() to runCatching { api.updates() }.getOrNull() }
            feed = pair.first.data?.let { if (it.isJsonArray) it.asJsonArray.toList() else it.arr("items", "data") }.orEmpty()
            updates = pair.second?.data?.let { if (it.isJsonArray) it.asJsonArray.toList() else it.arr("items", "data") }.orEmpty()
            if (feed.isEmpty() && updates.isEmpty()) error = "Belum ada data yang dapat ditampilkan. Periksa koneksi atau alamat backend."
        } catch (e: Exception) { error = e.message ?: "Gagal menghubungkan ke server Komikta." }
        loading = false
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { AppHeader("Baca cerita, temukan dunia.", onExplore) }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF303034), Color(0xFF171719), Color(0xFF242329)))).padding(22.dp)) {
                Column {
                    Surface(color = Color(0x2FFFFFFF), shape = RoundedCornerShape(8.dp)) { Text("  DISCOVER YOUR NEXT STORY  ", color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 7.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Text("Satu chapter lagi.\nSatu dunia baru.", color = White, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Temukan manga, manhwa, dan manhua pilihanmu.", color = Color(0xFFC4C4CA), fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onExplore, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp)) {
                        Text("Jelajahi koleksi", fontWeight = FontWeight.SemiBold); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
                Icon(Icons.Default.AutoStories, null, tint = Color(0x22FFFFFF), modifier = Modifier.align(Alignment.BottomEnd).size(112.dp))
            }
        }
        if (loading) item { LoadingBlock() }
        if (error.isNotBlank() && !loading) item { ErrorPanel(error, onExplore) }
        if (feed.isNotEmpty()) {
            item { SectionHeading("Pilihan untukmu", "Lihat semua", onExplore) }
            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) { lazyItems(feed.take(10)) { item -> PosterCard(item, onClick = { item.url().takeIf(String::isNotBlank)?.let(onOpen) }, width = 132.dp) } } }
        }
        if (updates.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x24C4A574)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Gold, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) { Text("Sorotan chapter", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text("Update terbaru dari koleksi Komikta", color = Muted, fontSize = 10.sp) }
                    Text("Lihat semua", color = Muted, fontSize = 11.sp, modifier = Modifier.clickable { onExplore() }.padding(5.dp))
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    lazyItems(updates.take(12)) { comic ->
                        Row(Modifier.width(286.dp).clip(RoundedCornerShape(16.dp)).background(Panel).clickable { comic.url().takeIf(String::isNotBlank)?.let(onOpen) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = imageUrl(comic.cover()), contentDescription = comic.title(), modifier = Modifier.size(66.dp, 84.dp).clip(RoundedCornerShape(10.dp)).background(Panel2), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(comic.title().ifBlank { "Komik terbaru" }, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Surface(color = Color(0x24C4A574), shape = RoundedCornerShape(7.dp)) { Text(comic.str("chapter", "latest_chapter", "latestChapter").ifBlank { "Chapter terbaru" }, color = Gold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), maxLines = 1) }
                                Text(comic.str("latest_chapter_time", "updated", "time").ifBlank { comic.str("status") }, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            item { SectionHeading("Semua update", "Jelajahi", onExplore) }
            lazyItems(updates.take(18)) { item -> CompactComicRow(item, onClick = { item.url().takeIf(String::isNotBlank)?.let(onOpen) }) }
        }
        if (feed.isNotEmpty() && updates.isEmpty()) {
            item { SectionHeading("Sedang populer", "Lihat semua", onExplore) }
            lazyItems(feed.drop(10).take(8)) { item -> CompactComicRow(item) { item.url().takeIf(String::isNotBlank)?.let(onOpen) } }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.3).sp)
        Text(action, color = Muted, fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onAction() }.padding(6.dp))
    }
}

@Composable
private fun PosterCard(item: JsonElement, onClick: () -> Unit, width: androidx.compose.ui.unit.Dp = 128.dp) {
    Column(Modifier.then(if (width == androidx.compose.ui.unit.Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(width)).clickable { onClick() }) {
        Box(Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(13.dp)).background(Panel2)) {
            val cover = imageUrl(item.cover())
            if (cover.isNotBlank()) AsyncImage(model = cover, contentDescription = item.title(), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoStories, null, tint = Muted, modifier = Modifier.size(36.dp)) }
            val badge = item.str("status", "type", "format")
            if (badge.isNotBlank()) Surface(color = Color(0xD9000000), shape = RoundedCornerShape(bottomEnd = 9.dp), modifier = Modifier.align(Alignment.TopStart)) { Text(badge.uppercase().take(12), color = White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp), maxLines = 1) }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title().ifBlank { "Tanpa judul" }, color = White, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val chapter = item.str("chapter", "latest_chapter", "latestChapter")
        if (chapter.isNotBlank()) Text(chapter, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompactComicRow(item: JsonElement, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).clickable { onClick() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = imageUrl(item.cover()), contentDescription = item.title(), modifier = Modifier.size(66.dp, 84.dp).clip(RoundedCornerShape(9.dp)).background(Panel2), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.title().ifBlank { "Tanpa judul" }, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.str("chapter", "latest_chapter", "latestChapter").ifBlank { item.str("genres", "format", "type").ifBlank { "Komik terbaru" } }, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = item.str("updated", "time", "latest_chapter_time", "status")
            if (meta.isNotBlank()) Text(meta, color = Color(0xFF85858B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun ExploreScreen(onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("latest") }
    var format by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun load() { scope.launch {
        loading = true; error = ""
        try {
            val resultItems = withContext(Dispatchers.IO) {
                val first = api.explore(query = query.trim(), genre = genre, format = format, sort = sort, page = 1, size = 200)
                val pageData = first.data
                val collected = pageData?.items?.toMutableList() ?: mutableListOf()
                // Ambil halaman lanjutan juga supaya katalog tidak berhenti di 60/200 judul.
                val pageCount = (pageData?.pages ?: 1).coerceIn(1, 20)
                if (first.status && pageCount > 1) {
                    for (page in 2..pageCount) {
                        val next = api.explore(query = query.trim(), genre = genre, format = format, sort = sort, page = page, size = 200)
                        val nextItems = next.data?.items.orEmpty()
                        if (!next.status || nextItems.isEmpty()) break
                        collected.addAll(nextItems)
                    }
                }
                collected
            }
            items = if (sort == "title") resultItems.sortedBy { it.title().lowercase() } else resultItems
            if (items.isEmpty()) error = "Tidak ada komik yang cocok. Coba kata kunci lain."
        } catch (e: Exception) { error = e.message ?: "Tidak dapat memuat katalog." }
        loading = false
    } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 18.dp)) {
        AppHeader("Temukan bacaan berikutnya")
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(horizontal = 13.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Muted)
            Spacer(Modifier.width(9.dp))
            BasicTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f).padding(vertical = 14.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp), decorationBox = { inner -> if (query.isEmpty()) Text("Cari judul komik...", color = Muted, fontSize = 13.sp); inner() })
            if (query.isNotEmpty()) Icon(Icons.Default.Close, "Hapus", tint = Muted, modifier = Modifier.clickable { query = "" })
            IconButton(onClick = { load() }) { Icon(Icons.Default.ArrowForward, "Cari", tint = White) }
        }
        Spacer(Modifier.height(13.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            lazyItems(listOf("Semua", "Manhwa", "Manhua", "Manga")) { label ->
                FilterChip(selected = (label == "Semua" && format.isBlank()) || format.equals(label, true), onClick = { format = if (label == "Semua") "" else label.lowercase(); load() }, label = { Text(label, fontSize = 11.sp) }, shape = RoundedCornerShape(10.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF343438), selectedLabelColor = White, labelColor = Muted))
            }
        }
        Spacer(Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            lazyItems(listOf("Semua", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Isekai", "Martial Arts", "Mystery", "Psychological", "Revenge", "Romance", "School Life", "Sci-fi", "Shounen", "Slice of Life", "Supernatural", "Thriller")) { label ->
                val value = if (label == "Semua") "" else label
                FilterChip(selected = genre.equals(value, true), onClick = { genre = value; load() }, label = { Text(label, fontSize = 11.sp) }, shape = RoundedCornerShape(10.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF343438), selectedLabelColor = White, labelColor = Muted))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Katalog komik", Modifier.weight(1f), color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { sort = when (sort) { "latest" -> "popular"; "popular" -> "title"; else -> "latest" }; load() }) { Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(when (sort) { "latest" -> "Terbaru"; "popular" -> "Populer"; else -> "Judul A-Z" }, color = Muted, fontSize = 11.sp) }
        }
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            error.isNotBlank() && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(error, Icons.Default.Search, "Coba lagi") { load() } }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 5.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                gridItems(items) { item -> PosterCard(item, onClick = { item.url().takeIf(String::isNotBlank)?.let(onOpen) }, width = androidx.compose.ui.unit.Dp.Unspecified) }
            }
        }
    }
}

@Composable
private fun DetailScreen(sourceUrl: String, saved: MutableList<String>, onBack: () -> Unit, onRead: (String) -> Unit) {
    var data by remember(sourceUrl) { mutableStateOf<JsonElement?>(null) }
    var loading by remember(sourceUrl) { mutableStateOf(true) }
    var error by remember(sourceUrl) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var chapterQuery by remember(sourceUrl) { mutableStateOf("") }
    var chapterNewestFirst by remember(sourceUrl) { mutableStateOf(true) }
    LaunchedEffect(sourceUrl) {
        try { val res = withContext(Dispatchers.IO) { api.detail(sourceUrl) }; data = res.data; if (data == null) error = res.message ?: "Detail komik tidak ditemukan." }
        catch (e: Exception) { error = e.message ?: "Gagal memuat detail komik." }
        loading = false
    }
    val d = data
    if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
    else if (d == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(error, Icons.Default.CloudOff, "Kembali") { onBack() } }
    else {
        val chapters = d.arr("chapters", "chapter_list", "chapterList")
        val visibleChapters = chapters.filter { it.str("title", "name", "chapter_title").contains(chapterQuery, true) || it.str("chapter_number", "number").contains(chapterQuery, true) }.let { if (chapterNewestFirst) it else it.reversed() }
        val title = d.title().ifBlank { "Detail komik" }
        val cover = imageUrl(d.cover())
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(310.dp).background(Panel)) {
                    if (cover.isNotBlank()) AsyncImage(model = cover, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .48f)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Ink))))
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0x88000000), CircleShape)) { Icon(Icons.Default.ArrowBack, "Kembali", tint = White) }
                    Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 17.dp), verticalAlignment = Alignment.Bottom) {
                        AsyncImage(model = cover, contentDescription = title, modifier = Modifier.size(118.dp, 160.dp).clip(RoundedCornerShape(13.dp)).background(Panel2), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f).padding(bottom = 3.dp)) {
                            Text(d.str("status", "type", "format").ifBlank { "KOMIKTA PICK" }.uppercase(), color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(7.dp)); Text(title, color = White, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(7.dp)); Text(d.str("author", "authors").ifBlank { "Penulis tidak diketahui" }, color = Muted, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { chapters.firstOrNull()?.str("url", "link")?.takeIf(String::isNotBlank)?.let(onRead) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink)) { Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Mulai membaca") }
                    OutlinedButton(onClick = { if (saved.contains(sourceUrl)) saved.remove(sourceUrl) else saved.add(sourceUrl) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = White)) { Icon(if (saved.contains(sourceUrl)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null); Spacer(Modifier.width(5.dp)); Text(if (saved.contains(sourceUrl)) "Tersimpan" else "Simpan") }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MetaPill(d.str("status").ifBlank { "Ongoing" }); MetaPill(d.str("format", "type").ifBlank { "Komik" });
                        val rating = d.str("user_rate", "rating", "rate"); if (rating.isNotBlank()) MetaPill("★ $rating", Gold)
                    }
                    Spacer(Modifier.height(17.dp)); Text("Sinopsis", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(7.dp)); Text(d.str("description", "synopsis").ifBlank { "Belum ada sinopsis untuk komik ini." }, color = Color(0xFFC2C2C8), fontSize = 13.sp, lineHeight = 20.sp)
                    val genres = d.textArray("genres", "genre")
                    if (genres.isNotEmpty()) { Spacer(Modifier.height(13.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { lazyItems(genres) { MetaPill(it) } } }
                }
            }
            item { Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 25.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("Daftar chapter", Modifier.weight(1f), color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text("${chapters.size} chapter", color = Muted, fontSize = 11.sp) } }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        BasicTextField(value = chapterQuery, onValueChange = { chapterQuery = it }, modifier = Modifier.weight(1f).padding(vertical = 12.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 12.sp), decorationBox = { inner -> if (chapterQuery.isEmpty()) Text("Cari chapter...", color = Muted, fontSize = 12.sp); inner() })
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { chapterNewestFirst = !chapterNewestFirst }, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Panel)) { Icon(if (chapterNewestFirst) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, "Urutkan chapter", tint = White) }
                }
            }
            if (chapters.isEmpty()) item { Text("Daftar chapter tidak tersedia.", Modifier.padding(20.dp), color = Muted, fontSize = 12.sp) }
            if (chapters.isNotEmpty() && visibleChapters.isEmpty()) item { Text("Chapter tidak ditemukan.", Modifier.padding(20.dp), color = Muted, fontSize = 12.sp) }
            lazyItems(visibleChapters) { chapter ->
                Row(Modifier.fillMaxWidth().clickable { chapter.str("url", "link").takeIf(String::isNotBlank)?.let(onRead) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Panel2), contentAlignment = Alignment.Center) { Icon(Icons.Default.MenuBook, null, tint = Muted, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(chapter.str("title", "name").ifBlank { "Chapter" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(chapter.str("time", "updated_at", "date"), color = Muted, fontSize = 10.sp) }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
                HorizontalDivider(color = Line.copy(alpha = .55f), modifier = Modifier.padding(start = 70.dp))
            }
        }
    }
}

@Composable
private fun ReaderScreen(chapterUrl: String, onBack: () -> Unit) {
    var data by remember(chapterUrl) { mutableStateOf<JsonElement?>(null) }
    var loading by remember(chapterUrl) { mutableStateOf(true) }
    var error by remember(chapterUrl) { mutableStateOf("") }
    LaunchedEffect(chapterUrl) {
        try { data = withContext(Dispatchers.IO) { api.chapter(chapterUrl) }.data; if (data == null) error = "Chapter tidak ditemukan." }
        catch (e: Exception) { error = e.message ?: "Gagal memuat chapter." }
        loading = false
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = White) }
            Column(Modifier.weight(1f)) { Text(data.title().ifBlank { "Pembaca Komik" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Mode baca vertikal · gulir untuk lanjut", color = Muted, fontSize = 10.sp) }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
            data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(error, Icons.Default.CloudOff, "Kembali") { onBack() } }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                val pages = data.arr("images", "pages", "image_urls")
                if (pages.isEmpty()) item { Text("Halaman chapter tidak tersedia.", Modifier.padding(24.dp), color = Muted) }
                lazyItems(pages) { page ->
                    val src = if (page.isJsonPrimitive) page.asString else page.str("url", "src", "image")
                    AsyncImage(model = imageUrl(src), contentDescription = "Halaman komik", modifier = Modifier.fillMaxWidth().background(Color.Black), contentScale = ContentScale.FillWidth)
                }
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, color: Color = Muted) { Surface(color = Panel2, shape = RoundedCornerShape(8.dp)) { Text(text.ifBlank { "Info" }, color = color, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), maxLines = 1) } }

@Composable
private fun LibraryScreen(saved: List<String>, onOpen: (String) -> Unit, onExplore: () -> Unit) {
    var tab by remember { mutableStateOf("Tersimpan") }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 20.dp)) {
        Text("Library", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
        Spacer(Modifier.height(5.dp)); Text("Semua bacaanmu, tersusun rapi.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(19.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Tersimpan", "Riwayat").forEach { FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Panel2, selectedLabelColor = White, labelColor = Muted)) } }
        Spacer(Modifier.height(20.dp))
        if (saved.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState(if (tab == "Tersimpan") "Komik yang kamu simpan akan muncul di sini." else "Riwayat bacaan akan tersedia setelah kamu mulai membaca.", Icons.Default.BookmarkBorder, "Jelajahi komik", onExplore) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { lazyItems(saved) { url -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).clickable { onOpen(url) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bookmark, null, tint = Accent); Spacer(Modifier.width(12.dp)); Text(url.substringAfterLast('/').take(40), color = White, modifier = Modifier.weight(1f), maxLines = 1); Icon(Icons.Default.ChevronRight, null, tint = Muted) } } }
    }
}

@Composable
private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Profil", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
        Spacer(Modifier.height(5.dp)); Text("Atur pengalaman membaca Komikta.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Panel2, Panel))).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF343438)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = White, modifier = Modifier.size(28.dp)) }; Spacer(Modifier.width(14.dp)); Column { Text("Pembaca Komikta", color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text("Baca lebih banyak, temukan cerita baru.", color = Muted, fontSize = 11.sp) } }
        }
        Spacer(Modifier.height(20.dp))
        ProfileItem(Icons.Default.Tune, "Pengaturan reader", "Preferensi membaca dan tampilan")
        ProfileItem(Icons.Default.Download, "Download offline", "Koneksi unduhan mengikuti dukungan server sumber")
        ProfileItem(Icons.Default.Info, "Tentang Komikta", "Versi aplikasi 1.0.0")
        Spacer(Modifier.height(18.dp)); Text("KOMIKTA  ·  BACA CERITA, TEMUKAN DUNIA", color = Color(0xFF66666D), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ProfileItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Panel2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Accent, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(3.dp)); Text(subtitle, color = Muted, fontSize = 10.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun LoadingBlock() { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { repeat(3) { Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(14.dp)).background(Panel)) } } }

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(18.dp)) { Icon(Icons.Default.CloudOff, null, tint = Muted); Spacer(Modifier.height(9.dp)); Text("Katalog belum tersedia", color = White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(5.dp)); Text(message, color = Muted, fontSize = 12.sp); Spacer(Modifier.height(10.dp)); Text("Coba jelajahi", color = White, fontSize = 12.sp, modifier = Modifier.clickable { onRetry() }) }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(66.dp).clip(RoundedCornerShape(20.dp)).background(Panel), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Muted, modifier = Modifier.size(29.dp)) }
        Spacer(Modifier.height(15.dp)); Text(message, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(13.dp)); Text(action, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Panel2).clickable { onAction() }.padding(horizontal = 15.dp, vertical = 10.dp))
    }
}
