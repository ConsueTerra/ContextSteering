/*
Context steering based control for AI
See https://andrewfray.wordpress.com/2013/03/26/context-behaviours-know-how-to-share/
https://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter18_Context_Steering_Behavior-Driven_Steering_at_the_Macro_Scale.pdf
and https://alliekeats.com/portfolio/contextbhvr.html for more in depth descriptions.
Essentially agents determine where to go by using a set of arrays called context maps, these maps
can either hold "interest": ie how much the ai wants to move in a particular direction, or
"danger": how much an ai should avoid a particular direction. Once the ai has the context of what
 to do, it can make a final decision on its heading.
Each of these maps can be combined like image filters, leading to simple and stateless
AI that is still amendmedable to emergent behaviors and avoids deadlock.

The strategy for implementing context steering is as follows:
1. identity the necessary behaviors (seek, avoid, face target, wander ect) and populate their context maps
2. combine behaviors together using simple arithmetic where possible like filters
3. Make a decision based on the final interest and danger context maps, importantly decision-making
should be the LAST step.
 */
import java.lang.Math.pow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
class AIAgent(ship: Ship) : Agent(ship) {

    var squadTarget : Vector2D? = null

    /**
     * Master steering function
     *
     *
     * Takes the current simulation sate and updates an agents heading and thrust using context
     * maps
     */
    override fun steer() : SteeringOutput{
        movementInterest.clearContext()
        rotationInterest.clearContext()
        danger.clearContext()

        wander.clearContext()
        wander.populateContext()

        offsetSeekMouse.clearContext()
        offsetSeekMouse.populateContext()

        faceMouse.clearContext()
        faceMouse.populateContext()

        //momentum.clearContext()
        squadFormation.populateContext()
        commitment.populateContext()

        borderDanger.clearContext()
        borderDanger.populateContext()

        shipDanger.clearContext()
        shipDanger.populateContext()

        movementInterest.addContext(wander)
        movementInterest.addContext(offsetSeekMouse)
        movementInterest.addContext(squadFormation)
        movementInterest.addContext(commitment)
        movementInterest.clipZero() //safeguard against neg weights

        rotationInterest.addContext(movementInterest)
        rotationInterest.multScalar(0.2)
        rotationInterest.addContext(faceMouse)
        //rotationInterest.addContext(ContextMap.scaled(momentum,0.8))

        danger.addContext(borderDanger)
        danger.addContext(shipDanger)

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
        // magnitude then it will lead to an agent jittering under a certain ship.velocity threshold.
        //mixing
        var rotationMovementPrior =
            max(min(ship.velocity.mag() / ship.MAXSPEED*2, 1.0), 0.0).pow(1.0)
        //println(rotationMovementPrior)
        //\rotationMovementPrior = 0.0;
        val temp: ContextMap = object : ContextMap(movementInterest) {}
        val movementWeight = (1 - rotationMovementPrior).pow(0.5)
        movementInterest.multScalar(movementWeight)
        movementInterest.addContext(
            ContextMap.scaled(
                rotationInterest,
                1 - movementWeight
            )
        )
        val rotationWeight = rotationMovementPrior.pow(0.5)
        if (rotationWeight < 0) throw RuntimeException()
        rotationInterest.multScalar(rotationWeight)
        rotationInterest.addContext(ContextMap.scaled(temp, 1 - rotationWeight))

        //masking, if the danger for a certain direction is greater than the threshold then it is
        // masked out
        movementInterest.softMaskContext(danger, 1.0)
        rotationInterest.softMaskContext(danger, 1.0)


        //decision time
        val heading = ship.heading.mult(2.0).add(rotationInterest.interpolatedMaxDir()).normal()
        var thrust = movementInterest.interpolatedMaxDir()
        if (thrust.mag() > 1.0) thrust = thrust.normal()
        return SteeringOutput(heading, thrust)
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
            val timeoffset = ship.offset * jitterRate
            val theta = SimplexNoise.noise(
                System.currentTimeMillis() % 100000 / jitterRate + timeoffset,
                System.currentTimeMillis() % 100000 / jitterRate + timeoffset
            ) * 2 * Math.PI
            val desiredDir = Vector2D(1.0, theta).toCartesian()
            dotContext(desiredDir, dotShift, weight)
        }
    }

    /**
     * Whenever an agent changes direction this context map will make the agent commit by
     * discounting previous directions, greatly reduces jittering
     */
    var commitment: ContextMap = object : ContextMap() {
        val weight = 1.0
        val hist = 0.8
        var headingHist: ContextMap = object : ContextMap() {}

        override fun populateContext() {
            clearContext()
            headingHist.multScalar(hist)
            val velNorm = ship.heading
            headingHist.dotContext(velNorm,1.0,(1-hist))
            dotContext(velNorm,-1.0,weight,false)
            for (i in 0 until numbins) {
                bins[i] *= headingHist.bins[i]
            }
        }
    }

    /**
     * Makes tha agent favor directions with same heading, magnitude increases with lower speed
     */
    var momentum: ContextMap = object :  ContextMap() {
        val weight = 2.0
        val falloff = 1.0
        val dotshift = -0.2
        val hist = 0.8
        override fun populateContext() {
            multScalar(hist)
            var velNorm = ship.heading
            val mag = ship.velocity.mag() / ship.MAXSPEED
            //velNorm = if (mag > 1e-4) velNorm.normal() else
               // Vector2D(1.0, Math.random() * Math.PI * 2).toCartesian()
            dotContext(velNorm, dotshift, (1 - mag).pow(falloff) * weight * (1- hist), clipZero =
            false)
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
        val offsetDist = 800.0
        val weight = 1.0
        val dotShift = 0.0
        override fun populateContext() {
            val center: Vector2D = Simulation.mouseCords
            val offset = center.add(ship.pos.mult(-1.0))
            var dist = offset.mag() + 1e-4
            val tetherl = dist / (ship.velocity.mag() + 1e-5) * 2
            val frowardTether = ship.pos.add(ship.velocity.normal().mult(tetherl))
            val tetherOffset = frowardTether.add(center.mult(-1.0)).normal()
            val target = center.add(tetherOffset.mult(offsetDist))
            var targetOffset = target.add(ship.pos.mult(-1.0))
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
        val weight = 10.0
        val maxWeight = 0.0
        val falloff = 800.0
        override fun populateContext() {
            val target: Vector2D = Simulation.mouseCords
            var offset = target.add(ship.pos.mult(-1.0))
            val dist = offset.mag() + 1e-4
            offset = offset.normal()
            offset = offset.mult(weight).add(ship.heading).normal()
            dotContext(offset,0.0, dist / falloff)
            for (i in 0 until numbins) {
                bins[i] = min(bins[i], maxWeight)
            }
        }
    }

    var squadFormation: ContextMap = object : ContextMap(){
        val weight = 2.0
        val maxWeight = 5.0
        val falloff = 100.0
        val dotShift = 0.0
        override fun populateContext() {
            clearContext()
            val target = this@AIAgent.ship.squad?.getFromationPos(this@AIAgent.ship)?: return
            var targetOffset = target.add(ship.pos.mult(-1.0))
            val dist = targetOffset.mag()
            if (dist/falloff < 1e-2) return
            squadTarget = target
            targetOffset = targetOffset.normal()
            dotContext(targetOffset, dotShift, min(dist/ falloff * weight, maxWeight))
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
     *  * **`dotShift`** shifts the dot product, giving danger to orthogonal
     * directions
     */
    var shipDanger: ContextMap = object : ContextMap() {
        val falloff = 50.0
        var dotShift = 0.1
        override fun populateContext() {
            for (otherShip in Simulation.ships) {
                if (otherShip === this@AIAgent.ship) continue
                val offset = otherShip.pos.add(ship.pos.mult(-1.0))
                val dist = offset.mag() + 1e-4
                val t = dist/(ship.velocity.mag()*2.0)
                val lookAhead = otherShip.velocity.mult(t)
                var targetOffset = offset.add(lookAhead)
                val targetDist = targetOffset.mag() + 1e-4
                targetOffset = targetOffset.normal()
                dotContext(targetOffset,dotShift,falloff/targetDist)
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
        var falloff = 100
        var dotShift = 0.2
        override fun populateContext() {
            for (i in 0 until numbins) {
                //north border
                var dir = Vector2D(0.0, -1.0)
                bins[i] += calcDanger(bindir[i], dir, ship.pos.y, 0.0)
                //south border
                dir = Vector2D(0.0, 1.0)
                bins[i] += calcDanger(bindir[i],dir,ship.pos.y, Simulation.h.toDouble())
                //west border
                dir = Vector2D(-1.0, 0.0)
                bins[i] += calcDanger(bindir[i], dir, ship.pos.x, 0.0)
                //east border
                dir = Vector2D(1.0, 0.0)
                bins[i] += calcDanger(bindir[i],dir,ship.pos.x, Simulation.w.toDouble())
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