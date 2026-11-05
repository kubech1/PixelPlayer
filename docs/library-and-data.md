# Biblioteca, datos y funciones derivadas

## El recorrido de una canción

Una canción local empieza como registro de MediaStore y metadatos de un archivo. El escaneo decide qué incorporar o actualizar; Room permite consultar el resultado; los repositorios lo transforman en modelos para la app. La UI recibe listas o páginas y usa `Song` para mostrar, seleccionar y solicitar reproducción.

| Paso | Propietario principal | Detalle que importa |
| --- | --- | --- |
| Detectar cambios / solicitar trabajo | [SyncManager](../app/src/main/java/com/theveloper/pixelplay/data/worker/SyncManager.kt), [MediaStoreObserver](../app/src/main/java/com/theveloper/pixelplay/data/observer/MediaStoreObserver.kt) | WorkManager, trabajo único, progreso y mantenimiento periódico |
| Escanear y reconciliar | [SyncWorker](../app/src/main/java/com/theveloper/pixelplay/data/worker/SyncWorker.kt) | Modos `INCREMENTAL`, `FULL` y `REBUILD`; filtros, timestamps y fases |
| Leer metadatos | [AudioMetadataReader](../app/src/main/java/com/theveloper/pixelplay/data/media/AudioMetadataReader.kt) | Lectura y fallbacks; no duplicar probes costosos sin necesidad |
| Agrupar artistas / álbumes | [ArtistParsingUtils](../app/src/main/java/com/theveloper/pixelplay/data/worker/ArtistParsingUtils.kt), [AlbumGroupingUtils](../app/src/main/java/com/theveloper/pixelplay/data/worker/AlbumGroupingUtils.kt) | Identidad y agrupación no equivalen a texto visible |
| Persistir y consultar | [MusicDao](../app/src/main/java/com/theveloper/pixelplay/data/database/MusicDao.kt), [PixelPlayDatabase](../app/src/main/java/com/theveloper/pixelplay/data/database/PixelPlayDatabase.kt) | Queries, índices, relaciones y migraciones |
| Servir biblioteca a consumidores | [MusicRepository](../app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepository.kt), [MusicRepositoryImpl](../app/src/main/java/com/theveloper/pixelplay/data/repository/MusicRepositoryImpl.kt) | Contrato público y composición de datos/filtros |
| Presentar páginas y categorías | [LibraryStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/LibraryStateHolder.kt), [LibraryViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/LibraryViewModel.kt) | Flows paginados, estados de carga y caché de páginas |

El worker puede diferir sincronización incremental de fondo mientras hay reproducción activa. Un trabajo en retry/backoff no significa necesariamente que esté escaneando; `SyncManager.isSyncing` trata ese caso. Cambiar esa política requiere revisar tanto la ejecución como el indicador de UI.

`FULL` y `REBUILD` no deben considerarse sinónimos. Antes de ofrecer un reescaneo o una limpieza, lee su camino real y cómo conserva favoritos, playlists y datos asociados.

## Identidad y modelos

[Song](../app/src/main/java/com/theveloper/pixelplay/data/model/Song.kt) contiene un ID `String`, URI de contenido, path, metadatos, referencias de artistas y campos opcionales de Telegram, Netease, Google Drive, QQ Music, Navidrome y Jellyfin. **No convertir indiscriminadamente todos los IDs a `Long`**: las fuentes tienen formatos distintos.

`artist` y `artistId` conservan compatibilidad con un artista primario; `artists`, `displayArtist` y `primaryArtist` expresan la información múltiple. La separación de artistas se realiza en sincronización, no mediante un split improvisado del texto en cada pantalla.

[SongEntity](../app/src/main/java/com/theveloper/pixelplay/data/database/SongEntity.kt) es persistencia, no el mismo contrato que `Song`. [MediaMapper](../app/src/main/java/com/theveloper/pixelplay/data/media/MediaMapper.kt) y [MediaItemBuilder](../app/src/main/java/com/theveloper/pixelplay/utils/MediaItemBuilder.kt) participan en las representaciones usadas por reproducción. Revisa ID, URI y extras juntos al modificar una conversión.

La tabla unificada `songs` usa una clave `Long`, mientras los modelos y registros originales de proveedor pueden utilizar otros formatos. `SongEntity.kt` define `SourceType`: local 0, Telegram 1, Netease 2, Drive 3, QQ Music 4, Navidrome 5 y Jellyfin 6. Distingue ID canónico de esa tabla, ID original del proveedor y URI; una conversión numérica solo es válida en el contrato que la exige. El mismo archivo declara bitrate en bits por segundo y sample rate en Hz.

## Persistencia

La base del teléfono se llama `pixelplay_database`; declara versión **43**, exportación de esquema y 26 entidades tras retirar las dos tablas de IA. [AppModule](../app/src/main/java/com/theveloper/pixelplay/di/AppModule.kt) registra migraciones, callback para artefactos de runtime y WAL. No se permite fallback destructivo en debug ni release: un fallo de migración debe conservar los datos y ser investigado. Las pruebas de migración validan el esquema explícitamente.

Los [esquemas exportados](../app/schemas) y las [pruebas de migración](../app/src/androidTest/java/com/theveloper/pixelplay/data/database/PixelPlayDatabaseMigrationTest.kt) son parte del contrato al cambiar tablas. La búsqueda incluye `SongSearchFtsEntity` y artefactos creados por callback; no basta con añadir un campo a una data class.

`PixelPlayDatabase` también crea y repara triggers para sincronización de favoritos y FTS. Al tocar escritura/borrado, revisar sus efectos derivados y el callback, no solo la query invocada directamente. El índice `file_path` ya aparece en la entidad inspeccionada, aunque el informe histórico de performance lo señalaba como ausente.

Los DAOs también cubren favoritos, playlists, historial de búsqueda, letras, engagement, temas de carátula y tablas por proveedor. Usa el [índice de configuración](reference/config-index.md) para localizar JSON de esquema y el [índice de código](reference/source-index.md) para todos los DAOs y entidades.

[UserPreferencesRepository](../app/src/main/java/com/theveloper/pixelplay/data/preferences/UserPreferencesRepository.kt) es un punto amplio de ajustes. Hay repositorios específicos para tema, playlists y ecualizador. Antes de añadir una clave, busca cómo se representa, su valor por defecto, su serialización y si entra en backup. No documentar ni registrar los valores privados de una instalación.

La migración 42→43 conserva identidad, carátulas y relaciones de playlists, normaliza la fuente antigua `AI` a `LOCAL` y elimina `is_ai_generated`, `ai_cache` y `ai_usage`. Los esquemas anteriores conservan el contrato histórico; no deben editarse para esconder campos retirados. La normalización también se aplica al importar listas antiguas desde preferencias o backups.

## Consultas, carpetas y bibliotecas grandes

El contrato `MusicRepository` ofrece flows de listas, Paging y consultas acotadas. No todos los consumidores necesitan materializar el catálogo completo. En una pantalla paginada, conserva filtros y orden al construir la cola: la página visible no representa necesariamente todo el contexto de reproducción.

[FolderTreeBuilder](../app/src/main/java/com/theveloper/pixelplay/data/repository/FolderTreeBuilder.kt), [FileExplorerStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/FileExplorerStateHolder.kt) y [FolderNavigationStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/FolderNavigationStateHolder.kt) cubren árbol, exploración y navegación. Las reglas de inclusión/exclusión se resuelven con [DirectoryRuleResolver](../app/src/main/java/com/theveloper/pixelplay/utils/DirectoryRuleResolver.kt) y utilidades de filtros.

Los permisos se adaptan a versiones Android mediante [MediaStorePermissionHelper](../app/src/main/java/com/theveloper/pixelplay/utils/MediaStorePermissionHelper.kt) y el manifest. El permiso de lectura no equivale a autorización para editar o borrar: revisar [SongMetadataEditor](../app/src/main/java/com/theveloper/pixelplay/data/media/SongMetadataEditor.kt), [SongRemovalStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SongRemovalStateHolder.kt) y [FileDeletionUtils](../app/src/main/java/com/theveloper/pixelplay/utils/FileDeletionUtils.kt).

## Carátulas, letras, playlists y estadísticas

| Función | Recorrido / fuentes |
| --- | --- |
| Carátulas | [AlbumArtUtils](../app/src/main/java/com/theveloper/pixelplay/utils/AlbumArtUtils.kt), [AlbumArtCacheManager](../app/src/main/java/com/theveloper/pixelplay/utils/AlbumArtCacheManager.kt), fetchers en `data/image`, Coil y [ThemeStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ThemeStateHolder.kt). Extracción, URI, caché y paleta son capas distintas. |
| Imágenes de artistas | [ArtistImageRepository](../app/src/main/java/com/theveloper/pixelplay/data/repository/ArtistImageRepository.kt), DAO y Deezer. Mantener fallbacks y comportamiento de caché. |
| Letras | [LyricsRepositoryImpl](../app/src/main/java/com/theveloper/pixelplay/data/repository/LyricsRepositoryImpl.kt), fuentes embebidas/locales/almacenadas y LRCLIB; [LyricsStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/LyricsStateHolder.kt) coordina UI. Confirmar precedencia concreta antes de cambiarla. |
| Importación de letras | [LyricsImportSecurity](../app/src/main/java/com/theveloper/pixelplay/utils/LyricsImportSecurity.kt), [LyricsUtils](../app/src/main/java/com/theveloper/pixelplay/utils/LyricsUtils.kt), [TtmlLyricsParser](../app/src/main/java/com/theveloper/pixelplay/utils/TtmlLyricsParser.kt). Tratar archivos externos como entrada no confiable. |
| Playlists | [LocalPlaylistDao](../app/src/main/java/com/theveloper/pixelplay/data/database/LocalPlaylistDao.kt), [PlaylistViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlaylistViewModel.kt), [M3uManager](../app/src/main/java/com/theveloper/pixelplay/data/playlist/M3uManager.kt). Revisar orden, duplicados y compatibilidad entre IDs. |
| Mezclas | [DailyMixManager](../app/src/main/java/com/theveloper/pixelplay/data/DailyMixManager.kt) puntúa y selecciona candidatos; [DailyMixStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/DailyMixStateHolder.kt) integra la presentación. La generación mediante LLM es otro recorrido: ver [integraciones](integrations.md). |
| Estadísticas | [ListeningStatsTracker](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ListeningStatsTracker.kt), [PlaybackStatsRepository](../app/src/main/java/com/theveloper/pixelplay/data/stats/PlaybackStatsRepository.kt), engagement y [StatsViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/StatsViewModel.kt). No confundir historial de escucha con métricas de rendimiento. |

## Al cambiar esta área

Revisa identidad → filtro/orden → persistencia → consumidores → backup, según lo afectado. Las pruebas de referencia están en `app/src/test/.../data/repository`, `data/worker`, `utils`, y `app/src/androidTest/.../data/database`. La presencia de una prueba no asegura cobertura de todas las rutas: [build y pruebas](build-and-testing.md) explica cómo seleccionar verificaciones.

