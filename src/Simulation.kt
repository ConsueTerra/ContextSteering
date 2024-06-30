import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Timer

class Simulation(numAIShips: Int) : ActionListener {
    var display: Display

    init {
        display = Display()
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
            ship.velocity = ship.velocity.mult(0.95)//dragging force
            ship.velocity = ship.velocity.add(accel).clip(ship.MAXSPEED)
            ship.pos = ship.pos.add(ship.velocity).bound(w.toDouble(), h.toDouble())
            if (ship.pos.x == 0.0 && ship.pos.y == 0.0) ship.pos = ship.pos.add(1.0, 1.0)
        }
        for (team in teams) {
            team.squads.forEach {it.updateHeading()}
        }
    }

    fun setup(numAIAgents: Int) {
        val numteams = (Math.random()*2+2).toInt()
        for (i in 0 until numteams){
            teams.add(Team())
        }
        if (false) {
            val playerShip = ShipTypes.drawRandomShip()()
            playerShip.pos = Vector2D(w/2.0, h/2.0)
            playerShip.agent = PlayerAgent(playerShip)
            ships.add(playerShip)
            playerShip.team = teams[0]
            teams[0].ships.add(playerShip)
            display.frame.addKeyListener((playerShip.agent as PlayerAgent).control)
        }

        for (i in 0 until numAIAgents) {
            val ship = createShip(w,h)
            ships.add(ship)
            val team = teams[i%numteams]
            ship.team = team
            team.ships.add(ship)
        }

        for(team in teams) setupSquads(team)
    }

    fun setupSquads(team: Team) {
        val maxsize = 5
        var squadSize = (Math.random()*(maxsize-1)+1).toInt()
        var j = 0
        var squad = Squad(team = team)
        team.squads.add(squad)
        for (ship in team.ships) {
            if (j == squadSize) {
                squad = Squad(team = team)
                team.squads.add(squad)
                squadSize = (Math.random()*(maxsize-1)+1).toInt()
                j = 0
            }
            squad.ships.add(ship)
            ship.squad = squad
            j =+ 1

        }
        val pullamount = 0.7
        for (squads in team.squads) {
            squad.ships.sortBy { -it.mass }
            val centerOfMass = squad.ships.fold(Vector2D(0.0,0.0)) {
                acc, ship -> acc.add(ship.pos)}.mult(1/squad.ships.size.toDouble())
            squad.ships.forEach {
                ship -> ship.pos = centerOfMass.mult(pullamount).add(ship.pos.mult(1- pullamount))}
        }

    }

    companion object {
        var ships: MutableList<Ship> = ArrayList()
        var teams: MutableList<Team> = ArrayList()
        var w = 2000
        var h = 2000
        var mouseCords = Vector2D(0.0,0.0)
    }
}
