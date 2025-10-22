package gui;

import model.RobotModel;

import javax.swing.*;
import java.util.Observable;
import java.util.Observer;

public class RobotCoordinatesWindow extends JInternalFrame implements Observer {
    private final JLabel coordsLabel;
    private final LocalizationManager localizationManager;

    public RobotCoordinatesWindow(RobotModel model, WindowManager windowManager) {

        super("", true, true, true, true);

        localizationManager = LocalizationManager.getInstance(windowManager);

        putClientProperty("translationKey", "coordinates.window.title");

        coordsLabel = new JLabel();
        add(coordsLabel);
        model.addObserver(this);
        setSize(200, 100);
        setLocation(100, 100);
        setVisible(true);

        update(null, null); // Инициализация текста
        localizationManager.updateUI(this);
    }


    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof RobotModel model) {
            coordsLabel.setText(String.format("x: %.2f, y: %.2f", model.getX(), model.getY()));
        }
    }
}