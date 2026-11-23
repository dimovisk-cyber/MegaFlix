#!/usr/bin/env python3
"""
MegaFlix overlay for NuvioTV.

Run from the repository root before building:  python3 megaflix/apply.py
It edits a fresh Nuvio checkout in place, so this fork can keep pulling upstream Nuvio
and every MegaFlix change lives here, in one place. Each edit asserts that the code it
expects is still there, so an upstream change fails loudly instead of silently.
"""
import base64
import glob
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
J = os.path.join(ROOT, "app/src/main/java/com/nuvio/tv")
RES = os.path.join(ROOT, "app/src/main/res")
failures = []


def read(path):
    with open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


def edit(path, pairs, required=True):
    """Replace exact snippets. Line endings of the file are preserved (CRLF-safe)."""
    full = os.path.join(ROOT, path) if not path.startswith("/") else path
    text = read(full)
    crlf = "\r\n" in text
    work = text.replace("\r\n", "\n")
    for old, new in pairs:
        if old not in work:
            msg = "%s: snippet not found: %r" % (os.path.relpath(full, ROOT), old[:90])
            if required:
                failures.append(msg)
            else:
                print("skip:", msg)
            continue
        work = work.replace(old, new)
    write(full, work.replace("\n", "\r\n") if crlf else work)


# ---------------------------------------------------------------- theme
edit(J + "/ui/theme/ThemeColors.kt", [(
    """    val Amber = ThemeColorPalette(
        secondary = NuvioPrimitives.amber500,
        secondaryVariant = NuvioPrimitives.amber700,
        focusRing = NuvioPrimitives.amber300,
        focusBackground = Color(0xFF3D2D1A),
        background = Color(0xFF0F0D0D),
        backgroundElevated = Color(0xFF1E1A1A),
        backgroundCard = Color(0xFF24201A)
    )""",
    """    // MegaFlix: orange #FF8000 on OLED black (replaces the stock Amber palette)
    val Amber = ThemeColorPalette(
        secondary = Color(0xFFFF8000),
        secondaryVariant = Color(0xFFE06A00),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF2B1A08),
        background = Color(0xFF000000),
        backgroundElevated = Color(0xFF141414),
        backgroundCard = Color(0xFF1A1A1A),
        surface = Color(0xFF141414),
        surfaceVariant = Color(0xFF222222),
        panel = Color(0xFF0D0D0D),
        menu = Color(0xFF141414),
        modal = Color(0xFF141414)
    )""")])
edit(J + "/domain/model/AppTheme.kt", [('AMBER("Amber")', 'AMBER("MegaFlix")')])
for f in ["/MainActivity.kt", "/ui/screens/settings/ThemeSettingsViewModel.kt", "/domain/model/ThemeAccess.kt"]:
    edit(J + f, [("?: AppTheme.WHITE", "?: AppTheme.AMBER")])
edit(J + "/domain/model/ThemeAccess.kt", [(
    """    } else {
        AppTheme.WHITE
    }""",
    """    } else {
        AppTheme.AMBER
    }""")])

# ---------------------------------------------------------------- TMDB in Brazilian Portuguese, on by default
edit(J + "/data/local/TmdbSettingsDataStore.kt", [
    ("enabled = prefs[enabledKey] ?: false", "enabled = prefs[enabledKey] ?: true"),
    ("modernHomeEnabled = prefs[modernHomeEnabledKey] ?: false", "modernHomeEnabled = prefs[modernHomeEnabledKey] ?: true"),
    ('language = prefs[languageKey] ?: "en"', 'language = prefs[languageKey] ?: "pt-br"'),
])
for f in ["/domain/model/TmdbSettings.kt", "/ui/screens/settings/TmdbSettingsViewModel.kt"]:
    edit(J + f, [
        ("    val enabled: Boolean = false,", "    val enabled: Boolean = true,"),
        ("    val modernHomeEnabled: Boolean = false,", "    val modernHomeEnabled: Boolean = true,"),
        ('    val language: String = "en",', '    val language: String = "pt-br",'),
    ])

# ---------------------------------------------------------------- identity / build
edit("app/build.gradle.kts", [
    ('        applicationId = "com.nuvio.tv"', '        applicationId = "com.megaflix.tv"'),
    ('buildConfigField("boolean", "FEATURE_IN_APP_UPDATES_ENABLED", "true")',
     'buildConfigField("boolean", "FEATURE_IN_APP_UPDATES_ENABLED", "false") // MegaFlix: no Nuvio updater'),
    ('buildConfigField("String", "GITHUB_OWNER", "\\"NuvioMedia\\"")', 'buildConfigField("String", "GITHUB_OWNER", "\\"dimovisk-cyber\\"")'),
    ('buildConfigField("String", "GITHUB_REPO", "\\"NuvioTV\\"")', 'buildConfigField("String", "GITHUB_REPO", "\\"MegaFlix\\"")'),
])
gradle = read(os.path.join(ROOT, "app/build.gradle.kts"))
gradle = re.sub(r'versionName = "([^"]+)"', lambda m: 'versionName = "%s-megaflix"' % m.group(1), gradle, count=1)
write(os.path.join(ROOT, "app/build.gradle.kts"), gradle)

# TMDB key comes from the repository secret TMDB_API_KEY (written to local.properties by the workflow)

n = 0
for f in glob.glob(os.path.join(RES, "values*/strings.xml")):
    t = read(f)
    t2 = re.sub(r'<string name="app_name">[^<]*</string>', '<string name="app_name">MegaFlix</string>', t)
    if t2 != t:
        write(f, t2)
        n += 1
print("app_name -> MegaFlix in", n, "files")

# ---------------------------------------------------------------- brand images (base64 text under megaflix/brand)
brand = os.path.join(ROOT, "megaflix", "brand")
for f in glob.glob(os.path.join(brand, "**", "*.b64"), recursive=True):
    rel = os.path.relpath(f, brand)[:-4]
    target = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(f) as src, open(target, "wb") as dst:
        dst.write(base64.b64decode(src.read()))
    print("brand:", rel)

# ================================================================ MegaFlix features
import shutil

# ---- our own sources (package com.nuvio.tv.megaflix) and resources
src_dir = os.path.join(ROOT, "megaflix", "android", "java")
dst_dir = os.path.join(J, "megaflix")
os.makedirs(dst_dir, exist_ok=True)
for f in sorted(os.listdir(src_dir)):
    shutil.copy(os.path.join(src_dir, f), os.path.join(dst_dir, f))
    print("source:", f)
res_src = os.path.join(ROOT, "megaflix", "android", "res")
for base, _, files in os.walk(res_src):
    for f in files:
        rel = os.path.relpath(os.path.join(base, f), res_src)
        os.makedirs(os.path.dirname(os.path.join(RES, rel)), exist_ok=True)
        shutil.copy(os.path.join(base, f), os.path.join(RES, rel))
        print("res:", rel)

# ---- manifest: YouCine visibility, megaflix:// links, Piloto service
edit("app/src/main/AndroidManifest.xml", [
    ("""            <action android:name="android.speech.RecognitionService" />
        </intent>
    </queries>""",
     """            <action android:name="android.speech.RecognitionService" />
        </intent>
        <!-- MegaFlix: YouCine as second option -->
        <package android:name="com.world.youcinetv" />
        <package android:name="com.world.youcinemobile" />
    </queries>"""),
    ("""    </application>""",
     """        <!-- MegaFlix: "YouCine" source -> exact title in YouCine, full screen -->
        <activity
            android:name=".megaflix.YouCineLinkActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:noHistory="true"
            android:taskAffinity=""
            android:theme="@android:style/Theme.Translucent.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="megaflix" android:host="youcine" />
            </intent-filter>
        </activity>

        <!-- MegaFlix Piloto: searches, opens and plays the title inside YouCine -->
        <service
            android:name=".megaflix.PilotService"
            android:exported="false"
            android:label="@string/megaflix_pilot_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/megaflix_pilot" />
        </service>
    </application>"""),
])

# ---- whole interface in Brazilian Portuguese unless the user picks another language
edit(J + "/NuvioApplication.kt", [(
    """        val tag = getSharedPreferences("app_locale", Context.MODE_PRIVATE)
            .getString("locale_tag", null)""",
    """        val megaflixLocale = getSharedPreferences("app_locale", Context.MODE_PRIVATE)
        if (!megaflixLocale.contains("locale_tag")) megaflixLocale.edit().putString("locale_tag", "pt-BR").commit()
        val tag = megaflixLocale.getString("locale_tag", null)""")])
ptbr = os.path.join(RES, "values-pt-rBR", "strings.xml")
edit(ptbr, [
    ('<string name="nav_library">Biblioteca</string>', '<string name="nav_library">Minha lista</string>'),
    ('<string name="hero_add_to_library">Adicionar à biblioteca</string>', '<string name="hero_add_to_library">Adicionar à Minha lista</string>'),
    ('<string name="hero_remove_from_library">Remover da biblioteca</string>', '<string name="hero_remove_from_library">Remover da Minha lista</string>'),
], required=False)

# ---- no Nuvio account screen at startup (Nuvio's cloud login does not serve MegaFlix): open straight to the home
edit(J + "/data/local/AppOnboardingDataStore.kt", [(
    "prefs[hasSeenAuthQrOnFirstLaunchKey] ?: false",
    "prefs[hasSeenAuthQrOnFirstLaunchKey] ?: true")])

# ---- silent intro over the home, fading into it
edit(J + "/MainActivity.kt", [(
    """    override fun onCreate(savedInstanceState: Bundle?) {""",
    """    override fun onCreate(savedInstanceState: Bundle?) {
        com.nuvio.tv.megaflix.MegaFlixIntro.attach(this, savedInstanceState)""")])

# ---- tabs: Canais TV, Esportes, Favoritos, Assistidos
edit(J + "/MainActivity.kt", [
    ("""                            add(Screen.Library.route)
""",
     """                            add(Screen.Library.route)
                            add(com.nuvio.tv.megaflix.MegaFlixRoutes.CHANNELS)
                            add(com.nuvio.tv.megaflix.MegaFlixRoutes.SPORTS)
                            add(com.nuvio.tv.megaflix.MegaFlixRoutes.FAVORITES)
                            add(com.nuvio.tv.megaflix.MegaFlixRoutes.HISTORY)
"""),
    ("""                            add(
                                DrawerItem(
                                    route = Screen.Library.route,
                                    label = strNavLibrary,
                                    iconRes = R.raw.sidebar_library
                                )
                            )
""",
     """                            add(
                                DrawerItem(
                                    route = com.nuvio.tv.megaflix.MegaFlixRoutes.CHANNELS,
                                    label = "Canais TV",
                                    icon = com.nuvio.tv.megaflix.MegaFlixIcons.Channels
                                )
                            )
                            add(
                                DrawerItem(
                                    route = com.nuvio.tv.megaflix.MegaFlixRoutes.SPORTS,
                                    label = "Esportes",
                                    icon = com.nuvio.tv.megaflix.MegaFlixIcons.Sports
                                )
                            )
                            add(
                                DrawerItem(
                                    route = Screen.Library.route,
                                    label = strNavLibrary,
                                    iconRes = R.raw.sidebar_library
                                )
                            )
                            add(
                                DrawerItem(
                                    route = com.nuvio.tv.megaflix.MegaFlixRoutes.FAVORITES,
                                    label = "Favoritos",
                                    icon = com.nuvio.tv.megaflix.MegaFlixIcons.Favorites
                                )
                            )
                            add(
                                DrawerItem(
                                    route = com.nuvio.tv.megaflix.MegaFlixRoutes.HISTORY,
                                    label = "Assistidos",
                                    icon = com.nuvio.tv.megaflix.MegaFlixIcons.History
                                )
                            )
"""),
])
edit(J + "/ui/navigation/NuvioNavHost.kt", [(
    """        rootComposable(Screen.Library.route) {""",
    """        rootComposable(com.nuvio.tv.megaflix.MegaFlixRoutes.FAVORITES) {
            com.nuvio.tv.megaflix.MegaFlixFavoritesScreen(
                onOpen = { id, type -> navController.navigate(Screen.Detail.createRoute(id, type)) }
            )
        }
        rootComposable(com.nuvio.tv.megaflix.MegaFlixRoutes.HISTORY) {
            com.nuvio.tv.megaflix.MegaFlixHistoryScreen(
                onOpen = { id, type -> navController.navigate(Screen.Detail.createRoute(id, type)) }
            )
        }
        rootComposable(com.nuvio.tv.megaflix.MegaFlixRoutes.CHANNELS) {
            com.nuvio.tv.megaflix.MegaFlixChannelsScreen(
                onOpenChannel = { id, type, base -> navController.navigate(Screen.Detail.createRoute(id, type, base)) }
            )
        }
        rootComposable(com.nuvio.tv.megaflix.MegaFlixRoutes.SPORTS) {
            com.nuvio.tv.megaflix.MegaFlixSportsScreen(
                onOpenChannel = { id, type, base -> navController.navigate(Screen.Detail.createRoute(id, type, base)) }
            )
        }
        rootComposable(Screen.Library.route) {""")])

# ---- YouCine as second option, at the end of every movie/episode source list
edit(J + "/data/repository/StreamRepositoryImpl.kt", [(
    """                else NetworkResult.Success(BuiltInUsenetService.arrangeOutcomes(result.data, unplayable::contains, unplayable::played,
                    sourceConfiguration.usenetSources.availableFirst, unplayable::sibling))""",
    """                else NetworkResult.Success(com.nuvio.tv.megaflix.MegaFlixYouCine.append(context, type, videoId, season, episode,
                    BuiltInUsenetService.arrangeOutcomes(result.data, unplayable::contains, unplayable::played,
                    sourceConfiguration.usenetSources.availableFirst, unplayable::sibling)))""")])
# back from YouCine lands on the title page, not on the source list
edit(J + "/ui/screens/stream/StreamScreen.kt", [(
    """        viewModel.rememberPlayedPlugin(playbackInfo.bingeGroup)
        return true
    }""",
    """        viewModel.rememberPlayedPlugin(playbackInfo.bingeGroup)
        // MegaFlix: YouCine plays on top; coming back shows the title page.
        if (url.startsWith("megaflix://")) onBackPress()
        return true
    }""")])

# ---- title page: Curtir button + Netflix DS buttons (orange accent)
HERO = J + "/ui/screens/detail/HeroSection.kt"
edit(HERO, [
    # Curtir (feeds Favoritos + recommendations), right after Minha lista
    ("""                            onLongPress = onLibraryLongPress,
                            onFocused = onHeroActionFocused
                        )
""",
     """                            onLongPress = onLibraryLongPress,
                            onFocused = onHeroActionFocused
                        )

                        val megaflixContext = androidx.compose.ui.platform.LocalContext.current
                        val megaflixLiked = com.nuvio.tv.megaflix.rememberIsLiked(meta.id)
                        ActionIconButton(
                            icon = if (megaflixLiked) com.nuvio.tv.megaflix.MegaFlixIcons.Liked else com.nuvio.tv.megaflix.MegaFlixIcons.Like,
                            contentDescription = if (megaflixLiked) "Curtido" else "Curtir",
                            onClick = { com.nuvio.tv.megaflix.MegaFlixTaste.toggle(megaflixContext, meta) },
                            selected = megaflixLiked,
                            toggleable = true,
                            onFocused = onHeroActionFocused
                        )
"""),
    # Play: Netflix TV style - focused white/black, unfocused translucent gray, 4dp corners
    ("""        colors = ButtonDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.White,
            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = androidx.compose.ui.graphics.Color.Black,
            focusedContentColor = androidx.compose.ui.graphics.Color.Black
        ),
        shape = ButtonDefaults.shape(
            shape = RoundedCornerShape(NuvioTheme.spacing.xxl)
        ),
        border = ButtonDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.spacing.xxl)
            )
        ),""",
     """        colors = ButtonDefaults.colors(
            containerColor = Color(0xB36D6D6E),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),
        shape = ButtonDefaults.shape(
            shape = RoundedCornerShape(4.dp)
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.04f),"""),
])
# round outlined icon buttons: thin gray ring, white when focused
hero = read(HERO)
old_colors = """        colors = IconButtonDefaults.colors(
            containerColor = if (selected) selectedContainerColor else NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.Secondary,
            contentColor = if (selected) selectedContentColor else NuvioTheme.colors.TextPrimary,
            focusedContentColor = NuvioTheme.colors.OnSecondary
        ),"""
old_colors_painter = """        colors = IconButtonDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.Secondary,
            contentColor = NuvioTheme.colors.TextPrimary,
            focusedContentColor = NuvioTheme.colors.OnSecondary
        ),"""
old_border = """        border = IconButtonDefaults.border(
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = CircleShape
            )
        ),"""
new_colors = """        colors = IconButtonDefaults.colors(
            containerColor = Color(0x66141414),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black
        ),"""
new_border = """        border = IconButtonDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xB3FFFFFF)),
                shape = CircleShape
            ),
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shape = CircleShape
            )
        ),"""
hero_n = hero.replace("\r\n", "\n")
for old, new, count in ((old_colors, new_colors, 1), (old_colors_painter, new_colors, 1), (old_border, new_border, 2)):
    if hero_n.count(old) != count:
        failures.append("HeroSection.kt: expected %d of %r" % (count, old[:60]))
    hero_n = hero_n.replace(old, new)
write(HERO, hero_n.replace("\n", "\r\n") if "\r\n" in hero else hero_n)

if failures:
    print("\nMegaFlix overlay could not be applied:")
    for m in failures:
        print("  -", m)
    sys.exit(1)
print("MegaFlix overlay applied.")
