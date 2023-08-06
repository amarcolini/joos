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
    var setValue: (Double) -> Unit,
    var format: (String) -> String = { it },
    var numberFormat: (Double) -> Double = { it },
) : TextField(format(initialValue.format(decimals))) {
    init {
        borderVisible = true
        minimumSize = Size(80.0, 40.0)
        size = minimumSize
        purpose = Purpose.Number
        insets = Insets(2.0)
        val roundTo = 10.0.pow(-decimals)
        val regex = Regex((if (isNegative) "-?" else "") + "\\d+(?:\\.\\d+)?")
        textChanged += { _, _, new ->
            val number =
                regex.find(new)?.value?.toDoubleOrNull()?.roundToNearest(roundTo)?.let(numberFormat)?.also(setValue)
            text = number?.format(decimals)?.let(format) ?: ""
        }
    }
}