package net.runelite.client.plugins.microbot.skilling.chopper;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ChopperOverlay extends OverlayPanel {

    @Inject
    ChopperOverlay(ChopperPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            panelComponent.getChildren().add(TitleComponent.builder().text("Chopper 1.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(Microbot.status).build());


        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
