import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Standard AI for controling a ship, uses context steering for determing movement, the goal of
 * the Agent class is to hold the current state of the agent, it context maps and output its
 * descion based on its surroundings. <br></br>
 * Notably the Agent class does nto direlty determine the movement on screen, it only sends
 * thurst and heading information to the physis engine. This makes it usefull in any context
 * that has a proper physics engine. <br></br>
 * The Agent class stores the following information:
 *
 *  * The current position and velocity
 *  * The current thrust and heading of the agent as determined by steering
 *  * A random [0,1] offset to be used for different behaviours
 *  * The master steering function that determines thrust and heading
 *  * #TODO A default list of tunable parameters that influence steering, affecting which
 * behaviors are on and their weight
 *  * A default mass and default max speed
 *
 */
class Agent {

    companion object {
        const val MAXSPEED = 10.0
        const val DEFAULTMASS = 10.0
        const val THRUSTPOWER = 10.0
    }

    lateinit var pos: Vector2D
    lateinit var velocity: Vector2D

    /**
     * The heading of the ship, in cartesian coordinates
     */
    var heading: Vector2D
    var mass: Double
    var thrust: Vector2D
    var offset = Math.random().toFloat()

    constructor(
        pos: Vector2D,
        velocity: Vector2D,
        heading: Vector2D,
        mass: Double,
        thrust: Vector2D
    ) {
        this.pos = pos
        this.velocity = velocity
        this.heading = heading
        this.mass = mass
        this.thrust = thrust
    }

    /**
     * Creates a default agent randomly on the simulation space with a random velocity
     * @param w the width of the simulation space
     * @param h the height of the simulation space
     */
    constructor(w: Int, h: Int) {
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
    }

    /**
     * Master steering function
     *
     *
     * Takes the current simulation sate and updates an agents heading and thrust using context
     * maps
     */
    fun steer() {
        movementInterest.clearContext()
        rotationInterest.clearContext()
        danger.clearContext()

        wander.clearContext()
        wander.populateContext()

        offsetSeekMouse.clearContext()
        offsetSeekMouse.populateContext()

        faceMouse.clearContext()
        faceMouse.populateContext()

        borderDanger.clearContext()
        borderDanger.populateContext()

        agentDanger.clearContext()
        agentDanger.populateContext()

        movementInterest.addContext(wander)
        movementInterest.addContext(offsetSeekMouse)

        rotationInterest.addContext(movementInterest)
        rotationInterest.multScalar(0.1)
        rotationInterest.addContext(faceMouse)

        danger.addContext(borderDanger)
        danger.addContext(agentDanger)

        //There are no resources that talk about steering where the heading of a ship is
        // different from where its accelerating, enabling strafing and drifting, movement and
        // rotation is a bit of a chicken and egg problem, as the ideal heading is in the
        // direction of movement and the ideal movement is where the ship is heading, regardless
        // this is my wip solution to this problem, movement and rotation interests are generated,
        // and then mixed together with high velocity favoring rotation priority (strafing) and
        // low velocity favoring movement priority (accelerating to top speed). This mixing was
        // designed with starfigters and smaller ships in mind, and as more parameters are
        // introduced this could be changed for larger ships
        // A current issue is that if the movement and rotation maps are equal and opposing
        // magnitude then it will lead to an agent jittering under a certain velocity threshold.
        //mixing
        val rotationMovementPrior =
            max(min(velocity.mag() / MAXSPEED, 1.0), 0.0)
        //rotationMovementPrior = 0.0f;
        val temp: ContextMap = object : ContextMap(movementInterest) {}
        val movementWeight = 1 - rotationMovementPrior
        movementInterest.multScalar(movementWeight)
        movementInterest.addContext(
            ContextMap.Companion.scaled(
                rotationInterest,
                1 - movementWeight
            )
        )
        val rotationWeight = rotationMovementPrior
        if (rotationWeight < 0) throw RuntimeException()
        rotationInterest.multScalar(rotationWeight)
        rotationInterest.addContext(ContextMap.Companion.scaled(temp, 1 - rotationWeight))

        //masking, if the danger for a certain direction is greater than the threshold then it is
        // masked out
        movementInterest.maskContext(danger, 0.5)
        rotationInterest.maskContext(danger, 0.5)


        //decision time
        heading = heading.mult(2.0).add(rotationInterest.interpolatedMaxDir()).normal()
        thrust = movementInterest.interpolatedMaxDir()
        //take the dot product here to prevent the ship thrusting in a direction its not facing
        thrust = thrust.mult(thrust.dot(heading)).mult(THRUSTPOWER)
    }

    /**
     * Interest maps correspond to how much a ship wants to move in a particular direction
     * (orbit, wander), and give rise to proactive/planning emergent behavior.
     *
     * This map is the final context to which to determine an agents thrust/movement
     */
    var movementInterest: ContextMap = object : ContextMap() {}

    /**
     * Interest maps correspond to how much a ship wants to move in a particular direction
     * (orbit, wander), and give rise to proactive/planning emergent behavior.
     *
     * This map is the final context to which to determine an agents heading
     */
    var rotationInterest: ContextMap = object : ContextMap() {}

    /** Danger maps on the other hand indicate how bad moving in a particular direction is, with
     * a high enough danger leading to an agent avoiding that direction regardless of interest
     * (obstacle avoidance). This gives rise to reactive emergent behavior.
     *
     * This context is the final danger map that masks the interest contexts
     */
    var danger: ContextMap = object : ContextMap() {}

    /**
     * Uses simplex noise to generate a random direction to follow, the direction oscillates based
     * on time. Uses an agents offset to give a different wanting per agent. Combining this
     * behavior with others gives a random organic look to agents.
     *
     * This Context is meant for **`movementInterest`**
     *
     *  * **`weight`** controls how strong this behavior is
     *  * **`dotShift`** shifts the dot product, allowing for backward directions
     * to be considered
     *  * **`jitterRate`** controls how frequently the wandering angle oscillates
     *
     */
    var wander: ContextMap = object : ContextMap() {
        val weight = 0.5
        val dotShift = 1.0
        val jitterRate = 2e4
        override fun populateContext() {
            val timeoffset = offset * jitterRate
            val theta = SimplexNoise.noise(
                System.currentTimeMillis() % 100000 / jitterRate + timeoffset,
                System.currentTimeMillis() % 100000 / jitterRate + timeoffset
            ) * 2 * Math.PI
            val desiredDir = Vector2D(1.0, theta).toCartesian()
            dotContext(desiredDir, dotShift, weight)
        }
    }

    /**
     * Makes the agent follow a orbit around the mouse. this does it by generating a target that
     * is offset by the orbit distance from the mouse and following that point. In order for the
     * agent to orbit properly once the distance is reached, the target point is calculated using
     * a frontal tether of the agent instead of itself. When the agent is at the orbit distance
     * it will follow this tether, and when its far it will move towards orbit.
     *
     * This Context is meant for **`movementInterest`**
     *
     *  * **`weight`** controls how strong this behavior is
     *  * **`dotShift`** shifts the dot product, allowing for backward directions
     * to be considered
     *  * **`offsetDist`** controls the orbiting distance
     *
     */
    var offsetSeekMouse: ContextMap = object : ContextMap() {
        val offsetDist = 200.0
        val weight = 2.0
        val dotShift = 0.0
        override fun populateContext() {
            val center: Vector2D = Simulation.mouseCords
            val offset = center.add(pos.mult(-1.0))
            var dist = offset.mag() + 1e-4
            val tetherl = dist / (velocity.mag() + 1e-5) * 2
            val frowardTether = pos.add(velocity.normal().mult(tetherl))
            val tetherOffset = frowardTether.add(center.mult(-1.0)).normal()
            val target = center.add(tetherOffset.mult(offsetDist))
            var targetOffset = target.add(pos.mult(-1.0))
            dist = targetOffset.mag()
            targetOffset = targetOffset.normal()
            dotContext(targetOffset, dotShift, weight)
        }
    }

    /**
     * Makes the agent face the mouse pointer
     *
     * This Context is meant for **`rotationInterest`**
     *
     *  * **`weight`** controls how strong this behavior is
     *  * **`maxWeight`** caps the maximum weight for a particular direction
     *  * **`falloff`** controls how this behavior scales with distance, higher
     * values will cause a lower weight (ie agent will only look at mouse from further away)
     *
     */
    var faceMouse: ContextMap = object : ContextMap() {
        val weight = 1.0
        val maxWeight = 3.0
        val falloff = 100.0
        override fun populateContext() {
            val target: Vector2D = Simulation.mouseCords
            var offset = target.add(pos.mult(-1.0))
            val dist = offset.mag() + 1e-4
            offset = offset.normal()
            for (i in 0 until numbins) {
                var mag: Double = bindir[i].dot(offset) * dist / falloff
                mag = if (mag < 0) 0.0 else mag
                mag = min(mag, maxWeight)
                bins[i] += mag
            }
        }
    }

    /**
     * Makes the agent avoid other agents, the smaller the distance between agents the higher
     * the danger
     *
     * This Context is meant for **`danger`**
     *
     *  * **`falloff`** controls how this behavior scales with distance, higher
     * values will cause higher danger and agents will react faster
     *
     */
    var agentDanger: ContextMap = object : ContextMap() {
        val falloff = 50.0
        override fun populateContext() {
            for (otherAgent in Simulation.agents) {
                if (otherAgent === this@Agent) continue
                var offset = otherAgent.pos.add(pos.mult(-1.0))
                val dist = offset.mag() + 1e-4
                offset = offset.normal()
                for (i in 0 until numbins) {
                    var mag: Double = bindir[i].dot(offset) * falloff / dist
                    mag = if (mag < 0) 0.0 else mag
                    bins[i] += mag
                }
            }
        }
    }

    /**
     * Makes the agent avoid the borders, the smaller the distance between the agent and
     * boundaries the higher the danger
     *
     * This Context is meant for **`danger`**
     *
     *  * **`falloff`** controls how this behavior scales with distance, higher
     * values will cause higher danger and agents will react faster
     *  * **`dotShift`** shifts the dot product, giving danger to orthogonal
     * directions
     *
     */
    var borderDanger: ContextMap = object : ContextMap() {
        var falloff = 50
        var dotShift = 0.2
        override fun populateContext() {
            for (i in 0 until numbins) {
                //north border
                var dir = Vector2D(0.0, -1.0)
                bins[i] += calcDanger(bindir[i], dir, pos.y, 0.0)
                //south border
                dir = Vector2D(0.0, 1.0)
                bins[i] += calcDanger(bindir[i],dir,pos.y, Simulation.h.toDouble())
                //west border
                dir = Vector2D(-1.0, 0.0)
                bins[i] += calcDanger(bindir[i], dir, pos.x, 0.0)
                //east border
                dir = Vector2D(1.0, 0.0)
                bins[i] += calcDanger(bindir[i],dir,pos.x, Simulation.w.toDouble())
            }
        }

        private fun calcDanger(bindir: Vector2D, dir: Vector2D, val1: Double, val2: Double): Double {
            val proximity = (abs(val1 - val2) + 1e-4)
            var mag = (bindir.dot(dir) + dotShift) * falloff / proximity
            mag = if (mag < 0) 0.0 else mag
            return mag
        }
    }
}