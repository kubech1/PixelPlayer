# Diseño del contexto para agentes

Consultado el **31 de octubre de 2026**. Esta página explica el uso de documentación por agentes; no describe una función de IA de la aplicación.

## Qué aportan las fuentes oficiales

| Fuente | Orientación aplicada |
| --- | --- |
| [OpenAI: AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md) | Codex combina instrucciones globales y del proyecto, con prioridad de guía más cercana al directorio actual. Descubre archivos específicos y limita el tamaño combinado. La guía describe 32 KiB por defecto. Se usa una entrada corta y se evita depender de que toda página enlazada se cargue automáticamente. |
| [OpenAI: revisar skills y prompts](https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra) | Recomienda consultar documentos según la tarea y reducir instrucciones redundantes. Se adopta un router temático, sin exigir el mapa completo para cada edición. |
| [Anthropic: memoria y contexto de proyecto](https://code.claude.com/docs/en/memory) | Recomienda instrucciones concretas, consistentes y breves; distingue contexto escrito de auto memory. Los imports cargan contenido, por lo que dividirlo mediante imports no reduce por sí mismo el contexto. Se evita un import masivo de documentación. |
| [GitHub: instrucciones de repositorio](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions) | Distingue instrucciones generales, por ruta y archivos de agentes. El soporte depende de la superficie. Se conserva contexto común y se documenta compatibilidad sin asumir que todas las funciones de Copilot leen los mismos archivos. |
| [Google: GEMINI.md](https://geminicli.com/docs/cli/gemini-md/) | Explica contexto jerárquico, carga según acceso e imports. Se toma la idea de separar reglas generales de detalle temático y comprobar qué contexto cargó el cliente. |

Son comportamientos documentados por cada proveedor, no pruebas realizadas con esos clientes en este equipo. La estructura elegida se registra en [ADR-0001](decisions/0001-context-structure.md).

## Tres capas con funciones distintas

1. **`AGENTS.md`:** acuerdos operativos del repositorio, comandos iniciales, restricciones importantes y selección de lectura. Se usa con frecuencia, por lo que conviene que sea pequeño.
2. **`docs/*.md`:** explicación semántica por subsistema, contratos, flujos y fuentes. Se consulta según el cambio.
3. **`docs/reference/*-index.md`:** inventario generado y completo dentro de su alcance. Sirve para localizar archivos y detectar desactualización mecánica; no sustituye el razonamiento sobre arquitectura.

Las skills describen cómo ejecutar tipos de trabajo. La documentación del proyecto explica este código. Mantener esas responsabilidades separadas evita copiar instrucciones de terceros y acumular reglas contradictorias.

## Portabilidad entre herramientas

El archivo canónico es `AGENTS.md`. En Codex se debe iniciar el trabajo en la raíz correcta; si no existe raíz Git, la guía oficial indica que solo comprueba el directorio actual. Esta copia local no contiene `.git`, por lo que iniciar desde una subcarpeta requiere especial atención al contexto cargado.

Claude Code puede leer `AGENTS.md` directamente en las versiones y condiciones descritas por [su documentación](https://code.claude.com/docs/en/memory#agentsmd). Una configuración con `CLAUDE.md` en el camino puede cambiar qué archivos se cargan. Para clientes que necesiten un adaptador, la guía admite `CLAUDE.md` con `@AGENTS.md`; no se creó aquí un segundo cuerpo de instrucciones.

Gemini CLI usa `GEMINI.md` como nombre por defecto y permite imports o configurar el nombre de contexto. Si se adopta ese cliente, se puede importar `AGENTS.md` desde un adaptador mínimo, o configurar el nombre, y comprobar el contexto con sus herramientas. No se modificaron ajustes personales de Gemini.

Copilot utiliza `.github/copilot-instructions.md`, instrucciones por ruta o archivos de agente según cliente/función. Si se incorpora, elegir el adaptador mínimo que soporte esa superficie y enlazar la guía canónica; un enlace no implica necesariamente un import automático. Confirmar qué instrucciones fueron usadas antes de asumir equivalencia con Codex.

No se añadieron archivos de proveedor que no fueran necesarios para el entorno solicitado. Esto evita cuatro versiones del mismo contenido con distinta vigencia. Los adaptadores futuros deben contener solo mecanismo de carga y particularidades reales del cliente.

## Evaluar si este contexto ayuda

Para una sesión nueva, pedir al agente que localice el propietario de una función, trace un recorrido y proponga las verificaciones de un cambio concreto. Contrastar sus respuestas con fuentes y detectar dónde tuvo que adivinar. Actualizar la página de esa área, en lugar de añadir instrucciones generales que obliguen a leer más en toda tarea.

La validación estructural automatizada no mide calidad de comprensión del modelo. Esta revisión no ejecutó evaluaciones comparativas entre proveedores ni garantiza que un agente cumpla cada instrucción.

