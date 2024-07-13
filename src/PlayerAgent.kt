import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import kotlin.math.PI


class PlayerAgent(ship: Ship) : Agent(ship) {
    var APressed = false
    var WPressed = false
    var DPressed = false
    var SPressed = false
    var QPressed = false
    var EPressed = false

    var control = object : KeyAdapter() {
        override fun keyTyped(e: KeyEvent?) {
        }
        override fun keyReleased(e: KeyEvent?) {
            when (e!!.keyCode) {
                KeyEvent.VK_A -> APressed  = false
                KeyEvent.VK_W -> WPressed  = false
                KeyEvent.VK_D -> DPressed  = false
                KeyEvent.VK_S -> SPressed  = false
                KeyEvent.VK_Q -> QPressed  = false
                KeyEvent.VK_E -> EPressed  = false
            }
        }
        override fun keyPressed(e: KeyEvent?) {
            when (e!!.keyCode) {
                KeyEvent.VK_A -> APressed  = true
                KeyEvent.VK_W -> WPressed  = true
                KeyEvent.VK_D -> DPressed  = true
                KeyEvent.VK_S -> SPressed  = true
                KeyEvent.VK_Q -> QPressed  = true
                KeyEvent.VK_E -> EPressed  = true
            }
        }
    }


    override fun steer(): SteeringOutput {
        var keyThrust = Vector2D(0.0,0.0)
        var keyHeading = 0.0
        if (APressed) keyThrust =  keyThrust.add(-1.0, 0.0)
        if (WPressed) keyThrust =  keyThrust.add(0.0, -1.0)
        if (DPressed) keyThrust =  keyThrust.add(1.0, 0.0)
        if (SPressed) keyThrust =  keyThrust.add(0.0, 1.0)
        if (QPressed) keyHeading += 0.1
        if (EPressed) keyHeading += -0.1
        keyThrust = if(keyThrust.mag() > 1e-5) keyThrust.normal() else keyThrust
        var heading = ship.heading.toPolar().add(0.0, keyHeading).toCartesian().normal()
        val oldthrust = if(ship.thrust.mag() > 1e-5) ship.thrust.normal() else ship.thrust
        var thrust = oldthrust.add(keyThrust)
        thrust = if (thrust.mag() > 1e-5) thrust.normal() else thrust
        heading = heading.mult(10.0).add(thrust).normal()
        return SteeringOutput(heading, thrust)
    }



}
