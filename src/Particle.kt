class Particle(
    val size :Double = 5.0,
    val damage : Double = 10.0,
    val velocity: Vector2D = Vector2D(0.0,0.0)
) {
    var pos = Vector2D(0.0,0.0)
    var distroyed = false
}