package dev.ultreon.bubbles.input;

import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.GamePlatform;
import dev.ultreon.bubbles.LoadedGame;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.render.gui.screen.CommandScreen;
import dev.ultreon.bubbles.render.gui.screen.Screen;
import dev.ultreon.bubbles.world.World;

public class DesktopInputHandler extends InputHandler<DesktopInput> {
    public DesktopInputHandler() {
        super(DesktopInput.get(), InputType.KeyboardAndMouse);
    }

    @Override
    public boolean tickScreen(Screen screen) {
        return true;
    }

    @Override
    public boolean tickWorld(World world, LoadedGame loadedGame) {
        if (!GamePlatform.get().isDesktop()) return false;

        if (KeyBindings.COMMAND.isJustPressed()) {
            BubbleBlaster.getInstance().showScreen(new CommandScreen());
            return true;
        }
        return false;
    }

    @Override
    public boolean tickPlayer(Player player) {
        if (!GamePlatform.get().isDesktop()) return false;

        var rotating = 0f;
        var moving = 0f;

        if (KeyBindings.FORWARD.isPressed()) moving += 1f;
        if (KeyBindings.ROTATE_LEFT.isPressed()) rotating -= 1f;
        if (KeyBindings.ROTATE_RIGHT.isPressed()) rotating += 1f;

        player.moving(moving);
        player.rotating(rotating);

        if (KeyBindings.USE_ITEM.isJustPressed()) player.useSelectedItem();
        if (KeyBindings.BOOST.isJustPressed()) player.boost();
        player.setBrake(KeyBindings.BRAKE.isPressed());
        return false;
    }
}
