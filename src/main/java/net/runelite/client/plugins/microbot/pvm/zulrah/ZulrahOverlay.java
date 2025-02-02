package net.runelite.client.plugins.microbot.pvm.zulrah;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.sound.sampled.Line;
import java.awt.*;

public class ZulrahOverlay extends OverlayPanel {

    @Inject
    ZulrahOverlay(ZulrahPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(129, 0));
            panelComponent.getChildren().add(TitleComponent.builder().text("Zulrah V1.0").color(Color.GREEN).build());
            panelComponent.getChildren().add(LineComponent.builder().left(Microbot.status).build());
            panelComponent.getChildren().add(LineComponent.builder().left(String.valueOf(ZulrahScript.state)).build());
            panelComponent.getChildren().add(LineComponent.builder().left("Session kills: " + ZulrahScript.sessionKills).build());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
