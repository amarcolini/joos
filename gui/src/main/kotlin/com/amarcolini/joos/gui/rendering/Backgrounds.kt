package com.amarcolini.joos.gui.rendering

import javafx.scene.image.Image

enum class Backgrounds(val image: Lazy<Image>) {
    Generic(lazy { Image("background/Generic.png") }),
    FreightFrenzy(lazy { Image("background/FreightFrenzy.png") }),
    UltimateGoal(lazy { Image("background/UltimateGoal.png") })
}