package com.amarcolini.joos.trajectory.config

import com.amarcolini.joos.geometry.Angle
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class AngleSerializer : JsonSerializer<Angle>() {
    override fun serialize(value: Angle?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeNumber(value?.defaultValue ?: return)
    }
}

class AngleDeserializer : JsonDeserializer<Angle>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Angle {
        return Angle(p?.doubleValue ?: return Angle())
    }
}