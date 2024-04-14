import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.Path2D
import javax.swing.JFrame
import javax.swing.JPanel

class Display(w: Int, h: Int) : JPanel() {
    init {
        val frame = JFrame("Agents")
        frame.setSize(w, h)
        frame.isVisible = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(this)
    }

    override fun paint(g: Graphics) {
        super.paintComponent(g)
        for (agent in Simulation.agents) {
            drawAgent(agent, g as Graphics2D)
        }
    }

    fun drawAgent(agent: Agent, g: Graphics2D) {
        val save = g.transform
        //draw agent
        g.translate(agent.pos.x, agent.pos.y)
        g.rotate(agent.heading.toPolar().y)
        g.color = Color.white
        g.fill(shape)
        g.color = Color.black
        g.draw(shape)
        g.transform = save

        //draw context map and other vectors
        g.translate(agent.pos.x, agent.pos.y)
        if (true) {
            for (i in 0 until ContextMap.numbins) {
                val dir: Vector2D = ContextMap.bindir[i]
                var mag = agent.movementInterest.bins[i]
                mag = if (mag < 0) 0.0 else mag
                g.color = Color.green
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * Companion.size * Companion.size).toInt(),
                    (dir.y * mag * Companion.size * Companion.size).toInt()
                )
                mag = agent.rotationInterest.bins[i]
                mag = if (mag < 0) 0.0 else mag
                g.color = Color.red
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * Companion.size * Companion.size).toInt(),
                    (dir.y * mag * Companion.size * Companion.size).toInt()
                )
            }
        }
        g.color = Color.blue
        g.drawLine(
            0,
            0,
            (agent.thrust.x * Companion.size).toInt(),
            (agent.thrust.y * Companion.size).toInt()
        )
        g.transform = save
    }

    val mouseCords: Vector2D
        /**
         * The current mouse cords, if the mouse its outside the bounds then the center of the canvas
         */
        get() {
            val point = this.mousePosition
                ?: return Vector2D(
                    Simulation.w / 2.0,
                    Simulation.h / 2.0
                )
            return Vector2D(point.x.toDouble(), point.y.toDouble())
        }

    companion object {
        var size = 10
        val shape: Path2D = Path2D.Double()

        init {
            shape.moveTo(0.0, (-size * 2).toDouble())
            shape.lineTo(-size.toDouble(), (size * 2).toDouble())
            shape.lineTo(size.toDouble(), (size * 2).toDouble())
            shape.closePath()
        }
    }
}
