import kotlin.math.max

class Ship (
    var pos: Vector2D,
    var velocity: Vector2D = Vector2D(0.0, 0.0),
    /**
     * The heading of the ship, in cartesian coordinates
     */
    var heading: Vector2D = Vector2D(1.0, 0.0),
    var thrust: Vector2D=  Vector2D(0.0, 0.0),
    var mass: Double = DEFAULTMASS,
    var size : Int= 10,
) {
    companion object {
        const val MAXSPEED = 10.0
        const val DEFAULTMASS = 10.0
        const val THRUSTPOWER = 10.0
    }
    var offset : Double = Math.random()
    lateinit var agent : Agent
    var squad : Squad? = null
    var team: Team? = null
    var health = 100;
    val shields: MutableList<Shield> = ArrayList()
    val weapons: MutableList<Weapon> = ArrayList()


    /**
     * Creates a default agent randomly on the simulation space with a random velocity
     * @param w the width of the simulation space
     * @param h the height of the simulation space
     */
    constructor(w: Int, h: Int) :
            this(Vector2D(0.0, 0.0)) {
        pos = Vector2D((Math.random() * w), (Math.random() * h))
        velocity = Vector2D(
            (Math.random() * MAXSPEED)- MAXSPEED / 2,
            (Math.random() * MAXSPEED) - MAXSPEED / 2
        )
        velocity = velocity.clip(MAXSPEED)
        heading =
            Vector2D((Math.random() - 0.5), (Math.random() - 0.5)).normal()
        mass = DEFAULTMASS
        thrust = Vector2D(0.0, 0.0)
        agent = AIAgent(this)
    }

    fun handleMovement() {
        val output  = agent.steer()
        heading = output.newHeading
        thrust = output.newThrust
        //take the dot product here to prevent the ship thrusting in a direction its not facing
        val dotProd = max(thrust.dot(heading), 0.0)
        thrust = thrust.mult(dotProd).mult(THRUSTPOWER)
    }

}
