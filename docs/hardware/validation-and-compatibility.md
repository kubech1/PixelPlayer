# Validación futura y compatibilidad de audio

Fecha de preparación: **31 de octubre de 2026**. Este documento es un **plan pendiente de ejecución** y una relación de hallazgos estáticos. La investigación de hardware no demuestra una mejora de rendimiento ni autoriza por sí misma una nueva función. El usuario definirá a continuación las optimizaciones deseadas.

## Relación con el código actual

| Propietario | Observado mediante lectura estática | Qué debe evitar una decisión futura |
| --- | --- | --- |
| [MusicService](../../app/src/main/java/com/theveloper/pixelplay/data/service/MusicService.kt) y [manifest](../../app/src/main/AndroidManifest.xml) | Servicio `MediaLibraryService` y declaración `mediaPlayback` | Dar por verificadas todas las reanudaciones/background de Android 17 por usar Media3 |
| [DualPlayerEngine](../../app/src/main/java/com/theveloper/pixelplay/data/service/player/DualPlayerEngine.kt) | Dos players, sesiones de audio, float configurable, preferencias de offload y mecanismos de fallback | Activar todo simultáneamente sin revisar transiciones, sesiones y recuperación ya existentes |
| [HiFiCapabilityChecker](../../app/src/main/java/com/theveloper/pixelplay/data/service/player/HiFiCapabilityChecker.kt) | Prueba `PCM_FLOAT`, estéreo a 44.100 Hz, primero buffer y luego creación de `AudioTrack`; caché invalidada por cambios de ruta | Interpretar ese resultado como certificación de DAC de 32 bits, alta resolución de toda ruta o enlace Bluetooth sin pérdidas |
| [AppHaptics](../../app/src/main/java/com/theveloper/pixelplay/presentation/utils/AppHaptics.kt) | Eventos semánticos compartidos por Compose/View, tres intensidades, envelopes sujetos a límites y fallback estándar | Confundir control implementado con calibración o sensación verificada en el motor S24 |
| [Configuración app](../../app/build.gradle.kts) | SDK de compilación y target 37 | Evaluar únicamente cambios Android 17 independientes del target |

Estos puntos orientan revisión, no diagnostican por sí solos bugs. La prueba float crea un objeto de reproducción, pero no reproduce una señal ni mide su salida. Su resultado no contiene información del códec Bluetooth negociado. El comportamiento completo del motor se explica en [reproducción](../playback.md).

## Reglas para diseñar las optimizaciones siguientes

1. **Capacidad y ruta antes que nombre comercial.** Un modelo conocido sirve para seleccionar pruebas. Un nombre Bluetooth editable no debe activar ajustes eléctricos, EQ o ganancias por sí solo.
2. **Fallback utilizable.** Mantener reproducción normal cuando falte soporte, cambie el dispositivo, falle una consulta o el usuario rechace un permiso. No tratar un receptor desconocido como error.
3. **Preferencias explícitas.** Si se añaden perfiles, deberán ser opcionales, reversibles y no propagarse accidentalmente a otra salida. Definir cómo se identifica y olvida una asociación sin almacenar direcciones o identificadores privados en logs/documentación.
4. **Continuidad antes que tasas máximas.** No forzar LDAC, 96/192 kHz, PCM float, offload o latencias mínimas globalmente. Seleccionar según necesidad y soporte real, y medir el resultado.
5. **Procesamiento consciente.** Distinguir EQ, normalización, headroom, limitación y DSP del sistema/receptor. No aumentar volumen al conectar ni corregir una respuesta acústica con cifras inventadas.
6. **Conservar comportamiento multiplataforma.** La salida local del teléfono, Cast y reproducción local en Wear no comparten necesariamente ruta de audio ni capacidad. Un cambio exclusivo del S24 no debe contaminar esos contratos.
7. **Rendimiento según actividad.** Animar y emitir estado cuando aporte valor. No mantener GPU, CPU, pantalla o vibrador activos para demostrar uso del hardware.

Estas restricciones guían la implementación aceptada en [ADR-0002](../decisions/0002-android17-and-no-ai.md) y [ADR-0003](../decisions/0003-audio-output-observation.md). Si nuevos requisitos introducen alternativas relevantes, evaluarlas con `architecture-decision`.

## Inventario mínimo antes de medir

Registrar en una ficha de prueba: fecha; modelo; Android/One UI/build/CSC/parche; versión/variante de PixelPlayer; firmware del receptor; batería, temperatura ambiente y modo de energía; fuente sintética o identificada sin exponer biblioteca privada; ruta; volumen; ANC/ambiente; efectos app/sistema/receptor; multipunto o Stereo Pair; distancia/interferencia y códec observado cuando sea accesible.

Separar en cada campo **declarado**, **consultado**, **observado** y **no disponible**. No guardar números de serie, MAC, cuentas ni dumps completos con datos personales. No modificar ajustes globales de radio, límites térmicos o políticas de batería como preparación automática.

## Matriz de rutas

Todos los casos figuran **pendientes**; las capacidades de las fichas no sustituyen estas pruebas.

| Ruta | Variantes mínimas | Resultado a observar |
| --- | --- | --- |
| Altavoces SM-S921B | Pantalla encendida/apagada; orientación; efectos sistema | Continuidad, estéreo, volumen estable y ausencia de saturación introducida |
| WH-1000XM5 Bluetooth | LDAC cuando se negocie; AAC/SBC cuando proceda; multipunto y ANC | Estabilidad, reconexión, controles, focus y procesamiento acumulado |
| Buds FE Bluetooth | SSC cuando se negocie; alternativa AAC/SBC; uno/dos auriculares | Reconexión, controles, ANC/ambiente y cambio de ruta durante llamada |
| XB100 Bluetooth | AAC/SBC; batería/carga; par solo si se dispone de segundo XB100 | Mezcla audible de canales, cambios de modo y estabilidad; sin suponer entrada USB |
| Radio de automóvil | Códec/perfiles reales sin imponerlos; arranque/parada del coche | Play/pause, metadatos, reconexión y volumen conservado; pruebas estacionado |
| Otro receptor desconocido | Capacidades mínimas y consultas fallidas/no disponibles | Audio y controles funcionales sin heredar perfil personal |
| USB opcional | Solo con DAC compatible disponible | Formatos informados/aceptados, desconexión y ruta de retorno; no presuponer bit-perfect |
| Cast y Wear | Remoto y reproducción local según función existente | Separación de sesión/ruta y ausencia de regresión por ajustes del teléfono |

Para cada ruta relevante: abrir canción, buscar posición, cambiar pista, transición/gapless/crossfade según configuración, pausar/reanudar, conectar/desconectar, apagar pantalla, llamada/notificación de otra app y recuperación. Probar llamadas no exige grabar conversaciones; basta observar interrupción/ruta/retorno con un escenario controlado.

## Qué medir y qué no inferir

| Pregunta | Evidencia que serviría | Evidencia insuficiente |
| --- | --- | --- |
| ¿La UI aprovecha 120 Hz sin trabajo extra? | Frame timing/jank en scroll, letras y reproductor; mismo escenario y modo de pantalla | Pantalla anunciada de 120 Hz o impresión subjetiva aislada |
| ¿Consume menos al reproducir? | Ensayos comparables con duración, batería/temperatura, ruta y carga de trabajo controladas | Una única lectura instantánea de CPU o cambio del flag de offload |
| ¿Offload funciona y conserva funciones? | Estado real durante reproducción, formato/ruta, continuidad y transiciones | Disponibilidad de API o preferencia «enabled» |
| ¿Mejora el audio? | Objetivo definido; comparación con volumen equiparado y, si procede, medición acústica válida | Impedancia, diámetro o número de bits sin escuchar/medir el recorrido |
| ¿La háptica es adecuada? | Capacidades consultadas y evaluación de acciones concretas con preferencias respetadas | Inferir actuador/frecuencia a partir de la gama del teléfono |
| ¿Es compatible con Android 17? | Recorridos de servicio/focus/widget ejecutados, con resultados y condiciones | Compilar con SDK 37 o inspeccionar el manifest |

La salida directa/offload debe consultarse para formato/atributos y reevaluarse al cambiar ruta; soporte no garantiza disponibilidad de recursos en todo momento. El soporte de gapless y velocidad puede diferir del soporte básico de offload. Además, **audio offload de reproducción y offload del codificador Bluetooth son conceptos distintos**. [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [capacidades de offload Media3](https://developer.android.com/reference/androidx/media3/exoplayer/RendererCapabilities.AudioOffloadSupport), [arquitectura Bluetooth AOSP](https://source.android.com/docs/core/connect/bluetooth/services).

## Registrar una conclusión sin perder sus límites

Usar el formato: **pregunta → condiciones → método → resultado → incertidumbre → decisión**. Si hay regresión, reproducirla y aplicar `systematic-debugging` antes de introducir workarounds por fabricante. Si no existe evidencia de mejora, conservar la alternativa estable y registrar qué falta.

Cuando se implemente un cambio, seleccionar build, tests, lint y pruebas físicas pertinentes de [build y pruebas](../build-and-testing.md). Ningún comando de compilación valida por sí mismo sonido, autonomía, refresco ni vibración. Actualizar estas páginas en la misma tarea y enlazar resultados reproducibles sin reemplazar las fichas oficiales por observaciones de una sola unidad.

## Estado después de implementar

La observación de salida es compartida y conserva desconocidos. XM5, Buds FE, XB100 y receptores ajenos utilizan la negociación normal de Android/Media3; no hay selección por nombre. Las pruebas automatizadas y de emulador figuran en [modernización](../modernization-status.md). La matriz física de receptores externos continúa pendiente: no inferir mejoras de sonido, latencia, jank, autonomía o temperatura a partir de la compilación. Cast y reproducción local Wear mantienen propietarios y rutas independientes.

### Verificación física registrada en SM-S921B (5 de noviembre de 2026)

- **Pregunta:** ¿El modo Hi-Fi (`PCM_FLOAT`) reproduce pistas FLAC masterizadas al límite digital (`0.0 dBFS` con picos inter-muestra superiores a `1.0`) sin distorsión en los altavoces integrados del SM-S921B con One UI 9 / Android 17?
- **Condiciones:** Samsung Galaxy S24 (`SM-S921B`), Android 17 (SDK 37), One UI 9 (`mOneUIversion=90000`), salida por altavoces internos (`AUDIO_DEVICE_OUT_SPEAKER`) en el hilo `AudioOut_15` (`AUDIO_OUTPUT_FLAG_DEEP_BUFFER`, `48000 Hz`, `HAL format: AUDIO_FORMAT_PCM_8_24_BIT`), efectos del sistema (`Dolby_FX`, `SoundAlivePlus`, `SoundBoosterPlus`, `SoundBoosterEQ`) activos en el mezclador.
- **Método:** Inspección de `logcat` (`DualPlayerEngine`, `HiFiAudio`, `CCodec`, `CCodecConfig`, `AudioTrack`), `dumpsys media.player` y `dumpsys media.audio_flinger` (`Signal power history` a 1000 ms y 50 ms) antes y después de desplegar la build debug actualizada.
- **Resultado:**
  - En `dumpsys media.player`, el sistema registra el decodificador propietario `c2.sec.flac.decoder` (`rank: 1`, `software-only: 1`) por delante del decodificador AOSP `c2.android.flac.decoder` (`rank: 8`).
  - Tras actualizar [AudioDecoderPolicy](../../app/src/main/java/com/theveloper/pixelplay/data/service/player/AudioDecoderPolicy.kt), [ObservedAudioSink](../../app/src/main/java/com/theveloper/pixelplay/data/audio/ObservedAudioSink.kt) y [HiFiAudioProcessor](../../app/src/main/java/com/theveloper/pixelplay/data/service/player/HiFiAudioProcessor.kt), `logcat` confirmó la selección de `c2.android.flac.decoder`, la configuración `in(enc=4, sr=44100, ch=2) -> out(enc=4, sr=44100, ch=2)` con `preGain=0.7943` (`-2.0 dB`), `ispGain=1.0000` y `outPeak=0.6187` en el primer bloque de 4608 cuadros, y `AudioFlinger` registró un `AudioTrack` en `AUDIO_FORMAT_PCM_FLOAT` (`00000005`) con `Underruns=0` y potencia de señal dinámica (`-24.9 dB` a `-14.7 dB` con `PortVol=-9 dB`) sin saturación plana.
- **Incertidumbre:** La verificación física cubre la ruta de altavoz interno del SM-S921B; las rutas Bluetooth (WH-1000XM5, Buds FE, SRS-XB100) y USB-C siguen pendientes de medición en dispositivo.
- **Decisión:** Mantener `HiFiAudioProcessor` como único propietario del acondicionamiento `PCM_FLOAT` dentro de `ObservedAudioSink` y priorizar los decodificadores software de referencia AOSP (`c2.android.*` / `omx.google.*`) frente a los decodificadores software propietarios `c2.sec.*` / `omx.sec.*`.
### S24 con WH-1000XM5 por Bluetooth (5 de noviembre de 2026)

En el SM-S921B con SDK 37, la build debug reprodujo por MediaStore los tres FLAC con Hi-Fi activo. El track activo de PixelPlayer en AudioFlinger usó PCM float estéreo a 44 100 Hz. El hilo de salida A2DP mezcló a 96 000 Hz con HAL PCM de 32 bits. Android registró LDAC a 96 000 Hz. Una prueba adicional con un FLAC sintético de 24 bits/192 kHz registró entrada, salida del procesador y track a 192 000 Hz en la misma ruta. Las capturas puntuales mostraron cero underruns. Quedan sin medir el bitrate LDAC instantáneo, la resolución efectiva y la señal acústica de los auriculares.

Se decodificaron las pistas completas a float estéreo y se procesaron con la clase compilada de la app en JVM, EQ plano y bloques de 4608 cuadros. Se midió el pico reconstruido con FFmpeg `ebur128=peak=true` antes y después del procesador. El margen anterior de 2 dB dejaba la Muestra A en +2,1 dBTP.

| Muestra | FLAC | Pico original | Pico con margen de 5,5 dB | Ataque de grave analizado |
| --- | --- | ---: | ---: | --- |
| Muestra A | 16 bits/44,1 kHz | +4,8 dBTP | −0,7 dBTP | 3:02,29 |
| Muestra B | 24 bits/44,1 kHz | −1,1 dBTP | −6,6 dBTP | 2:25,68 |
| Muestra C | 16 bits/44,1 kHz | +1,9 dBTP | −3,6 dBTP | 0:43,50 |

Para localizar ataques, se filtró la banda de 30–120 Hz y se comparó la energía estéreo en ventanas contiguas de 50 ms, con paso de 10 ms. En dos segundos alrededor de cada marca, tras compensar los 5,5 dB, la diferencia máxima muestra a muestra entre el original y el PCM procesado quedó por debajo de −144 dBFS. Los picos de la tabla corresponden a cada canción completa. Esta comparación termina antes de AudioFlinger, de la compresión LDAC y del procesado interno de Sony.

#### Reproducción continua de los fragmentos con beats en el dispositivo

Se reprodujeron dos veces cuatro tramos de los archivos de prueba por MediaStore con HiFi activo. En la primera pasada, con el APK habitual, AudioFlinger registró un único track activo del reproductor, PCM float estéreo a 44,1 kHz, en el mezclador A2DP a 96 kHz con los XM5 activos y LDAC. El contador de underruns del track marcó cero en las 92 capturas. Los máximos de potencia digital de banda completa en ventanas de 50 ms fueron −6,8 dB en la Muestra A (2:10–2:55), −10,5 dB en la Muestra B (2:20–2:51) y −10,6 / −10,3 dB en la Muestra C (0:40–1:02 / 1:48–1:56). Las posiciones de estos máximos se alinearon con MediaSession y son aproximadas.

En la segunda pasada, una build temporal comparó las muestras de entrada y salida del procesador directamente en el S24, por bloque con timestamp de presentación. Se incluyeron los bloques completos que tocan los límites de cada tramo (hasta 105 ms por bloque). Los 1019 bloques cubrieron los cuatro intervalos sin huecos. La máxima diferencia respecto de la entrada multiplicada por la ganancia fija de −5,5 dB quedó por debajo de −150 dBFS. Esto verifica la conservación de la forma de onda en estos tramos hasta la entrega al sink. La potencia del mezclador y los picos de muestra miden magnitudes distintas. La distorsión posterior al códec y la salida acústica quedan fuera de esta medición.

### Háptica expresiva y refresco 120 Hz en SM-S921B con One UI 9 (6 de noviembre de 2026)

- **Pregunta:** ¿Qué capacidades reales de actuador háptico y refresco adaptativo expone el `SM-S921B` en One UI 9 / Android 17, y cómo afectan a `HapticPolicy` e `InteractiveRefreshRate`?
- **Condiciones:** Samsung Galaxy S24 (`SM-S921B`), Android 17 (SDK 37), One UI 9 (`mOneUIversion=90000`), modo de fluidez de movimiento adaptativo (`REFRESH_RATE_MODE_SEAMLESS`).
- **Método:** Consulta directa mediante `adb shell dumpsys vibrator_manager` y `adb shell dumpsys display` sobre la unidad conectada, pruebas unitarias JVM en [HapticPolicyTest](../../app/src/test/java/com/theveloper/pixelplay/presentation/utils/HapticPolicyTest.kt) y despliegue de la build debug.
- **Resultado:**
  - `vibrator_manager` reporta `MOTOR_LINEAR_INDEX (5)` (`SEM_SUPPORTED_VIBRATION_TYPE_D`), sin soporte de envelopes PWLE (`mMaxEnvelopeEffectSize: 0`), y primitivas calibradas de 20 ms (`CLICK`, `TICK`, `LOW_TICK`), 100 ms (`QUICK_FALL`), 130 ms (`SPIN`), 150 ms (`QUICK_RISE`), 300 ms (`THUD`) y 500 ms (`SLOW_RISE`). Se reemplazó el uso de `THUD` (300 ms) y los pulsos simples por composiciones multi-primitiva expresivas en [HapticPolicy](../../app/src/main/java/com/theveloper/pixelplay/presentation/utils/HapticPolicy.kt) y se conectaron los eventos semánticos a los controles de transporte, toggles, barra de progreso y navegación inferior.
  - `display` reporta `hasArrSupport false`, `frameRateCategoryRate {normal=60.0, high=90.0}` y `renderFrameRate 120.00001` bajo `REFRESH_RATE_MODE_SEAMLESS` (`10–120 Hz`). En [InteractiveRefreshRate](../../app/src/main/java/com/theveloper/pixelplay/presentation/utils/InteractiveRefreshRate.kt) se condicionó `Modifier.preferredFrameRate(FrameRateCategory.High)` a `view.display?.hasArrSupport() == true` para no degradar la ventana de 120 Hz a 90 Hz en One UI 9.
- **Incertidumbre:** La preferencia subjetiva de intensidad (`LOW`/`MEDIUM`/`HIGH`) depende del usuario; el selector y previsualizador en Ajustes permiten ajustarla en vivo.
- **Decisión:** Conservar la prioridad de composiciones multi-primitiva sobre `MOTOR_LINEAR_INDEX (5)` sin `THUD`, y delegar el refresco de 120 Hz al modo `SEAMLESS` de One UI 9 cuando `hasArrSupport()` sea `false`.

### Diagnóstico de similitud acústica en SM-S921B (11 de noviembre de 2026)

- El [diagnóstico instrumentado](../../app/src/androidTest/java/com/theveloper/pixelplay/data/audio/LocalAudioAnalyzerDiagnosticTest.kt) se ejecutó en el S24 con Android 17, API 37 y build debug. Decodificó las muestras sintéticas WAV, FLAC, MP3, AAC y Opus. La variante del mismo tono a menor ganancia quedó por encima de la pista sintética de contraste.
- En una ejecución acotada sobre la biblioteca local, se decodificaron 6 de 6 FLAC y la segunda lectura de cada pista usó la caché de sesión. El recorrido tomó 85,842 s de reloj y 42,088 s de CPU del hilo llamador. El PSS de todo el proceso fue 175 327 KiB al inicio y el mayor valor muestreado fue 214 198 KiB.
- El intento de escanear la biblioteca completa excedió el límite de 240 s del diagnóstico de host. Las cifras de seis pistas caracterizan esa ejecución y dejan pendiente el coste de una biblioteca grande. El test guarda solo datos agregados.
- La evaluación auditiva ciega, el consumo energético y la interrupción de un lote al comenzar reproducción siguen pendientes. El analizador permanece en el source set debug y todavía no alimenta los mixes de Inicio.

### Rediseño de Inicio en emulador API 37

- Con 30 pistas sintéticas y compilación ART `speed` en ambos APK debug, cinco arranques en frío dieron una mediana hasta el primer fotograma de 1,63 s en la versión anterior y 1,76 s en la versión con «A mano». En cuatro recorridos de ida y vuelta, el p95 de tiempo por fotograma pasó de 150 a 101 ms.
- La mediana de `Fully drawn` fue de 1,93 s en la versión anterior y 3,80 s con «A mano». La versión anterior permitía declarar la pantalla lista tras el temporizador del marcador de carga. La actual espera a que se resuelva la selección de Inicio. El emulador y los criterios distintos limitan la comparación.

### Prueba del flujo de escucha ciega en emulador API 37

- Con el catálogo sintético ampliado, el diagnóstico decodificó 42 pistas y preparó diez comparaciones, con cinco candidatos por método y referencia. Doce pistas tenían género. Se usó una semilla fija para seleccionar referencias porque la biblioteca de prueba no tenía favoritos. La preparación tomó 39,1 s de reloj y 10,7 s de CPU del hilo llamador, con un máximo de PSS muestreado de 87 617 KiB para el proceso.
- Se verificó que los botones de respuesta permanecen deshabilitados hasta que termina la reproducción de la referencia y del candidato. Al omitir las diez referencias, el resumen indicó que la muestra estaba incompleta y deshabilitó los controles de la sesión.
- La prueba de calidad perceptual en la biblioteca real y las mediciones operativas en el S24 siguen pendientes.
