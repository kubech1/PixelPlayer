# Mantener el contexto mientras cambia el proyecto

## La documentación forma parte del cambio

Quien modifica un contrato, un flujo o una configuración actualiza su documentación en la misma tarea y, cuando haya Git, en el mismo conjunto de cambios. No se necesita una sesión separada para documentar trabajo ya autorizado.

La documentación debe permitir que otra persona o IA retome el proyecto sin repetir descubrimientos importantes. Eso no implica narrar cada edición, copiar el diff o describir línea por línea un código que ya se entiende solo.

## Qué actualizar

| Cambio realizado | Documentación que revisar | Verificación documental adicional |
| --- | --- | --- |
| Módulo, nueva frontera o cambio de responsabilidad | `architecture.md`, guía de lectura y rutas de `AGENTS.md` | Consumidores, dependencias y enlaces |
| ID, entidad, query, índice, migración, filtro o sync | `library-and-data.md`; backup/integraciones si afecta sus contratos | Esquemas, invariantes de datos y pruebas pertinentes |
| Cola, servicio, motor, focus, transición o salida de audio | `playback.md`; integraciones/Wear según destino | Secuencia de acciones y dueños de estado |
| Dispositivo objetivo, firmware, perfil de audio, capacidad de salida o medición física | `hardware/`; reproducción/UI/diagnósticos según efecto | Fecha, modelo, firmware, ruta, método y distinción entre ficha, consulta y resultado; conservar fallback genérico |
| Pantalla, navegación, tema o patrón de estado | `ui-and-state.md` | Ruta de pantalla, accesibilidad y evidencia visual cuando aplique |
| Proveedor, autenticación, endpoint, prompt o proxy | `integrations.md`, `backup-and-security.md` | Datos enviados, credenciales, errores y compatibilidad |
| Ruta/DTO/acción compartida con reloj | `wear-protocol.md` y ambos módulos | Valores por defecto, emisor, receptor y respuesta |
| Formato de backup, módulo exportable o restore | `backup-and-security.md` | Compatibilidad, validación y rollback |
| Métrica, diagnóstico o contrato de exportación | `diagnostics.md`; guía especializada si describe el payload | Unidad, alcance de observación y privacidad |
| Gradle, SDK/JDK, variantes, pruebas, benchmark o CI | `build-and-testing.md` | Comandos contra la configuración vigente |
| Archivos de código/configuración creados, movidos, eliminados o editados | Índices generados | Ejecutar `generate` y `check` |
| Nueva decisión duradera con alternativas relevantes | ADR bajo `docs/decisions/` | Contexto, razones, consecuencias y estado |
| Solo formato, typo o detalle interno sin cambio de contrato | Regenerar índices si cambia fuente indexada; narrativa solo si aporta | Puede explicarse brevemente por qué no requiere cambio narrativo |

Al añadir un subsistema que no encaje, crea una página temática y enlázala desde `docs/README.md`. Ajusta `category()` del generador para que su índice lo derive al tema correcto. No crear una página por archivo si repite su nombre y firma sin explicar decisiones.

## Procedimiento durante cada tarea

1. **Ubicar:** leer instrucciones aplicables, el documento temático pertinente y los archivos propietarios del comportamiento. Un typo no exige leer toda la guía.
2. **Contrastar:** si hay discrepancia, identificar la fuente actual. Distinguir bug de documentación, comportamiento intencional e hipótesis pendiente. No ajustar el código a una descripción antigua por inercia.
3. **Actualizar:** editar explicación, enlaces, contratos, limitaciones y comandos afectados. Si cambia el significado de un dato, documentar unidad, identidad, valor por defecto y consumidores importantes.
4. **Regenerar y re-indexar:** desde la raíz, ejecuta `python scripts/context_docs.py generate` cuando cambie fuente indexada (el generador solo escribe los dos índices; no reescribe explicaciones ni decisiones). Además, actualiza el grafo de `codebase-memory-mcp` ejecutando `index_repository` sobre la raíz del proyecto para mantenerlo al día tras cualquier cambio.
5. **Comprobar:** `python scripts/context_docs.py check`, más las comprobaciones técnicas apropiadas al cambio. Leer el resultado real.
6. **Entregar:** resumir cambio, documentación afectada y verificaciones. Registrar pendientes concretos con su efecto; no declarar completa una validación que no se ejecutó.

## Convenciones de escritura y vigencia

- Español claro, términos técnicos en inglés cuando ayuden. Explica primero propósito y recorrido; después archivos, invariantes y casos límite.
- Usa rutas relativas enlazadas desde el documento. Prefiere archivo y símbolo a números de línea que cambian con facilidad. No introducir rutas personales nuevas en instrucciones portables.
- Mantén `AGENTS.md` corto: reglas comunes, comandos y rutas de consulta. No importar automáticamente todo `docs/` ni reproducir instrucciones de cada skill.
- Marca una afirmación como **observada en código**, **verificada en ejecución**, **hipótesis** o **propuesta** cuando la distinción cambie cómo debe interpretarse. Un documento no necesita etiquetar trivialidades en cada línea.
- Las versiones se toman de Gradle/catálogo; esta guía registra una base fechada. Cuando se cambien, actualizar las menciones afectadas. No llamar «latest» a un valor solo por estar en una skill.
- No afirmar motivos históricos de decisiones que no estén documentados. Explica el efecto visible o registra que el motivo se desconoce.
- No guardar tokens, credenciales, rutas privadas de usuarios, contenido real de biblioteca ni resultados sensibles de pruebas. Las muestras deben ser sintéticas.
- KDoc es útil para API, invariantes no obvios, concurrencia, unidades y efectos secundarios. No exigir comentarios para cada getter ni generar comentarios que simplemente traduzcan el código.

## Decisiones y registro de trabajo

Los ADR usan números correlativos, título, estado, contexto, decisión, alternativas y consecuencias. Cambiar una decisión aceptada requiere explicar la nueva evidencia y enlazar la que la sustituye; conservar la historia útil.

`CHANGELOG.md` registra cambios del producto cuando la convención y el alcance lo requieran; no debe llenarse con cada edición documental. Los logs detallados de ejecución son evidencia de una tarea, no instrucciones permanentes. Solo mover una lección a `AGENTS.md` cuando sea duradera, específica y útil para trabajo futuro.

La auditoría inicial es una fotografía fechada. Para una evaluación posterior, añadir una sección o un nuevo informe fechado, sin reescribir el pasado como si todos los hallazgos hubieran existido siempre. Los informes históricos de rendimiento se conservan con su alcance y fecha conocidos; si no se conoce una fecha original, decirlo.

La investigación de [hardware](hardware/README.md) conserva fuentes y condiciones por dispositivo. Al cambiar Android/One UI, firmware de un receptor o política de audio, revalidar únicamente las afirmaciones afectadas y registrar qué se comprobó. Una prueba posterior debe añadir su resultado y condiciones, no convertir un dato anunciado en una medición ni marcar como ejecutada una matriz pendiente. No copiar funciones de modelos vecinos por compartir marca o versión Bluetooth.

## Qué detecta el comprobador y qué sigue siendo manual

[context_docs.py](../scripts/context_docs.py) usa solo biblioteca estándar de Python 3.10+. Genera índices deterministas de código, configuración y recursos textuales, con huella de rutas/contenido. Comprueba que coincidan con disco, que existan destinos de enlaces Markdown locales y que estén presentes los temas requeridos. Impone un presupuesto local de 16 KiB al `AGENTS.md` raíz, una decisión de este repositorio, no un límite universal de los proveedores.

No valida contenido semántico, compilación, disponibilidad de URLs externas, anclas Markdown, exactitud de contratos ni cumplimiento de instrucciones por un modelo. Los enlaces locales se verifican como rutas; si se añade un enlace con ancla, revisar la sección manualmente. El parser Markdown es deliberadamente acotado: usar enlaces inline sencillos en esta documentación.

La huella obliga a revisar el índice cuando cambia código; **no garantiza que alguien actualice la explicación correcta**. La matriz anterior y la revisión del diff cubren esa parte. No cambiar una fecha de revisión para simular una comprobación.

