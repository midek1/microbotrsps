package net.runelite.client.plugins.microbot.skilling.fishing;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class FishingOverlay extends OverlayPanel {

    @Inject
    FishingOverlay(FishingPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            panelComponent.getChildren().add(TitleComponent.builder().text("Fisher V1.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(Microbot.status).build());


        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
