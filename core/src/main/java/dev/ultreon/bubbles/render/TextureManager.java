package dev.ultreon.bubbles.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.bubbles.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureManager {
    private static final TextureManager instance = new TextureManager();

    @SuppressWarnings("GDXJavaStaticResource")
    public static final Texture DEFAULT_TEX;

    static {
        var pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
        pixmap.fill();
        pixmap.setColor(1, 1, 1, 1);
        pixmap.drawPixel(0, 0, 0xffffffff);
        pixmap.drawPixel(1, 1, 0xffffffff);

        DEFAULT_TEX = new Texture(pixmap);

        pixmap.dispose();
    }
    private final Map<Identifier, Texture> textureMap = new ConcurrentHashMap<>();

    public static TextureManager instance() {
        return instance;
    }

    private TextureManager() {

    }

    public Texture getTexture(Identifier entry) {
        this.textureMap.get(entry);
        return this.textureMap.get(entry);
    }

    public Texture getOrLoadTexture(Identifier entry) {
        if (this.textureMap.containsKey(entry)) {
            return this.textureMap.get(entry);
        }

        return this.loadTexture(entry, new TextureSource() {
            @Override
            public Texture create() {
                @NotNull ResourceManager resourceManager = BubbleBlaster.getInstance().getResourceManager();
                var resource = resourceManager.getResource(entry.withPath("textures/" + entry.path() + ".png"));
                if (resource == null) {
                    return DEFAULT_TEX;
                }
                return new NativeImage(resource);
            }
        });
    }

    public Texture loadTexture(Identifier entry, TextureSource source) {
        var texture = source.create();
        this.textureMap.put(entry, texture);
        return texture;
    }
}
