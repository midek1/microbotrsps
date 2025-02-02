package net.runelite.client.plugins.microbot.pvm.slayer;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class SlayerOverlay extends OverlayPanel {

    @Inject
    SlayerOverlay(SlayerPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            panelComponent.getChildren().add(TitleComponent.builder().text("AugSlayer 1.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(String.valueOf(SlayerScript.getState())).build());
            panelComponent.getChildren().add(LineComponent.builder().left("Task: " + Microbot.slayerTask).build());
            panelComponent.getChildren().add(LineComponent.builder().left("Left: " + Microbot.taskRemaining + ", Streak: " + Microbot.slayerStreak).build());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
