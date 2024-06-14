import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Stroke
import java.awt.geom.Path2D
import javax.swing.JFrame
import javax.swing.JPanel

class Display(w: Int, h: Int) : JPanel() {
    val frame : JFrame
    init {
        frame = JFrame("Agents")
        frame.setSize(w, h)
        frame.isVisible = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isFocusable = true
        frame.add(this)
    }

    override fun paint(g: Graphics) {
        super.paintComponent(g)
        for (ship in Simulation.ships) {
            drawShip(ship, g as Graphics2D)
        }
    }

    fun drawShip(ship: Ship, g: Graphics2D) {
        val save = g.transform
        val strokesave = g.stroke
        //draw agent
        g.translate(ship.pos.x, ship.pos.y)
        g.rotate(ship.heading.toPolar().y)
        g.scale(ship.size.toDouble(), ship.size.toDouble())
        g.color =  if (ship.agent is PlayerAgent) Color.green else Color.white
        g.color = ship.team?.color
        g.fill(shape)
        g.color = Color.black
        g.stroke = BasicStroke(1/ship.size.toFloat())
        g.draw(shape)
        g.transform = save
        g.stroke = strokesave

        //draw context map and other vectors
        g.translate(ship.pos.x, ship.pos.y)
        if (true && ship.agent is AIAgent) {
            val agent = ship.agent as AIAgent
            for (i in 0 until ContextMap.numbins) {
                val dir: Vector2D = ContextMap.bindir[i]
                var mag = agent.movementInterest.bins[i]
                mag = if (mag < 0) 0.0 else mag
                g.color = Color.green
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * ship.size * ship.size).toInt(),
                    (dir.y * mag * ship.size * ship.size).toInt()
                )
                mag = agent.rotationInterest.bins[i]
                mag = if (mag < 0) 0.0 else mag
                g.color = Color.red
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * ship.size * ship.size).toInt(),
                    (dir.y * mag * ship.size * ship.size).toInt()
                )
            }
        }
        g.color = Color.blue
        g.drawLine(
            0,
            0,
            (ship.thrust.x * ship.size).toInt(),
            (ship.thrust.y * ship.size).toInt()
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
        val shape: Path2D = Path2D.Double()

        init {
            shape.moveTo(0.0, -2.0)
            shape.lineTo(-1.0, 2.0)
            shape.lineTo(1.0, 2.0)
            shape.closePath()
        }
    }
}
