package dev.ultreon.bubbles.render;

import com.badlogic.gdx.graphics.Color;
import dev.ultreon.libs.commons.v0.exceptions.InvalidValueException;

import java.util.regex.Pattern;

public class Colors {
    public static final Color BLACK = Colors.grayscale(0x00);
    public static final Color GRAY_0 = Colors.BLACK;
    public static final Color GRAY_1 = Colors.grayscale(0x10);
    public static final Color GRAY_2 = Colors.grayscale(0x20);
    public static final Color GRAY_3 = Colors.grayscale(0x30);
    public static final Color DARK_GRAY = Colors.grayscale(0x40);
    public static final Color GRAY_4 = Colors.DARK_GRAY;
    public static final Color GRAY_5 = Colors.grayscale(0x50);
    public static final Color GRAY_6 = Colors.grayscale(0x60);
    public static final Color GRAY_7 = Colors.grayscale(0x70);
    public static final Color GRAY = Colors.grayscale(0x80);
    public static final Color GRAY_8 = Colors.GRAY;
    public static final Color GRAY_9 = Colors.grayscale(0x90);
    public static final Color GRAY_A = Colors.grayscale(0xa0);
    public static final Color GRAY_B = Colors.grayscale(0xb0);
    public static final Color LIGHT_GRAY = Colors.grayscale(0xc0);
    public static final Color GRAY_C = Colors.LIGHT_GRAY;
    public static final Color GRAY_D = Colors.grayscale(0xd0);
    public static final Color GRAY_E = Colors.grayscale(0xe0);
    public static final Color GRAY_F = Colors.grayscale(0xf0);
    public static final Color WHITE = Colors.grayscale(0xff);
    public static final Color RED = Colors.rgb(0xff0000);
    public static final Color ORANGE = Colors.rgb(0xff8000);
    public static final Color GOLD = Colors.rgb(0xffb000);
    public static final Color YELLOW = Colors.rgb(0xffff00);
    public static final Color YELLOW_GREEN = Colors.rgb(0x80ff00);
    public static final Color GREEN = Colors.rgb(0x00ff00);
    public static final Color MINT = Colors.rgb(0x00ff80);
    public static final Color CYAN = Colors.rgb(0x00ffff);
    public static final Color AZURE = Colors.rgb(0x0080ff);
    public static final Color BLUE = Colors.rgb(0x0000ff);
    public static final Color PURPLE = Colors.rgb(0x8000ff);
    public static final Color MAGENTA = Colors.rgb(0xff00ff);
    public static final Color ROSE = Colors.rgb(0xff0080);
    public static final Color TRANSPARENT = Colors.rgba(0x00000000);
    public static final Color CRIMSON = Colors.rgb(0xdc143c);

    public static Color rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 255);
    }

    public static Color rgb(long red, long green, long blue) {
        return rgba(red, green, blue, 255);
    }

    public static Color rgb(float red, float green, float blue) {
        return rgba(red, green, blue, 1f);
    }

    public static Color rgba(int red, int green, int blue, int alpha) {
        return rgba(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    public static Color rgba(long red, long green, long blue, long alpha) {
        return rgba(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    public static Color rgba(float red, float green, float blue, float alpha) {
        return new Color(red, green, blue, alpha);
    }

    public static Color rgb(int color) {
        var rgb = (long) color % 0x100000000L;
        return Colors.rgba((rgb & 0xff0000L) >> 16, (rgb & 0x00ff00L) >> 8, rgb & 0x0000ffL, 255);
    }

    public static Color rgba(int color) {
        var rgba = (long) color % 0x100000000L;
        return Colors.rgba((rgba & 0xff000000L) >> 24, (rgba & 0x00ff0000L) >> 16, (rgba & 0x0000ff00L) >> 8, rgba & 0x000000ffL);
    }

    public static Color argb(int color) {
        var argb = (long) color % 0x100000000L;
        return Colors.rgba((argb & 0x00ff0000L) >> 16, (argb & 0x0000ff00L) >> 8, argb & 0x000000ffL, (argb & 0xff000000L) >> 24);
    }

    public static Color bgr(int color) {
        var bgr = (long) color % 0x100000000L;
        return Colors.rgba(bgr & 0x0000ffL, (bgr & 0x00ff00L) >> 8, (bgr & 0xff0000L) >> 16, 255);
    }

    public static Color bgra(int color) {
        var bgra = (long) color % 0x100000000L;
        return Colors.rgba((bgra & 0x0000ff00L) >> 8, (bgra & 0x00ff0000L) >> 16, (bgra & 0xff000000L) >> 24, bgra & 0x000000ffL);
    }

    public static Color abgr(int color) {
        var abgr = (long) color % 0x100000000L;
        return Colors.rgba(abgr & 0x000000ffL, (abgr & 0x0000ff00L) >> 8, (abgr & 0x00ff0000L) >> 16, (abgr & 0xff000000L) >> 24);
    }

    public static Color grayscale(int brightness) {
        return new Color(brightness / 255.0f, brightness / 255.0f, brightness / 255.0f, 1.0f);
    }

    public static Color grayscale(int brightness, int alpha) {
        return new Color(brightness / 255.0f, brightness / 255.0f, brightness / 255.0f, alpha / 255.0f);
    }

    public static Color hex(String hex) {
        if (Pattern.matches("#[0-9a-fA-F]{6}", hex)) {
            int rgb = Integer.valueOf(hex.substring(1), 16);
            return Colors.rgb(rgb);
        } else if (Pattern.matches("#[0-9a-fA-F]{8}", hex)) {
            int rgb = Integer.parseUnsignedInt(hex.substring(1), 16);
            return Colors.rgba(rgb);
        } else if (Pattern.matches("#[0-9a-fA-F]{3}", hex)) {
            int rgb = Integer.valueOf(new String(new char[]{
                    hex.charAt(1), hex.charAt(1),
                    hex.charAt(2), hex.charAt(2),
                    hex.charAt(3), hex.charAt(3)}), 16);
            return Colors.rgb(rgb);
        } else if (Pattern.matches("#[0-9a-fA-F]{4}", hex)) {
            int rgb = Integer.valueOf(new String(new char[]{
                    hex.charAt(1), hex.charAt(1),
                    hex.charAt(2), hex.charAt(2),
                    hex.charAt(3), hex.charAt(3),
                    hex.charAt(4), hex.charAt(4)}), 16);
            return Colors.rgba(rgb);
        } else {
            if (!hex.isEmpty()) {
                if (hex.charAt(0) != '#') {
                    throw new InvalidValueException("First character create color code isn't '#'.");
                } else if (hex.length() != 3 && hex.length() != 4 && hex.length() != 6 && hex.length() != 8) {
                    throw new InvalidValueException("Invalid hex length, should be 3, 4, 6 or 8 in length.");
                } else {
                    throw new InvalidValueException("Invalid hex value. Hex values may only contain numbers and letters a to f.");
                }
            } else {
                throw new InvalidValueException("The color hex is empty, it should start with a hex, and then 3, 4, 6 or 8 hexadecimal digits.");
            }
        }
    }

    public static Color gdx(Color color) {
        return Colors.rgba((int) (color.r * 255), (int) (color.g * 255), (int) (color.b * 255), (int) (color.a * 255));
    }

    public static Color brighter(Color color) {
        return color.add(0.2f, 0.2f, 0.2f, 0f);
    }

    public static Color darker(Color color) {
        return color.sub(0.2f, 0.2f, 0.2f, 0f);
    }
}
