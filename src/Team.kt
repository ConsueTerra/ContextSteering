import java.awt.Color

class Team (
    val squads: MutableList<Squad> = ArrayList(),
    val ships: MutableList<Ship> = ArrayList(),
    val color: Color = Color(
        (Math.random()*255).toInt(),
        (Math.random()*255).toInt(),
        (Math.random()*255).toInt())
){
}