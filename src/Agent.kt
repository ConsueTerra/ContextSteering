/*
Context steering based control for AI
See https://andrewfray.wordpress.com/2013/03/26/context-behaviours-know-how-to-share/
https://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter18_Context_Steering_Behavior-Driven_Steering_at_the_Macro_Scale.pdf
and https://alliekeats.com/portfolio/contextbhvr.html for more in depth descriptions.
Essentially agents determine where to go by using a set of arrays called context maps, these maps
can either hold "interest": ie how much the ai wants to move in a particular direction, or
"danger": how much an ai should avoid a particular direction. Once the ai has the context of what
 to do, it can make a final decision on its heading.
Each of these maps can be combined together like image filters, leading to simple and stateless
AI that is still amendmedable to emergent behaviors and avoids deadlock.

The strategy for implementing context steering is as follows:
1. identity the necessary behaviors (seek, avoid, face target, wander ect) and populate their context maps
2. combine behaviors together using simple arithmetic where possible like filters
3. Make a decision based on the final interest and danger context maps, importantly decision making
should be the LAST step.
 */
import java.util.List;

/**
 * Standard AI for controling a ship, uses context steering for determing movement, the goal of
 * the Agent class is to hold the current state of the agent, it context maps and output its
 * descion based on its surroundings. <br />
 * Notably the Agent class does nto direlty determine the movement on screen, it only sends
 * thurst and heading information to the physis engine. This makes it usefull in any context
 * that has a proper physics engine. <br />
 * The Agent class stores the following information:
 * <ul>
 * <li>The current position and velocity
 * <li>The current thrust and heading of the agent as determined by steering
 * <li>A random [0,1] offset to be used for different behaviours
 * <li>The master steering function that determines thrust and heading
 * <li>#TODO A default list of tunable parameters that influence steering, affecting which
 * behaviors are on and their weight
 * <li>A default mass and default max speed
 * </ul>
 */
public class Agent {

    Vector2D pos;
    Vector2D velocity;
    /**
     * The heading of the ship, in cartesian coordinates
     */
    Vector2D heading;

    float mass;
    Vector2D thrust;

    float offset = (float) Math.random();



    public static final float MAXSPEED = 10;
    public static final float DEFAULTMASS = 10f;

    public static final float THRUSTPOWER = 10;

    public Agent(Vector2D pos, Vector2D velocity, Vector2D heading, float mass, Vector2D thrust) {
        this.pos = pos;
        this.velocity = velocity;
        this.heading = heading;
        this.mass = mass;
        this.thrust = thrust;
    }

    /**
     * Creates a default agent randomly on the simulation space with a random velocity
     * @param w the width of the simulation space
     * @param h the height of the simulation space
     */
    public Agent(int w, int h) {
        this.pos = new Vector2D((float) (Math.random()*w), (float) (Math.random()*h));
        this.velocity = new Vector2D((float) (Math.random()*MAXSPEED - MAXSPEED/2),
                                     (float) (Math.random()*MAXSPEED)- MAXSPEED/2);
        this.velocity = this.velocity.clip(MAXSPEED);
        this.heading = new Vector2D((float) (Math.random()-0.5),
                                    (float) (Math.random()-0.5)).normal();
        this.mass = DEFAULTMASS;
        this.thrust = new Vector2D(0,0);
    }

    /**
     * Master steering function
     * <p>
     * Takes the current simulation sate and updates an agents heading and thrust using context
     * maps
     */
    public void steer() {
        movementInterest.clearContext();
        rotationInterest.clearContext();

        danger.clearContext();

        wander.clearContext();
        wander.populateContext(null, null);
        offsetSeekMouse.clearContext();
        offsetSeekMouse.populateContext(this,null);

        faceMouse.clearContext();
        faceMouse.populateContext(this,null);

        borderDanger.clearContext();
        borderDanger.populateContext(this,null);
        agentDanger.clearContext();
        agentDanger.populateContext(this,null);

        movementInterest.addContext(wander);
        movementInterest.addContext(offsetSeekMouse);
        rotationInterest.addContext(movementInterest);
        rotationInterest.multScalar(0.1f);
        rotationInterest.addContext(faceMouse);

        danger.addContext(borderDanger);
        danger.addContext(agentDanger);

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
        float rotationMovementPrior = Float.max(Float.min(velocity.mag() / MAXSPEED, 1f), 0f);
        //rotationMovementPrior = 0.0f;
        ContextMap temp = new ContextMap(movementInterest) {};
        float movementWeight = 1 - rotationMovementPrior;
        movementInterest.multScalar(movementWeight);
        movementInterest.addContext(ContextMap.scaled(rotationInterest, 1-movementWeight));
        float rotationWeight = (float) rotationMovementPrior;
        if (rotationWeight < 0) throw new RuntimeException();
        rotationInterest.multScalar(rotationWeight);
        rotationInterest.addContext(ContextMap.scaled(temp, 1 - rotationWeight));

        //masking, if the danger for a certain direction is greater than the threshold then it is
        // masked out
        movementInterest.maskContext(danger, 0.5F);
        rotationInterest.maskContext(danger, 0.5f);


        //decision time
        heading = heading.mult(2).add(rotationInterest.interpolatedMaxDir()).normal();

        thrust = movementInterest.interpolatedMaxDir();
        //take the dot product here to prevent the ship thrusting in a direction its not facing
        thrust = thrust.mult(thrust.dot(heading)).mult(Agent.THRUSTPOWER);

    }

    /**
     * Interest maps correspond to how much a ship wants to move in a particular direction
     * (orbit, wander), and give rise to proactive/planning emergent behavior.<br />
     * This map is the final context to which to determine an agents thrust/movement
     */
    ContextMap movementInterest = new ContextMap() {};
    /**
     * Interest maps correspond to how much a ship wants to move in a particular direction
     * (orbit, wander), and give rise to proactive/planning emergent behavior.<br />
     * This map is the final context to which to determine an agents heading
     */
    ContextMap rotationInterest = new ContextMap() { };
    /** Danger maps on the other hand indicate how bad moving in a particular direction is, with
     * a high enough danger leading to an agent avoiding that direction regardless of interest
     * (obstacle avoidance). This gives rise to reactive emergent behavior. <br />
     * This context is the final danger map that masks the interest contexts
     */
    ContextMap danger = new ContextMap() {};

    /**
     * Uses simplex noise to generate a random direction to follow, the direction oscillates based
     * on time. Uses an agents offset to give a different wanting per agent. Combining this
     * behavior with others gives a random organic look to agents.<br />
     * This Context is meant for <b><code>movementInterest</code></b>
     * <ul>
     * <li><b><code>weight</code></b> controls how strong this behavior is
     * <li><b><code>dotShift</code></b> shifts the dot product, allowing for backward directions
     * to be considered
     * <li><b><code>jitterRate</code></b> controls how frequently the wandering angle oscillates
     * <ul/>
     */
    ContextMap wander = new ContextMap() {
        final float weight = 0.5f;
        final float dotShift = 1.0f;
        final double jitterRate = 2e4;
        @Override
        public void populateContext(Agent agent, List<ContextMap> otherContexts) {
            double timeoffset = offset * jitterRate;
            float theta = (float) SimplexNoise.noise(System.currentTimeMillis() % 100000 / jitterRate + timeoffset,
                    System.currentTimeMillis() % 100000 / jitterRate + timeoffset) * 2 * (float) Math.PI;
            Vector2D desiredDir = new Vector2D(1.0F, theta).toCartesian();
            this.dotContext(desiredDir, dotShift, weight);
        }
    };

    /**
     * Makes the agent follow a orbit around the mouse. this does it by generating a target that
     * is offset by the orbit distance from the mouse and following that point. In order for the
     * agent to orbit properly once the distance is reached, the target point is calculated using
     * a frontal tether of the agent instead of itself. When the agent is at the orbit distance
     * it will follow this tether, and when its far it will move towards orbit.<br />
     * This Context is meant for <b><code>movementInterest</code></b>
     * <ul>
     * <li><b><code>weight</code></b> controls how strong this behavior is
     * <li><b><code>dotShift</code></b> shifts the dot product, allowing for backward directions
     * to be considered
     * <li><b><code>offsetDist</code></b> controls the orbiting distance
     * <ul/>
     */
    ContextMap offsetSeekMouse = new ContextMap() {
        final float offsetDist = 200;
        final float weight = 2;
        final float dotShift = 0.0f;

        @Override
        public void populateContext(Agent agent, List<ContextMap> otherContexts) {
            Vector2D center = Simulation.mouseCords;
            Vector2D offset = center.add(agent.pos.mult(-1.0f));
            float dist = offset.mag() + (float) 1e-4;
            float tetherl = dist / (velocity.mag() + 1e-5f) * 2;
            Vector2D frowardTether = agent.pos.add(agent.velocity.normal().mult(tetherl));
            Vector2D tetherOffset = frowardTether.add(center.mult(-1)).normal();
            Vector2D target = center.add(tetherOffset.mult(offsetDist));
            Vector2D targetOffset = target.add(agent.pos.mult(-1));
            dist = targetOffset.mag();
            targetOffset = targetOffset.normal();
            this.dotContext(targetOffset, dotShift, weight);
        }
    };

    /**
     * Makes the agent face the mouse pointer<br />
     * This Context is meant for <b><code>rotationInterest</code></b>
     * <ul>
     * <li><b><code>weight</code></b> controls how strong this behavior is
     * <li><b><code>maxWeight</code></b> caps the maximum weight for a particular direction
     * <li><b><code>falloff</code></b> controls how this behavior scales with distance, higher
     * values will cause a lower weight (ie agent will only look at mouse from further away)
     * <ul/>
     */
    ContextMap faceMouse = new ContextMap() {
        final float weight = 1;
        final float maxWeight = 3.0f;
        final float falloff = 100.0f;
        @Override
        public void populateContext(Agent agent, List<ContextMap> otherContexts) {
            Vector2D target = Simulation.mouseCords;
            Vector2D offset = target.add(agent.pos.mult(-1.0f));
            float dist = offset.mag() + (float) 1e-4;
            offset = offset.normal();
            for (int i = 0; i < numbins; i++) {
                float mag = bindir[i].dot(offset) * dist / falloff;
                mag = mag < 0 ? 0.0F : mag;
                mag = Float.min(mag,maxWeight);
                bins[i] += mag;
            }
        }
    };

    /**
     * Makes the agent avoid other agents, the smaller the distance between agents the higher
     * the danger<br />
     * This Context is meant for <b><code>danger</code></b>
     * <ul>
     * <li><b><code>falloff</code></b> controls how this behavior scales with distance, higher
     * values will cause higher danger and agents will react faster
     * <ul/>
     */
    ContextMap agentDanger = new ContextMap() {
        final float falloff = 50.0f;
        @Override
        public void populateContext(Agent agent, List<ContextMap> otherContexts) {
            for (Agent otherAgent:Simulation.agents ) {
                if (otherAgent == agent) continue;
                Vector2D offset = otherAgent.pos.add(agent.pos.mult(-1));
                float dist = offset.mag() + 1e-4f;
                offset = offset.normal();
                for (int i = 0; i < numbins; i++) {
                    float mag = bindir[i].dot(offset) * falloff / dist;
                    mag = mag < 0 ? 0.0F : mag;
                    bins[i] += mag;
                }
            }
        }
    };

    /**
     * Makes the agent avoid the borders, the smaller the distance between the agent and
     * boundaries the higher the danger<br />
     * This Context is meant for <b><code>danger</code></b>
     * <ul>
     * <li><b><code>falloff</code></b> controls how this behavior scales with distance, higher
     * values will cause higher danger and agents will react faster
     * <li><b><code>dotShift</code></b> shifts the dot product, giving danger to orthogonal
     * directions
     * <ul/>
     */
    ContextMap borderDanger = new ContextMap() {
        float falloff = 50.f;
        float dotShift = 0.2f;
        @Override
        public void populateContext(Agent agent, List<ContextMap> otherContexts) {
            for (int i = 0; i < numbins; i++) {
                //north border
                Vector2D dir  = new Vector2D(0, -1);
                bins[i] += calcDanger(bindir[i], dir, agent.pos.y, 0.0f);
                //south border
                dir  = new Vector2D(0, 1);
                bins[i] += calcDanger(bindir[i], dir, agent.pos.y, Simulation.h);
                //west border
                dir  = new Vector2D(-1, 0);
                bins[i] += calcDanger(bindir[i], dir, agent.pos.x, 0.0f);
                //east border
                dir  = new Vector2D(1, 0);
                bins[i] += calcDanger(bindir[i], dir, agent.pos.x, Simulation.w);
            }

        }

        private float calcDanger(Vector2D bindir, Vector2D dir, float val1, float val2) {
            float proximity = (float) (Math.abs(val1 - val2) + 1e-4);
            float mag = (bindir.dot(dir)+ dotShift) * falloff / proximity;
            mag = mag < 0 ? 0.0F : mag;
            return mag;
        }
    };
}