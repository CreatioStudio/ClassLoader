package vip.creatio.cloader.msg.json;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class TextColor {

    public static final Map<String, TextColor> reg = new HashMap<>();

    //Vanilla
    public static final TextColor BLACK = new TextColor("black", "#000000", '0');
    public static final TextColor DARK_BLUE = new TextColor("dark_blue", "#0000aa", '1');
    public static final TextColor DARK_GREEN = new TextColor("dark_green", "#00aa00", '2');
    public static final TextColor DARK_AQUA = new TextColor("dark_aqua", "#00aaaa", '3');
    public static final TextColor DARK_RED = new TextColor("dark_red", "#aa0000", '4');
    public static final TextColor DARK_PURPLE = new TextColor("dark_purple", "#aa00aa", '5');
    public static final TextColor GOLD = new TextColor("gold", "#ffaa00", '6');
    public static final TextColor GRAY = new TextColor("gray", "#aaaaaa", '7');
    public static final TextColor DARK_GRAY = new TextColor("dark_gray", "#555555", '8');
    public static final TextColor BLUE = new TextColor("blue", "#5555ff", '9');
    public static final TextColor GREEN = new TextColor("green", "#55ff55", 'a');
    public static final TextColor AQUA = new TextColor("aqua", "#55ffff", 'b');
    public static final TextColor RED = new TextColor("red", "#ff5555", 'c');
    public static final TextColor LIGHT_PURPLE = new TextColor("light_purple", "#ff55ff", 'd');
    public static final TextColor YELLOW = new TextColor("yellow", "#ffff55", 'e');
    public static final TextColor WHITE = new TextColor("white", "#ffffff", 'f');

    //Format code
    public static final TextColor BOLD = new TextColor("bold", 'l');
    public static final TextColor STRIKETHROUGH = new TextColor("strikethrough", 'm');
    public static final TextColor UNDERLINE = new TextColor("underline", 'n');
    public static final TextColor ITALIC = new TextColor("italic", 'o');
    public static final TextColor OBFUSCATED = new TextColor("obfuscated", 'k');
    public static final TextColor RESET = new TextColor("reset", 'r');

    private String code;
    private String hex;
    private char id = 'r';

    private int red;
    private int green;
    private int blue;

    public static final String colorCodePattern = "(§x(§[0-9a-fA-F]){6}|(§[0-9a-fA-FklnmorKLNMOR])+)";
    public static final Pattern colorCode = Pattern.compile("(§x(§[0-9a-fA-F]){6}|(§[0-9a-fA-FklnmorKLNMOR])+)");
    private static final Pattern pattern = Pattern.compile("^#([0-9a-fA-F]{6})$");
    private static final Pattern hexCode = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern normalCode = Pattern.compile("(§[0-9a-fA-FklnmorKLNMOR])+");

    public static final Map<String, TextColor> vanilla;
    public static final Map<String, TextColor> format;

    static {
        Map<String, TextColor> m1 = new HashMap<>();
        m1.put("BLACK", BLACK);
        m1.put("DARK_BLUE", DARK_BLUE);
        m1.put("DARK_GREEN", DARK_GREEN);
        m1.put("DARK_AQUA", DARK_AQUA);
        m1.put("DARK_RED", DARK_RED);
        m1.put("DARK_PURPLE", DARK_PURPLE);
        m1.put("GOLD", GOLD);
        m1.put("GRAY", GRAY);
        m1.put("DARK_GRAY", DARK_GRAY);
        m1.put("BLUE", BLUE);
        m1.put("GREEN", GREEN);
        m1.put("AQUA", AQUA);
        m1.put("RED", RED);
        m1.put("LIGHT_PURPLE", LIGHT_PURPLE);
        m1.put("YELLOW", YELLOW);
        m1.put("WHITE", WHITE);
        vanilla = Collections.unmodifiableMap(m1);

        Map<String, TextColor> m2 = new HashMap<>();
        m2.put("BOLD", BOLD);
        m2.put("ITALIC", ITALIC);
        m2.put("STRIKETHROUGH", STRIKETHROUGH);
        m2.put("UNDERLINE", UNDERLINE);
        m2.put("OBFUSCATED", OBFUSCATED);
        m2.put("RESET", RESET);
        format = Collections.unmodifiableMap(m2);
    }

    //Vanilla
    private TextColor(String code, String hex, char id) {
        this.code = code;
        this.id = id;
        this.hex = hex;
        Color c = Color.decode(hex);
        this.red = c.getRed();
        this.green = c.getGreen();
        this.blue = c.getBlue();
        reg.put(this.code.toUpperCase(), this);
    }

    //Format
    private TextColor(String code, char id) {
        this.code = code;
        this.id = id;
        this.hex = null;
        reg.put("", this);
    }

    //Custom preset
    public TextColor(String code, String hex) {
        this.code = code;
        this.hex =  hex;
        Color c = Color.decode(hex);
        this.red = c.getRed();
        this.green = c.getGreen();
        this.blue = c.getBlue();
        reg.put(this.code.toUpperCase(), this);
    }

    //Temp color
    public TextColor(String clr) {
        this.code = null;
        Matcher m = pattern.matcher(clr);
        if (m.find()) {
            this.hex = clr;
        } else {
            boolean b = false;
            for (TextColor c : vanilla.values()) {
                if (c.hex.equalsIgnoreCase(clr)) {
                    this.code = c.code;
                    this.hex = c.hex;
                    b = true;
                    break;
                }
            }
            if (!b) {
                for (TextColor c : reg.values()) {
                    if (c.code.equalsIgnoreCase(clr)) {
                        this.code = c.code;
                        this.hex = c.hex;
                        break;
                    }
                }
            }
        }
        if (this.hex != null) {
            Color c = Color.decode(hex);
            this.red = c.getRed();
            this.green = c.getGreen();
            this.blue = c.getBlue();
            return;
        }
        throw new RuntimeException("No such registered color: " + clr);
    }

    //Convert color in json to String type, e.g #123456 --> §x§1§2§3§4§5§6
    public static String hexToColorCode(String jsonColor) {
        Matcher m = pattern.matcher(jsonColor);
        if (m.find()) {
            StringBuilder b = new StringBuilder("§x");
            for (char c : m.group(1).toCharArray()) {
                b.append("§").append(c);
            }
            return b.toString();
        } else {
            throw new RuntimeException("No such color in Minecraft: " + jsonColor);
        }
    }

    public String hexToColorCode() {
        return hexToColorCode(this.hex);
    }

    public static String colorCodeToHex(String ori) {
        Matcher mt = hexCode.matcher(ori);
        if (mt.find()) {
            return '#' + ori.substring(3, 15).replaceAll("§", "");
        }
        throw new RuntimeException("No such color: " + ori);
    }

    //Convert Hex to nearest vanilla color code
    public static String hexToVanilla(String hex) {
        Matcher m = pattern.matcher(hex);
        if (m.find()) {
            return nearestHEX(hex, vanilla);
        } else {
            for (TextColor t : vanilla.values()) {
                if (t.code.equals(hex)) return nearestHEX(t.hex, vanilla);
            }
        }
        throw new RuntimeException("No such color in Minecraft: " + hex);
    }
    public String hexToVanilla() {
        return hexToVanilla(this.hex);
    }

    //Convert Hex to nearest REGISTERED color code(which contains vanilla and custom)
    public static String hexToNearest(String hex) {
        return nearestHEX(hex, reg);
    }
    public String hexToNearest() {
        return nearestHEX(this.hex, reg);
    }

    private static String nearestHEX(String hex, Map<String, TextColor> range) {
        Color c2 = Color.decode(hex);
        TextColor smallest = RESET;
        int     c,
                c1 = 2147483647;
        int     r = c2.getRed(),
                g = c2.getGreen(),
                b = c2.getBlue();

        for (TextColor clr : range.values()) {
            if (clr.hex != null && clr.id != 'r') {
                c = Math.abs(clr.red - r) + Math.abs(clr.green - g) + Math.abs(clr.blue - b);
                if (c < c1) {
                    smallest = clr;
                    c1 = c;
                }
                else c = c1;
            }
        }
        return smallest.getColorCode();
    }

    String getColorCode() {
        if (this.id != 'r') return "§" + this.id;
        else if (this.hex != null) return this.hexToColorCode();
        else return "r";
    }

    public String getCode() {
        return this.code;
    }

    public String getHex() {
        return hex;
    }

    public char getId() {
        return id;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public static TextColor[] values() {
        return reg.values().toArray(new TextColor[0]);
    }

    public static TextColor valueOf(String name) {
        if (reg.containsKey(name)) {
            return reg.get(name);
        }
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
                "No color constant " + name);
    }
}
