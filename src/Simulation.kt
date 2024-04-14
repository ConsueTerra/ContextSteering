import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Timer

class Simulation(numAgents: Int) : ActionListener {
    var display: Display

    init {
        display = Display(w, h)
        setup(numAgents)
        val timer = Timer(16, this)
        timer.restart()
    }

    override fun actionPerformed(e: ActionEvent) {
        tick()
        display.repaint()
    }

    fun tick() {
        mouseCords = display.mouseCords
        for (agent in agents) {
            agent.steer()
            var accel = agent.thrust
            accel = accel.mult(1 / agent.mass)
            agent.velocity = agent.velocity.add(accel).clip(Agent.MAXSPEED)
            agent.pos = agent.pos.add(agent.velocity).wrap(w.toDouble(), h.toDouble())
            if (agent.pos.x == 0.0 && agent.pos.y == 0.0) agent.pos = agent.pos.add(1.0, 1.0)
        }
    }

    fun setup(numAgents: Int) {
        for (i in 0 until numAgents) {
            val agent = Agent(w, h)
            agents.add(agent)
        }
    }

    companion object {
        var agents: MutableList<Agent> = ArrayList()
        var w = 1000
        var h = 1000
        var mouseCords = Vector2D(0.0,0.0)
    }
}
