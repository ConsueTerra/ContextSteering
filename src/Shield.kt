import kotlin.math.PI
import kotlin.math.min

class Shield(
    val ship: Ship,
    val width: Double  = 10.0,
    val height: Double = 10.0,
    val center: Vector2D = Vector2D(0.0,0.0),
    val maxHealth: Double = 100.0,
    val regenRate : Double = 1.0
) {

    var health = maxHealth
    var tickDamage = 0.0
    val corners = listOf(
        Vector2D(width*-0.5, height*-0.5).add(center),
        Vector2D(width*0.5, height*-0.5).add(center),
        Vector2D(width*0.5, height*0.5).add(center),
        Vector2D(width*-0.5, height*0.5).add(center),
    )
    fun tick(mod :Double = 1.0) {
        require(!mod.isNaN()) {"NaN value"}
        val cornersTrans = transformCords()
        tickDamage = 0.0
        for (particle in Simulation.particles) {
            val intersected = polySphereIntersect(cornersTrans, particle.pos, particle.size)
            if (!intersected) continue
            health -= particle.damage
            tickDamage += particle.damage
            particle.distroyed = true
            if (health < 0.0) {
                ship.health += health
                health = 0.0
            }
        }
        health = min(health + regenRate * mod, maxHealth)
    }

    fun transformCords(centertrans: Boolean = false, heading : Vector2D = ship.heading) : List<Vector2D>{
        val output = mutableListOf<Vector2D>()
        if (centertrans) {
            val rotated = center.toPolar().add(0.0, heading.toPolar().y - PI/2.0).toCartesian()
            val cord = rotated.add(ship.pos)
            return listOf(cord)
        }
        for (corner in corners) {
            val rotated = corner.toPolar().add(0.0, heading.toPolar().y - PI/2.0).toCartesian()
            val cord = rotated.add(ship.pos)
            output.add(cord)
        }
        return output
    }

    companion object {
        val xwidth = 10.0; val ywidth = 10.0
        fun arrangeShields(ship: Ship) {
            val shipShape = ship.shape.map{it.mult(ship.size.toDouble())}
            val centerOfMass = shipShape.fold(Vector2D(0.0,0.0)) {
                    acc, vec -> acc.add(vec)}.mult(1/shipShape.size.toDouble())
            val corner1 = Vector2D(shipShape.maxOf{ it.x }, shipShape.maxOf{ it.y })
            val corner2 = Vector2D(shipShape.minOf{ it.x }, shipShape.minOf{ it.y })
            val dimensions = corner1.add(corner2.mult(-1.0))
            val numX = (dimensions.x / xwidth).toInt(); val numY = (dimensions.x / ywidth).toInt()
            val grid = Array(2*numX) {i -> Array(2*numY) {j -> Vector2D(i* xwidth/2.0, j* ywidth /2.0)}}
            grid.forEach { foo -> foo.forEach { it.add(dimensions.mult(-0.5)).add(centerOfMass) } }
            throw NotImplementedError()
        }
    }
}