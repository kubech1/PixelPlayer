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
| ABI del teléfono | Splits ARM activos por defecto; propiedad `pixelplay.enableAbiSplits` | [app/build.gradle.kts](../app/build.gradle.kts) |
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

# APK de teléfono. Sin splits para un artefacto local; revisar ABI de dependencias.
.\gradlew.bat :app:assembleDebug -Ppixelplay.enableAbiSplits=false

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

En otros shells, usar `./gradlew` en vez de `gradlew.bat`. Los workflows actuales usan `gradle` tras `setup-gradle`; localmente se documenta el Wrapper para reproducir la versión fijada.

## Qué verifican las pruebas presentes

| Área | Ubicación y ejemplos |
| --- | --- |
| JVM / lógica y repositorios | [app/src/test](../app/src/test): cola/estado, almacenamiento, filtros, letras, seguridad, backups, políticas de audio, diagnósticos y proveedores |
| Android / Room / workers | [app/src/androidTest](../app/src/androidTest): DAOs, migraciones, conteo de playlists, sync y benchmark de startup |
| Perfiles / macrobenchmark | [baselineprofile/src/main](../baselineprofile/src/main): generador, startup, player sheet y preparación del entorno |
| Wear / shared | No hay fuentes de tests propias en esos módulos en esta copia. Verificar ambos consumidores al cambiar contratos. |

`app` configura JUnit Platform para unit tests y contiene dependencias JUnit Jupiter/Vintage, coroutines test, MockK, Turbine, Truth y Room testing. Hay 70 archivos Kotlin en `app/src/test` y 6 en `app/src/androidTest` en la revisión inicial; esos conteos **no son cantidad de tests ejecutados ni una medida de cobertura**.

Para baseline profiles, `useConnectedDevices = true` y `automaticGenerationDuringBuild = false`: un build release ordinario no demuestra que se hayan regenerado perfiles. Descubre la tarea de generación disponible con `tasks --all`, prepara el target y ejecuta el escenario aplicable. [BenchmarkEnvironment](../baselineprofile/src/main/java/com/theveloper/pixelplay/baselineprofile/BenchmarkEnvironment.kt) configura permisos y valida foreground; no tomar resultados con setup fallido.

## Artefactos, firma y CI

- `app` tiene variantes debug, release y benchmark. Debug añade `.debug` al applicationId. Wear también aplica sufijo debug; mantener alineación de identidad y firma para Data Layer.
- App release activa R8 y shrink de recursos; usa reglas [proguard-rules.pro](../app/proguard-rules.pro). Si falta el keystore configurado, el build de app selecciona firma debug como fallback: un APK llamado release no equivale automáticamente a una distribución firmada con la clave oficial.
- El workflow de release genera/cachea una clave de CI y aclara que no es la clave oficial. No copiar valores de esa configuración a documentación como credenciales recomendadas.
- [phone-debug](../.github/workflows/phone-debug.yml), [phone-release](../.github/workflows/phone-release.yml), [wearos-apk](../.github/workflows/wearos-apk.yml) y [nightly-apk](../.github/workflows/nightly-apk.yml) construyen artefactos. [CodeQL](../.github/workflows/codeql.yml) es otro análisis. No interpretar sus nombres como evidencia de tests/lint completos: revisar pasos y logs reales.
- `checkReleaseBuilds = false` está configurado para lint en app; Wear ajusta también su política. Ejecutar el lint apropiado de forma explícita cuando el cambio lo requiera.

Esta preparación no cambió workflows ni añadió un bloqueo CI nuevo. Se puede incorporar el comprobador documental a CI cuando se decida el flujo del repositorio; hoy es una herramienta local reproducible.

## Qué registrar en una entrega

Comando y variante, resultado/código de salida, entorno relevante, caso funcional comprobado y limitaciones. «No ejecutado por falta de entorno» es distinto de «falló»; «compiló» es distinto de «se escuchó correctamente» y de «la migración conservó datos».

