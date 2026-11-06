# Samsung Galaxy S24 SM-S921B

Base documental: **31 de octubre de 2026**. Variante **SM-S921B**, no S24 Ultra, S24 FE ni variantes Snapdragon de otros mercados. Sistema actual declarado por el usuario: One UI 9 / Android 17; build, CSC, parche y almacenamiento de su unidad pendientes de inventario.

## Plataforma y recursos

| Componente | Especificación de la variante |
| --- | --- |
| SoC | Exynos 2400, S5E9945, fabricación de 4 nm |
| CPU | Diez núcleos: 1 × 3,2 GHz, 2 × 2,9 GHz, 3 × 2,6 GHz y 4 × 1,95 GHz |
| RAM | 8 GB LPDDR5X |
| Almacenamiento | Fichas regionales con 128/256 GB; sin microSD |
| Batería | 4.000 mAh típica; carga por cable de hasta 25 W y carga inalámbrica |
| Dimensiones/peso | 147 × 70,6 × 7,6 mm; aproximadamente 167 g |

Fuente para esta tabla y los datos de pantalla siguientes: [comparación Samsung que identifica expresamente el SM-S921B](https://www.samsung.com/de/support/mobile-devices/what-are-the-differences-between-the-galaxy-s25-series-and-s24-series/).

Samsung Semiconductor identifica la CPU como **Cortex-X4 + Cortex-A720 + Cortex-A520**; la GPU **Xclipse 940** usa arquitectura **AMD RDNA 3** con ray tracing por hardware. Describe encapsulado FOWLP y procesamiento de IA mediante NPU. Estas son capacidades del SoC, no aceleraciones que PixelPlayer obtenga automáticamente. [Exynos 2400, ficha del fabricante](https://semiconductor.samsung.com/processor/mobile-processor/exynos-2400/).

**Criterio para el proyecto:** decodificar música, consultar Room, analizar biblioteca y animar la UI son cargas distintas. Usar más núcleos, GPU o NPU solo aporta valor si existe un cuello de botella medido y un camino de ejecución compatible. No se justifica trasladar audio a GPU ni mantener trabajo continuo para «aprovechar» el chip. Tampoco se ha establecido aquí la capacidad sostenida bajo temperatura, carga inalámbrica o ahorro de batería.

## Pantalla

| Apartado | Dato |
| --- | --- |
| Panel | 6,2 pulgadas, Dynamic AMOLED 2X |
| Resolución/densidad | 2.340 × 1.080, FHD+, aproximadamente 418 ppp |
| Protección | Gorilla Glass Victus 2; no atribuirle Gorilla Armor del Ultra |
| Brillo | Hasta 2.600 nits de pico anunciado; no brillo sostenido de toda la pantalla |
| Refresco | Modo estándar 60 Hz; adaptativo hasta 120 Hz |

El anuncio de la serie especifica adaptación **1–120 Hz** para el S24. Es un rango del panel/sistema; no significa que una aplicación pueda solicitar cualquier valor de ese intervalo. El dato de cámara de vapor 1,9 veces mayor y Gorilla Armor de ese anuncio corresponde al **Ultra**, por lo que no se incorpora al modelo base. [Presentación oficial S24](https://news.samsung.com/global/enter-the-new-era-of-mobile-ai-with-samsung-galaxy-s24-series).

Una prueba de primera mano de Notebookcheck identifica panel **LTPO**, muestreo táctil de hasta **240 Hz** y modulación de brillo/PWM en torno a **240–480 Hz** en su unidad. También mide altavoces hasta 91,4 dB bajo su método y prueba almacenamiento UFS 4.0 en una unidad de 256 GB. Son resultados de aquella revisión, no mediciones sobre One UI 9 ni de esta unidad. No se ha identificado su proveedor exacto de panel ni su almacenamiento físico. [Revisión y mediciones de Notebookcheck](https://www.notebookcheck.net/Samsung-Galaxy-S24-review-The-best-small-Android-smartphone-gets-cheaper-but-not-better.809474.0.html).

Para la UI, 120 Hz deja unos **8,33 ms** entre cuadros; 60 Hz, **16,67 ms**. Esto es aritmética del refresco, no un benchmark. Una biblioteca estática no necesita dibujar continuamente. Deben medirse scroll, letras sincronizadas y visualizaciones por separado, respetando ahorro de energía, accesibilidad y preferencias de movimiento. La capacidad LTPO tampoco demuestra soporte de la API Android ARR; véase [sistema y APIs](android17-oneui9.md).

## Audio, puertos y sensores

Samsung confirma **altavoces estéreo**, salida por **USB-C**, **USB 3.2 Gen 1**, **Bluetooth 5.3** y ausencia de jack de 3,5 mm. La ficha enumera acelerómetro, barómetro, giroscopio, sensor geomagnético, Hall, luz, proximidad y huella. [Ficha oficial de un SM-S921B de 128 GB](https://www.samsung.com/sa_en/business/smartphones/galaxy-s/galaxy-s24-onyx-black-128gb-sm-s921bzkbmea/).

La conectividad Wi-Fi incluye **6E**, con bandas de 2,4/5/6 GHz según región. [Conectividad de la variante, Samsung](https://www.samsung.com/de/support/mobile-devices/what-are-the-differences-between-the-galaxy-s25-series-and-s24-series/). Esto puede ayudar a separar tráfico Wi-Fi de Bluetooth, pero no garantiza eliminar interferencia ni justifica cambiar automáticamente la red del usuario.

La ficha comercial anuncia reproducción de formatos como MP3, AAC/M4A, OGG, WAV, FLAC, APE y DFF/DSF. Mezcla extensiones, contenedores y codificaciones: **no es una lista de decodificadores de hardware**. Que Samsung anuncie esos formatos no prueba que el extractor/renderer de PixelPlayer los reproduzca todos ni que exista salida DSD nativa. Consultar los decodificadores reales y probar archivos representativos. [Formatos en la ficha Samsung](https://www.samsung.com/sa_en/business/smartphones/galaxy-s/galaxy-s24-onyx-black-128gb-sm-s921bzkbmea/), [API MediaCodecInfo](https://developer.android.com/reference/android/media/MediaCodecInfo).

No se obtuvo especificación primaria suficiente de diámetro, impedancia, amplificador, DAC, DSP, respuesta acústica o límite de distorsión de los altavoces internos. No sustituir estos datos por los de un S24 Ultra ni inferirlos de la marca AKG/Dolby. La reproducción estéreo debe conservarse y comprobarse en ambas orientaciones, con y sin efectos del sistema.

### Códecs Bluetooth

Una respuesta de moderación de Samsung de septiembre de 2024 lista **SBC, AAC, aptX, LDAC y SSC** para el S24 Exynos. Es evidencia histórica del proveedor, corroborada por la revisión independiente, **no una enumeración de la build One UI 9 del usuario**. No se encontró base suficiente para prometer aptX HD, Adaptive o Lossless. [Respuesta de Samsung sobre el S24](https://r1.community.samsung.com/t5/galaxy-s/audio-codec-do-s24/td-p/29257808).

El códec disponible depende de la intersección teléfono/receptor y de la política del sistema. La app no debe seleccionar un camino exclusivo por el nombre SM-S921B. Los códecs de cada equipo y sus condiciones están en [dispositivos de audio](personal-audio-devices.md).

## Motor de vibración y háptica

La inspección directa en el `SM-S921B` conectado (`dumpsys vibrator_manager` en One UI 9 / Android 17, 6 de noviembre de 2026) determinó:

- **Actuador lineal:** `mVibratorGroup: MOTOR_LINEAR_INDEX (5)`, `SEM_SUPPORTED_VIBRATION_TYPE_D`, `supportAch : true`.
- **Envelopes PWLE:** `mMaxEnvelopeEffectSize: 0`, `mMinEnvelopeEffectControlPointDurationMillis: 0` (`Vibrator.areEnvelopeEffectsSupported() == false`).
- **Primitivas de composición soportadas (`mCompositionPrimitiveMaxLength: 100`):** `CLICK` (20 ms), `TICK` (20 ms), `LOW_TICK` (20 ms), `QUICK_FALL` (100 ms), `SPIN` (130 ms), `QUICK_RISE` (150 ms), `THUD` (300 ms) y `SLOW_RISE` (500 ms).

[HapticPolicy](../../app/src/main/java/com/theveloper/pixelplay/presentation/utils/HapticPolicy.kt) prioriza por ello secuencias multi-primitiva expresivas basadas en `CLICK`, `TICK`, `LOW_TICK`, `QUICK_FALL`, `SPIN` y `QUICK_RISE` (descartando `THUD` de 300 ms para acciones de UI por su duración excesiva), conservando `BasicEnvelopeBuilder` para dispositivos que expongan soporte de envelopes y el feedback estándar del sistema como fallback. [Referencia oficial Vibrator](https://developer.android.com/reference/android/os/Vibrator).

`HapticGenerator` puede generar háptica a partir del audio desde API 31, pero requiere soporte de hardware/HAL y `isAvailable()`, además de sesión y permisos adecuados. Que la clase exista no demuestra que funcione en este S24. Sería una función a evaluar solo si los próximos requisitos la justifican, incluyendo consumo, preferencias y las dos sesiones del motor. [API HapticGenerator](https://developer.android.com/reference/android/media/audiofx/HapticGenerator).

## Pendiente antes de elegir optimizaciones

1. Registrar build/CSC/parche, almacenamiento y versiones de firmware de los receptores, sin guardar identificadores personales.
2. Consultar modos de pantalla, soporte ARR y capacidades hápticas efectivas.
3. Identificar salida activa, formatos aceptados y compatibilidad de offload por ruta; registrar por separado el códec Bluetooth observado.
4. Medir continuidad, tiempos de cuadro, consumo y temperatura en tareas reales. La ficha del SoC no sustituye estos resultados.

Procedimiento y puntos del código: [validación y compatibilidad](validation-and-compatibility.md).
