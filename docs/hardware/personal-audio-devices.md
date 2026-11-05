# Dispositivos personales de audio

Revisión documental: **31 de octubre de 2026**. Las cifras siguientes proceden de fichas/manuales, no de mediciones de las unidades del usuario. El modo de conexión y el firmware pueden cambiar funciones disponibles.

## Sony WH-1000XM5

| Apartado | Especificación |
| --- | --- |
| Diseño | Circumaural cerrado, dinámico |
| Transductor | 30 mm, imán de neodimio; cúpula compuesta de fibra de carbono |
| Procesamiento | Procesadores QN1 y V1; ANC, sonido ambiente y DSEE Extreme |
| Micrófonos | Ocho para su sistema de captación/control de ruido |
| Peso | Aproximadamente 250 g |
| Autonomía anunciada | Hasta 30 h con ANC / 40 h sin ANC; depende de condiciones |
| Bluetooth | 5.2; A2DP, AVRCP, HFP, HSP; alcance nominal aproximado de 10 m |
| Banda de radio | 2,4000–2,4835 GHz |

Fuente: [especificaciones oficiales Sony, región Sudáfrica](https://www.sony.com/za/electronics/headband-headphones/wh-1000xm5/specifications).

### Impedancia y frecuencias

| Magnitud | Condición publicada |
| --- | --- |
| Impedancia | **48 Ω** a 1 kHz encendidos por cable; **16 Ω** a 1 kHz apagados por cable |
| Sensibilidad | 102 dB/mW encendidos y 100 dB/mW apagados, a 1 kHz |
| Respuesta anunciada | 4 Hz–40 kHz, JEITA |
| Banda por Bluetooth | 20 Hz–20 kHz con muestreo de 44,1 kHz; 20 Hz–40 kHz con LDAC a 96 kHz y 990 kbps |

Fuente: [manual de especificaciones WH-1000XM5](https://helpguide.sony.net/mdr/wh1000xm5/v1/en/contents/TP1000541014.html). Los extremos de una respuesta anunciada sin una curva y tolerancia no describen su neutralidad ni sus desviaciones audibles. Los valores de impedancia corresponden a la conexión analógica; por Bluetooth el teléfono no alimenta eléctricamente esos 16/48 Ω.

### Códecs y conexiones

Los códecs de música publicados son **SBC, AAC y LDAC**. Sony diferencia 990 kbps para las familias de muestreo 48/96 kHz y 909 kbps para 44,1/88,2 kHz en el modo de mayor tasa descrito. No convertir «LDAC» en «sin pérdidas» ni tratar la tasa máxima como la que siempre negocia la conexión. [Códecs del manual Sony](https://helpguide.sony.net/mdr/wh1000xm5/v1/en/contents/TP1000534600.html).

La conexión multipunto permite mantener dos dispositivos conectados y alternar su uso. No debe aplicarse como regla vigente una incompatibilidad LDAC/multipunto tomada de firmware antiguo: habrá que registrar firmware, ajustes y códec activo en la combinación real. [Conexión simultánea con dos dispositivos](https://helpguide.sony.net/mdr/wh1000xm5/v1/en/contents/TP1000541784.html).

Con cable de 3,5 mm se puede escuchar encendidos o apagados; las funciones activas requieren alimentación y Bluetooth se desactiva al conectar el cable. En el S24 esa ruta necesitaría un DAC/adaptador USB-C compatible, si se decide utilizarlo. [Uso del cable](https://helpguide.sony.net/mdr/wh1000xm5/v1/en/contents/TP1000534744.html).

El USB de estos auriculares sirve para carga; no se debe diseñar una supuesta ruta USB Audio a los XM5. El manual indica que al conectarlos para cargar no se pueden encender ni reproducir por Bluetooth. [Carga y restricciones](https://helpguide.sony.net/mdr/wh1000xm5/v1/en/contents/TP1000534502.html).

**Consecuencia para futuras pruebas:** registrar ANC/ambiente, EQ/DSEE del receptor, multipunto y efectos del teléfono. Añadir procesamiento en PixelPlayer podría acumularse con ellos. Probar estabilidad en el bolsillo, interferencia y cambios de distancia antes de favorecer una tasa LDAC alta. No se conoce aquí la latencia de la unidad en cada combinación.

## Samsung Galaxy Buds FE, SM-R400

Son los **Buds FE originales**, no Buds2 Pro, Buds3 FE ni otro modelo con funciones diferentes.

| Apartado | Especificación |
| --- | --- |
| Transductor | Una vía de **6,5 mm** por auricular |
| Control de ruido | ANC y sonido ambiente |
| Micrófonos | Tres por auricular |
| Funciones excluidas en la comparación oficial | Sin 360 Audio, Adaptive EQ ni Voice Detect |
| Carga del estuche | Por cable |

Fuente: [comparación oficial Samsung de Buds FE, Buds2 Pro y Buds2](https://www.samsung.com/uk/support/mobile-devices/comparison-between-the-galaxy-budsfe-buds2-pro-and-buds2/). Una actualización del teléfono no concede por sí sola funciones de otro auricular.

| Apartado | Especificación |
| --- | --- |
| Códecs | **SSC (Samsung Scalable Codec), AAC, SBC** |
| Bluetooth/perfiles | 5.2; A2DP, AVRCP, HFP |
| Peso | Aproximadamente 5,6 g por auricular |
| Baterías | 60 mAh por auricular; estuche de 479 mAh |
| Autonomía anunciada | Hasta 6 h con ANC / 8,5 h sin ANC; 21/30 h incluyendo estuche |
| Ecosistema | Auto Switch entre dispositivos compatibles, sujeto a cuenta/configuración Samsung |

Fuente: [ficha oficial Samsung Canadá, SM-R400](https://www.samsung.com/ca/audio-sound/galaxy-buds/galaxy-buds-fe-graphite-sm-r400nzaaxac/).

**SSC aquí significa Scalable**, tal como lo denomina esa ficha. No equipararlo automáticamente con todas las modalidades Samsung Seamless/UHQ de auriculares posteriores. No se estableció soporte para LDAC, aptX, LC3, 24 bits/96 kHz o audio Bluetooth sin pérdidas en estos FE. Auto Switch tampoco prueba multipunto Bluetooth genérico simultáneo.

No se hallaron cifras primarias suficientes de **impedancia, sensibilidad, material del diafragma, respuesta acústica con tolerancia o límites concretos de tasa/muestreo SSC de este modelo** en las fuentes consultadas. No rellenar esos campos con 16/32 Ω, 20 Hz–20 kHz o especificaciones de los Buds Pro.

**Consecuencia para futuras pruebas:** el sellado y tamaño de las almohadillas deben registrarse antes de comparar EQ. Comprobar reconexión, uso de un solo auricular, ANC/ambiente, controles y llamadas; el comportamiento musical A2DP no describe la ruta de micrófono HFP.

## Sony SRS-XB100

| Apartado | Especificación y alcance |
| --- | --- |
| Sistema acústico | Un transductor de rango completo de aproximadamente **46 mm** y radiador pasivo |
| Formato | Un solo altavoz no constituye un par estéreo con separación L/R |
| Tamaño/peso | Aproximadamente 76 mm de diámetro × 95 mm; 274 g |
| Micrófono | Electret omnidireccional; banda publicada **100 Hz–7 kHz**, exclusiva del micrófono |

Fuente: [manual Sony, especificaciones en inglés](https://helpguide.sony.net/speaker/7856/v1/en/contents/TP1000953059.html). El radiador pasivo no es un segundo driver amplificado. La banda del micrófono no es la respuesta del altavoz.

Sony describe un **diafragma descentrado** y **Sound Diffusion Processor**, procesamiento interno para ampliar la dispersión percibida. Esto no es un control de DSP expuesto a PixelPlayer ni una medición de cobertura uniforme. No se estableció el material del diafragma en las fuentes consultadas. [Presentación técnica del XB100, Sony Europa](https://www.sony.eu/presscentre/sonys-new-compact-wireless-speaker-srs-xb100-now-available-in-europe).

El manual japonés aporta **4 Ω** para el transductor y **5 W** como salida máxima práctica a **1 kHz, 1 % THD, carga de 4 Ω**, tanto en batería como por alimentación USB. Se conserva la procedencia regional: no se ha comprobado el número de variante de la unidad del usuario. Esta carga pertenece al amplificador interno, no al teléfono conectado por Bluetooth. [Especificaciones Sony Japón](https://helpguide.sony.net/speaker/7856/v1/ja/contents/TP1000952083.html).

| Apartado | Dato publicado |
| --- | --- |
| Bluetooth | **5.3**, A2DP/AVRCP/HFP |
| Banda de radio | 2,4000–2,4835 GHz |
| Alcance | Aproximadamente 30 m en las condiciones nominales; obstáculos e interferencia lo reducen |
| Banda transmitida | **20 Hz–20 kHz con muestreo de 44,1 kHz** |

Fuente: [especificaciones Bluetooth del manual Sony](https://helpguide.sony.net/speaker/7856/v1/en/contents/TP1000946299.html). La banda transmitida **no demuestra reproducción acústica de graves de 20 Hz**. No se obtuvo aquí una curva acústica numérica completa con tolerancias y condiciones de volumen.

Admite **SBC y AAC**, no LDAC en la documentación consultada. [Códecs SRS-XB100](https://helpguide.sony.net/speaker/7856/v1/en/contents/TP1000953058.html). El modo de prioridad de calidad puede emplear AAC/SBC; prioridad de estabilidad usa SBC. **Stereo Pair fuerza SBC**, incluso aunque individualmente admita AAC. [Modos de calidad y restricciones](https://helpguide.sony.net/speaker/7856/v1/en/contents/TP1000946317.html). El par necesita dos XB100; el usuario solo ha declarado uno. [Stereo Pair](https://helpguide.sony.net/speaker/7856/v1/en/contents/TP1000948043.html).

Sony anuncia hasta **16 h**, protección **IP67**, USB-C **solo para carga** y uso de Sony Music Center limitado a actualización de firmware en este producto. No asumir EQ remoto por esa app ni entrada de audio USB. [Ficha de soporte Sony](https://www.sony.com/electronics/support/speakers-wireless-speakers/srs-xb100/specifications).

**Consecuencia para futuras pruebas:** comprobar mezcla de contenido estéreo en un solo altavoz, comportamiento a volumen alto y carga, continuidad al alternar AAC/SBC y reconexiones. Una elevación de graves en software no crea extensión física ilimitada y puede consumir margen o activar limitación interna. No se define aún una curva ni ganancia específica.

## Mediciones acústicas: qué se pudo establecer

Se consultaron las pruebas de primera mano de [RTINGS para WH-1000XM5](https://www.rtings.com/headphones/reviews/sony/wh-1000xm5-wireless), [Buds FE](https://www.rtings.com/headphones/reviews/samsung/galaxy-buds-fe-truly-wireless) y [SRS-XB100](https://www.rtings.com/speaker/reviews/sony/srs-xb100). Parte de sus gráficos/métricas detalladas estaba restringida en la consulta; **no se extrajeron curvas numéricas ni se generaron perfiles de EQ a partir de ellas**.

El texto accesible de las pruebas de auriculares evidencia la relevancia del ajuste y sellado. Eso no proporciona una calibración de las unidades del usuario. Una futura comparación debe identificar método, acoplador/micrófono, colocación, volumen, modo ANC, firmware y objetivo tonal. No mezclar medidas de auriculares y altavoces como si compartieran el mismo método.

## Matriz práctica de transporte

Resumen de las fuentes individuales anteriores; «posible» no equivale a «negociado ahora».

| Receptor | Códecs de música publicados | Condición que conviene comprobar |
| --- | --- | --- |
| WH-1000XM5 | SBC, AAC, LDAC | Calidad/estabilidad, firmware, multipunto, ANC y DSP |
| Buds FE SM-R400 | SBC, AAC, SSC Scalable | Compatibilidad Samsung y códec activo; no inferir UHQ/360 |
| SRS-XB100 | SBC, AAC | Stereo Pair utiliza SBC; USB no transporta audio |
| Radio u otro receptor desconocido | No predefinidos | Aceptar la negociación normal y conservar reproducción/controles |

La app debe responder al cambio de ruta y a las capacidades efectivas. El nombre Bluetooth puede cambiar y no identifica de forma fiable un modelo. Los requisitos de compatibilidad y la relación con el motor están en [validación](validation-and-compatibility.md).
