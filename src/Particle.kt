class Particle(
    val size :Double = 5.0,
    val damage : Double = 10.0,
    val velocity: Vector2D = Vector2D(0.0,0.0),
    val source: Ship? = null
) {
    var pos = Vector2D(0.0,0.0)
    var distroyed = false

    fun tick() {
        if (distroyed) killParticle()
        pos = pos.add(velocity)

        if (pos.x < 0.0 || pos.x > Simulation.W || pos.y < 0.0 || pos.y > Simulation.H) killParticle()
    }

    private fun killParticle() {
        Simulation.particles.remove(this)
    }
}