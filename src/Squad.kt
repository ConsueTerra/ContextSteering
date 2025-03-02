import kotlin.math.PI

class Squad (
    val ships : MutableList<Ship> = ArrayList(),
    val team: Team,
    var heading : Vector2D = Vector2D(1.0,0.0)
){
    val distance = 3.0
    val mindistance = 20.0
    fun getFromationPos(ship: Ship) : Vector2D {
        require(ships.contains(ship)) {"Ship is not in squad"}
        val i = ships.indexOf(ship)
        if (i==0) return ships[0].pos

        val cumulativeDistance = ships.subList(0,i).fold(0.0) { acc, thing -> acc + thing.size * distance + mindistance }
        val angle = (if (i % 2 == 0) PI/4.0 else -PI/4.0)// - PI / 2
        val targetOffset = heading.mult(-1.0).toPolar().add(0.0,angle).toCartesian().mult(cumulativeDistance)
        return ships[0].pos.add(targetOffset)
    }

    fun updateHeading() {
        val hist = 0.95
        val weightedHeading = ships[0].heading
        heading = heading.mult(hist).add(weightedHeading.mult(1-hist)).normal()
    }
}