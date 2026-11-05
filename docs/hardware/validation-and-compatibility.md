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

La observación de salida es compartida y conserva desconocidos. XM5, Buds FE, XB100 y receptores ajenos utilizan la negociación normal de Android/Media3; no hay selección por nombre. Las pruebas automatizadas y de emulador figuran en [modernización](../modernization-status.md). La matriz física de esta página continúa pendiente de S24 y receptores: no inferir mejoras de sonido, latencia, jank, autonomía o temperatura a partir de la compilación. Cast y reproducción local Wear mantienen propietarios y rutas independientes.
