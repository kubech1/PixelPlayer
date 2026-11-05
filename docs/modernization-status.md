# Estado de modernización

Revisión: 2 de noviembre de 2026. Inicio: 31 de octubre. [Alcance aceptado](decisions/0002-android17-and-no-ai.md), [decisión de audio](decisions/0003-audio-output-observation.md) y [resumen de evidencia](verification/2026-11-02-modernization.json).

Las tres etapas están implementadas. El cierre funcional sigue pendiente: hay un ANR de arranque que aislar, comprobaciones visuales incompletas y pruebas físicas que requieren el S24 y los receptores. No se han demostrado mejoras de rendimiento, sonido, autonomía o temperatura.

## Implementación por etapa

| Etapa | Resultado en código | Evidencia y límites |
| --- | --- | --- |
| Retirar IA | Fuera proveedores, SDK, generación, traducción, workers y UI exclusivos; Daily Mix y reglas locales conservados. Room 43, migración explícita, preferencias filtradas y backups antiguos normalizados | Tests de datos, backup y mezclas; migraciones instrumentadas y arranque con fixture 42. Sin fallback destructivo |
| Android 17 | Teléfono y benchmarks minSdk 37; Wear/shared minSdk 30. Rechazos foreground/focus, háptica semántica con tres intensidades y fallback, widgets con presupuesto agregado y caché de 4 MiB | APK y tests aprobados; sensación háptica y hosts One UI pendientes |
| S24 y rutas | Observación compartida de salida/sink/sesión, recuperación acotada de EQ, indicadores oficiales separados de inferencias, refresco alto solo durante interacción, trabajo visual según visibilidad | Tests de rutas, sesiones, callbacks tardíos y recuperación; Bluetooth, ARR y consumo reales pendientes |

No cambia el protocolo teléfono/reloj. No se fuerzan códecs, tasas, EQ ni volumen por nombre Bluetooth. Las letras bilingües guardadas y las playlists antiguas siguen siendo datos musicales normales.

## Entorno y comprobaciones ejecutadas

Windows, Gradle 9.5.1, launcher JBR 25 y daemon JBR 21; SDK 37.0 y build-tools 36.0.0. AVD `Medium_Phone_API_37.0`, sin audio, sin guardar snapshot y con datos de usuario en modo de solo lectura. Las fuentes originales se respaldaron fuera del repositorio; esta copia carece de metadatos Git.

- **410 tests JVM, cero fallos, errores o skips.** La última ejecución conjunta de tests y compilaciones debug tardó 33 segundos. Incluye preferencias/backup antiguos, Daily Mix, letras, rutas, sink, EQ, límites hápticos, bitmaps y visibilidad.
- **9 tests Android, cero fallos**, runner de 52,249 segundos. Arranque en frío, instalación y ejecución quedaron dentro de cinco minutos. Incluye 42→43, recorridos exportados 25–41→43 y reparación del esquema 23; preservación de playlists, canciones, orden, carátulas, favoritos e historial; bitmap real 4096×2048, caché y entradas inválidas; háptica desactivada/vista desconectada; inflación del fallback del widget con tinte y tres acciones.
- **Teléfono debug, APK de instrumentación y Wear debug compilados.** Los manifests de APK confirman mínimo 37 en teléfono y 30 en Wear, target 37 en ambos.
- **Release con R8 y shrink de recursos aprobado antes del último ajuste de volumen.** La repetición final se interrumpió automáticamente a los 300,2 segundos, con código 124, por el límite solicitado. La compilación Kotlin release avanzó, pero no hay resultado aprobatorio de minificación/empaquetado del código final. El APK release existente pertenece a la ejecución anterior; no entregarlo como actualizado con el ajuste de volumen. Los APK locales usan la clave debug de esta máquina, también el release: no acreditar actualización sobre una instalación firmada con otra clave.
- **Auditoría de dependencias release y DEX debug:** no aparecen los SDK retirados de inferencia Google, TensorFlow o PyTorch. Se conservaron librerías compartidas con funciones musicales.
- **Lint ejecutado y fallido:** 3413 errores, 2412 warnings y 46 hints. Frente a la primera etapa (3433 errores), desaparecen tres `NewApi` y 17 `ExtraTranslation`; cero diagnósticos de error nuevos al comparar ID, mensaje y archivo. Esta referencia ya incorporaba la etapa uno: no es un lint inicial del código original. Predominan traducciones y otros errores previos; no se creó baseline ni se desactivaron checks. Las 26 cadenas nuevas de esta tarea están en los 12 locales. No se repite el lint completo tras el ajuste final de volumen por el límite solicitado por el usuario.

Comandos principales, desde la raíz:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :wear:assembleDebug -Ppixelplay.enableAbiSplits=false --max-workers=2 --no-parallel
.\gradlew.bat :app:assembleRelease -Ppixelplay.enableAbiSplits=false --max-workers=2 --no-parallel
.\gradlew.bat :app:lintDebug --max-workers=2 --no-parallel
python -B scripts/context_docs.py generate
python -B scripts/context_docs.py check
```

La prueba de rechazo de reproducción reprodujo una escritura indebida de volumen `1.0`. La cancelación ahora conserva el volumen fuera de un crossfade; la suite final comprueba pausa de ambos players, retirada de la cola auxiliar y ausencia de escrituras de volumen en ese caso.

## Arranque y preservación de datos

Una prueba de arranque real de la app con base sintética 42 verificó después versión 43, integridad SQLite `ok`, playlist conservada y normalizada a `LOCAL`, orden, carátula, fechas, favoritos, letras bilingües e historial conservados; tablas y columna retiradas ausentes. No se empleó biblioteca personal.

Un intento previo de transferir el fixture por stdin binario de ADB en Windows truncó el archivo; se descartó como prueba inválida. La transferencia válida utilizó `adb push`, copia mediante `run-as` y verificación de tamaño/SHA-256 antes del arranque. No se atribuyó esa prueba inválida a pérdida de datos de la app.

El AVD contenía previamente una base 43 con otra identidad de esquema. Se preservaron la base y sus sidecars antes de sustituir la base activa para pruebas. Esa instalación no constituye una migración 42→43; no se añadió un borrado o migración destructiva para ocultar la discrepancia. Una prueba anterior de la primera etapa también comprobó arranque con base nueva.

## Revisión funcional breve e incidente abierto

El 2 de noviembre se abrió la app sobre el fixture válido y se observó reproducción de la pista sintética local de 20 segundos: UI en reproducción y MediaSession al final con posición aproximada de 20.033 ms, sin error de reproducción. El audio del emulador estaba deshabilitado; no es una escucha ni certifica gapless/crossfade.

Se navegó por Home, Settings y Behavior. La intensidad baja quedó comprobada por semántica `checked`; una captura posterior confirma la intensidad alta y los controles visibles en tema oscuro/inglés. La automatización devolvió `null root node` y reutilizó un XML anterior: se descartaron sus aserciones posteriores, sin corregir la app a partir de una lectura inválida. No se da por aprobado el recorrido completo de tres previews, desactivación desde UI, temas/locales o la pantalla de capacidades.

Durante el arranque en frío se observaron diálogos de System UI y de PixelPlayer sin respuesta. ActivityManager registró un **ANR en `SystemJobService` de WorkManager: `No response to onStartJob`**, carga 30,04 y presión de CPU avg10 de 92,98 %. La app se recuperó al esperar y permitió navegar/reproducir. El buffer de crashes no mostró una excepción Java nueva de PixelPlayer; sí registró un fallo del auxiliar nativo `crash_dump`. Estos datos sugieren saturación del entorno, pero **la causa del ANR no está establecida**: hay que aislar arranque de app/WorkManager y repetir en condiciones estables antes de cerrar aceptación funcional.

El diálogo separado de `Unable to start service ... MusicService`, fechado el 26 de octubre, era un informe guardado anterior; se descartó de la evaluación de crashes nuevos. Los logs y capturas sintéticos de trabajo se conservaron fuera del repositorio.

## Pendientes de aceptación

- Completar la minificación y el empaquetado release del último ajuste en una ventana de build apropiada, conservando el límite acordado para cada ejecución.
- Aislar el ANR observado y completar UI de capacidades, tres previews, temas/locales y movimiento reducido con lecturas actuales; no dar por resueltos por compilar o pasar tests.
- En S24 real: actualización de una instalación anterior, pantalla apagada, pausa prolongada, llamadas/focus, reanudación, notificación, pantalla bloqueada y widgets.
- WH-1000XM5, Buds FE, SRS-XB100 y receptor desconocido: seek, gapless/crossfade, cambios de salida, reconexiones y conservación de volumen/EQ. Mantener negociación normal y rutas Cast/Wear independientes.
- Háptica real: intensidades, preferencias del sistema, capacidades ausentes y fallback. No usar vibración continua con música.
- Benchmarks antes/después: seis iteraciones por ejecución, tres ejecuciones comparables a 60 Hz/adaptativo; registrar p95, jank y estado térmico.
- Reproducción comparable de diez minutos por ruta: continuidad, consumo y temperatura. No declarar mejoras dentro de la variación entre ensayos.

## Historia útil de verificación

La referencia original del 31 de octubre se interrumpió durante optimización de Kotlin; no fue un build aprobado. En la primera etapa pasaron 390 tests JVM y seis Room; en la segunda, 398 JVM y ocho Android. El resultado actual los sustituye para el código final. Los conteos no equivalen a cobertura exhaustiva ni a validación física.
