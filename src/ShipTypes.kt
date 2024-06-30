
object ShipTypes {
    class Starfighter : Ship(Vector2D(0.0,0.0),mass = 8.0, size = 8){}


    class Corv : Ship(Vector2D(0.0,0.0),mass = 20.0, size = 20) {}
    class Capital : Ship(Vector2D(0.0,0.0),mass = 64.0, size = 64) {}

    val shipTypes = arrayOf(::Starfighter, ::Corv, ::Capital)

    fun drawRandomShip() : () -> Ship {
        return shipTypes.random()
    }
}
