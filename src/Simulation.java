import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class Simulation implements ActionListener {

    static List<Agent> agents = new ArrayList<>();
    static int w = 1000;
    static int h = 1000;

    static Vector2D mouseCords;

    Display display;
    public Simulation(int numAgents) {
        display = new Display(w,h);
        setup(numAgents);
        Timer timer = new Timer(16, this);
        timer.restart();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tick();
        display.repaint();
    }
    void tick() {
        mouseCords = display.getMouseCords();
        for (Agent agent : agents){
            agent.steer();
            Vector2D accel = agent.thrust;
            accel = accel.mult(1/agent.mass);
            agent.velocity = agent.velocity.add(accel).clip(Agent.MAXSPEED);
            agent.pos = agent.pos.add(agent.velocity).wrap(w,h);
            if (agent.pos.x == 0.0f && agent.pos.y == 0.0f) agent.pos =  agent.pos.add(1.0f,1.0f);
        }
    }

    void setup(int numAgents) {
        for (int i = 0; i < numAgents; i++) {
            Agent agent = new Agent(w,h);
            agents.add(agent);
        }
    }

}
