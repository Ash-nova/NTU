package uk.ac.ntu.eprms.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.stage.Window;

/**
 * Small dialog helpers so each screen does not rebuild the same alerts.
 */
final class Dialogs {

    private Dialogs() {
    }

    static boolean confirm(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    static void warn(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    static HBox fieldRow(String labelText, Node field) {
        Label label = new Label(labelText);
        label.setMinWidth(120);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox row = new HBox(8, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
