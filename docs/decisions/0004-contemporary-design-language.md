# ADR 0004: lenguaje visual contemporáneo sobre Material 3

Estado: aceptada como dirección de diseño, 6 de noviembre de 2026. Pendiente de implementación por fases; la especificación es [Lenguaje visual](../design-language.md).

## Contexto

La interfaz usa Jetpack Compose y Material 3 (catálogo: `material3` 1.5.0-alpha22) con piezas de M3 Expressive. La barra de navegación inferior es un rectángulo con esquinas configurables (`NavBarStyle`, `navBarCompactMode`, `nav_bar_corner_radius`) y el mini reproductor es una tarjeta separada encima. El usuario busca un aspecto más propio y actual: elementos flotantes que reaccionan al scroll, profundidad, gradientes derivados del color de portada y microinteracciones con significado.

Restricciones observadas en código: `minSdk = 37` (`RenderEffect`, `RuntimeShader`/AGSL y `GraphicsLayer` disponibles sin alternativa por API); el reproductor precalienta `FullPlayerContent` y desplaza el sheet solo en fases de layout/draw; los loops decorativos comprueban visibilidad con `visualAnimationActivity()`; el DataStore de ajustes está excluido de los backups Android.

## Decisión

1. **Evolucionar Material 3, no sustituirlo.** M3 sigue aportando roles de color, semántica y componentes base. Encima se añade una capa propia: material flotante (píldoras), profundidad, gradientes de paleta y un `MotionScheme` propio.
2. **Navegación y mini reproductor como dos píldoras que se fusionan.** En reposo hay dos píldoras apiladas. Al hacer scroll hacia abajo, la navegación se reduce a un círculo con la pestaña activa y el mini reproductor baja a su lado en una sola fila. Se separan al subir, tras ~0,8 s quieto o al llegar al principio. Sin música, la navegación se compacta (sin etiquetas).
3. **Se retira la personalización de la barra.** Desaparecen `NavBarStyle`, `navBarCompactMode`, el radio configurable, `NavBarCornerRadiusScreen` y la página `NavBarLayout` del setup. Las claves se eliminan con una `DataMigration`. Se añade un único ajuste, «Compactar al desplazar», activado por defecto.
4. **Gestos del mini reproductor.** Horizontal = anterior/siguiente. Arriba = abrir el reproductor. Abajo = cerrar la cola, con deshacer (sustituye al cierre horizontal actual).
5. **Movimiento:** la estructura se mueve sin rebote (píldoras, sheet, pantallas, tema). El feedback a un toque rebota ligeramente (10–15 %): me gusta, interruptores, botones, indicador de pestaña, llegada de portada.
6. **Profundidad en el reproductor:** halo estático de la portada (sombra teñida, halo difuminado y borde de luz), animado solo en cambios de canción o pausa. Fondo de malla AGSL estático derivado de la paleta. Sin reactividad al audio.
7. **Material translúcido primero.** La píldora debe verse bien sin desenfoque. El desenfoque real de fondo se implementará en casa (sin dependencias) y solo se activará si los benchmarks lo aprueban.
8. **Ajuste «Efectos visuales»** con tres niveles (Completo, Equilibrado por defecto, Mínimo), que sustituye a `disable_blur_all_over`. El ahorro de batería baja un nivel. El movimiento reducido del sistema es un eje independiente.
9. **Barras superiores:** el título grande colapsa en una píldora flotante con el mismo material.

## Alternativas y compromisos

- **Sustituir M3 por un sistema propio completo:** descartado. Obligaría a reimplementar accesibilidad y componentes sin una ganancia visible proporcional.
- **Una sola píldora siempre en una fila** (tabs + portada): descartada porque oculta título y controles en reposo. **Un contenedor de dos niveles:** descartado porque no se percibe como píldora. **Dos píldoras siempre separadas:** descartadas por ocupar más espacio durante la lectura.
- **Ocultar la navegación al hacer scroll** (en lugar de compactar o fusionar): descartado como comportamiento principal porque haría que la app se comporte distinto con y sin música.
- **Desenfoque con la biblioteca Haze:** descartado para no añadir dependencias. El desenfoque propio queda condicionado a medición.
- **Halo reactivo al audio:** descartado. La API `Visualizer` exige `RECORD_AUDIO` y un análisis propio añadiría trabajo a la cadena de audio.
- **Fondo de portada difuminada:** descartado frente a la malla, porque las portadas con texto o muy contrastadas producen fondos poco controlables.

Consecuencias aceptadas: cambia un gesto aprendido (el deslizamiento horizontal deja de cerrar la cola), se pierden opciones de personalización y aumenta la complejidad del área inferior (estado de fusión compartido entre barra y sheet). El rendimiento es condición de aceptación: la fusión no debe recomponer ni volver a medir las listas en cada fotograma.

## Verificación

Pendiente. Cada fase de la [especificación](../design-language.md) define sus criterios: pruebas unitarias de políticas y migraciones, compilación, lint, macrobenchmarks de scroll para el desenfoque y QA en el SM-S921B (TalkBack, texto grande, temas, movimiento reducido, navegación por gestos y por botones, RTL). Compilar no equivale a validación visual.
