package dev.illum.orbitsimulator;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.JOptionPane;
import java.text.DecimalFormat;

public class OrbitSimulator extends Application {
    public static void main(String[] args) {

        if(isJavaFxAvalaible())
            launch(args);
        else {
            JOptionPane.showMessageDialog(null, "JavaFX is missing. Aborting.");
        }
    }

    private static boolean isJavaFxAvalaible() {
        try {
            ClassLoader.getSystemClassLoader().loadClass("javafx.application.Application");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static OrbitSimulator getInstance() { return instance; };
    private static OrbitSimulator instance;

    Circle sun;
    Circle planet;
    Circle centerPoint;

    SimpleDoubleProperty centerMass = new SimpleDoubleProperty(1.989);
    SimpleIntegerProperty centerMassExp = new SimpleIntegerProperty(30);

    Ellipse ellipsePath = new Ellipse();

    final double pixelToDistance = 1E9;
    final double maxPixels = 149.6;

    double ellipse_b = MeterToPixels(AUtoM(1));
    double ellipse_a = ellipse_b;
    double ellipse_c = 0;

    double maxSpeed = 0.0;
    double minSpeed = 0.0;

    Label periodLabel = new Label("Period: ");

    // Speed
    Label maxSpeedLabel = new Label("Max Speed: ");
    Label minSpeedLabel = new Label("Min Speed: ");
    Label currentSpeedLabel = new Label("Speed: ");

    // Ecentricity
    Label eccentValLabel = new Label("0.0");
    Label eccentLabel = new Label("Eccentricity: ");
    Slider eccentSlider = new Slider(0, 1, 0);

    // Semi-major axis
    TextField semiMajorAxisField = new TextField("1.0");

    // Semi-minor axis
    Label semiMinorAxisLabel = new Label("");

    static final int yearSeconds = 31536000;

    DecimalFormat twoDecimals = new DecimalFormat("#.00");
    DecimalFormat threeDecimals = new DecimalFormat("#.000");
    DecimalFormat fourDecimals = new DecimalFormat("#.0000");
    DecimalFormat exponentFormat = new DecimalFormat("0.##E0");

    StackPane animationPane;
    PathTransition transitionPlanet;


    // Center mass fields
    TextField centerMassField;
    TextField centerMassExpField;

    @Override
    public void start(Stage primaryStage) {
        instance = this;
        sun = new Circle(5);
        planet = new Circle(10);
        centerPoint = new Circle(1);
        centerPoint.setFill(Color.RED);

        ellipsePath = new Ellipse();
        ellipsePath.setStrokeWidth(0.5);
        ellipsePath.setStroke(Color.RED);
        ellipsePath.setFill(Color.TRANSPARENT);

        ellipsePath.setRadiusY(maxPixels);
        ellipsePath.setRadiusX(maxPixels);

        transitionPlanet = new PathTransition();
        transitionPlanet.setPath(ellipsePath);
        transitionPlanet.setNode(planet);
        transitionPlanet.setInterpolator(Interpolator.LINEAR);
        transitionPlanet.setDuration(Duration.seconds(1));
        transitionPlanet.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
        transitionPlanet.setCycleCount(Timeline.INDEFINITE);
        transitionPlanet.play();

        Timeline tl = new Timeline();
        tl.getKeyFrames().setAll(new KeyFrame(Duration.millis(6), event -> {

            double distance = (Math.sqrt(Math.pow(planet.getTranslateX()/maxPixels * ellipse_a - ellipse_c, 2)
                            + Math.pow(planet.getTranslateY()/maxPixels * ellipse_a, 2))) * pixelToDistance;

            double currentSpeed = Clamp(CalculateSpeed(distance)/1000, minSpeed, maxSpeed);

            double ratio = currentSpeed/maxSpeed;
            transitionPlanet.rateProperty().setValue(ratio);

            String speedString = twoDecimals.format(currentSpeed);
            if(currentSpeed > 999 || currentSpeed < 0.1) {
                speedString = exponentFormat.format(currentSpeed);
            }

            currentSpeedLabel.setText("Speed: " + speedString + " km/s");
        }));
        tl.setCycleCount( Animation.INDEFINITE );

        GridPane root = new GridPane();

        root.setAlignment(Pos.CENTER);
        //root.setGridLinesVisible(true);
        root.setHgap(10);
        root.setVgap(10);
        root.setPadding(new Insets(5, 5, 5, 5));

        animationPane = new StackPane();

        animationPane.getChildren().add(ellipsePath);
        animationPane.getChildren().add(sun);

        animationPane.getChildren().add(planet);
        animationPane.getChildren().add(centerPoint);

        GridPane parentControlPane = new GridPane();
        GridPane controlPane = new GridPane();
        controlPane.setHgap(10);
        controlPane.setVgap(4);
        controlPane.setPadding(new Insets(10, 10, 10, 10));

        GridPane centerMassControlPane = new GridPane();
        Label centerMassLabel = new Label("Center Mass: ");
        centerMassField = new TextField(Double.toString(centerMass.getValue()));
        centerMassField.setPrefWidth(50);
        Label centerMassExpLabel = new Label("x10^");
        centerMassExpField = new TextField(Integer.toString(centerMassExp.getValue()));
        centerMassExpField.setPrefWidth(30);

        centerMassField.textProperty().addListener(new UpdateCenterMassField(centerMass, centerMassField, true));
        centerMassExpField.textProperty().addListener(new UpdateCenterMassField(centerMassExp, centerMassExpField));

        centerMassControlPane.add(centerMassLabel, 0, 0);
        centerMassControlPane.add(centerMassField, 1, 0);
        centerMassControlPane.add(centerMassExpLabel, 2, 0);
        centerMassControlPane.add(centerMassExpField, 3, 0);


        controlPane.add(centerMassControlPane, 1, 0, 2, 1);

        eccentSlider.valueProperty().addListener((ov, old_val, new_val) -> {
            PrepareAnimationReset();

            ellipse_c = Round(ellipse_a * new_val.doubleValue());

            sun.setTranslateX(ellipse_c/ellipse_a * maxPixels);

            ellipse_b = Round(Math.sqrt(ellipse_a * ellipse_a - ellipse_c * ellipse_c));

            UpdateEccentricity();

            centerPoint.setTranslateX(ellipsePath.getTranslateX());
            centerPoint.setTranslateY(ellipsePath.getTranslateY());

            ellipsePath.setRadiusY(ellipse_b/ellipse_a * maxPixels);
            ellipsePath.setRadiusX(maxPixels);
            transitionPlanet.setPath(ellipsePath);
            transitionPlanet.play();

            UpdateLabels();

        });

        semiMajorAxisField.textProperty().addListener((observable, oldValue, newValue) -> {

            double new_val;

            try {
                new_val = Double.parseDouble(newValue);
                if(new_val < 0 || new_val == 0)
                    return;
            } catch(NumberFormatException e) {
                return;
            }

            PrepareAnimationReset();

            ellipse_a = MeterToPixels(AUtoM(new_val));
            ellipse_b = ellipse_a;
            ellipse_c = 0;

            UpdateEccentricity();

            centerPoint.setTranslateX(ellipsePath.getTranslateX());
            centerPoint.setTranslateY(ellipsePath.getTranslateY());

            ellipsePath.setRadiusY(maxPixels);
            ellipsePath.setRadiusX(maxPixels);
            transitionPlanet.setPath(ellipsePath);
            transitionPlanet.play();

            UpdateLabels();
        });

        controlPane.add(periodLabel, 0, 0);
        controlPane.add(maxSpeedLabel, 0, 1);
        controlPane.add(minSpeedLabel, 0, 2);
        controlPane.add(currentSpeedLabel, 0, 3);

        controlPane.add(eccentLabel, 1, 1);
        controlPane.add(eccentSlider, 2, 1);
        controlPane.add(eccentValLabel, 3, 1);

        Button moonDefaultButton = new Button("Set Moon Values");
        moonDefaultButton.setOnAction(event -> setMoonValues());

        Button earthDefaultButton = new Button("Set Earth Values");
        earthDefaultButton.setOnAction(event -> SetEarthValues());

        Label semiMajorAxisLabel = new Label("Semi-Major Axis (AU):");

        controlPane.add(semiMajorAxisLabel, 1, 2);
        controlPane.add(semiMajorAxisField, 2, 2);

        controlPane.add(new Label("Semi-Minor Axis (AU):"), 1, 3);
        controlPane.add(semiMinorAxisLabel, 2, 3);

        controlPane.add(moonDefaultButton, 5, 0);
        controlPane.add(earthDefaultButton, 5, 1);

        parentControlPane.getChildren().add(controlPane);

        root.add(animationPane, 0, 0);
        GridPane.setHgrow(animationPane, Priority.ALWAYS);
        GridPane.setVgrow(animationPane, Priority.ALWAYS);

        root.add(parentControlPane, 0, 1);

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Orbit Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();


        UpdateLabels();
        tl.play();
    }

    public void UpdateEccentricity() {
        double eccentricity = (ellipse_c / ellipse_a);
        eccentValLabel.textProperty().setValue(fourDecimals.format(eccentricity));
        eccentSlider.setValue(eccentricity);
    }

    public void UpdateLabels() {
        double periodSeconds = Math.sqrt(
                4 * Math.PI * Math.PI * Math.pow(ellipse_a * pixelToDistance, 3)
                        / (6.67 * Math.pow(10, -11) * centerMass.getValue() * Math.pow(10, centerMassExp.getValue())) );
        double periodYear = periodSeconds/yearSeconds;

        String periodString = twoDecimals.format(periodYear);
        if(periodYear > 999 || periodYear < 0.1) {
            periodString = exponentFormat.format(periodYear);
        }
        periodLabel.setText("Period: " + periodString + " yr");

        maxSpeed = CalculateSpeed((ellipse_a - ellipse_c) * pixelToDistance)/1000;
        String maxSpeedString = twoDecimals.format(maxSpeed);
        if(maxSpeed > 999 || maxSpeed < 0.1) {
            maxSpeedString = exponentFormat.format(maxSpeed);
        }

        maxSpeedLabel.setText("Max Speed: "
                + maxSpeedString
                + " km/s"
        );

        minSpeed = CalculateSpeed((ellipse_a + ellipse_c) * pixelToDistance)/1000;
        String minSpeedString = twoDecimals.format(minSpeed);
        if(minSpeed > 999 || minSpeed < 0.1) {
            minSpeedString = exponentFormat.format(minSpeed);
        }

        minSpeedLabel.setText("Min Speed: "
                + minSpeedString
                + " km/s"
        );

        semiMinorAxisLabel.setText(MtoAU(ellipse_b * pixelToDistance) + "");
    }

    public void UpdateCenterMassFields() {
        centerMassField.setText(String.valueOf(centerMass.getValue()));
        centerMassExpField.setText(String.valueOf(centerMassExp.getValue()));
    }

    public double CalculateSpeed(double distance) {
        return Math.sqrt(
                (6.67 * Math.pow(10, -11) * centerMass.getValue() * Math.pow(10, centerMassExp.getValue())) *
                        (2.0 / (distance) - 1 / (ellipse_a * pixelToDistance)));
    }

    public double Round(double num) {
        return Math.round(num * 10000.0) / 10000.0;
    }

    public static double Clamp(double val, double min, double max) {
        return Math.min(Math.max(val, min), max);
    }

    public static double AUtoM(double au) {
        return au * 1.496 * Math.pow(10, 11);
    }
    public static double MtoAU(double m) {
        return m / (1.496 * Math.pow(10, 11));
    }

    public void PrepareAnimationReset() {
        animationPane.getChildren().remove(ellipsePath);
        ellipsePath = new Ellipse();
        ellipsePath.setStrokeWidth(0.5);
        ellipsePath.setStroke(Color.RED);
        ellipsePath.setFill(Color.TRANSPARENT);
        animationPane.getChildren().add(ellipsePath);

        transitionPlanet.stop();
    }

    public double MeterToPixels(double meters) {
        return meters/pixelToDistance;
    }

    public void SetEarthValues() {
        semiMajorAxisField.setText("1.0");
        PrepareAnimationReset();

        double eccentricity = 0.0167;

        ellipse_a = MeterToPixels(149.6E9);

        ellipse_c = Round(ellipse_a * eccentricity);

        sun.setTranslateX(ellipse_c/ellipse_a * maxPixels);

        ellipse_b = Round(Math.sqrt(ellipse_a * ellipse_a - ellipse_c * ellipse_c));

        UpdateEccentricity();

        centerPoint.setTranslateX(ellipsePath.getTranslateX());
        centerPoint.setTranslateY(ellipsePath.getTranslateY());

        ellipsePath.setRadiusY(ellipse_b/ellipse_a * maxPixels);
        ellipsePath.setRadiusX(maxPixels);
        transitionPlanet.setPath(ellipsePath);
        transitionPlanet.play();

        centerMass.set(1.989);
        centerMassExp.set(30);

        UpdateCenterMassFields();
        UpdateLabels();
    }

    public void setMoonValues() {
        semiMajorAxisField.setText("0.00257188153");
        PrepareAnimationReset();

        double eccentricity = 0.0549;

        ellipse_a = MeterToPixels(384748000);

        ellipse_c = Round(ellipse_a * eccentricity);

        sun.setTranslateX(ellipse_c/ellipse_a * maxPixels);

        ellipse_b = Round(Math.sqrt(ellipse_a * ellipse_a - ellipse_c * ellipse_c));

        UpdateEccentricity();

        centerPoint.setTranslateX(ellipsePath.getTranslateX());
        centerPoint.setTranslateY(ellipsePath.getTranslateY());

        ellipsePath.setRadiusY(ellipse_b/ellipse_a * maxPixels);
        ellipsePath.setRadiusX(maxPixels);
        transitionPlanet.setPath(ellipsePath);
        transitionPlanet.play();

        centerMass.set(5.972);
        centerMassExp.set(24);

        UpdateCenterMassFields();
        UpdateLabels();
    }
}
