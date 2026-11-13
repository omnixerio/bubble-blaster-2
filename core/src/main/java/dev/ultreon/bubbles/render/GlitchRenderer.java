package dev.ultreon.bubbles.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.init.Fonts;

import java.security.SecureRandom;
import java.util.Random;

public class GlitchRenderer {
    private final char[] chars = " ~!@#$%^&*()_+{}|:\"<>?,./;'[]\\-=`1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private int matrixW;
    private int matrixH;
    private final BitmapFont font = Fonts.GAME_GLITCH.get();
    private char[][] matrix;
    private int[][] cMatrix;
    private final int charW;
    private int charH;

    public GlitchRenderer(BubbleBlaster game) {
        var a = this.font.getData().getGlyph('A');
        if (a == null)
            this.charW = 1;
        else this.charW = a.width + 4;
        this.charH = (int) this.font.getData().lineHeight;
        if (this.charH <= 0)
            this.charH = 1;
        this.matrixW = game.getWidth() / this.charW;
        this.matrixH = game.getHeight() / this.charH;
        this.matrix = new char[this.matrixW][this.matrixH];
        this.cMatrix = new int[this.matrixW][this.matrixH];
    }

    public void resize(int width, int height) {
        this.matrixW = width / this.charW;
        this.matrixH = height / this.charH;
        this.matrix = new char[this.matrixW][this.matrixH];
        this.cMatrix = new int[this.matrixW][this.matrixH];
    }

    public void tick() {
    }

    public void addChar(char c, int x, int y, int color) {
        this.matrix[x][y] = c;
        this.cMatrix[x][y] = color;
    }

    private char randomChar() {
        return this.chars[new SecureRandom().nextInt(this.chars.length)];
    }

    public void render(Renderer renderer) {
        var rand = new SecureRandom();
        this.addChar(this.randomChar(), rand.nextInt(this.matrixW), rand.nextInt(this.matrixH), new Random().nextInt(0xffffff));

        for (var x = 0; x < this.matrixW; x++) {
            for (var y = 0; y < this.matrixH; y++) {
                var matrix = this.matrix[x][y];
                var xi = x * this.charW;
                var yi = y * this.charH;

                renderer.setColor("#000");
                if (matrix != 0) {
                    renderer.fill(xi, yi, this.charW, this.charH, Colors.BLACK);
                }

                renderer.drawText(this.font, Character.toString(matrix), xi + 2, yi, Colors.rgb(this.cMatrix[x][y]));
//                renderer.text("" + matrix, xi + 2, yi + charH / 1.5f + 4);
            }
        }
    }
}
