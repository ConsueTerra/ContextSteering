import java.awt.BasicStroke
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.pow


class Display() : JPanel() {
    val frame : JFrame
    var zoom = canvasWidth.toDouble() / Simulation.w.toDouble()
    var topLeft : Vector2D = Vector2D(-canvasWidth*zoom, -canvasHeight*zoom)
    var canvasTransform : AffineTransform = AffineTransform()
    var showContext = true
    var showTargets = true

    init {
        frame = JFrame("Agents")
        frame.setSize(canvasWidth, canvasHeight)
        frame.isVisible = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isFocusable = true
        frame.add(this)
        val panningHandler = PanningHandler()
        addMouseListener(panningHandler)
        addMouseMotionListener(panningHandler)
        addMouseWheelListener(panningHandler)

        val options = JFrame("Options")
        val toggleContext = JButton("toggle Contexts")
        val toggleTargets = JButton("toggle targets")
        options.layout = FlowLayout()
        options.add(toggleContext)
        options.add(toggleTargets)
        options.pack()
        options.isVisible = true

        toggleContext.addActionListener { e ->  toggleContext()}
        toggleTargets.addActionListener { e ->  toggleTarget()}
    }

    override fun paint(g: Graphics) {
        super.paintComponent(g)
        val globalTransform : AffineTransform = (g as Graphics2D).transform
        panZoom(g)
        for (ship in Simulation.ships) {
            drawShip(ship, g)
        }
        g.drawRect(0,0,Simulation.w,Simulation.h)
        g.transform(globalTransform)
    }

    fun panZoom (g :Graphics2D) {
        canvasTransform = AffineTransform()
        canvasTransform.translate((width/2).toDouble(), (height/2).toDouble())
        canvasTransform.scale(zoom, zoom)
        canvasTransform.translate(((-width/2).toDouble()), (-height/2).toDouble())
        canvasTransform.translate(topLeft.x,topLeft.y)
        g.transform(canvasTransform)
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
        if (showContext && ship.agent is AIAgent) {
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

        if (showTargets && ship.agent is AIAgent) {
            val target = (ship.agent as AIAgent).squadTarget
            if (target != null) {
                g.drawRect(target.x.toInt()-5,target.y.toInt()-5, 10,10 )
            }
        }
    }

    val mouseCords: Vector2D
        /**
         * The current mouse cords, if the mouse its outside the bounds then the center of the canvas
         */
        get() {
            var point : Point2D = this.mousePosition ?:  Point2D.Double((width/2).toDouble(),
                (height/2).toDouble())
            point = canvasTransform.inverseTransform(point, null)
            return Vector2D(point.x, point.y)
        }

    companion object {
        val shape: Path2D = Path2D.Double()

        init {
            shape.moveTo(0.0, -2.0)
            shape.lineTo(-1.0, 2.0)
            shape.lineTo(1.0, 2.0)
            shape.closePath()
        }

        val canvasHeight = 1000
        val canvasWidth = 1000
    }

    inner class PanningHandler : MouseListener, MouseMotionListener, MouseWheelListener {
        var referenceX = 0.0
        var referenceY = 0.0

        // saves the initial transform at the beginning of the pan interaction
        var initialTransform: AffineTransform? = null

        // capture the starting point
        override fun mousePressed(e: MouseEvent) {

            // first transform the mouse point to the pan and zoom
            // coordinates
            try {
                val startPoint = canvasTransform.inverseTransform(e.getPoint(), null)
                // save the transformed starting point and the initial
                // transform
                referenceX = startPoint.x
                referenceY = startPoint.y
                initialTransform = canvasTransform
            } catch (te: NoninvertibleTransformException) {
                println(te)
            }

        }

        override fun mouseDragged(e: MouseEvent) {

            // first transform the mouse point to the pan and zoom
            // coordinates. We must take care to transform by the
            // initial tranform, not the updated transform, so that
            // both the initial reference point and all subsequent
            // reference points are measured against the same origin.
            try {
                val startPoint = initialTransform!!.inverseTransform(e.getPoint(), null)
                // the size of the pan translations
                // are defined by the current mouse location subtracted
                // from the reference location
                val deltaX: Double = startPoint.x - referenceX
                val deltaY: Double = startPoint.y - referenceY

                // make the reference point be the new mouse point.
                referenceX = startPoint.x
                referenceY = startPoint.y
                topLeft = topLeft.add(deltaX,deltaY)

            } catch (te: NoninvertibleTransformException) {
                println(te)
            }
        }
        override fun mouseWheelMoved(e: MouseWheelEvent?) {
            val amount = e?.wheelRotation ?: return
            zoom *= (1.5).pow(amount)
        }

        override fun mouseClicked(e: MouseEvent?) {}
        override fun mouseEntered(e: MouseEvent?) {}
        override fun mouseExited(e: MouseEvent?) {}
        override fun mouseMoved(e: MouseEvent?) {}
        override fun mouseReleased(e: MouseEvent?) {}

    }

    fun toggleContext() {
        showContext = !showContext
    }

    fun toggleTarget() {
        showTargets = !showTargets
    }

}
