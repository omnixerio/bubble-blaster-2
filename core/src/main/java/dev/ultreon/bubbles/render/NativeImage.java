package dev.ultreon.bubbles.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.bubbles.resources.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

public class NativeImage extends Texture {
    public NativeImage(byte[] bytes) {
        super(new Pixmap(bytes, 0, bytes.length));
    }

    public NativeImage(Resource resource) {
        this(resource.loadOrGet());
    }

    public NativeImage(InputStream stream) throws IOException {
        super(NativeImage.setupPixmap(new Pixmap(NativeImage.read(stream))));
        this.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.MipMapLinearLinear);
    }

    private static Pixmap setupPixmap(Pixmap pixmap) {
        pixmap.setFilter(Pixmap.Filter.BiLinear);
        return pixmap;
    }

    public NativeImage(URL url) throws IOException {
        super(NativeImage.setupPixmap(new Pixmap(NativeImage.readAndClose(url.openStream()))));
    }

    public NativeImage(File file) throws IOException {
        super(NativeImage.setupPixmap(new Pixmap(NativeImage.readAndClose(new FileInputStream(file)))));
    }

    private static ByteBuffer readAndClose(InputStream inputStream) throws IOException {
        var read = NativeImage.read(inputStream);
        inputStream.close();
        return read.flip();
    }

    private static ByteBuffer read(InputStream inputStream) throws IOException {
        return ByteBuffer.wrap(inputStream.readAllBytes()).flip();
    }
}
