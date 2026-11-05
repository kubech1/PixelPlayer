# ADR-0002: Android 17 y retirada de IA del producto

Estado: aceptado por el usuario el 31 de octubre de 2026. Implementación y evidencia: [estado de modernización](../modernization-status.md).

## Contexto

El dispositivo prioritario es el Galaxy S24 SM-S921B con Android 17/One UI 9 declarado por el usuario. Sus receptores habituales son WH-1000XM5, Buds FE SM-R400 y SRS-XB100. Deben seguir funcionando otras salidas. La IA del producto se considera prescindible; las mezclas locales tienen valor independiente.

## Decisión

- Retirar inferencia, proveedores, credenciales, generación y traducción mediante IA, conservando playlists y letras guardadas. Las playlists antiguas de fuente `AI` se convierten en `LOCAL`.
- Migrar Room 42→43 sin borrar biblioteca, historial ni relaciones. Los backups antiguos omiten módulos retirados y no reintroducen preferencias de IA.
- Elevar a API 37 el mínimo de `app` y sus benchmarks. Mantener `wear` y `shared` en API 30; un consumidor puede requerir un mínimo mayor que la biblioteca compartida.
- Usar APIs públicas del Android 17 inicial y capacidades consultadas. No depender de APIs 37.2, permisos privilegiados, nombres Bluetooth o supuestas capacidades de un modelo.
- Háptica expresiva mediante eventos semánticos e intensidad elegida por el usuario, respetando desactivación y política del sistema.
- Conservar sonido, volumen y EQ; mejorar diagnóstico y recuperación por ruta sin perfiles tonales nuevos.
- Verificar cada etapa antes de avanzar y diferenciar pruebas JVM, emulador y dispositivo físico.

## Alternativas

Conservar Android 11 con ramas de compatibilidad era viable, pero el usuario eligió Android 17 como mínimo. Eliminar Daily Mix y todas las listas generadas descartaría funciones y datos que se decidió conservar. Forzar frecuencias, códecs o refresco por marca no demuestra una mejora y perjudicaría equipos desconocidos.

## Consecuencias

El teléfono deja de admitir Android anterior a 17. Wear mantiene su alcance independiente. Permanecen referencias históricas a IA solo cuando son necesarias para migración, pruebas y esquemas anteriores; la documentación para agentes de programación sigue vigente. La validación acústica, háptica y energética del S24 requiere hardware real y no se sustituye por compilar ni emular.
