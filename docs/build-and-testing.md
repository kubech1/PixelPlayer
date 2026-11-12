# Entorno, compilación y verificación

## Fuente de la configuración

Consulta primero los archivos propietarios; no deducir requisitos actuales de una versión antigua de Android Studio en el README.

| Aspecto | Base inspeccionada el 31-10-2026 | Fuente |
| --- | --- | --- |
| Gradle Wrapper | 9.5.1, distribución con SHA-256 | [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties) |
| Android Gradle Plugin / Kotlin | 9.2.1 / 2.4.0 declarados | [libs.versions.toml](../gradle/libs.versions.toml) |
| JVM | Java/JVM 21; daemon JetBrains 21 configurado | [daemon JVM](../gradle/gradle-daemon-jvm.properties), builds de módulos |
| Android | `compileSdk`/`targetSdk` 37 para apps; `minSdk` 37 en teléfono/baselineprofile y 30 en Wear/shared | [app](../app/build.gradle.kts), [wear](../wear/build.gradle.kts), [shared](../shared/build.gradle.kts) |
| Versión del producto | Propiedades `APP_VERSION_NAME` y `APP_VERSION_CODE` | [gradle.properties](../gradle.properties) |
| ABI del teléfono | `arm64-v8a`. Debug local admite `x86_64` con `pixelplay.enableX86Debug` | [app/build.gradle.kts](../app/build.gradle.kts) |
| UI compiler reports | Opt-in `pixelplay.enableComposeCompilerReports` | Build de app y [compose_stability.conf](../app/compose_stability.conf) |

Esto registra valores del repositorio, no certifica compatibilidad de todo el conjunto ni afirma que sean las versiones más recientes. Al actualizarlos, revisar la documentación oficial de las herramientas implicadas y comprobar el build.

Necesitas una instalación de Java apropiada, Android SDK correspondiente y un Android Studio compatible con el AGP configurado si usas el IDE. `local.properties` es configuración local; no copiarlo al repositorio. Telegram admite parámetros locales de API en el build; documentar nombres de claves, nunca credenciales privadas.

El entorno de modernización comprobó JBR 25 como launcher, daemon JBR 21 y SDK 37.0 en la instalación local. Ver [resultados y pendientes](modernization-status.md); la tabla por sí sola no es evidencia de ejecución.

## Comandos desde la raíz, en PowerShell

Los siguientes comandos son **procedimientos derivados de la configuración**, no resultados de ejecución de esta revisión:

```powershell
# Comprobar toolchain/Wrapper y descubrir tareas reales.
.\gradlew.bat --version
.\gradlew.bat tasks --all

# APK de teléfono ARM64.
.\gradlew.bat :app:assembleDebug

# APK debug de teléfono con x86_64 adicional para emuladores Intel/AMD.
.\gradlew.bat :app:assembleDebug '-Ppixelplay.enableX86Debug=true'

# APK de reloj.
.\gradlew.bat :wear:assembleDebug

# Pruebas JVM del teléfono y lint explícito.
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug :wear:lintDebug

# Prueba instrumental: requiere dispositivo/emulador preparado.
.\gradlew.bat :app:connectedDebugAndroidTest

# Solo documentación: Python 3.10+; no requiere SDK Android.
python scripts/context_docs.py check
```

Para una clase concreta, se puede filtrar la tarea JVM con `--tests "paquete.ClaseTest"`; usa el paquete real del archivo. En instrumentación, restringe clase/escenario con los argumentos del runner cuando corresponda. No ejecutar una batería completa por un cambio puramente tipográfico si una comprobación documental es suficiente.

En otros shells, usar `./gradlew` en vez de `gradlew.bat`. El workflow del APK usa el Wrapper para reproducir la versión fijada.

## Graphify

Graphify indexa localmente el código de `app`, `wear`, `shared` y `baselineprofile` para ubicar símbolos y explorar relaciones. Con `uv` disponible, instala la CLI y genera el grafo desde la raíz. La versión comprobada es `graphifyy` 0.9.106 con Python 3.12. Si PowerShell no encuentra `graphify` tras la instalación, ejecuta `uv tool update-shell` y abre una terminal nueva.

```powershell
uv tool install --python 3.12 graphifyy==0.9.106
graphify extract . --code-only
graphify cluster-only . --no-label
graphify explain MusicService
```

Ejecuta `graphify update .` desde la raíz antes de cerrar una tarea que añada, modifique, mueva o elimine código indexable. Úsalo también al cambiar de rama o al ajustar `.graphifyignore`. Si un cambio en `.gitignore` altera la cobertura del grafo, ejecuta `graphify update . --force` para aplicar todas las exclusiones. Comprueba el resultado del comando antes de dar el grafo por actualizado.

Los resultados se generan en `graphify-out/` y quedan fuera de Git. La extracción `--code-only` omite el análisis semántico de documentación y multimedia. Consulta los manifiestos, recursos XML y contratos de ejecución en sus archivos fuente. [Referencia de la CLI](https://docs.graphify.com/open-source/configuration/cli).

## Qué verifican las pruebas presentes

| Área | Ubicación y ejemplos |
| --- | --- |
| JVM / lógica y repositorios | [app/src/test](../app/src/test): cola/estado, almacenamiento, filtros, letras, seguridad, backups, políticas de audio, diagnósticos y proveedores |
| Android / Room / workers / interfaz | [app/src/androidTest](../app/src/androidTest): DAOs, migraciones, conteo de playlists, sync y componentes Compose |
| Perfiles / macrobenchmark | [baselineprofile/src/main](../baselineprofile/src/main): generador, startup, player sheet y preparación del entorno |
| Wear / shared | No hay fuentes de tests propias en esos módulos en esta copia. Verificar ambos consumidores al cambiar contratos. |

`app` configura JUnit Platform para unit tests y contiene dependencias JUnit Jupiter/Vintage, coroutines test, MockK, Turbine, Truth y Room testing. Hay 70 archivos Kotlin en `app/src/test` y 6 en `app/src/androidTest` en la revisión inicial; esos conteos **no son cantidad de tests ejecutados ni una medida de cobertura**.

Para baseline profiles, `useConnectedDevices = true` y `automaticGenerationDuringBuild = false`: un build release ordinario no demuestra que se hayan regenerado perfiles. Descubre la tarea de generación disponible con `tasks --all`, prepara el target y ejecuta el escenario aplicable. [BenchmarkEnvironment](../baselineprofile/src/main/java/com/theveloper/pixelplay/baselineprofile/BenchmarkEnvironment.kt) configura permisos y valida foreground; no tomar resultados con setup fallido.

Para Inicio, [HomeMixContentTest](../app/src/androidTest/java/com/theveloper/pixelplay/presentation/components/HomeMixContentTest.kt) comprueba los accesos de «A mano» y las acciones independientes de detalle y reproducción tras desplazar el carrusel. Usa texto al 200 % y desactiva el movimiento en la prueba del carrusel.

[LocalAudioAnalyzerDiagnosticTest](../app/src/androidTest/java/com/theveloper/pixelplay/data/audio/LocalAudioAnalyzerDiagnosticTest.kt) usa tonos generados y el analizador del source set debug. Su opción `audioSimilarityScanLocal` decodifica en el dispositivo y registra métricas agregadas. Requiere permiso de audio en la app debug. Usa `audioSimilarityMaxTracks` para acotar la muestra. Las muestras sintéticas adicionales se pasan por `audioSimilarityFixtureUris` como URI separadas por `|`.

### Preparar una prueba auditiva ciega

Instala los APK debug y androidTest, concede el permiso de audio y pausa el reproductor. Usa una biblioteca con al menos diez pistas decodificables y suficientes coincidencias de artista o género para preparar diez comparaciones.

```powershell
adb shell pm grant com.theveloper.pixelplay.debug android.permission.READ_MEDIA_AUDIO
adb shell am instrument -w -e class com.theveloper.pixelplay.data.audio.LocalAudioAnalyzerDiagnosticTest -e audioSimilarityBlindEval true -e audioSimilarityMaxTracks 12 -e audioSimilarityBlindSeed 20261111 com.theveloper.pixelplay.debug.test/androidx.test.runner.AndroidJUnitRunner
adb shell am start -n com.theveloper.pixelplay.debug/com.theveloper.pixelplay.data.audio.BlindAudioEvaluationActivity
```

La instrumentación prepara un piloto reproducible de 12 pistas. El valor `0` en `audioSimilarityMaxTracks` solicita la biblioteca local completa y puede requerir varios minutos. La pantalla de prueba consume la sesión al abrirse. Presenta dos listas de cinco candidatos en orden ciego para cada una de las diez referencias. Las listas comparan el análisis acústico con el orden por artista y género. El resumen cuenta cuántas referencias alcanzan tres coincidencias de cinco en cada método. Una sesión incompleta deja el umbral sin evaluar.

## Artefactos, firma y CI

- `app` tiene variantes debug, release y benchmark. Debug añade `.debug` al applicationId. Wear también aplica sufijo debug; mantener alineación de identidad y firma para Data Layer.
- App release activa R8 y shrink de recursos con [proguard-rules.pro](../app/proguard-rules.pro). Un build release local sin keystore usa la firma debug por defecto.
- Cada push a `master` ejecuta [phone-release](../.github/workflows/phone-release.yml). También se puede lanzar manualmente sobre `master`. CI compila con `-Ppixelplay.unsignedRelease=true`, verifica que la única ABI sea `arm64-v8a`, firma con la clave de distribución y sube `PixelPlay-phone-release-arm64-v8a.apk` para descarga directa en **Actions → Artifacts**. [CodeQL](../.github/workflows/codeql.yml) compila las fuentes de teléfono y reloj para su análisis estático.
- `checkReleaseBuilds = false` está configurado para lint en app; Wear ajusta también su política. Ejecutar el lint apropiado de forma explícita cuando el cambio lo requiera.

### Configurar la firma de distribución

En **Settings → Secrets and variables → Actions**, configura estos secretos del repositorio.

| Secreto | Valor |
| --- | --- |
| `PIXELPLAY_RELEASE_KEYSTORE_BASE64` | Contenido del keystore de distribución codificado en Base64 en una sola línea |
| `PIXELPLAY_RELEASE_STORE_PASSWORD` | Contraseña del keystore |
| `PIXELPLAY_RELEASE_KEY_ALIAS` | Alias de la clave de distribución |
| `PIXELPLAY_RELEASE_KEY_PASSWORD` | Contraseña de esa clave |

Conserva un respaldo privado del keystore. Si ya distribuiste PixelPlayer, usa la misma clave para que Android acepte las actualizaciones sobre esas instalaciones. El workflow requiere los cuatro secretos y comprueba la firma antes de subir el archivo. [Firma de apps Android](https://developer.android.com/studio/publish/app-signing), [secretos de GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).

## Qué registrar en una entrega

Comando y variante, resultado/código de salida, entorno relevante, caso funcional comprobado y limitaciones. «No ejecutado por falta de entorno» es distinto de «falló»; «compiló» es distinto de «se escuchó correctamente» y de «la migración conservó datos».

