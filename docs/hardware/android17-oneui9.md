# Android 17 y One UI 9

Revisión: **31 de octubre de 2026**. El usuario declara estas versiones en su SM-S921B. No se ha leído su build, CSC, parche, estado beta/estable ni ajustes. La investigación distingue capacidades públicas del sistema de funciones utilizables por PixelPlayer.

## Situación de las versiones

Samsung presentó One UI 9 sobre Android 17 en su programa beta de mayo de 2026. Una comunicación oficial de octubre ya incluye la serie S24 entre los dispositivos con One UI 9 disponible. Esto respalda la plausibilidad del contexto declarado, pero no verifica una instalación concreta ni una distribución idéntica en todos los países. [Anuncio de One UI 9 beta](https://news.samsung.com/in/samsung-launches-one-ui-9-beta-for-galaxy-s26-series-users), [disponibilidad y funciones, Samsung Brasil](https://news.samsung.com/br/now-brief-recebe-novidades-na-one-ui-9-e-permite-personalizar-resumos-diarios-no-seu-dispositivo-galaxy).

**Observado en código:** [app/build.gradle.kts](../../app/build.gradle.kts) declara `compileSdk = 37` y `targetSdk = 37`. Por ello importa revisar tanto los cambios generales de Android 17 como los condicionados por target 37. No se ha ejecutado una APK para confirmar esta configuración instalada.

## Android 17: cambios relevantes para este reproductor

### Reproducción en segundo plano

Android 17 exige actividad visible o un foreground service que no sea `SHORT_SERVICE` para reproducir, solicitar audio focus o cambiar volumen desde segundo plano. Con target 37 se añade que ese servicio posea capacidades **while-in-use**, normalmente por haberse iniciado con una acción del usuario o mientras la app era visible. Las llamadas de reproducción/volumen pueden fallar silenciosamente y focus devolver `AUDIOFOCUS_REQUEST_FAILED`. La excepción de alarmas no es un mecanismo para música. [Reglas oficiales de background audio](https://developer.android.com/about/versions/17/changes/bg-audio).

**Implicación a comprobar:** iniciar y reanudar desde UI, notificación, auriculares, widget, Auto y Wear; bloqueo de pantalla, pausa prolongada, pérdida de focus y recuperación tras detenerse el servicio. Que PixelPlayer use Media3 y declare `mediaPlayback` ayuda a estructurar el ciclo de vida, pero no demuestra cumplimiento de todos los recorridos. No cambiar permisos, arrancar servicios o reanudar música arbitrariamente para eludir restricciones.

### Memoria y widgets

Android 17 incorpora límites de memoria de aplicación vinculados a la RAM total. Puede identificarse una terminación del limitador mediante `ApplicationExitInfo`, razón `REASON_OTHER` y descripción que contiene `MemoryLimiter:AnonSwap`. No se encontró un presupuesto fijo específico del S24 que deba codificarse en la app. [Cambios para todas las apps](https://developer.android.com/about/versions/17/behavior-changes-all).

Con target 37, los bitmaps de un payload `RemoteViews` se contabilizan entre orientaciones y tamaños; exceder el límite del sistema puede lanzar `IllegalArgumentException`. Afecta también a imágenes usadas mediante Glance. No calcular un presupuesto universal copiando únicamente la resolución física del S24. El sistema también cambia internals de `MessageQueue`, por lo que no deben apoyarse optimizaciones en reflexión sobre sus campos privados. [Cambios para apps con target Android 17](https://developer.android.com/about/versions/17/behavior-changes-17).

**Implicación a comprobar:** carátulas y sus variantes en widgets, cachés de imágenes, lista de biblioteca y dos players durante transiciones. Ante una terminación, diferenciar límite de memoria, crash, restricción de batería y cierre solicitado; no atribuirla automáticamente a One UI.

## Pantalla adaptativa y movimiento

Android ARR necesita soporte de plataforma/HAL; no basta tener OLED LTPO. La guía oficial contempla integración Compose desde 1.9 con `Modifier.preferredFrameRate`. Antes de proponerla, confirmar dependencias y soporte de pantalla. Es una preferencia, no garantía de una frecuencia sostenida. [Guía de adaptive refresh rate](https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate).

`Display.hasArrSupport()` existe desde API 36. `getSupportedModes()`, tasas de render y capacidades HDR deben consultarse sobre la pantalla activa; una pantalla externa o un cambio de modo puede modificar resultados. No confundir el mínimo físico 1 Hz anunciado con un modo disponible para renderizar la app. [Referencia Display](https://developer.android.com/reference/android/view/Display).

En la lectura directa sobre el `SM-S921B` con One UI 9 (`dumpsys display`, 6 de noviembre de 2026), el panel principal reporta `renderFrameRate 120.00001`, `REFRESH_RATE_MODE_SEAMLESS`, `mHighSpeedRefreshRates: min=10, max=120, passiveList[24, 10, 30, 60, 120]`, pero `hasArrSupport false` y `frameRateCategoryRate FrameRateCategoryRate {normal=60.0, high=90.0}`. Invocar `Modifier.preferredFrameRate(FrameRateCategory.High)` cuando `hasArrSupport` es `false` limita la ventana a `90.0 Hz`; por ello [InteractiveRefreshRate](../../app/src/main/java/com/theveloper/pixelplay/presentation/utils/InteractiveRefreshRate.kt) comprueba `view.display?.hasArrSupport() == true` antes de aplicar el modificador, permitiendo que One UI 9 mantenga los 120 Hz nativos en modo `SEAMLESS`.

**Criterio:** interacción y scroll pueden beneficiarse del refresco alto; una vista quieta o reproducción con pantalla apagada no requieren animación permanente. No forzar 120 Hz globales, brillo pico o efectos HDR para «aprovechar» la pantalla. Medir cuadro, consumo y temperatura; respetar movimiento reducido y escala de animación.

## One UI: efectos y políticas fuera de PixelPlayer

| Función del sistema/ecosistema | Relevancia y límite |
| --- | --- |
| Dolby Atmos, ecualizador, Adapt Sound y opciones UHQ | Pueden modificar la cadena percibida. Registrar estado y disponibilidad por ruta, sin asumir que todos aparecen o son aplicables a cada receptor |
| Sound Assistant | Puede aportar controles de volumen/ruta por app; su disponibilidad depende de versión, región y dispositivo. No equivale a una API pública para que PixelPlayer controle esos ajustes |
| Apps en suspensión/suspensión profunda y restricciones de batería | Pueden afectar continuidad y reanudación. Diagnosticar antes de recomendar excepciones; no desactivar globalmente ahorro o protección térmica |
| Quick Panel, reproductor del sistema, Now Bar/Now Brief | Contexto para probar presentación y controles. La publicidad de una integración Samsung no demuestra acceso público para apps de terceros |

Fuentes: [efectos de sonido Samsung](https://www.samsung.com/us/support/answer/ANS10002953/), [Sound Assistant en Galaxy Store](https://galaxystore.samsung.com/detail/com.samsung.android.soundassistant), [restricciones y audio en segundo plano, Samsung](https://www.samsung.com/us/support/troubleshoot/TSG10001337/), [One UI 9 y Quick Panel](https://news.samsung.com/in/samsung-launches-one-ui-9-beta-for-galaxy-s26-series-users), [Now Brief](https://news.samsung.com/br/now-brief-recebe-novidades-na-one-ui-9-e-permite-personalizar-resumos-diarios-no-seu-dispositivo-galaxy).

La [comparación específica de Buds FE](https://www.samsung.com/uk/support/mobile-devices/comparison-between-the-galaxy-budsfe-buds2-pro-and-buds2/) prevalece sobre recomendaciones genéricas de «Galaxy Buds» al evaluar 360 Audio: esos FE no lo anuncian. Evitar trasladar funciones entre generaciones.

**Propuesta de evaluación, sin implementación:** comparar una cadena con efectos desactivados y otra con las preferencias habituales del usuario. Cambiar una variable cada vez y registrar volumen, EQ de la app, EQ del sistema y DSP del receptor. No restablecer ajustes personales automáticamente.

## Qué puede consultar una app normal y qué no

| Área | API o posibilidad pública | Precaución |
| --- | --- | --- |
| Dispositivos/ruta | `AudioDeviceInfo`, callbacks de dispositivos y routing del reproductor/track | Un dispositivo conectado no necesariamente es la salida activa |
| Tasas, canales, encodings | Capacidades informadas por `AudioDeviceInfo` | No son el códec A2DP negociado; una lista vacía de sample rates puede significar tasas arbitrarias según el contrato de la API |
| Salida directa/offload | `AudioManager.getDirectPlaybackSupport()` y perfiles para atributos | Depende de formato, atributos y ruta; hay que reevaluar al cambiar dispositivos |
| Códecs de decodificación | `MediaCodecInfo`, MIME y capacidades; indicadores hardware/software/vendor | Declaración del sistema, no benchmark ni garantía sobre extractores y renderers de la app |
| Háptica | Consultas de `Vibrator` y efectos estándar | Soporte físico y amplitud/frecuencia son condicionales; ver ficha del teléfono |

Fuentes: [AudioDeviceInfo](https://developer.android.com/reference/android/media/AudioDeviceInfo), [AudioManager](https://developer.android.com/reference/android/media/AudioManager), [MediaCodecInfo](https://developer.android.com/reference/android/media/MediaCodecInfo), [Vibrator](https://developer.android.com/reference/android/os/Vibrator).

**Límite Bluetooth importante:** `BluetoothA2dp.getSupportedCodecTypes()` figura desde API 35 pero exige `BLUETOOTH_PRIVILEGED`. No es una solución general para una app distribuida normalmente. Tampoco enumera por sí sola el códec activo del receptor. No diseñar una función que prometa leer/forzar todos los códecs mediante reflexión o solo con `BLUETOOTH_CONNECT`. El códec activo puede registrarse manualmente mediante diagnósticos del sistema/receptor que realmente lo expongan. [Permisos y contrato BluetoothA2dp](https://developer.android.com/reference/android/bluetooth/BluetoothA2dp).

La ruta USB tiene otra excepción distinta: Android admite atributos preferidos de mezclador para USB, pero el modo **bit-perfect es opcional y depende del HAL**. No aplicarlo a Bluetooth ni afirmar que cualquier DAC USB lo ofrece; además puede excluir mezcla, volumen y efectos. [Preferred mixer attributes, AOSP](https://source.android.com/docs/core/audio/preferred-mixer-attr).

No se ha demostrado soporte efectivo de ARR, envelopes hápticos, HapticGenerator, USB bit-perfect o un nuevo códec en la build del usuario. Esas celdas permanecen pendientes en el [plan de validación](validation-and-compatibility.md).

## Implementación de la modernización

El teléfono y sus benchmarks requieren API 37; Wear/shared siguen en 30. Se implementaron háptica semántica con envelopes disponibles en Android 17 inicial, presupuesto agregado de widgets, manejo de rechazos foreground/focus, observación por ruta/sesión y solicitud temporal de categoría alta de refresco. Se consultan ARR y modos reales de display. Los detalles y la evidencia están en [estado de modernización](../modernization-status.md) y [ADR de audio](../decisions/0003-audio-output-observation.md). No se alteran ajustes globales de One UI, codecs Bluetooth, volumen ni perfiles tonales. One UI 9 sigue siendo información declarada por el usuario hasta comprobar el dispositivo conectado.
