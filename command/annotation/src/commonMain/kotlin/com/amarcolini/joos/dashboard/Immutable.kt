package com.amarcolini.joos.dashboard

/**
 * Specifies to the Joos annotation processor that this class, when used as a config variable, should be
 * a mutable property/field.
 */
@Target(AnnotationTarget.CLASS)
annotation class Immutable