package dev.ultreon.bubbles.input;

import com.badlogic.gdx.Input.Keys;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.input.KeyBinding.Type;
import dev.ultreon.bubbles.registry.Registries;

public class KeyBindings {
    public static KeyBinding FORWARD = KeyBindings.register("forward", new KeyBinding(Keys.W, Type.KEYBOARD));
    public static KeyBinding ROTATE_LEFT = KeyBindings.register("rotate_left", new KeyBinding(Keys.A, Type.KEYBOARD));
    public static KeyBinding ROTATE_RIGHT = KeyBindings.register("rotate_right", new KeyBinding(Keys.D, Type.KEYBOARD));
    public static KeyBinding USE_ITEM = KeyBindings.register("use_item", new KeyBinding(Keys.SPACE, Type.KEYBOARD));
    public static KeyBinding BOOST = KeyBindings.register("boost", new KeyBinding(Keys.SHIFT_RIGHT, Type.KEYBOARD));
    public static KeyBinding BRAKE = KeyBindings.register("brake", new KeyBinding(Keys.S, Type.KEYBOARD));
    public static KeyBinding COMMAND = KeyBindings.register("command", new KeyBinding(Keys.SLASH, Type.KEYBOARD));

    private static KeyBinding register(String name, KeyBinding value) {
        Registries.KEY_BINDINGS.register(BubbleBlaster.id(name), value);
        return value;
    }
}
