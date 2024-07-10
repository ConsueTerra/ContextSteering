import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D


fun polySphereIntersect(cords : List<Vector2D>, center : Vector2D, radius : Double) : Boolean{
    val poly: Path2D = Path2D.Double()
    val start = cords[0]
    poly.moveTo(start.x,start.y)
    for (vec in cords.subList(1,cords.size)) {
        poly.lineTo(vec.x,vec.y)
    }
    poly.closePath()

    val sphere = Ellipse2D.Double(center.x,center.y,radius,radius)
    if (poly.bounds.intersects(sphere.bounds)) {
        val a = Area(poly)
        a.intersect(Area(sphere))
        if (!a.isEmpty) {
            return true
        }
    }
    return false
}