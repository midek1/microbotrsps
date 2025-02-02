package net.runelite.client.plugins.microbot.pvm.magearena2;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class MageArena2Overlay extends OverlayPanel {

    @Inject
    MageArena2Overlay(MageArena2Plugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            //panelComponent.getChildren().add(TitleComponent.builder().text("MA2 V1.0.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(Microbot.status).build());
            panelComponent.getChildren().add(LineComponent.builder().left(MageArena2Script.state.toString()).build());
            panelComponent.getChildren().add(LineComponent.builder().left("Session kills: " + MageArena2Script.sessionKills).build());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
