package dev.illum.orbitsimulator;

import javafx.beans.binding.NumberExpressionBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

public class UpdateCenterMassField implements ChangeListener<String> {

    private NumberExpressionBase value;
    private TextField parent;
    private boolean allowDecimal = false;

    public UpdateCenterMassField(NumberExpressionBase value, TextField parent) {
        this.parent = parent;
        this.value = value;
    }

    public UpdateCenterMassField(NumberExpressionBase value, TextField parent, boolean allowDecimal) {
        this.value = value;
        this.parent = parent;
        this.allowDecimal = allowDecimal;
    }

    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

        try {
            if (allowDecimal) {
                if (!newValue.matches("[\\d.]*"))
                    parent.setText(newValue.replaceAll("[^\\d.]", ""));
                if (validChange(parent.getText()))
                    ((DoubleProperty) this.value).set(Double.parseDouble(parent.getText()));
            } else {
                if (!newValue.matches("[\\d]*"))
                    parent.setText(newValue.replaceAll("[^\\d]", ""));
                if (validChange(parent.getText()))
                    ((IntegerProperty) this.value).set(Integer.parseInt(parent.getText()));
            }

            OrbitSimulator.getInstance().UpdateLabels();
        } catch (java.lang.NumberFormatException e) {}
    }

    private boolean validChange(String num) {
        return !num.equals("") || !num.startsWith("0");
    }
}
