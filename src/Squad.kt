import kotlin.math.PI

class Squad (
    val ships : MutableList<Ship> = ArrayList(),
    val team: Team,
    var heading : Vector2D = Vector2D(1.0,0.0)
){
    val distance = 5.0
    fun getFromationPos(ship: Ship) : Vector2D {
        require(ships.contains(ship)) {"Ship is not in squad"}
        val i = ships.indexOf(ship)
        if (i==0) return ships[0].pos
        val offset = ships[0].pos.add(ship.pos.mult(-1.0))
        val dist = offset.mag() + 1e-4
        val t = dist/(ship.velocity.mag())
        val lookAhead =  ships[0].velocity.mult(t)

        val cumulativeDistance = ships.subList(0,i).fold(0.0) { acc, thing -> acc + thing.size * distance }
        val angle = (if (i % 2 == 0) PI/4.0 else -PI/4.0) - PI / 2
        val targetOffset = heading.mult(-1.0).toPolar().add(0.0,angle).toCartesian().mult(cumulativeDistance)
        return ships[0].pos.add(targetOffset).add(lookAhead)
    }

    fun updateHeading() {
        val hist = 0.8
        var weightedHeading = Vector2D(0.0,0.0)
        for (i in 0 until ships.size) {
            weightedHeading = weightedHeading.add(ships[i].heading.mult(1/(i*5+1).toDouble()))
        }
        weightedHeading = weightedHeading.normal()
        heading = heading.mult(hist).add(weightedHeading.mult(1-hist))
    }
}