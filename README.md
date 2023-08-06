
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

<br>

You can find the full documentation :sparkles:[here](https://amarcolini.github.io/joos_docs/):sparkles:

## Why Joos?

Joos was made out of a desire to further the possibilities of FTC programming, and pursue my passion for robotics. Robotics is
an amazing field, and it's extremely complex. Joos is my way of exploring that.

The entire library is designed to be fluid to use, while also enabling you to write code that *just works*. Sometimes, 
programming problems can be difficult to solve, and it can be frustrating to spend so much time on seemingly trivial tasks. Joos
should remove that necessity, opening up more coding possibilities. Instead of spending 3 weeks banging your head asking
yourself, “Why doesn’t this work?”, you could be spending those 3 weeks asking yourself, “How does this work?”. I have learned so
much through programming for FTC, and I hope others can accomplish the same.

Joos is supposed to handle the basics, along with some more complex tasks too, so that others can focus on furthering their
knowledge and pushing towards even more complex goals.

Lastly, nothing is perfect, so if you feel that Joos is not as good as you'd hoped, or you have ideas to contribute, feel free
to provide feedback! It's the best way for Joos to grow.

## Installation

For the `command` and `navigation` modules, installation is as follows:

### Gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation "com.github.amarcolini.joos:$module:0.4.8"
}
```

Note that since the `command` module implicitly imports the `navigation` module,
only one implementation statement is needed.

To use the GUI, you can either download the image specific to your platform from the releases page (the launcher is located 
in the bin folder), or import it into an empty java module like so:

### Gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

//Don't forget to set java version to 11
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    //Gradle will automatically retrieve the correct dependencies based on your operating system
    implementation "com.github.amarcolini.joos:gui:0.4.8"
}
```