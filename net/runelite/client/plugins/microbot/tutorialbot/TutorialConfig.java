package net.runelite.client.plugins.microbot.tutorialbot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tutorialbot")
public interface TutorialConfig extends Config {

    @ConfigItem(
            keyName = "characterName",
            name = "Desired Character Name",
            description = "Optional custom name. If blank, a random valid name will be generated.",
            position = 0
    )
    default String characterName() {
        return "";
    }

    @ConfigItem(
            keyName = "autoAcceptMainland",
            name = "Teleport to Mainland",
            description = "Automatically accept teleportation to Lumbridge upon completion.",
            position = 1
    )
    default boolean autoAcceptMainland() {
        return true;
    }
}
