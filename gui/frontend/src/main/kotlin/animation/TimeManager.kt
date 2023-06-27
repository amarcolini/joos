package animation

import GUIApp.Companion.animate
import io.nacular.doodle.animation.Animation
import io.nacular.doodle.animation.invoke
import io.nacular.doodle.animation.transition.linear
import io.nacular.doodle.animation.tweenDouble
import io.nacular.doodle.utils.Pool
import io.nacular.doodle.utils.SetPool
import io.nacular.doodle.utils.autoCanceling
import io.nacular.doodle.utils.observable
import io.nacular.measured.units.Time.Companion.seconds
import io.nacular.measured.units.times
import kotlinx.browser.window

object TimeManager {
    private val listeners_ = SetPool<(Double, Double) -> Unit>()
    val listeners: Pool<(old: Double, new: Double) -> Unit> = listeners_

    var time: Double by observable(0.0) { old, new ->
        listeners_.forEach {
            it(old, new)
        }
    }
        private set

    var duration: Double by observable(5.0) { _, _ ->
        animation = null
    }

    private val animationChanged_ = SetPool<(Animation<Double>?, Animation<Double>?) -> Unit>()
    val animationChanged: Pool<(old: Animation<Double>?, new: Animation<Double>?) -> Unit> = animationChanged_
    var animation: Animation<Double>? by autoCanceling { old, new ->
        animationChanged_.forEach {
            it(old, new)
        }
    }
        private set

    fun play() {
        if (time >= duration) time = 0.0
        animation = animate(time to duration, using = tweenDouble(linear, (duration - time) * seconds)) {
            time = it
        }
    }

    fun setTime(time: Double, play: Boolean? = null) {
        TimeManager.time = time.coerceIn(0.0, duration)
        if (play ?: (animation != null)) play()
        else stop()
    }

    fun stop() {
        animation = null
    }

    init {
        window.asDynamic()["play"] = TimeManager::play
    }
}