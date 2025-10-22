package gui;

import model.RobotModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.*;

public class GameVisualizer extends JPanel {
    private final RobotModel model;
    private final Timer repaintTimer;

    public GameVisualizer(RobotModel model) {
        this.model = model;
        setDoubleBuffered(true);
        // Таймер перерисовки (30 FPS)
        repaintTimer = new Timer(30, e -> repaint());
        repaintTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                model.setTarget(e.getX(), e.getY());
            }
        });
    }

    public RobotModel getModel() {
        return model;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        // Отрисовка цели
        drawTarget(g2d);
        // Отрисовка робота
        drawRobot(g2d);
    }

    private void drawRobot(Graphics2D g) {
        int robotX = (int)model.getX();
        int robotY = (int)model.getY();

        AffineTransform t = AffineTransform.getRotateInstance(
                model.getDirection(), robotX, robotY);
        g.setTransform(t);

        g.setColor(Color.MAGENTA);
        fillOval(g, robotX, robotY, 30, 10);
        g.setColor(Color.BLACK);
        drawOval(g, robotX, robotY, 30, 10);
        g.setColor(Color.WHITE);
        fillOval(g, robotX + 10, robotY, 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, robotX + 10, robotY, 5, 5);
    }

    private void drawTarget(Graphics2D g) {
        g.setTransform(new AffineTransform());
        g.setColor(Color.GREEN);
        fillOval(g, model.getTargetX(), model.getTargetY(), 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, model.getTargetX(), model.getTargetY(), 5, 5);
    }

    private static void fillOval(Graphics g, int x, int y, int w, int h) {
        g.fillOval(x - w/2, y - h/2, w, h);
    }

    private static void drawOval(Graphics g, int x, int y, int w, int h) {
        g.drawOval(x - w/2, y - h/2, w, h);
    }

    public void shutdown() {
        repaintTimer.stop();
    }
}
