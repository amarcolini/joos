package util

import com.amarcolini.joos.serialization.format
import io.nacular.doodle.controls.text.TextField
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

    init {
        borderVisible = true
        minimumSize = Size(80.0, 40.0)
        size = minimumSize
        purpose = Purpose.Number
        insets = Insets(2.0)
        val roundTo = 10.0.pow(-decimals)
        val regex = Regex((if (isNegative) "-?" else "") + "\\d+(?:\\.\\d+)?")
        textChanged += { _, _, new ->
            regex.find(new)?.value?.toDoubleOrNull()?.roundToNearest(roundTo)?.let(numberFormat)?.also {
                value = it
            }
        }
    }
}