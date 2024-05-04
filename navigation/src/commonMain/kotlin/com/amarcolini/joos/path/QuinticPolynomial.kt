package com.amarcolini.joos.path


/**
 * Quintic Bezier Polynomial
 */
class QuinticPolynomial(
    start: Double,
    startDeriv: Double,
    startSecondDeriv: Double,
    end: Double,
    endDeriv: Double,
    endSecondDeriv: Double
) {
    private val p0: Double = start
    private val p1: Double = 0.2 * startDeriv + p0
    private val p2: Double = 0.05 * startSecondDeriv + 2 * p1 - p0
    private val p3: Double = 0.05 * endSecondDeriv + 2 * (end - 0.2 * endDeriv) - end
    private val p4: Double = end - 0.2 * endDeriv
    private val p5: Double = end

    val controlPoints: DoubleArray = doubleArrayOf(p0, p1, p2, p3, p4, p5)

    val coeffs = doubleArrayOf(
        (p5 - 5 * p4 + 10 * p3 - 10 * p2 + 5 * p1 - p0),
        5 * (p4 - 4 * p3 + 6 * p2 - 4 * p1 + p0),
        10 * (p3 - 3 * p2 + 3 * p1 - p0),
        10 * (p2 - 2 * p1 + p0),
        (5 * p1 - 5 * p0),
        p0
    )

    /**
     * Returns the value of the polynomial at `t`.
     */
    operator fun get(t: Double): Double = coeffs[0] * t * t * t * t * t +
            coeffs[1] * t * t * t * t +
            coeffs[2] * t * t * t +
            coeffs[3] * t * t +
            coeffs[4] * t + coeffs[5]

    val dcoeffs = doubleArrayOf(
        5 * (p5 - 5 * p4 + 10 * p3 - 10 * p2 + 5 * p1 - p0),
        5 * (4 * p4 - 16 * p3 + 24 * p2 - 16 * p1 + 4 * p0),
        5 * (6 * p3 - 18 * p2 + 18 * p1 - 6 * p0),
        5 * (4 * p2 - 8 * p1 + 4 * p0),
        5 * (p1 - p0)
    )

    /**
     * Returns the derivative of the polynomial at `t`.
     */
    fun deriv(t: Double): Double = dcoeffs[0] * t * t * t * t +
            dcoeffs[1] * t * t * t +
            dcoeffs[2] * t * t +
            dcoeffs[3] * t + dcoeffs[4]

    val d2coeffs = doubleArrayOf(
        20 * ((p5 - 5 * p4 + 10 * p3 - 10 * p2 + 5 * p1 - p0)),
        20 * (3 * p4 - 12 * p3 + 18 * p2 - 12 * p1 + 3 * p0),
        20 * (3 * p3 - 9 * p2 + 9 * p1 - 3 * p0),
        20 * (p2 - 2 * p1 + p0)
    )

    /**
     * Returns the second derivative of the polynomial at `t`.
     */
    fun secondDeriv(t: Double): Double = d2coeffs[0] * t * t * t +
            d2coeffs[1] * t * t +
            d2coeffs[2] * t +
            d2coeffs[3]

    /**
     * Returns the third derivative of the polynomial at `t`.
     */
    fun thirdDeriv(t: Double): Double = 60 * ((p5 - 5 * p4 + 10 * p3 - 10 * p2 + 5 * p1 - p0) * t * t +
            (2 * p4 - 8 * p3 + 12 * p2 - 8 * p1 + 2 * p0) * t +
            p3 - 3 * p2 + 3 * p1 - p0)


    override fun toString(): String =
        "(1-t)^5*$p0 + 5(1-t)^4*t*$p1 + 10(1-t)^3*t^2*$p2 + 10(1-t)^2*t^3*$p3 + 5(1-t)*t^4*$p4 + t^5*$p5"
}