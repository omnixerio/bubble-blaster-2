package dev.ultreon.bubbles.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Shape2D;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.GamePlatform;
import dev.ultreon.bubbles.LoadedGame;
import dev.ultreon.bubbles.entity.player.Player;
import dev.ultreon.bubbles.render.gui.screen.Screen;
import dev.ultreon.bubbles.world.World;
import dev.ultreon.libs.commons.v0.Mth;

public class MobileInputHandler extends InputHandler<MobileInput> {
    public boolean shoot = false;
    public boolean boost = false;
    public boolean brake = false;

    public MobileInputHandler() {
        super(MobileInput.get(), InputType.TouchScreen);
    }

    @Override
    public boolean tickScreen(Screen screen) {
        return true;
    }

    @Override
    public boolean tickWorld(World world, LoadedGame loadedGame) {
        if (!GamePlatform.get().isMobile())
            return false;

        var game = BubbleBlaster.getInstance();
        if (MobileInput.isTouchDown() && this.getPauseBtnRegion().contains(MobileInput.getTouchPos()))
            game.pauseGame();

        return true;
    }

    @Override
    public boolean tickPlayer(Player player) {
        if (!GamePlatform.get().isMobile()) return false;

        var touchPressure = MobileInput.getTouchPressure();
        if (touchPressure < 0.1) touchPressure = 0;
        player.moving(Mth.clamp(touchPressure, 0, 1));
        player.setRotation(player.getAngleTo(MobileInput.getTouchPos()));

        this.shoot = this.getShootBtnRegion().contains(MobileInput.getTouchPos(1));

        if (this.shoot) {
            this.shoot = false;
            player.useSelectedItem();
        }
        if (this.boost) {
            this.boost = false;
            player.boost();
        }
        player.setBrake(this.brake);
        return false;
    }

    public Shape2D getShootBtnRegion() {
        return new Circle(BubbleBlaster.getInstance().getWidth() - 100, BubbleBlaster.getInstance().getHeight() - 100, 100);
    }

    public Shape2D getPauseBtnRegion() {
        return new Circle(BubbleBlaster.getInstance().getWidth() / 2f, 100, 100);
    }
}
