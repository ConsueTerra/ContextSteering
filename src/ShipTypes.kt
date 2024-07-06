
object ShipTypes {
    class Starfighter : Ship(Vector2D(0.0,0.0),mass = 10.0, size = 10){
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
    }

    class Corv : Ship(Vector2D(0.0,0.0),mass = 20.0, size = 20) {
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
    }
    class Capital : Ship(Vector2D(0.0,0.0),mass = 64.0, size = 64) {
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
    }

    val shipTypes = arrayOf(::Starfighter, ::Corv,::Starfighter, ::Corv, ::Capital)

    fun drawRandomShip() : () -> Ship {
        return shipTypes.random()
    }
}
