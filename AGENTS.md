# Trabajo con PixelPlayer

Comunícate en español. Conserva las convenciones existentes y limita los cambios al objetivo solicitado. Distingue evidencia de ejecución, lectura estática, hipótesis y propuestas.

## Contexto y lectura por tarea

Aplicación Android de música en Kotlin, Jetpack Compose y Material 3. `app` es teléfono; `wear` incluye control remoto y reproducción local del reloj; `shared` contiene contratos serializados; `baselineprofile` genera perfiles y ejecuta macrobenchmarks sobre `app`. PixelPlayer/PixelPlay son nombres que coexisten en este código.

Entrada técnica: [docs/README.md](docs/README.md). Lee solo los temas pertinentes, no todo el índice para cada edición.

| Trabajo | Contexto a consultar |
| --- | --- |
| Módulos, arranque, Hilt, responsabilidades | [Arquitectura](docs/architecture.md) |
| Escaneo, Room, modelos, preferencias, letras, playlists | [Biblioteca y datos](docs/library-and-data.md) |
| Media3, cola, sesión, audio y transiciones | [Reproducción](docs/playback.md) |
| SM-S921B, equipos de audio, Android 17/One UI 9 y compatibilidad por ruta | [Hardware y validación](docs/hardware/README.md) |
| Pantallas, navegación, temas y estado | [Interfaz](docs/ui-and-state.md) |
| Proveedores, streaming, Cast, Auto/widgets | [Integraciones](docs/integrations.md) |
| Teléfono/reloj, DTO, rutas y transferencias | [Wear](docs/wear-protocol.md) |
| Backup/restore y entradas externas | [Backup y seguridad](docs/backup-and-security.md) |
| Logs, métricas, stalls, informes | [Diagnósticos](docs/diagnostics.md) |
| Toolchain, variantes, comandos, tests y CI | [Build y pruebas](docs/build-and-testing.md) |
| Encontrar un archivo concreto | [Código](docs/reference/source-index.md) y [configuración](docs/reference/config-index.md) generados |

## Reglas específicas del código

- Confirma versiones y opciones en Gradle/catálogo; no copies requisitos antiguos del README o de una skill. La base documental usa Java/JVM 21 y Android SDK 37.
- `MusicService`/Media3 poseen la reproducción duradera. Identifica servicio, controller, holder y estado UI antes de cambiar un flujo. Mantén separado el tick de posición del estado amplio de pantallas.
- Los IDs de canciones abarcan varias fuentes; no asumir que todos son numéricos ni que una página de biblioteca representa la cola completa.
- Cambios de Room requieren revisar esquema, migración y consumidores. No habilitar fallback destructivo para resolver fallos; validar preservación de datos y esquema también en debug.
- Cambios de `shared` requieren revisar teléfono y reloj: emisor, receptor, defaults y compatibilidad de DTO/rutas. El reloj no es solo un mando.
- No mover disco/red/lectura de metadatos a composición o al hilo principal sin justificación. Conserva cancelación y liberación de jobs/listeners/players.
- Tratar backups, URI y respuestas remotas como entradas no confiables. No registrar secretos ni contenido privado en documentación o fixtures.
- Los informes históricos de rendimiento contienen hipótesis y fotografías anteriores; revalida cada hallazgo antes de tratarlo como bug vigente.
- Para optimizaciones de hardware, distingue ficha, capacidades consultadas y medición real. Detecta capacidades por ruta, conserva fallback para equipos desconocidos y no deduzcas resolución del DAC/códec Bluetooth a partir de PCM float. Contexto y pruebas pendientes en `docs/hardware/`.

## Skills disponibles

Residen en `$CODEX_HOME/skills` (por defecto `~/.codex/skills`). Lee el `SKILL.md` correspondiente antes de aplicarlo; no ejecutar las seis para todo cambio.

| Skill | Cuándo usarla |
| --- | --- |
| `architecture-decision` | Alternativas y compromisos relevantes; ADR cuando aporte valor |
| `code-simplifier` | Claridad del código trabajado, preservando comportamiento y alcance |
| `systematic-debugging` | Reproducir, reunir evidencia y encontrar causa antes de corregir |
| `verification-before-completion` | Comprobaciones pertinentes antes de afirmar éxito |
| `ui-ux-pro-max` | Interfaz, interacción y accesibilidad; consultas `--stack jetpack-compose` |
| `motion-design` | Propósito, timing, easing y accesibilidad de animaciones |

Para UI/UX Pro Max usa la ruta absoluta de `scripts/search.py` dentro de la skill; `CLAUDE_PLUGIN_ROOT` no es la ruta de este entorno. CSS/GSAP/React requieren adaptación, no son implementaciones Compose. Conserva tokens/componentes existentes. Accesibilidad y fluidez prevalecen sobre recetas decorativas como tres capas de movimiento.

Las versiones/recomendaciones de skills requieren comprobar compatibilidad cuando importe. No instalar otras herramientas solo porque aparezcan como referencias relacionadas. No sobrescribir un diseño persistido con `--force` sin autorización explícita; al persistir nuevo diseño, indicar `--output-dir` del proyecto. [Procedencia de skills](docs/skills-review.md).

## Comandos y evidencia

Desde la raíz en Windows, usa `.\gradlew.bat`. Procedimientos y requisitos: [build y pruebas](docs/build-and-testing.md).

```powershell
.\gradlew.bat :app:assembleDebug -Ppixelplay.enableAbiSplits=false
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
python scripts/context_docs.py check
```

Selecciona tareas según el cambio; no es una batería obligatoria para editar documentación. Instrumentación, Wear y benchmarks necesitan entorno/dispositivos apropiados. UI requiere verificar interacción, semántica, temas, texto y movimiento reducido según alcance. No equiparar compilar con validación funcional ni afirmar rendimiento sin medición.

## Actualización de documentación

Aplica [la matriz de mantenimiento](docs/documentation-maintenance.md) en la misma tarea que cambia el código. Actualiza las páginas afectadas por contratos, flujos, comandos o decisiones. No duplicar cada diff en prosa.

Si cambia código/configuración indexado, ejecuta `python scripts/context_docs.py generate` y luego `python scripts/context_docs.py check`. No editar los índices a mano. El check comprueba estructura/enlaces y vigencia mecánica, no exactitud semántica ni build Android.

En la entrega informa qué documentación cambió, qué se verificó y qué quedó sin comprobar. Registra decisiones duraderas en `docs/decisions/`; usa `AGENTS.md` para reglas estables y breves, no como log de cada sesión.
