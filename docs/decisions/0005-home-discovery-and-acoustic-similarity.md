# ADR 0005. Descubrimiento en Inicio y similitud acústica

Estado: aceptada. La similitud acústica está en evaluación.

## Contexto

Inicio mostraba dos selecciones automáticas con objetivos similares. El collage destacaba la forma de los recortes y ocupaba espacio que podía dedicarse a explorar otras partes de la biblioteca. Los metadatos y las reproducciones permiten recomendar pistas por afinidad, por antigüedad o por escasa exposición. La similitud sonora requiere analizar la señal.

## Decisión

- Unificar las dos selecciones en un Mix del día y mostrar hasta tres mixes que respondan a motivos distintos. Usar portadas reales de las canciones, con un halo localizado en el destacado.
- Calcular los mixes con la biblioteca visible, incluidas sus fuentes conectadas. Persistir selecciones ordenadas y exposición reciente en DataStore. Mantener las listas iniciadas por el usuario en la cola del reproductor.
- Validar el análisis determinista de archivos locales con un prototipo separado de la reproducción. La disponibilidad en Inicio requiere una comparación auditiva con el ranking por etiquetas y mediciones operativas. Tras validar, el usuario habilitará el análisis en Ajustes y se procesarán lotes durante la carga con PixelPlayer en reposo.

## Alternativas y consecuencias

La portada editorial fija aporta una identidad reconocible por tipo de mix. Se eligió el arte de la biblioteca porque conecta visualmente la selección con las pistas que contiene y permite derivar el halo de ese mismo arte.

La revisión visual posterior reunió las selecciones en un carrusel. El Mix del día integra carátulas de la biblioteca sobre una portada gráfica. Las selecciones rotativas tienen composiciones propias por tipo. El carrusel y el detalle comparten ese arte. [Inicio y detalle de mixes](../ui-and-state.md#inicio-y-detalle-de-mixes)

El lanzamiento conjunto habría ligado el rediseño a la decodificación, al coste energético y a una evaluación perceptual. Las entregas independientes permiten validar primero las selecciones que se sustentan en metadatos e historial. Los géneros insuficientemente etiquetados o un catálogo pequeño pueden reducir el número de mixes disponibles.
