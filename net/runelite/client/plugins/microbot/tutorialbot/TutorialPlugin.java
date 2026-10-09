package net.runelite.client.plugins.microbot.tutorialbot;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Tutorial Island Bot",
        configName = "tutorialislandbot",
        description = "Automates Tutorial Island from character creation to mainland Lumbridge",
        tags = {"tutorial", "island", "microbot", "quest", "starter", "bot"},
        enabledByDefault = false
)
public class TutorialPlugin extends Plugin {

    @Inject
    private TutorialScript tutorialScript;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private TutorialOverlay tutorialOverlay;

    @Inject
    private TutorialConfig config;

    @Provides
    TutorialConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TutorialConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        Microbot.log("[TutorialBot] Tutorial Island Bot started");
        if (overlayManager != null && tutorialOverlay != null) {
            overlayManager.add(tutorialOverlay);
        }
        tutorialScript.run(this, config);
    }

    @Override
    protected void shutDown() {
        Microbot.log("[TutorialBot] Tutorial Island Bot stopped");
        if (overlayManager != null && tutorialOverlay != null) {
            overlayManager.remove(tutorialOverlay);
        }
        tutorialScript.shutdown();
    }
}
