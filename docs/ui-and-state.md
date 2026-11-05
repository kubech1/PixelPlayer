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
| Ajustes y setup | [SettingsCategoryScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SettingsCategoryScreen.kt), [SetupScreen](../app/src/main/java/com/theveloper/pixelplay/presentation/screens/SetupScreen.kt), [SettingsViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SettingsViewModel.kt), [SetupViewModel](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/SetupViewModel.kt) |
| Tema y color de portada | [ui/theme](../app/src/main/java/com/theveloper/pixelplay/ui/theme), [ThemeStateHolder](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ThemeStateHolder.kt), [ColorSchemeProcessor](../app/src/main/java/com/theveloper/pixelplay/presentation/viewmodel/ColorSchemeProcessor.kt) |
| Widgets del launcher | [ui/glancewidget](../app/src/main/java/com/theveloper/pixelplay/ui/glancewidget), [WidgetUpdateManager](../app/src/main/java/com/theveloper/pixelplay/data/service/WidgetUpdateManager.kt) |

Los archivos grandes contienen varias responsabilidades de presentación. Una corrección pequeña debe localizar el componente/handler concreto; no exige reescribir toda la pantalla.

## Observar el estado que se necesita

`PlayerViewModel` ofrece slices de estado y delegaciones. `LibraryViewModel` expone los flows paginados de `LibraryStateHolder` con `cachedIn(viewModelScope)`. La canción actual, la posición, los temas, la búsqueda y la cola no tienen idéntica frecuencia de cambio.

Para añadir UI, identifica:

1. Quién posee la verdad: servicio, repositorio, preferencia, holder o estado efímero de Compose.
2. Qué estado necesita realmente ese componente y cómo se observa con su ciclo de vida.
3. Qué evento actualiza el estado y qué sucede si se interrumpe, cambia la canción o se cierra la pantalla.
4. Qué valores deben sobrevivir recreación o reinicio y cuáles no.

Evita duplicar estado persistente en `remember` sin sincronización o recopilar un objeto amplio solo para un boolean. Esto es una pauta de diseño; se debe comprobar el efecto concreto antes de afirmar que un cambio reduce recomposiciones.

## Temas, recursos y movimiento

[Theme](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Theme.kt), [ColorRoles](../app/src/main/java/com/theveloper/pixelplay/ui/theme/ColorRoles.kt), [Type](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Type.kt) y [Shape](../app/src/main/java/com/theveloper/pixelplay/ui/theme/Shape.kt) son referencias de estilo. La paleta de una portada pasa por extracción y procesamiento, no es simplemente un color fijo.

Los recursos están en [app/src/main/res](../app/src/main/res): XML de valores, traducciones, drawables, fuentes, layouts de soporte y configuración Android. Modifica claves de recursos con sus consumidores; no confundir la UI Compose con ausencia de XML. El [índice de configuración](reference/config-index.md) cubre los recursos textuales; imágenes y fuentes binarias se localizan en estos directorios y `assets/` en la raíz.

Para animaciones, parte del propósito de la interacción. Conserva semántica y continuidad al interrumpir o invertir una transición, usa preferencias de movimiento reducido y comprueba texto grande, temas y orientación. Los valores de las skills de diseño son recomendaciones: CSS, GSAP y ejemplos de React no se pegan en Compose.

## Cambios visibles y verificación

Las pruebas existentes incluyen lógica de letras, scrollbars, anclaje del player, temas scoped y animación de navegación de carpetas. No equivalen a una inspección visual de toda la aplicación. Un cambio visual necesita, según su alcance, capturas y prueba de interacción en un dispositivo/emulador, semántica accesible y contraste sobre los colores realmente compuestos.

En el resumen del trabajo, distingue «prueba de lógica pasó», «compiló» y «verificado visualmente». Si solo se hizo lectura estática, no atribuirle resultados de UX observados.

## Háptica y trabajo visual

[AppHaptics](../app/src/main/java/com/theveloper/pixelplay/presentation/utils/AppHaptics.kt) adapta Compose y View a eventos semánticos compartidos. `haptics_enabled` conserva su valor; `haptic_intensity` admite LOW/MEDIUM/HIGH, con MEDIUM por defecto. Los ajustes existentes ofrecen selección y previsualización. El wrapper respeta el interruptor, la vista, las preferencias del sistema y USAGE_TOUCH; no acepta flags para saltarse esos ajustes.

Se consultan capacidades y límites de envelopes. Se utiliza `BasicEnvelopeBuilder` (API 36, disponible en la base 37), con primitivas o feedback estándar como fallback. No se usa el Builder de 37.2 ni vibración sincronizada con música. El limitador compartido por ventana evita duplicados y restringe ticks/límites repetitivos a 50 ms. La intensidad en el fallback estándar depende del sistema.

`AppVisibility` sigue `ProcessLifecycleOwner`, separado del servicio de música. El tick UI normal y el de Mashup requieren suscriptores y UI visible; el muestreador del slider exige lifecycle RESUMED. Los loops decorativos de onda/EQ y el vinilo de letras verifican bounds visibles, lifecycle y animaciones del sistema. La categoría de refresco alta se solicita durante scroll o contacto directo con el player; vuelve a Default al terminar o perder visibilidad. No se fija un refresco global ni se promete un resultado de rendimiento sin medición.
