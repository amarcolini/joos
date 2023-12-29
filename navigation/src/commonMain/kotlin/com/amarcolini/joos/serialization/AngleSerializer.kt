package com.amarcolini.joos.serialization

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AngleSerializer : KSerializer<Angle> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Angle", PrimitiveKind.STRING)

    const val degreeDescriptor = "°"
    const val radianDescriptor = "rad"

    override fun serialize(encoder: Encoder, value: Angle) {
        val string = value.value.toString() + when (value.units) {
            AngleUnit.Degrees -> degreeDescriptor
            AngleUnit.Radians -> radianDescriptor
        }
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Angle {
        val string = decoder.decodeString()
        val units = if (string.contains(degreeDescriptor)) AngleUnit.Degrees
        else if (string.contains(radianDescriptor)) AngleUnit.Radians
        else AngleUnit.Radians
        return Angle(Regex("\\d+\\.?\\d*").find(string)?.value?.toDoubleOrNull() ?: 0.0, units)
    }
}