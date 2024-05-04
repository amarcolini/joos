package util

import com.amarcolini.joos.serialization.format
import io.nacular.doodle.controls.text.TextField
import io.nacular.doodle.event.KeyCode
import io.nacular.doodle.event.KeyEvent
import io.nacular.doodle.event.KeyListener
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.utils.roundToNearest
import kotlin.math.pow

class NumberField(
    initialValue: Double = 0.0,
    var decimals: Int = 1,
    var isNegative: Boolean = true,
    var valueSetter: (Double) -> Unit,
    var format: (String) -> String = { it },
    var numberFormat: (Double) -> Double = { it },
) : TextField(format(initialValue.format(decimals))) {
    var value: Double = initialValue
        set(value) {
            text = value.also(valueSetter).format(decimals).let(format)
            field = value
        }

    private val roundTo = 10.0.pow(-decimals)
    private val regex = Regex((if (isNegative) "-?" else "") + "\\d+(?:\\.\\d+)?")
    private fun updateValue() {
        regex.find(text)?.value?.toDoubleOrNull()?.roundToNearest(roundTo)?.let(numberFormat)?.also {
            value = it
        }
    }

    init {
        borderVisible = true
        minimumSize = Size(80.0, 40.0)
        size = minimumSize
        purpose = Purpose.Number
        insets = Insets(2.0)
        this.focusChanged += { _, _, new ->
            if (!new) updateValue()
        }
        this.keyChanged += object : KeyListener {
            override fun pressed(event: KeyEvent) {
                if (event.code == KeyCode.Enter || event.code == KeyCode.NumpadEnter) {
                    updateValue()
                }
            }
        }
    }
}