import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.concurrent.CopyOnWriteArrayList
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
            ship.tick()
            var accel = ship.thrust
            accel = accel.mult(1 / ship.mass)
            ship.velocity = ship.velocity.mult(DRAGFORCE)//dragging force
            ship.velocity = ship.velocity.add(accel).clip(ship.MAXSPEED)
            ship.pos = ship.pos.add(ship.velocity).bound(W.toDouble(), H.toDouble())
            if (ship.pos.x == 0.0 && ship.pos.y == 0.0) ship.pos = ship.pos.add(1.0, 1.0)
        }
        for (team in teams) {
            team.squads.forEach {it.updateHeading()}
        }

        for (particle in particles) {
            particle.tick()
        }
        if (Math.random() < 0.3) {
            val center = Vector2D(W/2.0,H/2.0)
            targetShip?.let { spawnParticle(Vector2D(W/2.0,H/2.0), it.pos.add(center.mult(-1.0))) }
        }

    }

    fun setup(numAIAgents: Int) {
        val numteams = (Math.random()*2+3).toInt()
        for (i in 0 until numteams){
            teams.add(Team())
        }
        if (true) {
            //val playerShip = object : Ship(Vector2D(0.0, 0.0),mass = 65.0, size = 65) {
            //    override fun arrangeShields() {
            //        (ShipTypes.Capital::arrangeShields)(this as ShipTypes.Capital)
            //    }
            //}
            val playerShip = ShipTypes.Capital()
            playerShip.pos = Vector2D(W/2.0, H/2.0)
            playerShip.agent = PlayerAgent(playerShip)
            ships.add(playerShip)
            playerShip.team = teams[0]
            teams[0].ships.add(playerShip)
            display.frame.addKeyListener((playerShip.agent as PlayerAgent).control)
        }

        for (i in 0 until numAIAgents) {
            val ship = createShip(W,H)
            ships.add(ship)
            val team = teams[i%numteams]
            ship.team = team
            team.ships.add(ship)
        }

        for(team in teams) setupSquads(team)
    }

    fun setupSquads(team: Team) {
        val maxsize = 5
        val minSize = 3
        var squadSize = (Math.random()*(maxsize-minSize)+minSize).toInt()
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
            j++

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
        val ships: MutableList<Ship> = CopyOnWriteArrayList()
        val teams: MutableList<Team> = ArrayList()
        val particles : MutableList<Particle> = CopyOnWriteArrayList()
        const val W = 4000
        const val H = 4000
        var mouseCords = Vector2D(0.0,0.0)
        const val DRAGFORCE = 0.95
        var targetShip: Ship? = null

        fun spawnParticle(cords : Vector2D, dir : Vector2D = Vector2D(0.0,0.0),
                          speed : Double = 10.0, spread : Double= 0.5) {
            var velocity = Vector2D(speed,((Math.random() * spread) - spread / 2) + dir.toPolar().y).toCartesian()
            val particle = Particle(velocity = velocity)
            particle.pos = cords
            particles.add(particle)
        }
    }

}
