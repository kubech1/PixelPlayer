# ADR-0001: contexto breve y documentación temática con índices generados

Estado: **aceptado para esta base documental**. Fecha: 31 de octubre de 2026.

## Contexto

El usuario necesita documentación para comprender todo el proyecto con agentes de IA y mantener ese conocimiento mientras cambia el código. La copia inspeccionada contiene 711 archivos Kotlin y dos scripts JavaScript del producto. La documentación previa se concentra en presentación, rendimiento y procedencia de skills; falta un mapa de contratos y recorridos.

Las guías oficiales consultadas favorecen contexto acotado, específico y actualizado. Una explicación manual exhaustiva por función tendría alto costo de mantenimiento y duplicaría código; un índice de nombres por sí solo no explica el proyecto.

## Decisión

Mantener `AGENTS.md` como entrada canónica breve. Añadir `docs/README.md` como guía de lectura, páginas por subsistema y una política de actualización vinculada al tipo de cambio. Generar índices de código y configuración con una herramienta local reproducible, sin dependencias de red. Mantener fuentes y límites de evidencia en los documentos.

No añadir una base vectorial, servicio de búsqueda, generador de documentación por LLM ni adaptadores de todos los proveedores sin una necesidad concreta. La documentación no cambia la arquitectura del producto.

## Alternativas consideradas

| Alternativa | Ventaja | Motivo de descarte como base |
| --- | --- | --- |
| Un `AGENTS.md` con todo | Una única ubicación | Carga excesiva y mezcla de instrucciones, inventario e historia |
| Solo comentarios/KDoc y referencia generada | Cerca del código | No explica flujos entre módulos, entorno ni mantenimiento |
| Solo README e índice de archivos | Poco esfuerzo inicial | Exige redescubrir identidad, estado, transporte y riesgos |
| Documentación narrativa por cada archivo y función | Gran detalle aparente | Alta duplicación y riesgo de desactualización; difícil de navegar |
| Recuperación vectorial o plataforma externa | Búsqueda semántica a escala | Infraestructura y mantenimiento no justificados para este encargo |

## Consecuencias

La entrada permanece manejable y la lectura se dirige al área del cambio. El índice cubre mecánicamente archivos que no se describen uno por uno en prosa. La huella detecta cambios de fuente, pero la exactitud de las explicaciones sigue necesitando revisión humana o del agente responsable del cambio.

No hay garantía de lectura idéntica entre clientes ni verificación funcional de la app a partir de documentos. Los detalles se actualizan según [mantenimiento](../documentation-maintenance.md). Nuevas fronteras del producto o dificultades reales de recuperación pueden justificar dividir páginas o revisar esta decisión.

Fuentes y comportamiento de proveedores: [diseño del contexto](../context-design.md).

