package model;

import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

public class RobotModel extends Observable {
    private double x = 100;
    private double y = 100;
    private double direction = 0;
    private double targetX = 150;
    private double targetY = 100;
    private final Timer timer = new Timer("robot_timer", true);

    public RobotModel() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updatePosition(0.1);
                setChanged();
                notifyObservers();
            }
        }, 0, 30);
    }

    private void updatePosition(double duration) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance < 0.5) return;

        direction = Math.atan2(dy, dx);
        x += 10 * Math.cos(direction) * duration;
        y += 10 * Math.sin(direction) * duration;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getDirection() { return direction; }
    public int getTargetX() { return (int)targetX; }
    public int getTargetY() { return (int)targetY; }

    public void setTarget(double tx, double ty) {
        targetX = tx;
        targetY = ty;
    }

    public void shutdown() {
        timer.cancel();
    }
}