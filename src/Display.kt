import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Display extends JPanel {

    static int size = 10;
    static final Path2D shape = new Path2D.Double();

    static {
        shape.moveTo(0, -size * 2);
        shape.lineTo(-size, size * 2);
        shape.lineTo(size, size * 2);
        shape.closePath();
    }

    public Display(int w, int h) {
        JFrame frame = new JFrame("Agents");
        frame.setSize(w,h);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
    }

    @Override
    public void paint (Graphics g) {
        super.paintComponent(g);
        for (Agent agent : Simulation.agents) {
            drawAgent(agent, (Graphics2D) g);
        }
    }

    public void drawAgent(Agent agent,Graphics2D g){
        AffineTransform save = g.getTransform();
        //draw agent
        g.translate(agent.pos.x, agent.pos.y);
        g.rotate(agent.heading.toPolar().y);
        g.setColor(Color.white);
        g.fill(shape);
        g.setColor(Color.black);
        g.draw(shape);
        g.setTransform(save);

        //draw context map and other vectors
        g.translate(agent.pos.x, agent.pos.y);

        if (true) {
            for (int i = 0; i < ContextMap.numbins; i++) {
                Vector2D dir = ContextMap.bindir[i];
                float mag = agent.movementInterest.bins[i];
                mag = mag < 0 ? 0.0F: mag;
                g.setColor(Color.green);
                g.drawLine(0,0, (int) (dir.x * mag * size* size), (int) (dir.y *mag* size* size));
                mag = agent.rotationInterest.bins[i];
                mag = mag < 0 ? 0.0F: mag;
                g.setColor(Color.red);
                g.drawLine(0,0, (int) (dir.x * mag * size* size), (int) (dir.y *mag* size* size));
            }
        }

        g.setColor(Color.blue);
        g.drawLine(0,0, (int) (agent.thrust.x * size), (int) (agent.thrust.y * size));
        g.setTransform(save);


    }

    /**
     * The current mouse cords, if the mouse its outside the bounds then the center of the canvas
     */
    public Vector2D getMouseCords() {
        Point point = this.getMousePosition();
        if (point == null) {
            return new Vector2D((float) Simulation.w / 2, (float) Simulation.h / 2);
        }
        return new Vector2D(point.x, point.y);
    }
}
