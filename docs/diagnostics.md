# Diagnósticos y rendimiento

## Tres clases de evidencia

1. **Instrumentación de la app:** registra lo que ocurre durante uso real.
2. **Pruebas y benchmarks:** ejecutan escenarios concretos bajo condiciones registradas.
3. **Inspección estática:** identifica patrones y formula hipótesis; no mide jank ni latencia por sí sola.

No convertir un hallazgo de la tercera categoría en un resultado de las dos primeras.

## Instrumentación disponible

| Componente | Para qué sirve |
| --- | --- |
| [PerformanceMetrics](../app/src/main/java/com/theveloper/pixelplay/data/diagnostics/PerformanceMetrics.kt) | Agregados y contadores de operaciones instrumentadas |
| [DebugPerformanceReportCollector](../app/src/main/java/com/theveloper/pixelplay/data/diagnostics/DebugPerformanceReportCollector.kt) | Ensamblar el snapshot de dispositivo, app, biblioteca, playback y diagnósticos |
| [DebugPerformanceReport](../app/src/main/java/com/theveloper/pixelplay/data/diagnostics/DebugPerformanceReport.kt) | Modelo y presentación del informe |
| [AdvancedPerformanceDiagnostics](../app/src/main/java/com/theveloper/pixelplay/data/diagnostics/AdvancedPerformanceDiagnostics.kt), controller y [MainThreadStallMonitor](../app/src/main/java/com/theveloper/pixelplay/data/diagnostics/MainThreadStallMonitor.kt) | Diagnóstico adicional y observación de stalls |
| [DeviceCapabilitiesViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/DeviceCapabilitiesViewModel.kt) y [pantalla](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/DeviceCapabilitiesScreen.kt) | Entrada de UI a capacidades e informe |
| [ReleaseTree](../app/src/main/java/com/theveloper/pixelplay/ReleaseTree.kt), [CrashHandler](../app/src/main/java/com/theveloper/pixelplay/utils/CrashHandler.kt) | Logging release y persistencia de fallos |

El collector lee datos del motor en `Dispatchers.Main` y hace agregación/ensamblado en `Dispatchers.IO`. Mantener el confinamiento al hilo del player. Algunas cifras son observaciones de la sesión o estimaciones, no un censo exhaustivo de todos los archivos.

La documentación especializada [performance-report.md](performance-report.md) explica instrumentación y exportación; se conserva como referencia con una nota de vigencia. Para el contrato exacto del payload revisar modelo y collector actuales, incluidos diagnósticos avanzados.

## Cómo investigar un síntoma

| Síntoma | Recorrido inicial |
| --- | --- |
| Primer escaneo lento | `SyncWorker`, `AudioMetadataReader`, extracción/caché de portada |
| Scroll o expansión de secciones | Pantalla/componente afectado, subscriptions de estado, trabajo durante composición, medición de frames |
| Corte al cambiar canción | Motor dual, preparación de fuente, transición, audio focus y offload |
| Bloqueos al abrir player | Hidratación, controller, tema/portada y operaciones en main |
| Consumo o cortes en reloj | Transferencias, servicio local del reloj, conexión y estado remoto/local |

Registrar pasos de reproducción, build, dispositivo, tamaño de biblioteca, formato y ruta de audio. Capturar el escenario antes/después en condiciones comparables. Si el dato depende de una función activable, documentar su configuración.

## Informes históricos

[app/performance_analysis.md](../app/performance_analysis.md) es una revisión estática anterior, con estimaciones, líneas históricas e hipótesis expresas. En la inspección inicial actual, `PlayerUiState` ya no contiene `currentPosition`: el estado estable y la posición tienen propietarios separados. `SongEntity` también declara el índice `file_path` que aquel informe señalaba como ausente. No aplicar todas sus recomendaciones como si describieran defectos vigentes.

El documento puede orientar nuevas preguntas; cada hallazgo debe revalidarse contra código y medición antes de convertirlo en tarea. No se eliminaron sus observaciones originales.

## Privacidad y verificación

Revisar el payload concreto antes de compartir un diagnóstico. No dar por hecho que cualquier log, excepción o informe futuro excluye identificadores solo porque una versión del reporte lo declaraba. Redactar tokens, rutas privadas y contenido de biblioteca en artefactos compartidos.

Las pruebas de `app/src/test/.../data/diagnostics` verifican lógica/estructuras. Los macrobenchmarks y perfiles viven en [baselineprofile](../baselineprofile/src/main/java/com/theveloper/pixelplay/baselineprofile); su ejecución se detalla en [build y pruebas](build-and-testing.md). Esta tarea documental no produjo perfiles, mediciones en dispositivo ni cifras nuevas de rendimiento.

## Interpretación de capacidades de audio

La pantalla usa la misma instantánea de salida que el servicio y se refresca al reanudarse. Distingue dispositivos disponibles, ruta prevista por Android, configuración de sink y sesión local. La ruta no certifica el recorrido final. El códec Bluetooth puede ser desconocido; PCM float no identifica LDAC/SSC/SBC ni el DAC.

Los formatos de offload se consultan a 44,1 kHz estéreo mediante el bitmask de `getDirectPlaybackSupport`; se separan de la preferencia del motor y del offload/reposo observado. Los codecs usan el flag oficial de aceleración y exponen vendor por separado; el reporte del decoder activo conserva la inferencia por nombre como un campo distinto. Un decoder de extensión sin flag de plataforma puede quedar desconocido. Cambiar la ruta no vuelve a ejecutar la consulta de toda la biblioteca ni el cálculo de tamaños de archivos.
