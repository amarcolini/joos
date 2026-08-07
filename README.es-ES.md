

![logo](logo.svg)

---

[![](https://jitpack.io/v/amarcolini/joos.svg?style=flat-square)](https://jitpack.io/#amarcolini/joos)

Una biblioteca kotlin integral diseñada para FTC. Basada en [Road Runner](https://github.com/acmerobotics/road-runner).

**Nota**: Este proyecto se encuentra actualmente en sus etapas alfa. La API está sujeta a cambios.

### Características
- Soporte para planificación y seguimiento de trayectorias avanzadas
- Paradigma basado en comandos
- Interfaz gráfica fácil de usar para la generación de trayectorias
- [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard) integrado

<br>

Puedes encontrar la documentación completa :sparkles:[aquí](https://amarcolini.github.io/joos_docs/):sparkles:

## ¿Por qué Joos?

Joos se creó por el deseo de ampliar las posibilidades de la programación en FTC y de seguir mi pasión por la robótica. La robótica es un campo increíble y extremadamente complejo. Joos es mi forma de explorarlo.

Toda la biblioteca está diseñada para ser fluida de usar, al tiempo que te permite escribir código que *simplemente funciona*. A veces, los problemas de programación pueden ser difíciles de resolver y puede resultar frustrante dedicar tanto tiempo a tareas aparentemente triviales. Joos debería eliminar esa necesidad, abriendo más posibilidades de codificación. En lugar de pasar 3 semanas dándote golpes en la cabeza preguntándote: «¿Por qué esto no funciona?», podrías estar pasando esas 3 semanas preguntándote: «¿Cómo funciona esto?». He aprendido mucho programando para FTC y espero que otros puedan lograr lo mismo.

Joos está pensado para manejar lo básico, junto con algunas tareas más complejas también, para que otros puedan centrarse en profundizar sus conocimientos y avanzar hacia objetivos aún más complejos.

Por último, nada es perfecto, así que si sientes que Joos no es tan bueno como esperabas o tienes ideas para contribuir, ¡no dudes en proporcionar retroalimentación! Es la mejor manera de que Joos crezca.

## Instalación

Para los módulos `command` y `navigation`, la instalación es la siguiente:

### Gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation "com.github.amarcolini.joos:$module:0.4.9"
}
```

Ten en cuenta que, dado que el módulo `command` importa implícitamente el módulo `navigation`, solo se necesita una declaración de implementación.

La guía de instalación completa se puede encontrar en [la documentación](https://amarcolini.github.io/joos_docs/).
