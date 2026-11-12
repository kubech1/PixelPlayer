# Interfaz, navegación y estado

## Encontrar la pieza correcta

La interfaz del teléfono está escrita con Jetpack Compose y Material 3. [MainActivity](../app/src/main/java/com/theveloper/pixelplay/MainActivity.kt) contiene el contenedor principal; [AppNavigation](../app/src/main/java/com/theveloper/pixelplay/presentation/navigation/AppNavigation.kt) conecta destinos; las pantallas están en [presentation/screens](../app/src/main/java/com/theveloper/pixelplay/presentation/screens) y las piezas reutilizables en [presentation/components](../app/src/main/java/com/theveloper/pixelplay/presentation/components).

Las cuentas tienen recorridos propios en `presentation/telegram`, `gdrive`, `netease`, `qqmusic`, `navidrome` y `jellyfin`. No todas las pantallas pertenecen a `presentation/screens`; el [índice](reference/source-index.md) permite encontrarlas por paquete.

| Cambio | Archivos iniciales |
| --- | --- |
| Navegación raíz, back y transiciones entre destinos | [Screen](../app/src/main/java/com/theveloper/pixelplay/presentation/navigation/Screen.kt), [MainRootRoutes](../app/src/main/java/com/theveloper/pixelplay/presentation/navigation/MainRootRoutes.kt), [Transitions](../app/src/main/java/com/theveloper/pixelplay/presentation/navigation/Transitions.kt), `AppNavigation` |
| Biblioteca y carpetas | [LibraryScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/LibraryScreen.kt), [LibraryMediaTabs](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/LibraryMediaTabs.kt), [LibrarySongsAndFavoritesTabs](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/LibrarySongsAndFavoritesTabs.kt), holders de biblioteca/carpetas |
| Player completo y mini player | [components/player](../app/src/main/java/com/theveloper/pixelplay/presentation/components/player), [PlayerSheetState](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/PlayerSheetState.kt), `PlayerViewModel` |
| Cola y letras | [QueueBottomSheet](../app/src/main/java/com/theveloper/pixelplay/presentation/components/QueueBottomSheet.kt), [LyricsSheet](../app/src/main/java/com/theveloper/pixelplay/presentation/components/LyricsSheet.kt), holders correspondientes |
| Ajustes y setup | [SettingsCategoryScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SettingsCategoryScreen.kt), [SetupScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SetupScreen.kt), [SettingsViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SettingsViewModel.kt), [SetupViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SetupViewModel.kt), [StatsViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/StatsViewModel.kt) |
| Tema y color de portada | [ui/theme](../app/src/main/java/com/theveloper/pixelplay/ui/theme), [ThemeStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ThemeStateHolder.kt), [ColorSchemeProcessor](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ColorSchemeProcessor.kt) |
| Widgets del launcher | [ui/glancewidget](../app/src/main/java/com/theveloper/pixelplay/ui/glancewidget), [WidgetUpdateManager](../app/src/main/java/com/theveloper/pixelplay/data/service/WidgetUpdateManager.kt) |

Los archivos grandes contienen varias responsabilidades de presentación. Una corrección pequeña debe localizar el componente/handler concreto; no exige reescribir toda la pantalla.

Se retiraron de la interfaz los avisos e insignias de versión Beta (`Beta 0.7.5` / `Beta 0.5`), el historial de cambios integrado, el anuncio de Play Store, la categoría de Copia y restauración y las Opciones de desarrollador (`ExperimentalSettingsScreen`). Las utilidades de mantenimiento de usuario (**Regenerar Mix del día**, **Reiniciar estadísticas** mediante `StatsViewModel.resetAllStats()` sobre `PlaybackStatsRepository` y `EngagementDao`, y **Regenerar paletas**) residen en `SettingsCategory.LIBRARY`, mientras que **Resolución de carátulas** (`AlbumArtQuality`) y **Letras animadas / desenfoque** residen en `SettingsCategory.APPEARANCE`.

## Inicio y detalle de mixes

[Inicio](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/HomeScreen.kt) empieza con «A mano». El primer acceso abre el último álbum identificable del historial. En ausencia de álbum, abre «Añadido recientemente» si ese mix está disponible, o Biblioteca. El segundo abre la pestaña «Me gusta» cuando está habilitada. Si está oculta, se ofrece Biblioteca cuando todavía no ocupa el primer acceso. Los accesos usan píldoras de superficie neutra y se apilan con texto grande o en espacios estrechos.

«Explora tu biblioteca» muestra un carrusel horizontal con el Mix del día en primer lugar y hasta tres selecciones rotativas del mismo tamaño. Si «Añadido recientemente» ocupa un acceso, se muestra una sola vez. Siguen tres pistas recientes y las estadísticas de hoy y de la semana. Al desplazar Inicio, una píldora flotante contiene el título y otra reúne los iconos de proveedores y Ajustes. Cada grupo conserva su extremo durante la transición.

La portada y el título abren el detalle del mix. Reproducir inicia la lista en su orden, aunque la reproducción aleatoria persistente esté activada. El detalle conserva la selección que abrió y ofrece reproducción, aleatorio y acciones de pista. La ruta anterior `daily_mix` conserva el destino del Mix del día y el argumento `mixId` selecciona un mix rotativo.

«Me gusta» abre la raíz de Biblioteca con el argumento de pestaña `LIKED`. Si el destino elegido en la barra inferior ya está en la pila activa, la navegación vuelve directamente a esa entrada. Los destinos fuera de la pila activa conservan el mecanismo de restauración de la barra principal.

Inicio solicita un resumen breve de escucha. La carga de estadísticas que recorre el catálogo comienza al abrir la pantalla de Estadísticas. La selección de mixes se solicita al entrar en Inicio o al abrir directamente un detalle que aún carezca de datos. Se vuelve a calcular al terminar una sincronización o al cambiar las carpetas bloqueadas.

[HomeMixArtwork](../app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeMixArtwork.kt) comparte el arte del carrusel con el detalle. [HomeMixPoster](../app/src/main/java/com/theveloper/pixelplay/presentation/components/HomeMixPoster.kt) dibuja los fondos en Compose. El Mix del día combina surcos cobrizos con hasta tres miniaturas de álbumes distintos. Cada rotativo elige una variante de posición y recorte a partir de los IDs ordenados de su selección. Si falla una carátula, intenta la siguiente candidata y la ilustración sigue visible aunque no cargue ninguna. Los fundidos, el reflejo breve al entrar en el detalle y la compactación del encabezado respetan movimiento reducido.

Las [mediciones exploratorias de arranque y desplazamiento](hardware/validation-and-compatibility.md#rediseño-de-inicio-en-emulador-api-37) corresponden a la versión anterior con un Mix del día destacado.

## Observar el estado que se necesita

`PlayerViewModel` ofrece slices de estado y delegaciones. `LibraryViewModel` expone los flows paginados de `LibraryStateHolder` con `cachedIn(viewModelScope)`. La canción actual, la posición, los temas, la búsqueda y la cola no tienen idéntica frecuencia de cambio.

Para añadir UI, identifica:

1. Quién posee la verdad: servicio, repositorio, preferencia, holder o estado efímero de Compose.
2. Qué estado necesita realmente ese componente y cómo se observa con su ciclo de vida.
3. Qué evento actualiza el estado y qué sucede si se interrumpe, cambia la canción o se cierra la pantalla.
4. Qué valores deben sobrevivir recreación o reinicio y cuáles no.

Evita duplicar estado persistente en `remember` sin sincronización o recopilar un objeto amplio solo para un boolean. Esto es una pauta de diseño; se debe comprobar el efecto concreto antes de afirmar que un cambio reduce recomposiciones.

## Temas, recursos y movimiento

[Theme](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Theme.kt), [Motion](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Motion.kt), [Depth](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Depth.kt), [ColorRoles](../app/src/main/java/com/theveloper/pixelplay/ui/theme/ColorRoles.kt), [Type](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Type.kt) y [Shape](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Shape.kt) son referencias de estilo. `PixelPlayTheme` instala `PixelPlayMotionScheme` sobre Material 3 (`PixelMotion` separa muelles estructurales críticamente amortiguados de muelles de feedback con rebote ligero). La paleta de una portada pasa por extracción y procesamiento, no es simplemente un color fijo.

Los recursos están en [app/src/main/res](../app/src/main/res): XML de valores, traducciones, drawables, fuentes, layouts de soporte y configuración Android. Modifica claves de recursos con sus consumidores; no confundir la UI Compose con ausencia de XML. El [índice de configuración](reference/config-index.md) cubre los recursos textuales; imágenes y fuentes binarias se localizan en estos directorios y `assets/` en la raíz.

El rediseño contemporáneo se implementa por fases según [Lenguaje visual](design-language.md) y el [ADR 0004](decisions/0004-contemporary-design-language.md). Con la Fase 1 completada, `MainActivity` expone tres contextos raíz: `LocalMotionPreferences` (`rememberSystemAnimationsEnabled` y `rememberTouchExplorationEnabled` en [VisualAnimationActivity](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/VisualAnimationActivity.kt)), `LocalVisualEffects` ([VisualEffectsLevel](../app/src/main/java/com/theveloper/pixelplay/data/preferences/VisualEffectsLevel.kt) con niveles `FULL`, `BALANCED` por defecto y `MINIMAL`, migración desde `disable_blur_all_over` y rebaja automática con ahorro de batería en [VisualEffectsState](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/VisualEffectsState.kt)) y `LocalBottomChrome` ([BottomChromeMetrics](../app/src/main/java/com/theveloper/pixelplay/presentation/components/bottomchrome/BottomChromeMetrics.kt)), que centraliza las alturas inferiores en reposo de las pantallas principales y de detalle. [Depth](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Depth.kt) provee `Modifier.pillSurface` para la navegación y el mini reproductor.

Para animaciones, parte del propósito de la interacción. Conserva semántica y continuidad al interrumpir o invertir una transición, usa preferencias de movimiento reducido y comprueba texto grande, temas y orientación. Los valores de las skills de diseño son recomendaciones: CSS, GSAP y ejemplos de React no se pegan en Compose.

## Cambios visibles y verificación

Las pruebas existentes incluyen lógica de letras, scrollbars, anclaje del player, temas scoped y animación de navegación de carpetas. No equivalen a una inspección visual de toda la aplicación. Un cambio visual necesita, según su alcance, capturas y prueba de interacción en un dispositivo/emulador, semántica accesible y contraste sobre los colores realmente compuestos.

En el resumen del trabajo, distingue «prueba de lógica pasó», «compiló» y «verificado visualmente». Si solo se hizo lectura estática, no atribuirle resultados de UX observados.

## Háptica y trabajo visual

[AppHaptics](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/AppHaptics.kt) y [HapticPolicy](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/HapticPolicy.kt) adaptan Compose y View a diez eventos semánticos compartidos (`SELECTION`, `NAVIGATION_STEP`, `ACTION`, `TOGGLE_ON`, `TOGGLE_OFF`, `CONFIRMATION`, `REJECTION`, `DRAG_START`, `DRAG_END`, `LIMIT`). `haptics_enabled` conserva su valor; `haptic_intensity` admite `LOW`/`MEDIUM`/`HIGH`, con `MEDIUM` por defecto. Los ajustes existentes ofrecen selección y previsualización. El wrapper respeta el interruptor, la vista, las preferencias del sistema y `USAGE_TOUCH`; no acepta flags para saltarse esos ajustes.

Se consultan capacidades reales del actuador (`areEnvelopeEffectsSupported()`, límites de puntos/duración y `arePrimitivesSupported()`). Cuando el hardware soporta primitivas de composición (como el actuador lineal `MOTOR_LINEAR_INDEX (5)` del `SM-S921B`, donde `areEnvelopeEffectsSupported()` devuelve `false` y las primitivas táctiles son `CLICK(20ms)`, `TICK(20ms)`, `LOW_TICK(20ms)`, `QUICK_FALL(100ms)`, `SPIN(130ms)` y `QUICK_RISE(150ms)`), `HapticPolicy` construye secuencias multi-primitiva expresivas mediante `VibrationEffect.Composition` evitando `THUD(300ms)`. Si el dispositivo soporta envelopes PWLE (`BasicEnvelopeBuilder`, API 36+), los usa cuando están disponibles, y conserva las constantes de `HapticFeedbackType` / `View.performHapticFeedback` como fallback. Los controles de transporte (`AnimatedPlaybackControls`, `UnifiedPlayerSheetShared`), barra de progreso (`FullPlayerContent`) y barra de navegación inferior (`PlayerInternalNavigationBar`) emiten eventos semánticos diferenciados para Play/Pause, salto de pista, favoritos, aleatorio, repetición, scrubbing/extremos de barra y cambio de pestaña. El limitador compartido por ventana evita duplicados y restringe ticks/límites repetitivos a 50 ms.

`AppVisibility` sigue `ProcessLifecycleOwner`, separado del servicio de música. El tick UI normal y el de Mashup requieren suscriptores y UI visible; el muestreador del slider exige lifecycle RESUMED. Los loops decorativos de onda/EQ y el vinilo de letras verifican bounds visibles, lifecycle y animaciones del sistema. En [InteractiveRefreshRate](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/InteractiveRefreshRate.kt), `Modifier.preferredFrameRate(FrameRateCategory.High)` se solicita durante scroll o contacto directo con el player únicamente cuando `Display.hasArrSupport() == true`; en pantallas One UI 9 con `REFRESH_RATE_MODE_SEAMLESS` (10–120 Hz) donde `hasArrSupport` es `false` y `frameRateCategoryRate.high` es `90.0 Hz`, se delega en el gestor nativo del sistema para preservar los 120 Hz.

En [UnifiedPlayerSheetV2](../app/src/main/java/com/theveloper/pixelplay/presentation/components/UnifiedPlayerSheetV2.kt), [UnifiedPlayerSheetLayers](../app/src/main/java/com/theveloper/pixelplay/presentation/components/UnifiedPlayerSheetLayers.kt) y [FullPlayerCompositionPolicy](../app/src/main/java/com/theveloper/pixelplay/presentation/components/scoped/FullPlayerCompositionPolicy.kt), el árbol de `FullPlayerContent` se precalienta con dimensiones constantes de pantalla completa y permanece montado entre cambios de canción sin desmontar sus secciones con placeholders (`FullPlayerLoadingTweaks`), mientras que los desplazamientos de transición (`visualSheetTranslationYProvider`, `currentHorizontalPaddingStartPxProvider`) se leen únicamente en la fase de placement/draw para evitar remediciones por fotograma. En listas y miniaturas, [SmartImage](../app/src/main/java/com/theveloper/pixelplay/presentation/components/SmartImage.kt) decodifica por defecto en texturas `Bitmap.Config.HARDWARE` (`allowHardware = true`) y [EnhancedSongListItem](../app/src/main/java/com/theveloper/pixelplay/presentation/components/subcomps/EnhancedSongListItem.kt) omite `updateTransition` y capas de recorte duplicadas en filas estáticas.

## Píldoras inferiores (Fase 2)

En reposo, la píldora de navegación se centra y ajusta su ancho a las tres pestañas. Mide 236 × 56 dp a escala de texto normal. Al fusionarse, se desplaza al inicio de la fila y los tres destinos ceden el área táctil a un único botón que vuelve a abrir la navegación.

La indicación de pulsación se recorta a la forma redondeada del indicador sin reducir el área táctil de la pestaña. Ajustes, sus categorías y la selección de paleta ocultan el mini reproductor y liberan su espacio inferior. Al volver a una pantalla con píldoras, reaparece con la misma pista.

La barra y el mini reproductor usan superficies en píldora. [BottomChromeState](../app/src/main/java/com/theveloper/pixelplay/presentation/components/bottomchrome/BottomChromeState.kt) conserva el estado efímero en Compose; la política recibe desplazamiento consumido mediante `onPostScroll`. Fusiona al acumular 24 dp hacia el final, separa al volver 24 dp, al alcanzar el principio o tras 800 ms sin desplazamiento (incluido el fling). Cambio de ruta, teclado, expansión, TalkBack y la preferencia desactivada restablecen la geometría. Las animaciones desactivadas usan `snap` para la fusión y el cambio horizontal de canción.

`LocalBottomChrome` reserva espacio constante en reposo. La fracción se lee en layout/dibujo, sin cambiar `sheetCollapsedTargetY` por fotograma. El círculo fusionado abre la navegación; reseleccionar Inicio, Buscar o Biblioteca lleva la lista al principio. Las páginas adyacentes del pager de Biblioteca no reciben la acción. El doble toque en Buscar conserva la activación de búsqueda.

El arrastre horizontal del mini reproductor cambia de canción con los callbacks existentes (25 % del ancho o 1000 dp/s; dirección invertida en RTL). Solo portada y texto siguen el dedo. Arrastrar hacia abajo desde colapsado cierra la cola con deshacer (48 dp o 800 dp/s); una cancelación del sistema no confirma el cierre. El gesto hacia arriba sigue abriendo el reproductor.

`nav_compact_on_scroll` es booleano y vale `true` por defecto. [RetiredUiPreferencesMigration](../app/src/main/java/com/theveloper/pixelplay/data/preferences/RetiredUiPreferences.kt) retira estilo, modo compacto y radio antiguos. Desde la Fase 3 también retira la preferencia de esquinas suaves. Se retiraron la pantalla de radio y la página correspondiente del setup. El overlay externo usa radio fijo de 32 dp. La evidencia de compilación, tests y QA se registra en [Lenguaje visual](design-language.md); lectura estática y build no certifican fluidez ni accesibilidad en hardware.

El reproductor completo se cierra con Atrás o arrastrando hacia abajo. El toque en su fondo se consume sin cerrar el panel. En letras animadas, el desenfoque conserva su interruptor, respeta el nivel global de efectos y usa el valor predeterminado de 2,5 dp por línea de distancia, con un máximo de 10 dp. La selección de paleta conserva los estilos y su vista previa. Las instalaciones anteriores conservan la precisión de color guardada y las nuevas usan el valor predeterminado.

[Buscar](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SearchScreen.kt) conserva el campo y los filtros de consulta. Cuando la consulta está vacía,
[GenreCategoriesGrid](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/GenreCategoriesGrid.kt) presenta los géneros como píldoras neutras en cuadrícula o lista. Pasa a una columna en espacios estrechos o con texto grande, conservando la preferencia de cuadrícula. [GenreArtwork](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/search/components/GenreArtwork.kt) dibuja escenas vectoriales dentro de un círculo. Reconoce variantes como «Electrónica» y distingue «Tropical House» de «House». Las etiquetas sin escena propia reciben una composición abstracta estable y las personalizaciones conservan su imagen dentro del círculo.
Tocar abre el detalle; mantener pulsado activa la selección múltiple, cuyo indicador muestra el orden seleccionado.
Los cambios de disposición y selección usan `PixelMotion` y respetan `LocalMotionPreferences`, con movimiento inmediato cuando las animaciones del sistema están desactivadas.
El paginador de Biblioteca compone las páginas visibles y usa la precarga de Compose durante el desplazamiento horizontal.

## Portada compartida del reproductor (Fase 3)

El reproductor conserva el esquema de controles entre fotogramas y lo actualiza al cambiar la paleta. El color del fondo se interpola durante el dibujo y la malla toma ese color como base. Las solicitudes de precarga de carátulas vecinas se cancelan cuando cambia la página de referencia. El indicador de progreso reemplaza interpolaciones pendientes al recibir una posición nueva y detiene la onda cuando su amplitud llega a cero.

El contorno del panel pasa de una píldora al reproductor completo con un radio uniforme. La forma y la capa de portada leen en layout o dibujo el mismo progreso visual, que combina la expansión con Atrás predictivo. El menú de Apariencia retira «Usar esquinas suaves».

La transición usa los límites de la miniatura y del elemento activo del carrusel. Dibuja una sola portada entre ambos y reutiliza el dibujo de las imágenes ya cargadas. Si alguno de los extremos todavía no está disponible al iniciar, el reproductor abre al instante con su fundido habitual. El árbol precalentado y la política de carga diferida siguen controlando cuándo se muestra el carrusel.

## Microinteracciones y barras superiores

El corazón del reproductor y de la ficha de canción celebra únicamente la acción local de añadir un favorito. El estado del icono sigue los favoritos de la biblioteca. Con movimiento desactivado, el cambio es inmediato. Aleatorio, repetir y la llegada de la portada respetan la misma preferencia de movimiento.

Las pantallas con encabezado colapsable muestran un título grande al inicio y una píldora al desplazarse. La píldora agrupa Atrás, el título y las acciones. Su altura considera el tamaño del texto y se posiciona dentro del área segura de la ventana. Álbum, Artista y Género usan la misma barra.

## Halo y fondo de malla (Fase 4)

La portada activa conserva su recorte y recibe la sombra y el halo en una capa situada detrás del carrusel. La miniatura del halo se solicita con Coil al llegar al reproductor completo. Al cambiar de pista o salir del reproductor, se cancela la solicitud anterior. Las portadas vecinas conservan su apariencia y la portada compartida viaja sola durante la apertura y el cierre.

El fondo de malla usa los colores de la paleta del reproductor y cuatro posiciones reproducibles por canción. Atenúa el color y el grano según el contraste con el texto principal. Los niveles efectivos de efectos visuales conservan su reducción por ahorro de batería. Mínimo muestra el fondo sólido y una sombra neutra. Equilibrado añade la malla, el halo y el borde de luz. Completo desenfoca la capa del halo y la extiende detrás del encabezado, el título y el artista. Con las animaciones del sistema desactivadas se mantienen los efectos estáticos.
