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

if failures:
    print("\nMegaFlix overlay could not be applied:")
    for m in failures:
        print("  -", m)
    sys.exit(1)
print("MegaFlix overlay applied.")
