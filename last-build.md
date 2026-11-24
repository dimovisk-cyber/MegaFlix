# MegaFlix build 10: success

commit: ace405b9a7e5e1e57ca51d3bfe508e87b32f79d9

## Errors
```
```
## Log tail
```
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerObservers.kt:783:8 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerObservers.kt:791:8 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStreams.kt:773:83 Unnecessary safe call on a non-null receiver of type 'String'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStreams.kt:804:83 Unnecessary safe call on a non-null receiver of type 'String'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStreams.kt:2085:44 Elvis operator (?:) always returns the left operand of non-nullable type 'NextEpisodeInfo'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerSubtitleTiming.kt:434:38 Unnecessary safe call on a non-null receiver of type 'ResponseBody'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTracks.kt:1709:79 Unnecessary safe call on a non-null receiver of type 'Subtitle'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTracks.kt:1818:49 Elvis operator (?:) always returns the left operand of non-nullable type 'String'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTracks.kt:1858:51 Elvis operator (?:) always returns the left operand of non-nullable type 'Subtitle'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt:236:26 'val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>' is deprecated. Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerSidecarSubtitles.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerSubtitleDataSource.kt:71:26 Unnecessary safe call on a non-null receiver of type 'ResponseBody'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerSubtitleRtlFix.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt:54:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/RewrappableExtractor.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSpotSampler.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSubtitleSyncController.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncDecoder.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncDecoder.kt:48:24 This class is not recommended for use in Kotlin. Use 'kotlin.Any' instead.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncExtractors.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncExtractors.kt:152:48 Unnecessary non-null assertion (!!) on a non-null receiver of type 'Format'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncFallback.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/AudioSyncTracker.kt:257:37 Unnecessary non-null assertion (!!) on a non-null receiver of type 'SubtitleAudioAligner.Estimate'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/PlaybackAudioTap.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/SubtitleCandidatePool.kt:308:19 'constructor(p0: String!): Locale' is deprecated. Deprecated in Java.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/SubtitleCandidatePool.kt:309:33 'constructor(p0: String!): Locale' is deprecated. Deprecated in Java.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/SubtitleCandidatePool.kt:338:42 'constructor(p0: String!): Locale' is deprecated. Deprecated in Java.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/asr/AsrSyncEngine.kt:86:24 This class is not recommended for use in Kotlin. Use 'kotlin.Any' instead.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/audiosync/asr/AsrSyncEngine.kt:289:50 Java type mismatch: inferred type is 'AsrSyncEngine.Segment?', but 'AsrSyncEngine.Segment' was expected.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncExtractorsFactory.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncLibassSubtitles.kt:1:13 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncSubtitleHttp.kt:56:56 The corresponding parameter in the supertype 'Callback' is named 'e'. This may cause problems when calling this function with named arguments.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutomaticSubtitleSync.kt:2346:52 Unnecessary non-null assertion (!!) on a non-null receiver of type 'Long'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/EmbeddedSubtitleTimelineLoader.kt:44:8 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/EmbeddedSubtitleTimelineLoader.kt:1867:56 The corresponding parameter in the supertype 'Callback' is named 'e'. This may cause problems when calling this function with named arguments.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/EmbeddedSubtitleTimelineLoader.kt:1902:57 Elvis operator (?:) always returns the left operand of non-nullable type 'ResponseBody'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/plugin/PluginViewModel.kt:29:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/profile/ProfileSelectionViewModel.kt:118:22 This declaration needs opt-in. Its usage should be marked with '@kotlinx.coroutines.ExperimentalCoroutinesApi' or '@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)'
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/search/SearchDiscoverSection.kt:607:14 'fun Modifier.focusRestorer(onRestoreFailed: (() -> FocusRequester)?): Modifier' is deprecated. Use focusRestorer(FocusRequester) instead.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/search/SearchViewModel.kt:62:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/search/SearchViewModel.kt:788:74 Elvis operator (?:) always returns the left operand of non-nullable type 'String'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/AboutScreen.kt:150:62 'val Icons.Filled.OpenInNew: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.OpenInNew.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/DebugSettingsViewModel.kt:36:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/SimklSettingsViewModel.kt:46:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/SubtitleFontsViewModel.kt:37:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/SupportersContributorsViewModel.kt:49:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/TraktViewModel.kt:58:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/settings/UsenetDiagnosticsCard.kt:126:55 Unnecessary safe call on a non-null receiver of type 'JSONObject'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt:153:26 'val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>' is deprecated. Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt:901:33 Experimental Haze API
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt:901:46 Experimental Haze API
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt:901:61 Experimental Haze API
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt:88:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt:1018:58 Unnecessary safe call on a non-null receiver of type 'List<AddonStreams>'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt:2152:63 Unnecessary safe call on a non-null receiver of type 'ResponseBody'.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseScreen.kt:573:22 'fun Modifier.focusRestorer(onRestoreFailed: (() -> FocusRequester)?): Modifier' is deprecated. Use focusRestorer(FocusRequester) instead.
w: file:///home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/ui/screens/tmdb/TmdbEntityBrowseViewModel.kt:27:5 This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.
- To opt in to applying to both value parameter and field, add '-Xannotation-default-target=param-property' to your compiler arguments.
- To keep applying to the value parameter only, use the '@param:' annotation target.

See https://youtrack.jetbrains.com/issue/KT-73255 for more details.

> Task :app:compileFullReleaseJavaWithJavac
Note: /home/runner/work/MegaFlix/MegaFlix/app/src/main/java/com/nuvio/tv/megaflix/YouCineInstaller.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.

> Task :app:hiltAggregateDepsFullRelease

> Task :app:hiltJavaCompileFullRelease
warning: Kapt support in Moshi Kotlin Code Gen is deprecated and will be removed in 2.0. Please migrate to KSP. https://github.com/square/moshi#codegen
1 warning

> Task :app:processFullReleaseJavaRes
> Task :app:generateFullReleaseLintVitalReportModel
> Task :app:mergeFullReleaseJavaResource
> Task :app:transformFullReleaseClassesWithAsm
> Task :app:mergeFullReleaseGeneratedProguardFiles
> Task :app:expandFullReleaseArtProfileWildcards
> Task :app:produceFullReleaseComposeMapping
> Task :app:reportFullReleaseComposeMappingErrors
> Task :app:lintVitalAnalyzeFullRelease
> Task :app:minifyFullReleaseWithR8
WARNING: R8: Unable to include all startup classes in classes.dex. Startup classes were distributed in: classes.dex, classes2.dex.

> Task :app:lintVitalReportFullRelease
> Task :app:lintVitalFullRelease
> Task :app:mergeFullReleaseComposeMapping
> Task :app:generateSentryProguardUuidFullRelease
> Task :app:convertShrunkResourcesToBinaryFullRelease
> Task :app:optimizeFullReleaseResources
> Task :app:l8DexDesugarLibFullRelease
> Task :app:compileFullReleaseArtProfile
> Task :app:embedFullReleaseUpdateDexMetadata
> Task :app:mergeFullReleaseAssets
> Task :app:injectSentryDebugMetaPropertiesIntoAssetsFullRelease
> Task :app:compressFullReleaseAssets
> Task :app:packageFullRelease
> Task :app:createFullReleaseApkListingFileRedirect
> Task :app:assembleFullRelease

> Task :app:uploadSentryProguardMappingsFullRelease
> skipping upload.

[Incubating] Problems report is available at: file:///home/runner/work/MegaFlix/MegaFlix/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 20m 56s
70 actionable tasks: 70 executed
app/build/outputs/apk/full/release/app-full-armeabi-v7a-release.apk
app/build/outputs/apk/full/release/app-full-x86_64-release.apk
app/build/outputs/apk/full/release/app-full-x86-release.apk
app/build/outputs/apk/full/release/app-full-arm64-v8a-release.apk
app/build/outputs/apk/full/release/app-full-universal-release.apk
-rw-r--r-- 1 runner runner 211740753 Nov 24 00:15 MegaFlix.apk
release: megaflix-build-10
```
