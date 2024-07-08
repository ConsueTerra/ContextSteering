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
import java.awt.geom.Rectangle2D
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.pow


class Display() : JPanel() {
    val frame : JFrame
    var zoom = canvasWidth.toDouble() / Simulation.W.toDouble()
    var topLeft : Vector2D = Vector2D((canvasWidth-Simulation.W)*zoom, (canvasHeight-Simulation.H)*zoom)
    var canvasTransform : AffineTransform = AffineTransform()
    var showContext = true
    var showTargets = true
    var showShields = false
    val linel= 10

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
        val toggleShields = JButton("toggle shields")
        options.layout = FlowLayout()
        options.add(toggleContext)
        options.add(toggleTargets)
        options.add(toggleShields)
        options.pack()
        options.isVisible = true

        toggleContext.addActionListener { e ->  toggleContext()}
        toggleTargets.addActionListener { e ->  toggleTarget()}
        toggleShields.addActionListener {e -> toggleShields()}
    }

    override fun paint(g: Graphics) {
        super.paintComponent(g)
        val globalTransform : AffineTransform = (g as Graphics2D).transform
        panZoom(g)
        for (ship in Simulation.ships) {
            drawShip(ship, g)
        }
        g.color = Color.blue
        g.drawRect(0,0,Simulation.W,Simulation.H)
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
        g.translate(ship.pos.x, ship.pos.y)
        g.rotate(ship.heading.toPolar().y)
        g.color =  if (ship.agent is PlayerAgent) Color.green else Color.white
        g.color = ship.team?.color
        val shape = shipShape(ship)
        g.fill(shape)
        g.color = Color.black
        g.draw(shape)
        if (showShields) {

            for (shield in ship.shields) {
                val health = (shield.health/shield.maxHealth) * 0.6
                val shieldColor = Color.getHSBColor(health.toFloat(),1.0f,1.0f)
                g.color= shieldColor
                val rec = Rectangle2D.Double(shield.corners[0].x,shield.corners[0].y, shield.width,shield.height)
                g.draw(rec)
            }

        }
        g.transform = save

        //draw context map and other vectors
        g.translate(ship.pos.x, ship.pos.y)
        if (showContext && ship.agent is AIAgent) {
            val agent = ship.agent as AIAgent
            for (i in 0 until ContextMap.NUMBINS) {
                val dir: Vector2D = ContextMap.bindir[i]
                var mag = agent.movementInterest.bins[i]
                mag = if (mag < 0) 0.0 else mag
                g.color = Color.green
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * linel * linel).toInt(),
                    (dir.y * mag * linel * linel).toInt()
                )
                mag = agent.shieldAwareness.bins[i]
                mag = if (mag < 0) -mag else mag
                g.color = Color.red
                g.drawLine(
                    0,
                    0,
                    (dir.x * mag * linel * linel).toInt(),
                    (dir.y * mag * linel * linel).toInt()
                )
            }
        }
        g.color = Color.blue
        g.drawLine(
            0,
            0,
            (ship.thrust.x * linel).toInt(),
            (ship.thrust.y * linel).toInt()
        )
        g.transform = save

        if (showTargets && ship.agent is AIAgent) {
            var target = (ship.agent as AIAgent).squadTarget
            if (target != null) {
                g.drawRect(target.x.toInt()-5,target.y.toInt()-5, 10,10 )
            }
            g.color = Color(161,78,37)
            target = (ship.agent as AIAgent).orbitTarget
            if (target != null && (ship.squad?.ships?.indexOf(ship) == 0)) {
                g.drawRect(target.x.toInt()-5,target.y.toInt()-5, 10,10 )
            }
            g.color = Color.RED
            target = (ship.agent as AIAgent).dangerTarget
            if (target != null && false) {
                g.drawRect(target.x.toInt()-5,target.y.toInt()-5, 10,10 )
            }
        }
    }

    val mouseCords: Vector2D
        /**
         * The current mouse cords, if the mouse its outside the bounds then the center of the canvas
         */
        get() {
            var point : Point2D = this.mousePosition ?:  Point2D.Double((Simulation.W/2).toDouble(),
                (Simulation.H/2).toDouble())
            if (this.mousePosition!= null)  point = canvasTransform.inverseTransform(point, null)
            return Vector2D(point.x, point.y)
        }

    companion object {
        fun shipShape(ship: Ship) : Path2D {
            val shape: Path2D = Path2D.Double()
            val start = ship.shape[0].mult(ship.size.toDouble())
            shape.moveTo(start.x,start.y)
            for (vec in ship.shape.subList(1,ship.shape.size)) {
                val scaled = vec.mult(ship.size.toDouble())
                shape.lineTo(scaled.x,scaled.y)
            }
            shape.closePath()
            return shape
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

    fun toggleShields() {
        showShields = !showShields
    }

}
