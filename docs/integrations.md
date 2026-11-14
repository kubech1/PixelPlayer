# Integraciones externas y controles adicionales

## Dos tipos de integración

Los proveedores musicales aportan biblioteca, URLs y credenciales. Los controles externos —Cast, Android Auto, widgets y Wear— actúan sobre reproducción o la trasladan a otra ruta. No son intercambiables: añadir una fuente de canciones no añade automáticamente soporte para reproducirla en todos los destinos.

## Proveedores musicales

| Proveedor / función | Código propietario | Recorrido de UI |
| --- | --- | --- |
| Telegram | [data/telegram](../app/src/main/java/com/theveloper/pixelplay/data/telegram): cliente TDLib, repositorio, caché y proxy | [presentation/telegram](../app/src/main/java/com/theveloper/pixelplay/presentation/telegram) |
| Google Drive | [data/gdrive](../app/src/main/java/com/theveloper/pixelplay/data/gdrive): API, repositorio y proxy | [presentation/gdrive](../app/src/main/java/com/theveloper/pixelplay/presentation/gdrive) |
| Netease | [data/netease](../app/src/main/java/com/theveloper/pixelplay/data/netease), red y cifrado en `data/network/netease` | [presentation/netease](../app/src/main/java/com/theveloper/pixelplay/presentation/netease) |
| QQ Music | [data/qqmusic](../app/src/main/java/com/theveloper/pixelplay/data/qqmusic), interceptores y firmas en [data/remote](../app/src/main/java/com/theveloper/pixelplay/data/remote) | [presentation/qqmusic](../app/src/main/java/com/theveloper/pixelplay/presentation/qqmusic) |
| Navidrome | [data/navidrome](../app/src/main/java/com/theveloper/pixelplay/data/navidrome), API/parser en [data/network](../app/src/main/java/com/theveloper/pixelplay/data/network), [NavidromeSyncWorker](../app/src/main/java/com/theveloper/pixelplay/data/worker/NavidromeSyncWorker.kt) | [presentation/navidrome](../app/src/main/java/com/theveloper/pixelplay/presentation/navidrome) |
| Jellyfin | [data/jellyfin](../app/src/main/java/com/theveloper/pixelplay/data/jellyfin), API/parser en `data/network` | [presentation/jellyfin](../app/src/main/java/com/theveloper/pixelplay/presentation/jellyfin) |
| LRCLIB / Deezer | [LrcLibApiService](../app/src/main/java/com/theveloper/pixelplay/data/network/lyrics/LrcLibApiService.kt), [DeezerApiService](../app/src/main/java/com/theveloper/pixelplay/data/network/deezer/DeezerApiService.kt); letras e imágenes de artistas | Consumidores de letras y artistas |

Cada proveedor tiene representación persistente y conversión de IDs. Antes de cambiar autenticación, revisar logout, caché, proxy y restauración. Esta documentación describe integración presente en el código, **no verifica disponibilidad del servicio ni éxito de una cuenta real**.

Los scripts [qq_sign.js](../app/src/main/assets/qq_sign.js) y [vm_new.js](../app/src/main/assets/vm_new.js) forman parte del mecanismo de firma de QQ. Son código distribuido con la app; una simplificación o reformateo requiere entender el protocolo y sus pruebas, no tratarlo como un recurso visual prescindible.

### Modo local

**Ajustes → Gestión de música → Música en la nube** habilita el catálogo y los accesos de los proveedores. Las instalaciones nuevas empiezan en modo local. En una instalación existente, la detección inicial usa la configuración de cuentas guardada y las fuentes de Telegram ya vinculadas. El valor que elija el usuario prevalece.

Al apagar la nube, las consultas y el escaneo de archivos locales siguen activos. Las sincronizaciones de los proveedores quedan suspendidas y sus pistas desaparecen de Biblioteca y Buscar. El botón de nube de Inicio y la entrada de cuentas quedan ocultos. Las cuentas, el catálogo indexado y la caché se conservan para volver a activar la función. La pertenencia a Biblioteca local depende del origen, así que una pista descargada por el proveedor conserva su origen remoto.

## Streaming y resolución de URI

[DualPlayerEngine](../app/src/main/java/com/theveloper/pixelplay/data/service/player/DualPlayerEngine.kt) distingue URIs locales, HTTP(S) y esquemas de proveedor. [CloudStreamProxy](../app/src/main/java/com/theveloper/pixelplay/data/stream/CloudStreamProxy.kt) ofrece infraestructura de proxy local, vinculada a `127.0.0.1`, con preparación, caché de URLs y transporte de contenido. Cada proveedor resuelve su fuente; no asumir que todos comparten exactamente la misma autenticación.

[CloudStreamSecurity](../app/src/main/java/com/theveloper/pixelplay/data/stream/CloudStreamSecurity.kt) valida identificadores, Range, MIME, longitud y URLs. Las reglas para hosts configurados por usuario y servicios LAN necesitan revisar el caso concreto; bloquear toda IP privada o permitir cualquier red no son sustitutos de esa política. Revisar redirecciones y credenciales junto con el destino final, no solo el string inicial.

La configuración Android está en [network_security_config.xml](../app/src/main/res/xml/network_security_config.xml). En esta copia el manifest declara `usesCleartextTraffic="false"`, pero el XML referenciado permite cleartext en `base-config`; no documentar el proyecto como «HTTPS obligatorio» a partir de un atributo aislado. La evaluación de la política efectiva por caso queda fuera de esta inspección documental.

## Mezclas locales y retirada de IA

Daily Mix y Your Mix usan reglas locales de [DailyMixManager](../app/src/main/java/com/theveloper/pixelplay/data/DailyMixManager.kt), engagement y favoritos. No necesitan modelos ni enviar la biblioteca a proveedores de IA.

Se retiraron del código del producto los clientes, prompts, generación de playlists/metadatos, traducción por IA, ajustes, pantallas y workers correspondientes. Las playlists y letras ya guardadas se conservan. [RetiredAiData](../app/src/main/java/com/theveloper/pixelplay/data/preferences/RetiredAiData.kt) contiene exclusivamente compatibilidad: claves a retirar y normalización de fuentes antiguas; no ejecuta inferencia.

Room 42→43 elimina tablas de caché/uso y convierte las playlists de fuente `AI` en `LOCAL`. Al iniciar, se cancelan trabajos antiguos por etiqueta y se retiran sus notificaciones. Los backups anteriores omiten el módulo de IA con advertencia y filtran preferencias retiradas. Consulta [backup y seguridad](backup-and-security.md) y [estado de validación](modernization-status.md).

## Cast y servidor HTTP

[CastStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/CastStateHolder.kt), [CastRouteStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/CastRouteStateHolder.kt) y [CastTransferStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/CastTransferStateHolder.kt) coordinan UI y ruta. El servicio conecta con [CastSyncCoordinator](../app/src/main/java/com/theveloper/pixelplay/data/service/CastSyncCoordinator.kt), [CastPlayer](../app/src/main/java/com/theveloper/pixelplay/data/service/player/CastPlayer.kt) y el estado remoto.

[MediaFileHttpServerService](../app/src/main/java/com/theveloper/pixelplay/data/service/http/MediaFileHttpServerService.kt) sirve recursos al destino de Cast. Es una frontera distinta del proxy loopback de proveedores. [CastSessionSecurity](../app/src/main/java/com/theveloper/pixelplay/data/service/http/CastSessionSecurity.kt) construye una política de direcciones y tokens por sesión. No eliminar esas validaciones para resolver un fallo de conectividad ni registrar URLs con tokens sin redacción.

## Android Auto, widgets, tiles y anuncios

- [AutoMediaBrowseTree](../app/src/main/java/com/theveloper/pixelplay/data/service/auto/AutoMediaBrowseTree.kt) y callbacks de `MusicService` construyen la navegación de medios para clientes externos. Hay implementación presente aunque el README original diga «Soon»; la certificación o funcionamiento completo en un vehículo no se verificó aquí.
- [WidgetUpdateManager](../app/src/main/java/com/theveloper/pixelplay/data/service/WidgetUpdateManager.kt), callbacks de [Glance](../app/src/main/java/com/theveloper/pixelplay/ui/glancewidget) y providers de notificación conectan controles con el servicio. El estado de un widget no debe convertirse en una segunda verdad de la reproducción.
- [data/service/tile](../app/src/main/java/com/theveloper/pixelplay/data/service/tile) contiene acciones de Quick Settings.
- [data/github](../app/src/main/java/com/theveloper/pixelplay/data/github) y [app-announcements.properties](../remote-config/app-announcements.properties) manejan información externa de GitHub/anuncios. No confundir configuración remota de anuncios con un backend que almacene toda la biblioteca.
- La comunicación con reloj está detallada en [Wear](wear-protocol.md).

## Imágenes de widgets en API 37

[WidgetBitmapSafety](../app/src/main/java/com/theveloper/pixelplay/ui/glancewidget/WidgetBitmapSafety.kt) dimensiona carátulas antes de crear ImageProvider. La caché conserva 4 MiB y distingue contenido SHA-256/URI, dimensiones, densidad y límite de bytes. Entradas corruptas o sin presupuesto muestran placeholder.

[WidgetBitmapPolicy](../app/src/main/java/com/theveloper/pixelplay/ui/glancewidget/WidgetBitmapPolicy.kt) reserva un presupuesto conservador conjunto para todos los size buckets: una o hasta cinco carátulas según layout, un fondo y una barra por bucket, con margen restante. Los cuatro widgets aplican el mismo presupuesto; fondos y progreso también se acotan. El framework conserva la decisión final: ante rechazo por memoria de bitmaps, se publica un RemoteViews sin carátulas con controles reales de anterior/reproducir-pausar/siguiente. Su funcionamiento bajo el host de One UI requiere validación física.
