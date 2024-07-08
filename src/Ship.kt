import kotlin.math.max

private const val DEFAULTMASS = 10.0
abstract class Ship (
    var pos: Vector2D,
    var velocity: Vector2D = Vector2D(0.0, 0.0),
    /**
     * The heading of the ship, in cartesian coordinates
     */
    var heading: Vector2D = Vector2D(1.0, 0.0),
    var thrust: Vector2D=  Vector2D(0.0, 0.0),
    val mass : Double = DEFAULTMASS,
    val size : Int= 10,

) {
    open val MAXSPEED = 10.0
    val THRUSTPOWER = 10.0
    val shape = listOf(Vector2D(0.0, -2.0),Vector2D(-1.0, 2.0),Vector2D(1.0,2.0))

    val offset : Double = Math.random()
    lateinit var agent : Agent
    var squad : Squad? = null
    var team: Team? = null
    var health = 100.0;
    val shields: MutableList<Shield> = ArrayList()
    val weapons: MutableList<Weapon> = ArrayList()

    init {
        arrangeShields()
    }

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
        thrust = Vector2D(0.0, 0.0)
        agent = AIAgent(this)
    }

    abstract fun arrangeShields()

    fun tick() {
        handleShields()
        handleMovement()
        fireWeapons()
    }

    fun fireWeapons() {
        //TODO
    }

    fun handleShields() {
        val sum = shields.sumOf {(it.maxHealth - it.health)/it.health}
        val weights = shields.map {(it.maxHealth - it.health)/it.health / sum}
        for (i in 0 until shields.size) {
            shields[i].tick(mod = weights[i])
        }
        if (health < 0.0) killship()
    }

    fun killship() {
        Simulation.ships.remove(this)
        team?.ships?.remove(this)
        squad?.ships?.remove(this)
    }

    private fun handleMovement() {
        val output  = agent.steer()
        heading = output.newHeading
        thrust = output.newThrust
        //take the dot product here to prevent the ship thrusting in a direction its not facing
        val dotProd = max(thrust.dot(heading), 0.0)
        thrust = thrust.mult(dotProd).mult(THRUSTPOWER)
    }
}
/**
 * Creates a default ship randomly on the simulation space with a random velocity
 * @param w the width of the simulation space
 * @param h the height of the simulation space
 */
fun createShip(w: Int, h: Int,
               shipFactory : () -> Ship = ShipTypes.drawRandomShip(),
               agentFactory: (ship: Ship) -> Agent = {ship -> AIAgent(ship)}) : Ship {
    val ship = shipFactory()
    ship.pos = Vector2D((Math.random() * w), (Math.random() * h))
    ship.velocity = Vector2D(
        (Math.random() * ship.MAXSPEED)- ship.MAXSPEED / 2,
        (Math.random() * ship.MAXSPEED) - ship.MAXSPEED / 2
    )
    ship.velocity = ship.velocity.clip(ship.MAXSPEED)
    ship.heading =
        Vector2D((Math.random() - 0.5), (Math.random() - 0.5)).normal()
    ship.thrust = Vector2D(0.0, 0.0)
    ship.agent = agentFactory(ship)
    return  ship
}