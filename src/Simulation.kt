import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Timer

class Simulation(numAIShips: Int) : ActionListener {
    var display: Display

    init {
        display = Display(w, h)
        setup(numAIShips)
        val timer = Timer(16, this)
        timer.restart()
    }

    override fun actionPerformed(e: ActionEvent) {
        tick()
        display.repaint()
    }

    fun tick() {
        mouseCords = display.mouseCords
        for (ship in ships) {
            ship.handleMovement()
            var accel = ship.thrust
            accel = accel.mult(1 / ship.mass)
            ship.velocity = ship.velocity.add(accel).clip(Ship.MAXSPEED)//#TODO fix this
            ship.pos = ship.pos.add(ship.velocity).bound(w.toDouble(), h.toDouble())
            if (ship.pos.x == 0.0 && ship.pos.y == 0.0) ship.pos = ship.pos.add(1.0, 1.0)
        }
    }

    fun setup(numAIAgents: Int) {
        for (i in 0 until numAIAgents) {
            val ship = Ship(w, h)
            ships.add(ship)
        }
        val playerShip = Ship(Vector2D(w/2.0, h/2.0))
        playerShip.agent = PlayerAgent(playerShip)
        ships.add(playerShip)
        display.frame.addKeyListener((playerShip.agent as PlayerAgent).control)
    }

    companion object {
        var ships: MutableList<Ship> = ArrayList()
        var w = 1000
        var h = 1000
        var mouseCords = Vector2D(0.0,0.0)
    }
}
