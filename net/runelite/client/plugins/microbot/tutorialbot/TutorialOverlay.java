package net.runelite.client.plugins.microbot.tutorialbot;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class TutorialOverlay extends OverlayPanel {

    private final TutorialScript script;

    @Inject
    public TutorialOverlay(TutorialPlugin plugin, TutorialScript script) {
        super(plugin);
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!Microbot.isLoggedIn() || !script.isRunning()) {
            return null;
        }

        panelComponent.setPreferredSize(new Dimension(240, 0));
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Tutorial Island Bot")
                .color(new Color(255, 170, 0))
                .build());

        Instant start = script.getStartTime();
        if (start != null) {
            long totalSeconds = Duration.between(start, Instant.now()).getSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(timeStr)
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Stage:")
                .right(script.getCurrentStageName())
                .rightColor(Color.CYAN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Progress (Varp 281):")
                .right(String.valueOf(script.getTutorialVarp()))
                .rightColor(Color.GREEN)
                .build());

        String status = Microbot.status != null ? Microbot.status : "Running";
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Action:")
                .right(status)
                .rightColor(Color.YELLOW)
                .build());

        return super.render(graphics);
    }
}
