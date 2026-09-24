package viz.fxpanel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ExpandablePanel extends VBox {

    private static Image DOWN_ICON;
    private static Image LEFT_ICON;

    static {
        try {
            var downURL = ExpandablePanel.class.getResource("/viz/icons/down.png");
            if (downURL != null) DOWN_ICON = new Image(downURL.toExternalForm());

            var leftURL = ExpandablePanel.class.getResource("/viz/icons/left.png");
            if (leftURL != null) LEFT_ICON = new Image(leftURL.toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Node m_panel;
    public String m_sLabel;
    public Button editButton;
    private final ImageView iconView = new ImageView();

    public ExpandablePanel(String sLabel, Node panel) {
        this(sLabel, panel, false);
    }

    public ExpandablePanel(String sLabel, Node panel, boolean visible) {
        this.m_sLabel = sLabel;
        this.m_panel = panel;

        setSpacing(4);
        setPadding(new Insets(2));

        String name = panel.getClass().getSimpleName();
        setId(name);

        // Configure Edit/Toggle Button
        editButton = new Button(sLabel);
        editButton.setId(name + "Button");
        editButton.setAlignment(Pos.CENTER_LEFT);
        editButton.setMaxWidth(Double.MAX_VALUE);

        // Icon setup
        iconView.setFitWidth(12);
        iconView.setFitHeight(12);
        iconView.setPreserveRatio(true);
        editButton.setGraphic(iconView);

        // Configure panel visibility & managed state
        // In JavaFX, managed must be bound to visible so collapsed components don't take layout space
        panel.managedProperty().bind(panel.visibleProperty());
        setOpen(visible);

        if (panel instanceof Region region) {
            region.setStyle("-fx-border-color: gray; -fx-border-width: 1px;");
            VBox.setVgrow(region, Priority.ALWAYS);
        }

        editButton.setOnAction(e -> setOpen(!panel.isVisible()));

        getChildren().addAll(editButton, panel);
    }

    public void setOpen(boolean isOpen) {
        m_panel.setVisible(isOpen);
        if (iconView != null) {
            iconView.setImage(isOpen ? DOWN_ICON : LEFT_ICON);
        }
    }

    public boolean isOpen() {
        return m_panel.isVisible();
    }
}