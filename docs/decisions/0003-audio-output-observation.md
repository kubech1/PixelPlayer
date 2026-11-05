# ADR 0003: observación compartida de audio y recuperación acotada

Estado: aceptada para el alcance de modernización, 1 de noviembre de 2026.

## Contexto

Servicio y pantalla de capacidades consultaban datos separados. Una avería del ecualizador podía desactivar sus efectos durante todo el proceso; los nombres de codecs Samsung se presentaban como prueba de aceleración. Las consultas de biblioteca y almacenamiento no deben repetirse ante cada cambio de salida.

## Decisión

`AudioOutputRepository` posee un `StateFlow` compartido y una fuente de consultas Android pequeña y sustituible en pruebas. Servicio y ViewModel adquieren/liberan un único listener de dispositivos. La reanudación y los callbacks de sesión/sink refrescan la observación. Se conserva la ruta prevista por `getAudioDevicesForAttributes`, separada de dispositivos disponibles y de la configuración del sink.

Un `ForwardingAudioSink` delega todos los callbacks originales de Media3 y solo observa capacidades/configuración/sesión. Los tokens de sink evitan que la precarga del auxiliar reemplace los datos del principal. No se reconstruyen players al cambiar una observación. Las inferencias sobre codecs por nombre quedan separadas del indicador oficial de `MediaCodecInfo`.

Una revisión de ruta invalida la prueba PCM float y los datos de sink anteriores. La prueba solo verifica aceptación de AudioTrack a 44,1 kHz estéreo, no resolución del DAC ni códec Bluetooth. Offload solicitado, soporte consultado para un formato y configuración/reposo observado son datos distintos. El códec Bluetooth queda desconocido sin una API pública fiable; un nombre Bluetooth nunca selecciona perfiles ni tecnologías.

El ecualizador conserva los ajustes solicitados. Una inicialización fallida bloquea nuevos intentos para esa ruta/sesión; una sesión o ruta nueva permite otro intento. No hay temporizadores de reintento. La enumeración de codecs se conserva en la pantalla y el almacenamiento/biblioteca se carga por separado de los cambios de ruta.

## Alternativas y compromisos

Mantener consultas independientes era más simple localmente, pero permite estados contradictorios y trabajo duplicado. Reiniciar ambos players por callback rompería continuidad y negociación, y no demuestra una mejora: se descarta. Forzar codecs o perfiles por nombre de receptor se descarta por falta de evidencia del transporte negociado y por la compatibilidad con equipos desconocidos.

La instantánea requiere manejo explícito de desconocidos, invalidación y propiedad del listener. Una ruta prevista no certifica el recorrido final ni sustituye pruebas físicas de continuidad, EQ, consumo o temperatura. Los fallos de consulta publican desconocido en vez de conservar una ruta antigua como si siguiera vigente.

## Verificación

Ver resultados ejecutados y límites en [estado de modernización](../modernization-status.md). Pruebas de cambio/fallo de ruta, separación principal/auxiliar y gate de recuperación, más compilación y QA en emulador, no certifican sonido real en S24/receptores.
