# Wear OS y protocolo compartido

## El reloj tiene dos contextos de reproducción

Wear puede controlar el teléfono mediante Data Layer y reproducir audio local mediante su propio servicio. Esa distinción debe mantenerse visible al cambiar cola, volumen, favoritos, descarga o navegación.

| Contexto | Propietarios |
| --- | --- |
| Control remoto del teléfono | [WearPlaybackController](../wear/src/main/java/com/theveloper/pixelplay/data/WearPlaybackController.kt), [WearStateRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearStateRepository.kt), receptor de comandos en teléfono |
| Reproducción local del reloj | [WearLocalPlayerRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearLocalPlayerRepository.kt) conecta con [WearPlaybackService](../wear/src/main/java/com/theveloper/pixelplay/data/WearPlaybackService.kt), que posee ExoPlayer/MediaSession |
| Biblioteca y archivos del reloj | [WearDeviceMusicRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearDeviceMusicRepository.kt), [WearTransferRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearTransferRepository.kt), [data/local](../wear/src/main/java/com/theveloper/pixelplay/data/local) |
| UI, navegación y selección de salida | [presentation](../wear/src/main/java/com/theveloper/pixelplay/presentation), `WearPlayerViewModel`, pantallas de salida, cola, volumen y descargas |

La app se inicia con [WearApp](../wear/src/main/java/com/theveloper/pixelplay/WearApp.kt) y [WearMainActivity](../wear/src/main/java/com/theveloper/pixelplay/presentation/WearMainActivity.kt). La base local `WearMusicDatabase` declara versión 5 en esta revisión, con migraciones propias; no es la misma base del teléfono.

## Contratos, no strings duplicados

[shared](../shared/src/main/java/com/theveloper/pixelplay/shared) contiene 18 archivos Kotlin de DTO y constantes. No debe depender de modelos internos de pantalla ni de servicios del teléfono. [WearDataPaths](../shared/src/main/java/com/theveloper/pixelplay/shared/WearDataPaths.kt) es la fuente de nombres de rutas y claves de transporte.

| Familia | Transporte / contratos | Contrapartes principales |
| --- | --- | --- |
| Estado del player y portada | DataItem; `WearPlayerState`, paleta y letras; portada como Asset | [WearStatePublisher](../app/src/main/java/com/theveloper/pixelplay/data/service/wear/WearStatePublisher.kt) → [WearDataListenerService](../wear/src/main/java/com/theveloper/pixelplay/data/WearDataListenerService.kt) |
| Acciones de reproducción | MessageClient; `WearPlaybackCommand`, `WearPlaybackResult` | `WearPlaybackController` → [WearCommandReceiver](../app/src/main/java/com/theveloper/pixelplay/data/service/wear/WearCommandReceiver.kt) y respuesta |
| Volumen | Mensajes; `WearVolumeCommand`, `WearVolumeState` | Controller y [WearVolumeRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearVolumeRepository.kt) ↔ teléfono |
| Navegación de biblioteca | Mensajes; request/response y library items | [WearLibraryRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearLibraryRepository.kt) ↔ receptor del teléfono |
| Transferencia | Mensajes de solicitud/metadatos/progreso y ChannelClient para audio/portada | Coordinadores del teléfono ↔ `WearTransferRepository` |
| Favoritos | Mensajes y `WearFavoriteSyncPayload` | [WearFavoriteSyncRepository](../wear/src/main/java/com/theveloper/pixelplay/data/WearFavoriteSyncRepository.kt) ↔ teléfono |

El DTO de estado es intencionalmente más pequeño que el estado completo de la app. `queueRevision` permite detectar cambios de cola; `positionUpdatedElapsedRealtimeMs` es un anclaje local del reloj y su comentario indica que no lo envía el teléfono. No confundir tiempo monotónico local con timestamp transportado.

## Transferir una canción

El recorrido combina solicitud identificada, metadatos, stream, progreso y registro local. [WearTransferMetadata](../shared/src/main/java/com/theveloper/pixelplay/shared/WearTransferMetadata.kt) incluye `requestId`, identidad, formato y datos para crear la entrada local. Los archivos y la portada viajan por canales separados.

En el teléfono, [PhoneDirectWatchTransferCoordinator](../app/src/main/java/com/theveloper/pixelplay/data/service/wear/PhoneDirectWatchTransferCoordinator.kt), [WearPhoneTransferSender](../app/src/main/java/com/theveloper/pixelplay/data/service/wear/WearPhoneTransferSender.kt), stores de cancelación/estado y [WatchTransferForegroundService](../app/src/main/java/com/theveloper/pixelplay/data/service/wear/WatchTransferForegroundService.kt) coordinan el trabajo. En reloj, el listener despacha a `WearTransferRepository`. Existen caminos de reproducción temporal, además de descargas persistentes.

Al cambiar transferencias, comprobar correlación de `requestId`, orden de llegada de metadatos/canal, cancelación, errores de espacio/IO, archivos parciales y actualización de DB. No concluir que «se envió el mensaje» significa «la canción quedó descargada».

## Compatibilidad y validación

Para un campo nuevo, revisar valores por defecto y decodificación en ambos extremos. Para una acción nueva, revisar DTO, emisor, receptor, respuesta y manifest/capabilities pertinentes. No cambiar rutas en un solo módulo.

La firma e identidad de las aplicaciones forman parte de la conexión teléfono/reloj; ambos builds declaran el mismo applicationId base y el sufijo debug. Ver [build](build-and-testing.md). Esta revisión no emparejó dispositivos ni probó transmisión o reproducción local: el protocolo descrito proviene del código.

Para una entrega funcional se necesitan teléfono y reloj/emulador apropiados, reconexión, control remoto, modo local, transferencia cancelada y comportamiento offline según el cambio. No hay un conjunto equivalente de tests en `wear/src/test` en esta copia; documentar la evidencia real de la verificación manual.

