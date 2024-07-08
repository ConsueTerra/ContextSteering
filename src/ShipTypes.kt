
object ShipTypes {
    class Starfighter : Ship(Vector2D(0.0,0.0),mass = 10.0, size = 10){
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
        override fun arrangeShields() {
            shields.add(Shield(this, width = 20.0, height = 40.0))
        }
    }

    class Corv : Ship(Vector2D(0.0,0.0),mass = 20.0, size = 20) {
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
        override fun arrangeShields() {
            shields.add(Shield(this, width = 35.0, height = 30.0, center = Vector2D(0.0,25.0)))
            shields.add(Shield(this, width = 14.0, height = 50.0, center = Vector2D(7.0,-15.0)))
            shields.add(Shield(this, width = 14.0, height = 50.0, center = Vector2D(-7.0,-15.0)))
        }
    }
    class Capital : Ship(Vector2D(0.0,0.0),mass = 64.0, size = 64) {
        override val MAXSPEED = THRUSTPOWER / (mass* (1-Simulation.DRAGFORCE))
        override fun arrangeShields() {
            shields.add(Shield(this, width = 50.0, height = 30.0, center = Vector2D(0.0,115.0)))
            shields.add(Shield(this, width = 40.0, height = 40.0, center = Vector2D(-45.0,110.0)))
            shields.add(Shield(this, width = 40.0, height = 40.0, center = Vector2D(45.0,110.0)))

            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(-42.0,66.0)))
            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(42.0,66.0)))
            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(-30.0,18.0)))
            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(30.0,18.0)))
            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(-20.0,-30.0)))
            shields.add(Shield(this, width = 24.0, height = 48.0, center = Vector2D(20.0,-30.0)))
            shields.add(Shield(this, width = 20.0, height = 40.0, center = Vector2D(-10.0,-74.0)))
            shields.add(Shield(this, width = 20.0, height = 40.0, center = Vector2D(10.0,-74.0)))

            shields.add(Shield(this, width = 18.0, height = 36.0, center = Vector2D(0.0,-112.0)))
        }
    }

    val shipTypes = arrayOf(::Starfighter, ::Corv,::Starfighter, ::Corv, ::Capital)

    fun drawRandomShip() : () -> Ship {
        return shipTypes.random()
    }
}
