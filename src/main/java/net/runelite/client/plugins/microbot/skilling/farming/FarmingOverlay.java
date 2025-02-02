package net.runelite.client.plugins.microbot.skilling.farming;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class FarmingOverlay extends OverlayPanel {

    @Inject
    FarmingOverlay(FarmingPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        //setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            panelComponent.getChildren().add(TitleComponent.builder().text("August Farming V1.0.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(Microbot.status).build());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
