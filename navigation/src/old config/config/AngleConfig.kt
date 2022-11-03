package com.amarcolini.joos.trajectory.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class AngleSerializer : JsonSerializer<com.amarcolini.joos.geometry.Angle>() {
    override fun serialize(value: com.amarcolini.joos.geometry.Angle?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeNumber(value?.defaultValue ?: return)
    }
}

class AngleDeserializer : JsonDeserializer<com.amarcolini.joos.geometry.Angle>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): com.amarcolini.joos.geometry.Angle {
        return com.amarcolini.joos.geometry.Angle(p?.doubleValue ?: return com.amarcolini.joos.geometry.Angle())
    }
}