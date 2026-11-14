# ADR 0006. Música en la nube como opción

Estado: aceptada.

## Contexto

PixelPlayer administra una biblioteca local y catálogos de proveedores. El filtro de Biblioteca cambia lo que se muestra en algunas vistas. La opción Música en la nube tiene que aplicarse también a las consultas, sincronización y reproducción.

## Decisión

Persistir un interruptor global en DataStore. Las instalaciones nuevas empiezan con el catálogo local. La inicialización de una instalación existente puede conservar la nube activa si encuentra una cuenta configurada. Cuando se apaga, el filtro efectivo es Local y se ocultan los accesos a proveedores. Los catálogos, cuentas, filtros guardados y cachés se conservan.

El origen de una pista se mantiene aunque el proveedor haya escrito el audio en un archivo de caché. Room aplica `source_type` en las consultas unificadas. La sesión Media3 conserva el origen al resolver URI y restaurar la cola. Al pasar a modo local, una pista local en curso conserva su posición y las entradas de proveedor salen de la cola. Si sonaba una pista remota, la sesión queda en pausa.

## Consecuencias

El filtro Fuente sigue disponible con la nube habilitada. Los trabajadores de sincronización consultan la preferencia antes de sincronizar proveedores. Biblioteca, Buscar e Inicio pueden reaccionar al cambio sin reconstruir la base de datos.
