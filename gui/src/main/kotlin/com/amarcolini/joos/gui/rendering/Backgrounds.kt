package com.amarcolini.joos.gui.rendering

import javafx.scene.image.Image
import com.amarcolini.joos.gui.Global

enum class Backgrounds(url: String) {
    Generic("joos/gui/background/Generic.png"),
    FreightFrenzy("joos/gui/background/FreightFrenzy.png"),
    UltimateGoal("joos/gui/background/UltimateGoal.png");

    val image = lazy {
        Global.resources?.get(url)?.get(0)?.open()?.let { Image(it) } ?: Image(url)
    }
}