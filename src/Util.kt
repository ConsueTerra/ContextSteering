

fun polySphereIntersect(cords : List<Vector2D>, center : Vector2D, radius : Double) : Boolean{
    for (cord in cords) {
        val dist = center.add(cord.mult(-1.0)).mag()
        if (dist < radius) return true
    }
    return false
}