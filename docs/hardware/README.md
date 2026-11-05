# Hardware personal y compatibilidad

Investigación documental revisada el **31 de octubre de 2026**. Prepara las próximas decisiones sobre PixelPlayer; todavía no introduce optimizaciones, perfiles de ecualización ni cambios en el teléfono.

## Contexto del usuario

El teléfono principal es un **Samsung Galaxy S24 SM-S921B**. El usuario declara **One UI 9 y Android 17**. Sus salidas habituales son **Sony WH-1000XM5**, **Galaxy Buds FE originales, SM-R400**, y **Sony SRS-XB100**. También utiliza equipos ajenos, como radios de automóvil y otros altavoces Bluetooth.

Estos datos orientan las pruebas, pero no definen requisitos exclusivos de hardware. La app debe conservar una reproducción correcta en dispositivos y rutas desconocidos. No se ha consultado físicamente ninguno de estos equipos durante esta investigación.

| Necesidad | Lectura |
| --- | --- |
| Pantalla, CPU/GPU, memoria, conectividad, audio y háptica del teléfono | [Galaxy S24 SM-S921B](galaxy-s24-sm-s921b.md) |
| Transductores, impedancias, frecuencias, Bluetooth y funciones de cada equipo de audio | [Dispositivos de audio](personal-audio-devices.md) |
| Restricciones de Android 17, One UI 9 y APIs utilizables por una app normal | [Sistema operativo](android17-oneui9.md) |
| Relación con el código, límites de diseño y pruebas pendientes | [Validación y compatibilidad](validation-and-compatibility.md) |

## Cómo interpretar la evidencia

- **Especificación oficial:** capacidad o condición publicada por el fabricante; no es una medición de la unidad del usuario.
- **Medición independiente:** resultado del laboratorio citado, con su unidad, firmware y método; no se traslada automáticamente al teléfono actual.
- **Declarado por el usuario:** contexto aceptado para planificar, pendiente de inventario técnico cuando se pruebe.
- **Lectura estática:** lo que hace el código inspeccionado; no demuestra su resultado en ejecución.
- **Pendiente:** dato no establecido por las fuentes consultadas o que depende de la conexión real. No sustituirlo por cifras de otro modelo.

Las fuentes están enlazadas junto a las afirmaciones. Se priorizaron Samsung, Sony, Android Developers y AOSP; se consultaron mediciones de primera mano cuando aportaban información adicional. Las fichas regionales se identifican cuando importa. No se obtuvo un inventario completo de chips, amplificadores, DAC, DSP y actuadores internos mediante desmontaje.

## El recorrido de audio que necesitamos distinguir

En una ruta Bluetooth habitual, el archivo se decodifica, la app y el sistema pueden procesar o mezclar el audio, el sistema negocia y codifica el transporte Bluetooth, y el receptor realiza su procesamiento y conversión a sonido. Una ruta con offload, una salida USB o Cast cambia partes de este recorrido. Véase la [arquitectura Bluetooth de AOSP](https://source.android.com/docs/core/connect/bluetooth/services) y la [reproducción de PixelPlayer](../playback.md).

Por tanto, son datos diferentes:

| Dato | Qué describe | Qué no demuestra por sí solo |
| --- | --- | --- |
| FLAC, AAC, MP3, DSD en un archivo | Contenedor/codificación de origen | El códec Bluetooth ni una ruta sin conversiones |
| PCM float / frecuencia de muestreo | Representación y temporización en una etapa digital | Resolución analógica efectiva del DAC o mejor sonido |
| SBC, AAC, LDAC, SSC | Transporte Bluetooth negociable | Códec activo, tasa efectiva o ausencia de pérdidas |
| 2,4 GHz | Banda de radio | Frecuencia acústica reproducida |
| 20 Hz–20 kHz de transmisión | Banda nominal del enlace bajo condiciones indicadas | Graves y agudos medidos del altavoz |
| Impedancia en ohmios | Carga eléctrica de un transductor o entrada concreta | Volumen/calidad por Bluetooth o ajuste de ganancia del teléfono |
| Bluetooth 5.2/5.3 | Versión de la tecnología Bluetooth | Compatibilidad automática con LC3, LE Audio o todos los códecs opcionales |

El objetivo futuro debe expresarse mediante resultados comprobables: continuidad, fidelidad respecto de la fuente, respuesta elegida por el usuario, interacción fluida y consumo razonable. Seleccionar el número más alto en cada ajuste no garantiza esos resultados.

## Vigencia

Actualizar estas páginas cuando cambie el firmware relevante, aparezca una nueva ruta o se realice una medición. Registrar fecha, modelo, versión, modo de conexión y método; conservar la distinción entre ficha y resultado. El procedimiento completo está en [mantenimiento documental](../documentation-maintenance.md).
