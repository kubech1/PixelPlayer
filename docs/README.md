# Entender PixelPlayer

Esta es la entrada a la documentación técnica para personas y agentes de IA. El proyecto se presenta como **PixelPlayer**, conserva **PixelPlay** en clases y recursos, y declara `PixelPlay` como nombre del proyecto Gradle. Son nombres del mismo código, no aplicaciones independientes.

## Una primera imagen mental

Piensa en tres recorridos que se encuentran en el reproductor:

1. **Encontrar música:** MediaStore y proveedores remotos aportan canciones; repositorios y Room permiten consultarlas como biblioteca.
2. **Reproducirla:** la interfaz envía acciones a una sesión Media3. Un servicio mantiene la reproducción y un motor con dos players permite transiciones.
3. **Presentarla y controlarla:** Compose, widgets, Cast, Android Auto y Wear OS muestran o controlan partes de ese estado. El reloj también tiene reproducción local propia.

No todo vive en un único ViewModel. `PlayerViewModel` es un coordinador importante, pero delega biblioteca, cola, reproducción, búsqueda, letras, temas y otras funciones en state holders.

## Leer según el trabajo

| Necesidad | Documento |
| --- | --- |
| Entender módulos, arranque y límites | [Arquitectura](architecture.md) |
| Escaneo, modelos, consultas, preferencias, letras y estadísticas | [Biblioteca y datos](library-and-data.md) |
| Audio, cola, sesión, crossfade y controladores | [Reproducción](playback.md) |
| SM-S921B, audio personal, Android 17/One UI 9 y pruebas por dispositivo | [Hardware y compatibilidad](hardware/README.md) |
| Pantallas, navegación, estado, temas y recursos | [Interfaz y estado](ui-and-state.md) |
| Proveedores musicales, mezclas locales, red, Cast y otros controles | [Integraciones](integrations.md) |
| Contratos teléfono/reloj, transferencias y modo local | [Wear y protocolo compartido](wear-protocol.md) |
| Exportación, restauración y fronteras de confianza | [Backup y seguridad](backup-and-security.md) |
| Instrumentación, informes y análisis de rendimiento | [Diagnósticos](diagnostics.md) |
| Preparar entorno y elegir verificaciones | [Build y pruebas](build-and-testing.md) |
| Mantener esta documentación durante el desarrollo | [Mantenimiento](documentation-maintenance.md) |
| Evaluación de la documentación previa y límites actuales | [Auditoría documental](documentation-audit.md) |
| Por qué se organizaron así los archivos y qué dicen los proveedores | [Diseño del contexto](context-design.md) |
| Localizar cualquier archivo de código | [Índice de código generado](reference/source-index.md) |
| Localizar manifiestos, XML, esquemas Room y configuración | [Índice de configuración generado](reference/config-index.md) |
| Retirada de IA y modernización Android: cambios y evidencia | [Estado de modernización](modernization-status.md) |
| Estado compartido de audio y recuperación por ruta/sesión | [ADR de audio](decisions/0003-audio-output-observation.md) |
| Procedencia de las seis skills | [Revisión de skills](skills-review.md) |

Para una primera sesión, lee [arquitectura](architecture.md) y el tema del cambio. El índice completo es una herramienta de consulta; cargarlo entero en cada tarea consume contexto sin necesidad.

## Qué significa «documentado» aquí

La documentación narrativa explica responsabilidades y recorridos contrastados mediante inspección estática de fuentes centrales. El índice enumera los archivos del alcance definido por su generador, incluidos tests y scripts; no afirma que cada función haya sido revisada o ejecutada.

Las afirmaciones de comportamiento se acompañan de archivos propietarios. Si el código cambia, esos archivos y las pruebas pertinentes son la evidencia que debe revisarse. Los comandos propuestos no equivalen a comandos ejecutados. Los informes anteriores conservan su valor histórico y sus hipótesis; no son automáticamente la descripción actual del proyecto.

Base inicial revisada el **31 de octubre de 2026**, sobre esta copia local sin metadatos `.git`. Consulta [la auditoría](documentation-audit.md) para alcance y comprobaciones. Los índices se pueden volver a generar sin depender de Git.

## Vocabulario rápido

| Término | En este proyecto |
| --- | --- |
| `Song` | Modelo de canción usado por la app; contempla identificadores locales y de proveedores |
| `MediaItem` | Representación que Media3 utiliza para reproducir y transportar metadatos |
| DAO / Entity | Consultas y registros persistentes de Room |
| State holder | Componente que posee o coordina una parte del estado y sus acciones |
| `StablePlayerState` | Estado de reproducción separado de las emisiones frecuentes de posición |
| Proxy de streaming | Puente HTTP local para resolver y servir fuentes de proveedores |
| Data Layer | Transporte entre teléfono y Wear OS para mensajes, estado y canales |
| ADR | Registro de una decisión relevante, sus razones y consecuencias |

