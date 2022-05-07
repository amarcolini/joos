
![logo](logo.svg)

---

[![](https://jitpack.io/v/amarcolini/joos.svg?style=flat-square)](https://jitpack.io/#amarcolini/joos)

A comprehensive kotlin library designed for FTC. Based on [Road Runner](https://github.com/acmerobotics/road-runner).

**Note**: This project is currently in its alpha stages. The API is subject to change.

### Features
- Support for advanced trajectory planning and following
- Command-based paradigm
- Easy to use GUI for trajectory generation
- [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard) built in

## Installation

For the `command` and `navigation` modules, installation is as follows:

### Gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation "com.github.amarcolini.joos:$module:0.4.4-alpha"
}
```

Note that since the `command` module implicitly imports the `navigation` module,
only one implementation statement is needed.

To use the GUI, you can either download the image specific to your platform from the releases page (the launcher is
in the bin folder), or import it like so:

### Gradle

````groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
    // Change 'win' to 'linux' or 'mac' depending on your operating system
  implementation "com.github.amarcolini.joos:gui:0.4.4-alpha:win"
}
````
