# Lenguaje visual de PixelPlayer

Esta guía describe los patrones visuales vigentes. Las fases conservan las especificaciones y la evidencia de su implementación. Consulta el [ADR 0004](decisions/0004-contemporary-design-language.md) para el contexto de la decisión y [Interfaz y estado](ui-and-state.md) para los recorridos de la app.

## Píldoras flotantes y continuidad

Las píldoras son el lenguaje principal de los controles que flotan sobre el contenido. Úsalas para navegación, acciones de barra y accesos cortos. Las portadas mantienen el formato que necesita su composición. En superficies de lectura, la jerarquía nace del contenido, los márgenes y la tipografía.

- **Anclajes.** Cada grupo conserva una posición reconocible durante la transformación. En Inicio, el título permanece al inicio y nube y Ajustes permanecen al final. Al compactar, aparecen dos superficies separadas. Los dos iconos de la derecha comparten superficie y conservan objetivos táctiles independientes de 48 dp. Los anclajes respetan el sentido de lectura y los insets.
- **Material.** Comparte `pillSurface` para los controles flotantes. El fondo general permanece neutro en claro y oscuro. El color más expresivo pertenece al arte y a los tintes localizados definidos para cada transición.
- **Transformaciones.** Haz que tamaño, posición, forma y opacidad respondan al mismo gesto o cambio de estado. La fusión de la barra inferior y el mini reproductor es la referencia. Comparten `mergeFraction` y reserva inferior estable. El contenido conserva su posición mientras los controles se recolocan. Comprueba la inversión a mitad del gesto, el cambio de ruta, el teclado y la expansión del reproductor.
- **Accesibilidad.** La semántica y los objetivos táctiles siguen a los controles durante la transformación. Revisa claro, oscuro, texto al 200 %, RTL y movimiento reducido. El estado final debe ser reconocible al aplicar el cambio con `snap`. Aplica el movimiento expresivo a la respuesta de una acción y deja estáticas las ilustraciones de Inicio y Buscar.

## Arte en Inicio y Buscar

En Inicio, Compose dibuja las composiciones con trazados y gradientes. El Mix del día combina surcos de cobre y magenta con hasta tres carátulas de álbumes distintos. Los rotativos tienen motivos propios, como una onda nacarada, un pliegue violeta o un abanico coral. La carátula de una canción de la selección ocupa el espacio que deja libre la ilustración. El recorte, la posición y la orientación son estables en el carrusel y el detalle. Si faltan carátulas, la ilustración llena la tarjeta. Las fotos de las portadas provienen de la biblioteca. El arte de los mixes se mantiene en código, sin imágenes generadas.

En Buscar, las píldoras neutras contienen una escena vectorial en un círculo y el nombre del género en Google Sans Rounded. El arte evoca cómo se siente escucharlo. Rock muestra una guitarra roja y blanca, Dance usa pliegues satinados y Electrónica cruza haces de luz en la niebla. El nombre puede ocupar dos líneas y la cuadrícula cede a la lista si el ancho o la escala de texto requieren más espacio. El círculo conserva la imagen cuando el usuario ha personalizado un género. Para TalkBack, el nombre del género identifica la acción.

Las ilustraciones quedan estáticas en reposo. Al abrir el detalle, un reflejo breve recorre las superficies pulidas cuando los efectos de profundidad y las animaciones están activos.

## Blanco y negro y Biblioteca

El fondo base es blanco `#FFFFFF` en Claro y negro `#000000` en Oscuro. El texto principal usa el color inverso. Las superficies flotantes y los estados de controles se distinguen con grises neutros. Las ilustraciones, las carátulas y sus efectos localizados aportan color. El reproductor expandido combina esa base con un degradado granulado de la carátula y recupera los acentos cromáticos del transporte, el texto y la iconografía. El halo de la portada se dibuja por separado.

Biblioteca deja las pestañas visibles de partida y conserva el gesto lateral y el selector compacto. Al bajar, la pestaña activa se compacta en una píldora a la izquierda. Centrar y Ordenar mantienen sus objetivos táctiles circulares independientes a la derecha. El título Biblioteca, la primera pestaña y Aleatorio parten del mismo margen. Centrar se representa con un punto y conserva su etiqueta accesible. Al subir se recupera la tira de pestañas.

En el estado compacto, Aleatorio cede su etiqueta cuando el ancho es limitado para dejar espacio al nombre de la pestaña; el icono conserva su descripción accesible. Las listas atenúan el contenido en el borde superior al desplazarse.

Canciones y Me gusta usan filas en píldora con la misma geometría en reposo, pausa y reproducción. Mientras una pista suena, su paleta difuminada nace detrás de la carátula y se desvanece en la primera mitad de la fila. La forma recorta todo el efecto. En pausa se usa un indicador discreto. El movimiento reducido conserva la distinción entre estados.

## Instrucciones para quien implemente

- Sigue [AGENTS.md](../AGENTS.md) y limita los cambios al alcance acordado. Mantén las tareas de disco, red y metadatos fuera del hilo principal. `MusicService` y Media3 conservan la responsabilidad de reproducción.
- Verifica cada flujo visible al que afecta el cambio y usa las fases de esta guía para localizar patrones y su evidencia.
- **No añadas dependencias.** Nada de Haze, Lottie ni bibliotecas de animación o desenfoque. Todo con Compose, `graphics-shapes`, `RenderEffect` y `RuntimeShader` (`minSdk = 37`).
- **Rendimiento.** Lee los valores animados de posición, opacidad y forma en `layout {}`, `graphicsLayer {}` o en dibujo. Si una transición cambia la métrica del texto, limita la remedición a ese componente. Consulta `visualSheetTranslationYProvider` y `currentHorizontalPaddingStartPxProvider` en [UnifiedPlayerSheetV2](../app/src/main/java/com/theveloper/pixelplay/presentation/components/UnifiedPlayerSheetV2.kt).
- **Estado efímero de UI** (fusión, celebración) en state holders de Compose, no en `PlayerViewModel`. **Preferencias** en `UserPreferencesRepository`.
- Al terminar cada fase, actualiza [ui-and-state.md](ui-and-state.md), marca la fase en la tabla de estado del final de este documento, ejecuta `python scripts/context_docs.py generate` y luego `check`. En la entrega, distingue «compiló», «pruebas pasaron» y «verificado visualmente».

## Vocabulario

| Término | Significado |
| --- | --- |
| Píldora de navegación | Contenedor flotante con Inicio, Buscar y Biblioteca (hoy, el `Surface` del `bottomBar` de [MainActivity](../app/src/main/java/com/theveloper/pixelplay/MainActivity.kt)) |
| Píldora del mini reproductor | El estado `COLLAPSED` del sheet de [UnifiedPlayerSheetV2](../app/src/main/java/com/theveloper/pixelplay/presentation/components/UnifiedPlayerSheetV2.kt) |
| Separadas | Estado de reposo: las dos píldoras apiladas |
| Fusionadas | Estado de scroll: una fila con el círculo de navegación y el mini reproductor compacto |
| `mergeFraction` | 0 = separadas, 1 = fusionadas. Animada; nunca se lee en composición |
| Estructural / feedback | Familias de movimiento definidas en la Fase 1 |

---

## Fase 1. Fundamentos

### 1.1 Movimiento: `PixelMotion` y `MotionScheme` propio

Crear `ui/theme/Motion.kt` con:

| Token | Especificación | Uso |
| --- | --- | --- |
| `structural` | `spring(dampingRatio = 1f, stiffness = 380f)` | Fusión y separación, compactación, barras superiores |
| `structuralSlow` | `spring(dampingRatio = 1f, stiffness = 200f)` | Cambios de color y tema, transiciones de halo y malla |
| `feedback` | `spring(dampingRatio = 0.55f, stiffness = 700f)` (≈12 % de rebote) | Me gusta, pulsación de botones, llegada de portada |
| `indicator` | `spring(dampingRatio = 0.75f, stiffness = 600f)` | Indicador de pestaña |
| Duraciones | `quick = 150`, `standard = 300`, `slow = 450` ms | Efectos con `tween` (alpha, color) |
| Curva de entrada | `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` | Apariciones |

`PixelPlayMotionScheme : MotionScheme`:
- `fastSpatialSpec` usa un muelle con rebote leve (`dampingRatio = 0.7f`, `stiffness = 1400f`). Lo usan componentes pequeños de M3 (interruptores, botones).
- `defaultSpatialSpec` y `slowSpatialSpec` van críticamente amortiguados (`dampingRatio = 1f`).
- Los `*EffectsSpec` usan `tween` con las duraciones de la tabla.

Pásalo a `MaterialTheme(motionScheme = …)` en [Theme.kt](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Theme.kt). **Confirma** en `material3` 1.5.0-alpha22 la firma y si `MotionScheme` requiere `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. No inventes la API.

Los usos locales de `MotionScheme.expressive()` (por ejemplo, `RecentlyPlayedRangeSelector`) pasan a `MaterialTheme.motionScheme` solo si se tocan por otro motivo. No es una migración masiva.

### 1.2 Accesibilidad del movimiento

Extraer de [VisualAnimationActivity](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/VisualAnimationActivity.kt) un `rememberSystemAnimationsEnabled(): State<Boolean>`, basado en `ValueAnimator.areAnimatorsEnabled()` con el observer existente, y reutilizarlo. Añadir `rememberTouchExplorationEnabled(): State<Boolean>` con `AccessibilityManager.addTouchExplorationStateChangeListener`, liberando el listener en `onDispose`.

Exponer ambos valores en un `LocalMotionPreferences` (`animationsEnabled`, `touchExploration`), provisto en `MainActivity`. Regla: si `animationsEnabled == false`, los cambios de estado se aplican con `snap()` y se omiten partículas, anillos y estiramientos, pero los colores y efectos estáticos se conservan.

### 1.3 Ajuste «Efectos visuales»

- `enum class VisualEffectsLevel { FULL, BALANCED, MINIMAL }`, guardado como string en la clave `visual_effects_level`. Valor por defecto: `BALANCED`.
- `DataMigration` siguiendo el patrón de [RetiredAiData](../app/src/main/java/com/theveloper/pixelplay/data/preferences/RetiredAiData.kt), registrada en `produceMigrations` de [UserPreferencesRepository](../app/src/main/java/com/theveloper/pixelplay/data/preferences/UserPreferencesRepository.kt). Si existe `disable_blur_all_over` y no existe la nueva clave: `true → MINIMAL`, `false → BALANCED`. Después se elimina la clave antigua. Debe poder repetirse sin efectos.
- **Nivel efectivo:** si `PowerManager.isPowerSaveMode` está activo, baja un nivel (`FULL→BALANCED`, `BALANCED→MINIMAL`). Se escucha `ACTION_POWER_SAVE_MODE_CHANGED` con un receiver registrado y liberado con el ciclo de vida. Se expone como `LocalVisualEffects`.
- **Consumidores actuales que hay que migrar:** [ScreenWrapper](../app/src/main/java/com/theveloper/pixelplay/presentation/components/ScreenWrapper.kt) (desenfoque y atenuación), [LyricsSheet](../app/src/main/java/com/theveloper/pixelplay/presentation/components/LyricsSheet.kt) (lee la clave `disable_blur_all_over` directamente del DataStore; debe usar el nuevo valor), `PlayerViewModel.disableBlurAllOver`, `SettingsViewModel` y el interruptor de [SettingsCategoryScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SettingsCategoryScreen.kt), que se convierte en un selector de 3 opciones con textos en todas las traducciones existentes.
- **Migración:** `disableBlurAllOver = true` pasa a `MINIMAL` y `false` a `BALANCED`. `FULL` añade el desenfoque de la capa del halo de portada.

### 1.4 Material de las píldoras y profundidad

`Modifier.pillSurface` dibuja una sombra neutra de 6 dp, una superficie con opacidad del 92 % y un borde de luz de 1 dp que se omite en Mínimo. La superficie es carbón en tema oscuro y gris pálido en claro. Los iconos y las etiquetas usan blanco y negro, respectivamente, también sobre el fondo del indicador seleccionado y el botón de reproducción.

Al compactarse, ambas píldoras muestrean un solo degradado horizontal basado en la portada, limitado al 12 % de opacidad. Sin paleta de portada se conserva el fondo neutro. La prueba JVM comprueba un contraste de al menos 4,5:1 en reposo y al compactar, con tintes y fondos extremos.

### 1.5 Medidas inferiores centralizadas

Crear `presentation/components/bottomchrome/BottomChromeMetrics.kt` con `LocalBottomChrome`:
- `contentBottomPadding: Dp`: altura en reposo de lo que flota abajo (inset del sistema, píldoras visibles, separaciones). Es **constante** durante el scroll.
- `mergeFractionProvider: () -> Float`.

Sustituir las llamadas a `resolveNavBarOccupiedHeight`/`resolveMainScreenBottomGradientHeight` en Home, Search, Library, AlbumDetail, ArtistDetail, PlaylistDetail, DailyMix y `DailyMixSection`. **Antes de sustituir**, comprueba la fórmula actual de cada pantalla y conserva su significado: el último elemento debe quedar visible por encima de las píldoras en reposo.

**Criterio de la Fase 1:** sin cambios visuales salvo el selector de efectos. La migración tiene pruebas (sin clave, `true`, `false`, repetida). Compila y pasan `:app:testDebugUnitTest` y `:app:lintDebug`.

---

## Fase 2. Píldoras con fusión y gestos

### 2.1 Medidas

| Elemento | Separadas | Fusionadas | Sin música (compacta) |
| --- | --- | --- | --- |
| Margen horizontal | navegación centrada, mini reproductor a 16 dp | 16 dp | centrada |
| Píldora de navegación | 236 × 56 dp a escala normal, crece según las etiquetas | círculo de 56 dp al inicio | 184 × 52 dp |
| Indicador de pestaña | cápsula de 48 dp de alto, ancho del ítem − 4 dp | — (solo el icono activo) | cápsula de 48 × 44 dp |
| Etiquetas | 12 sp Medium, bajo el icono | ocultas | ocultas |
| Píldora del mini reproductor | ancho = pantalla − 32 dp, alto 64 dp, portada de 44 dp | alto 56 dp, inicio en 16 + 56 + 8 = 80 dp, portada de 40 dp | — |
| Separación entre píldoras | 8 dp | 8 dp (horizontal) | — |
| Distancia al borde inferior | inset del sistema + 8 dp | igual | igual |
| Radio | siempre alto / 2 (píldora real) | igual | igual |

Con texto grande, el ancho se adapta a la etiqueta más larga hasta el espacio disponible y la altura reservada crece con la línea de texto. Las medidas en reposo se calculan fuera de la animación.

### 2.2 Estado y política de fusión

`BottomChromeState` (holder recordado en `MainActivity`) con un `Animatable` para `mergeFraction` y una política pura `BottomChromeMergePolicy`, probable con unit tests y reloj inyectado. **Entrada:** un `NestedScrollConnection` alrededor del contenido del `NavHost`. Usa `onPostScroll` para medir el desplazamiento realmente consumido.

| Evento | Resultado |
| --- | --- |
| Desplazamiento vertical consumido hacia el final acumulado ≥ 24 dp | Fusionar |
| Desplazamiento hacia arriba acumulado ≥ 24 dp | Separar |
| Desplazamiento hacia arriba no consumido (`available.y > 0`, principio de lista) | Separar |
| 800 ms sin desplazamiento (incluido el fling, contando desde el último delta u `onPostFling`) | Separar |
| Toque en el círculo | Separar; el acumulador vuelve a 0 |
| Cambio de ruta, teclado visible, sheet expandido | Separar (reset) |
| `abs(x) > abs(y)` | Ignorar (pagers y carruseles) |
| Ajuste «Compactar al desplazar» desactivado, o TalkBack (`touchExploration`) activo | Fusión desactivada |

- La animación usa `PixelMotion.structural`, o `snap()` si las animaciones están desactivadas. Cambiar de dirección reinicia el acumulador.
- La fusión y la separación no emiten háptica.
- El mismo estado sirve sin música: `mergeFraction` controla entonces la compactación de la píldora de navegación.

### 2.3 Píldora de navegación

Reescribir [PlayerInternalNavigationBar](../app/src/main/java/com/theveloper/pixelplay/presentation/components/PlayerInternalNavigationBar.kt) y `CustomNavigationBarItem` (retirado en la Fase 2) como un `Layout` propio que lea `mergeFraction` en la fase de layout:
- Ancho: `lerp(restWidth, 56.dp, f)`, desplazando la píldora desde el centro al inicio (respetando RTL).
- Las etiquetas pierden alpha y escala en el intervalo 0–0,4 de `f`.
- Los iconos no activos pierden alpha y bajan a escala 0,8 en el intervalo 0,2–0,7.
- El icono activo se centra en el círculo.
- Sin música, la compactación interpola el ancho hasta el valor compacto, centrada.

**Indicador deslizante:** la posición de sus bordes izquierdo y derecho se anima por separado. El borde de avance usa `stiffness = 800f` y el de arrastre `400f` (ambos con `dampingRatio = 0.75f`), lo que produce el estiramiento. Colores: indicador `secondaryContainer`, icono y etiqueta activos `onSecondaryContainer`, inactivos `onSurfaceVariant`.

**Volver a tocar la pestaña activa** sube la lista al principio. Se implementa con un `SharedFlow<String>` de reselección por ruta que escuchen Home, Search y Library; en Library, la lista de la pestaña visible. El doble toque de Buscar (`onSearchNavIconDoubleTapped`) conserva su comportamiento.

**Semántica:**
- Cada ítem mantiene `Role.Tab`, el estado de selección y su etiqueta, aunque esté oculta.
- La superficie pasa a ser un único botón al terminar el desvanecimiento de las etiquetas o antes de que las pestañas dejen de tener 48 dp disponibles. Su descripción es «Mostrar navegación, pestaña actual: …».
- Las zonas táctiles miden al menos 48 dp.

Sustituir el `Surface` del `bottomBar` de `MainActivity` (la forma de `NavBarShapeCache`, las esquinas animadas y la elevación) por `pillSurface`. Conservar la traslación de ocultación existente (`playerContentExpansionFraction` y `navBarVisibilityProgressState`), aplicada en `graphicsLayer`.

### 2.4 Píldora del mini reproductor y su fusión

- **No modificar `sheetCollapsedTargetY` por fotograma.** Hoy, un cambio en ese valor dispara `LaunchedEffect` → `syncToExpansion`.
- Añadir a [SheetVisualState](../app/src/main/java/com/theveloper/pixelplay/presentation/components/scoped/SheetVisualState.kt) un `mergeFractionProvider` que afecte solo al estado colapsado, ponderado por `(1 - expansionFraction)`:
  - Traslación Y hasta la fila del círculo, sumada en la `layout {}` exterior donde hoy se lee `visualSheetTranslationYProvider`.
  - Relleno inicial de 16 a 80 dp, vía `currentHorizontalPaddingStartPxProvider`.
  - Alto de 64 a 56 dp, vía `playerContentAreaHeightPxProvider`.
- **Forma colapsada:** radio = alto / 2. Eliminar las ramas de `navBarStyle`/`navBarCornerRadiusDp` de `SheetVisualState`. Las esquinas de la forma expandida no cambian.
- [MiniPlayerContentInternal](../app/src/main/java/com/theveloper/pixelplay/presentation/components/UnifiedPlayerSheetShared.kt) recibe `compactFractionProvider: () -> Float`. El artista y los botones de anterior y siguiente pierden alpha (`graphicsLayer`) y colapsan su ancho en layout. Quedan la portada (44 → 40 dp), el título en una línea con `AutoScrollingText` y el botón de play/pausa.
- **En pantallas sin navegación** (`routesWithHiddenNavigationBar`), la píldora se compacta igual pero conserva el inicio en 16 dp.
- **Gradiente continuo al fusionar:** ambas píldoras dibujan una capa `Brush.horizontalGradient` definida en coordenadas de la raíz (de `tint` a la izquierda a `playerAreaBackground` a la derecha), con alpha = `mergeFraction × 0,5`. Las posiciones en la raíz se obtienen una vez con `onGloballyPositioned` y se leen en la fase de dibujo.

### 2.5 Gestos

- **Horizontal = anterior/siguiente.** Sustituir el uso de `miniPlayerDismissHorizontalGesture` (`MiniPlayerDismissGestureHandler` (retirado en la Fase 2)) por `miniPlayerSkipHorizontalGesture`:
  - La portada y el texto siguen al dedo; el contenedor no se mueve.
  - Se confirma con un desplazamiento ≥ 25 % del ancho de la píldora o una velocidad ≥ 1000 dp/s. Izquierda = siguiente y derecha = anterior, invertido en RTL.
  - Al confirmar, se ejecuta el salto y se emite la háptica. El contenido saliente se conserva mientras se desliza fuera con `structural`; si cambió la pista, la nueva entra por el lado contrario con `feedback`. Si el gesto se cancela o sigue la misma pista, el contenido vuelve con `structural`.
  - Usa los mismos callbacks `onNext`/`onPrevious` que los botones, sin cambiar su semántica en Media3.
- **Abajo = cerrar la cola con deshacer.** Extender [SheetVerticalDragGestureHandler](../app/src/main/java/com/theveloper/pixelplay/presentation/components/scoped/SheetVerticalDragGestureHandler.kt) y `resolveVerticalSheetTargetState`:
  - Desde `COLLAPSED`, un arrastre hacia abajo ≥ 48 dp o una velocidad hacia abajo ≥ 800 dp/s llama a `dismissPlaylistAndShowUndo()`.
  - La píldora baja y se desvanece. Hoy el arrastre hacia abajo en colapsado solo hace un rebote de `0.2 × miniHeight`, que se conserva por debajo del umbral.
- **Arriba = abrir.** Sin cambios.
- **Riesgo:** con 16 dp de margen, un deslizamiento horizontal que empiece en la zona de gestos del sistema puede activar el atrás. No usar `systemGestureExclusion` (rompería el atrás y tiene un límite de 200 dp). Verificarlo en el dispositivo.

### 2.6 Retirada de la personalización

Eliminar:
- `NavBarStyle` (archivo retirado en la Fase 2) y las claves `nav_bar_style`, `nav_bar_compact_mode` y `nav_bar_corner_radius`, con sus flows, setters, `sanitizeNavBarCornerRadius` y las constantes MIN/MAX.
- Los campos correspondientes en `PlayerViewModel` (incluida la configuración del reproductor que transporta `navBarCornerRadius`), `SettingsViewModel` y `SetupViewModel`.
- `NavBarCornerRadiusScreen` (retirado en la Fase 2), `Screen.NavBarCrRad`, su ruta en [AppNavigation](../app/src/main/java/com/theveloper/pixelplay/presentation/navigation/AppNavigation.kt) y sus entradas en `routesWithHiddenNavigationBar`/`routesWithHiddenMiniPlayer`.
- Las opciones de barra de `SettingsCategoryScreen` y la página `SetupPage.NavBarLayout` de [SetupScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SetupScreen.kt).
- Los textos que queden sin uso, en todas las traducciones.
- El radio de [ExternalPlayerOverlay](../app/src/main/java/com/theveloper/pixelplay/presentation/components/external/ExternalPlayerOverlay.kt), que pasa a ser un token fijo.

Añadir:
- `DataMigration` que elimine las tres claves.
- Preferencia `nav_compact_on_scroll` (por defecto `true`), con un interruptor en la misma categoría de ajustes donde estaban las opciones de barra.
- Actualizar [PlayerBottomAnchoringTest](../app/src/test/java/com/theveloper/pixelplay/presentation/components/PlayerBottomAnchoringTest.kt), [UserPreferencesRepositoryTest](../app/src/test/java/com/theveloper/pixelplay/data/preferences/UserPreferencesRepositoryTest.kt) y `PlayerViewModelTest`.

La Fase 2 conserva `useSmoothCorners` hasta que la Fase 3 fija la forma del panel y retira el ajuste. El DataStore de ajustes está excluido del backup Android ([Backup y seguridad](backup-and-security.md)).

### 2.7 Borde inferior

Degradado vertical de `background` con alpha 0 arriba a 0,85 abajo. Su altura es `contentBottomPadding + 24 dp`, interpolada hacia la altura fusionada en la fase de dibujo con `mergeFractionProvider`. Sustituye a los degradados actuales de Home, Search y Library.

**Criterio de la Fase 2:** pruebas de la política (umbral, dirección, inactividad, principio de lista, horizontal ignorado, TalkBack, ajuste), de los gestos (umbrales y RTL) y de la migración. Ninguna recomposición de la lista por fotograma durante la fusión (comprobar con Layout Inspector o con el contador de recomposiciones). QA de la matriz de verificación del final de este documento.

---

## Fase 3. Del mini reproductor al reproductor completo

El sheet ya interpolaba el alto, los rellenos y la forma. El mini reproductor y el carrusel dibujaban portadas independientes.

- La forma interpola desde la píldora (radio igual a la mitad del alto) hasta el panel expandido. Se retira «Usar esquinas suaves» y su clave de DataStore. El contorno usa un radio uniforme.
- [PlayerArtworkTransition](../app/src/main/java/com/theveloper/pixelplay/presentation/components/scoped/PlayerArtworkTransition.kt) mide la portada circular y el elemento activo del carrusel en layout. Dibuja una capa decorativa entre ambas posiciones, reutilizando las capas de imagen de los extremos y sin cambiar el árbol precalentado de [FullPlayerCompositionPolicy](../app/src/main/java/com/theveloper/pixelplay/presentation/components/scoped/FullPlayerCompositionPolicy.kt).
- El movimiento usa el progreso del arrastre y de Atrás predictivo. Si el carrusel aún se está preparando al abrir, el panel abre de inmediato con el fundido existente. Si las animaciones están desactivadas, el arrastre directo sigue disponible y los cambios automáticos se aplican al instante.

**Criterio:** sin recomposición por fotograma de `FullPlayerContent` durante el arrastre. Cerrar devuelve todo a la píldora, incluso desde el estado fusionado (la fusión se separa al expandir).

---

## Fase 4. Halo de portada y fondo de malla

### 4.1 Halo (solo en la portada actual del [carrusel](../app/src/main/java/com/theveloper/pixelplay/presentation/components/AlbumCarouselSelection.kt))

| Capa | Especificación | Nivel mínimo |
| --- | --- | --- |
| Sombra teñida | `elevation = 24.dp`, ambient y spot = primario de la paleta (alpha 0,45 / 0,55) | BALANCED (en MINIMAL, sombra neutra) |
| Halo difuminado | Miniatura de 32 px de la misma portada (Coil a tamaño pequeño, en segundo plano), escalada a 1,25 × los límites de la portada, alpha 0,55 en oscuro y 0,35 en claro, con máscara radial (`BlendMode.DstIn`). La miniatura se suaviza en segundo plano antes del escalado bilineal | BALANCED |
| Desenfoque extra del halo | `RenderEffect.createBlurEffect(16.dp)` solo sobre la capa del halo | FULL |
| Borde de luz | Trazo interior de 1 dp: blanco (alpha 0,22) arriba, transparente a mitad y negro (alpha 0,18) abajo | BALANCED |

En **Completo**, el halo mide 1,9 veces la portada y alcanza el encabezado, el título y el artista. El tema claro recibe un refuerzo de opacidad y la máscara radial difumina el color hacia los controles. La primera entrada combina un fundido de 550 ms y un crecimiento del 4 %. Al cambiar de pista, las miniaturas se funden en una sola capa de dibujo. En pausa conserva el 80 % de intensidad. Equilibrado conserva un halo más localizado y baja al 60 % en pausa. Con movimiento reducido, la transición es inmediata.

### 4.2 Fondo de malla

El fondo expandido usa una malla AGSL de cuatro puntos estables derivados del identificador de la pista. Un patrón de puntos estático intercala tonos cercanos de la malla y del fondo base con una densidad que sigue el degradado. El color llega a la zona de transporte y se funde con blanco en claro y con negro en oscuro. En claro el color tiene más presencia y en oscuro se atenúa.

- El color del fondo cambia con la paleta en 600 ms y se actualiza de inmediato con movimiento reducido. El mini reproductor usa su superficie habitual.
- En Equilibrado y Completo, la píldora inferior y los botones de minimizar, dispositivo, cola y letra reutilizan el fondo grabado y desenfocan la porción detrás de cada control. Los segmentos inferiores se apoyan en la misma capa desenfocada. Completo usa un radio de desenfoque mayor. Con ahorro de batería se usa tinte translúcido. Mínimo conserva un fondo sólido.
- El contraste del título se calcula contra el extremo más desfavorable del degradado. Los pares cromáticos de transporte se toman de los roles de contenedor de Material.
- Las cabeceras de álbum y artista usan el mismo grano para fundir la portada y su paleta con la superficie. Si falta la foto del artista, el patrón se conserva y el degradado usa la paleta del primer álbum con portada disponible. Los detalles de mixes usan los tonos de sus ilustraciones procedurales y se funden con la superficie al llegar a la lista.

**Criterio de la Fase 4:** capturas comparativas en tema claro y oscuro con 3 portadas sintéticas (clara, oscura, saturada). Sin trabajo de dibujo continuo con la reproducción en pausa ni con la pantalla quieta (comprobar con el perfilador de GPU o con el trazado de `RenderThread`).

---

## Fase 5. `FavoriteButton` y microinteracciones

### 5.1 `FavoriteButton`

Nuevo componente en `presentation/components/FavoriteButton.kt`, con el estilo `FULL` o `REDUCED`.

| Momento | FULL (me gusta) | REDUCED | Quitar el me gusta |
| --- | --- | --- | --- |
| Pulsación | escala a 0,85 (`tween(90)`) | igual | igual |
| Confirmación | icono relleno, escala a 1,2 (120 ms con la curva de entrada) y después a 1,0 con `feedback` | escala con `feedback`, sin pasar por 1,2 | icono vacío, vuelta a 1,0 con `structural` |
| Anillo | de 0,5 a 1,6 × el tamaño del icono, trazo de 2 dp a 0 y alpha de 0,6 a 0, en 350 ms | — | — |
| Partículas | 7 puntos repartidos a 360/7 ± 10°, distancia de 0,6 a 1,5 × el icono, radio de 2,5 dp a 0, en 400 ms. Colores alternos terciario y primario de la paleta | — | — |
| Háptica | `Confirm` (como hoy) | `Confirm` | `ToggleOff` |

- **Regla:** la celebración se dispara en el `onClick` del usuario al pasar de no favorito a favorito, de forma optimista. **Nunca** desde `LaunchedEffect(isFavorite)`: no debe saltar al cambiar a una canción que ya era favorita ni con cambios que lleguen del reloj o de la notificación.
- Los segmentos de [FullPlayerContent](../app/src/main/java/com/theveloper/pixelplay/presentation/components/player/FullPlayerContent.kt) recortan sus propios fondos. El corazón dibuja el anillo y las partículas por encima de ese recorte.
- Con las animaciones desactivadas, el icono cambia sin escala ni partículas.
- **Semántica:** comportamiento de conmutador (`toggleable`) con `stateDescription` y textos desde recursos (hoy están escritos a mano: «Favorito»/«Favorite»).
- **Dónde va:**
  - La fila de botones de `FullPlayerContent` y [BottomToggleRow](../app/src/main/java/com/theveloper/pixelplay/presentation/components/player/BottomToggleRow.kt): FULL, conservando el fondo del segmento. Puede ser un slot de contenido en [ToggleSegmentButton](../app/src/main/java/com/theveloper/pixelplay/presentation/components/ToggleSegmentButton.kt) o un `FavoriteToggleSegment`.
  - [SongInfoBottomSheet](../app/src/main/java/com/theveloper/pixelplay/presentation/components/SongInfoBottomSheet.kt): FULL.
  - Las acciones individuales compactas pueden usar REDUCED. En las listas actuales, la acción individual abre la ficha de canción.
  - La acción masiva de `MultiSelectionBottomSheet` y la notificación de Media3 no cambian.

### 5.2 Otras microinteracciones

- **`ToggleSegmentButton`:** escala de pulsación a 0,96 y vuelta con `feedback`. Los segmentos del reproductor redondean más las esquinas que siguen el contorno exterior de la fila.
- **Repetir:** el icono cambia (todas ↔ una) con un fundido cruzado más escala de 0,8 a 1 con `feedback`.
- **Aleatorio:** al activarlo, el icono gira de −8° a 0° con `feedback`.
- **Llegada de portada** al cambiar de canción: escala de 0,96 a 1 con `feedback`, coordinada con el halo de la Fase 4.

**Criterio de la Fase 5:** prueba de que la celebración no se dispara por cambios externos de `isFavorite` (por ejemplo, separando la lógica de disparo en una función pura). Verificación visual con las animaciones del sistema activadas y desactivadas.

---

## Fase 6. Barras superiores en píldora

Evolucionar [CollapsibleCommonTopBar](../app/src/main/java/com/theveloper/pixelplay/presentation/components/CollapsibleCommonTopBar.kt) y [ExpressiveTopBarContent](../app/src/main/java/com/theveloper/pixelplay/presentation/components/ExpressiveTopBarContent.kt), que usan About, Accounts, AlbumDetail, ArtistDetail, ArtistSettings y otras pantallas:
- **Expandida:** título grande sobre un fondo transparente, sin barra sólida.
- **Colapsada:** píldora flotante de al menos 48 dp de alto, con margen de 16 dp y 8 dp de separación después del inset superior seguro. La altura crece con el texto del sistema. El ancho se ajusta al contenido, con un máximo de 360 dp limitado por el espacio entre los márgenes. Contiene Atrás, el título (`titleMedium`, una línea con elipsis) y las acciones, y usa `pillSurface`.
- **Transición:** la impulsa el `collapseFraction` existente. El mismo título interpola su posición y tamaño entre el encabezado y el interior de la píldora, y vuelve al expandirse. La superficie aparece entre 0,6 y 1,0, con el valor leído en layout o dibujo.
- Álbum, Artista y Género comparten la barra. Álbum y Artista la usan también en sus variantes experimentales.
- Inicio compacta el título en una píldora al inicio y agrupa proveedores y Ajustes en otra al final. Los dos iconos pierden sus fondos circulares individuales al compactar. Buscar, Biblioteca y proveedores usan sus encabezados específicos.

**Criterio:** zona táctil de atrás de al menos 48 dp, título legible con texto grande y sin solapamiento con la barra de estado en horizontal.

---

## Matriz de verificación por fase visual

Elige los casos que correspondan al cambio. El emulador permite repetir los recorridos. El SM-S921B aporta evidencia física de gestos, contraste y fluidez.

| Caso | Qué comprobar |
| --- | --- |
| Tema claro y oscuro, 3 portadas sintéticas | Contraste, tinte y gradiente continuo |
| Texto al 200 % | Etiquetas, altura de las píldoras, títulos |
| TalkBack | Orden, roles, estado de pestaña, sin fusión automática, círculo anunciado |
| Animaciones del sistema desactivadas | Cambios instantáneos, sin partículas |
| Ahorro de batería | Nivel efectivo rebajado |
| Navegación por gestos y por 3 botones | Distancia al borde inferior, choque con el gesto atrás |
| RTL (pseudolocale `ar-XB`) | Anclaje del círculo y dirección de los gestos invertidos |
| Horizontal, pantalla dividida y ventana libre | Insets (ya saneados con `sanitizeNavigationBarBottomInset`) |
| Buscar con el teclado abierto | Píldoras ocultas |
| Inicio de reposo a compacto y regreso | Título y acciones conservan sus extremos y los dos iconos comparten una superficie |
| «A mano» → «Me gusta» → Inicio o Buscar | Ambas pestañas responden con la barra visible, incluso al entrar por un acceso directo |
| Carrusel y géneros con o sin carátulas | Arte reconocible en cada formato, etiqueta legible y correspondencia entre carrusel y detalle |
| Sin música → con música → cerrar cola → deshacer | Transiciones de una a dos píldoras y vuelta |

Comandos (ver [Build y pruebas](build-and-testing.md)): `.\gradlew.bat :app:assembleDebug '-Ppixelplay.enableX86Debug=true'`, `:app:testDebugUnitTest`, `:app:lintDebug` y `python scripts/context_docs.py check`.

### Evidencia de la Fase 2

- En la revisión de la píldora centrada se comprobó en un emulador API 37 su aspecto con y sin mini reproductor. Con la reproducción en pausa, un gesto horizontal pasó de la pista de prueba 15 a la 16, el gesto hacia abajo cerró la cola y Deshacer restauró la pista 16. La suite JVM registró 382 pruebas sin fallos. La prueba instrumental verificó el objetivo táctil, la acción semántica de reapertura y la ausencia de recomposición de su lista de prueba durante la fusión.
- En el emulador API 37 se observaron las dos píldoras en reposo y la navegación de Inicio a Biblioteca. `BottomChromeCompositionTest` ejecutó la animación real del holder y contó las composiciones de una lista de prueba. El informe XML registró 1 prueba sin fallos. El proceso auxiliar del APK de pruebas registró por separado un error de carga de `androidx.startup.InitializationProvider`.
- El primer arranque sobre una base local antigua del emulador falló con un `identity hash` de Room distinto. La base anterior se conservó y la comprobación visual se hizo con una base nueva. Este cambio no introduce migraciones de Room.
- `:app:lintDebug` reportó errores globales. El primero fue `MissingDefaultResource` para `sharing_playlists_message`, reproducible por inspección de `HEAD`. El informe no señaló errores en los componentes nuevos o reescritos de las píldoras.
- En el Galaxy S24 (SM-S921B, Android API 37, One UI 9) se instaló el APK debug ARM64. Se verificaron la fusión al desplazar, la separación en reposo, el cambio de pista con gesto horizontal, el cierre de la cola y Deshacer, y la ocultación de las píldoras con el teclado. Con texto al 200 %, los objetivos de las tres pestañas permanecieron separados.
- Se confirmó que TalkBack activó la exploración táctil. Al activarse, PixelPlayer dejó de estar en primer plano, por lo que la interacción con las píldoras mediante TalkBack sigue pendiente. Se restauraron los ajustes de accesibilidad.
- En una revisión posterior en el S24, el recorte de la pulsación de Buscar dejó intactas las esquinas exteriores en ambos temas. Las dos píldoras se compactaron con un tinte de portada y recuperaron la superficie neutra al separarse. Reproducción y Apariencia abrieron sin el mini reproductor, que reapareció con la misma pista al volver.

### Evidencia de la Fase 3

- En el emulador API 37, la portada de una pista sintética creció y se desplazó durante el arrastre. El gesto Atrás desde el borde devolvió el reproductor a la píldora. Abrir el teclado de Buscar ocultó las dos píldoras y cerrarlo devolvió el mini reproductor con la misma pista.
- Las pruebas instrumentadas cubren los píxeles del recorrido reversible, el fundido si falta el carrusel al comenzar y el contador de recomposiciones del contenido de prueba.
- En el S24 se verificó la apertura por arrastre y el regreso a la misma pista con Atrás desde el borde. Una apertura usó el fundido y una repetición con la portada precargada mostró su desplazamiento durante el arrastre. Queda pendiente verificar en el teléfono la cancelación a mitad del gesto.
- Con un APK diagnóstico que marca las composiciones aplicadas de `FullPlayerContent`, Perfetto registró 2 composiciones en 163 fotogramas de la app durante la apertura y 0 en 157 durante Atrás. Los datos corresponden a un recorrido en el S24.

### Evidencia de la Fase 4

- El APK debug incluye `arm64-v8a` y `x86_64`. La suite JVM pasó con 385 pruebas al repetirla con el emulador en reposo, después de un fallo de la prueba de velocidad Hi-Fi. El informe de lint conservó los hallazgos de la fase 3 sin añadir otros.
- En API 37 se capturaron tres portadas sintéticas en tema claro y oscuro y se compararon los tres niveles de efectos. Entre Equilibrado y Completo cambiaron los píxeles de la zona exterior del halo. Las regiones muestreadas en el centro de la portada y en el fondo conservaron los mismos píxeles. También se probaron una pista sin portada, el arrastre de apertura, Atrás desde el borde, los saltos rápidos y el movimiento reducido.
- Pasaron cinco pruebas instrumentadas en API 37. Cubren fusión de las píldoras, transición reversible de portada, apertura con carga diferida, aislamiento del desenfoque y geometría activa en el estilo heredado Two Peek. El primer intento se detuvo por un ANR de arranque en el emulador. El reintento pasó después de compilar los APK con el perfil de ART `speed`.
- Con ahorro de batería activo, Completo produjo los mismos píxeles que Equilibrado en la región muestreada del halo. Se comprobó No Peek y One Peek en la app y Two Peek en la prueba del carrusel.
- En el emulador, la traza Perfetto cubrió 10,07 s en pausa y 10,05 s de reproducción estable. Registró cero ejecuciones de `PixelPlay.playerMesh.draw` y `PixelPlay.playerHalo.draw` en ambos intervalos. El cambio de pista registró cinco ejecuciones de la malla y una del halo.
- En el S24, con Completo y una canción de la biblioteca del teléfono, Perfetto registró cero ejecuciones de ambos marcadores durante 10,10 s en pausa y 10,08 s de reproducción estable. El cambio de pista registró 16 dibujos de la malla y uno del halo. Los marcadores cuentan ejecuciones de las capas en Compose.
- En las capturas del S24 de una misma portada y con tema oscuro, pasar de Equilibrado a Completo cambió los píxeles del halo en las regiones muestreadas. El interior de la portada y la región de fondo muestreada coincidieron. Quedan pendientes una comparación de los tres niveles con la misma portada y la medición física de contraste y degradación con ahorro de batería.

## Estado

El rediseño de [Inicio](ui-and-state.md#inicio-y-detalle-de-mixes) aplica las píldoras de superficie neutra en «A mano» y en el encabezado al desplazar. El carrusel reúne las selecciones con el mismo peso visual y confina el color al arte. La [decisión sobre recomendaciones](decisions/0005-home-discovery-and-acoustic-similarity.md) describe la selección y la validación acústica.

| Fase | Estado | Notas |
| --- | --- | --- |
| 1. Fundamentos | Implementada (código y pruebas JVM; pendiente validación visual en dispositivo) | `Motion.kt` (`PixelMotion`, `PixelPlayMotionScheme`, `LocalMotionPreferences`), `VisualEffectsLevel` (`FULL`/`BALANCED`/`MINIMAL` + `DataMigration` + ahorro de batería en `VisualEffectsState.kt`), `Depth.kt` (`Modifier.pillSurface` + contraste garantizado) y `BottomChromeMetrics.kt` (`LocalBottomChrome` en 8 pantallas). |
| 2. Píldoras y gestos | Implementada. QA físico parcial. | Fusión al desplazar, reselección, gestos del mini reproductor, migración y ajuste de compactación. La prueba instrumental comprobó que una lista de prueba no se recompone durante la animación de fusión o separación. |
| 3. Transformación hacia el reproductor | Implementada. QA físico parcial. | Portada compartida reversible, apertura inmediata con carrusel no disponible y retirada de «Usar esquinas suaves». |
| 4. Halo y malla | Implementada. Validada en emulador, QA físico parcial. | Malla en el reproductor completo, halo de la portada activa, degradación por nivel y dibujo sin invalidaciones continuas en los intervalos medidos. |
| 5. FavoriteButton y microinteracciones | Implementada | Celebración por toque, movimiento reducido y entrada de portada al cambiar de pista. |
| 6. Barras superiores | Implementada | Píldora al desplazar, título adaptable y acciones compartidas. |
