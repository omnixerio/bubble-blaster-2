package dev.ultreon.bubbles.input;

import com.badlogic.gdx.Gdx;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.GamePlatform;
import dev.ultreon.bubbles.LoadedGame;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.render.gui.screen.Screen;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.libs.commons.v0.Mth;

import static dev.ultreon.bubbles.BubbleBlaster.TPS;

public class ControllerInputHandler extends InputHandler<ControllerInput> {
    private static final float DEAD_ZONE = 0.2f;
    private static final float MOUSE_SPEED = 200f;
    private final BubbleBlaster game = BubbleBlaster.getInstance();

    public ControllerInputHandler() {
        super(ControllerInput.get(), InputType.Controller);
    }

    @Override
    public boolean tickScreen(Screen screen) {
        var controller = this.input.controller;

        if (controller == null || !controller.isConnected()) return false;

        var mapping = controller.getMapping();

        var moveX = controller.getAxis(mapping.axisLeftX);
        var moveY = controller.getAxis(mapping.axisLeftY);

        if (moveX > -DEAD_ZONE && moveX < DEAD_ZONE) moveX = 0f;
        if (moveY > -DEAD_ZONE && moveY < DEAD_ZONE) moveY = 0f;

        var cursorPos = DesktopInput.getMousePos();
        if (moveX > 0 || moveY > 0) {
            cursorPos.add(moveX * MOUSE_SPEED / TPS, moveY * MOUSE_SPEED / TPS);
            cursorPos.x = Mth.clamp(cursorPos.x, 0, BubbleBlaster.getInstance().getWidth());
            cursorPos.y = Mth.clamp(cursorPos.y, 0, BubbleBlaster.getInstance().getHeight());

//            Vector2 screenCoordinates = this.game.viewport.toScreenCoordinates(cursorPos, this.game.getTransform());
//            Gdx.input.setCursorPosition((int) screenCoordinates.x, (int) screenCoordinates.y);
            Gdx.input.setCursorPosition((int) cursorPos.x, (int) cursorPos.y);
        }

        return true;
    }

    @Override
    public boolean tickWorld(World world, LoadedGame loadedGame) {
        return false;
    }

    @Override
    public boolean tickPlayer(Player player) {
        var controller = this.input.controller;

        if (controller == null || !controller.isConnected()) return false;

        var mapping = controller.getMapping();

        var moveX = controller.getAxis(mapping.axisLeftX);
        var moveY = controller.getAxis(mapping.axisLeftY);

        if (GamePlatform.get().isDesktop()) {
            var triggerL = controller.getAxis(4);
            var triggerR = controller.getAxis(5);
            if (triggerL < DEAD_ZONE) triggerL = 0;
            if (triggerR < DEAD_ZONE) triggerR = 0;
            if (triggerL > 0) {
                player.moving(triggerL);
            } else if (triggerR > 0.5f) {
                player.setBrake(true);
            } else if (triggerR <= 0.5f) {
                player.setBrake(false);
            }
        }

        if (moveX > -DEAD_ZONE && moveX < DEAD_ZONE) moveX = 0f;

        player.rotating(moveX);

        if (controller.getButton(mapping.buttonA)) player.useSelectedItem();
        if (controller.getButton(mapping.buttonB)) player.boost();

        return true;
    }
}
