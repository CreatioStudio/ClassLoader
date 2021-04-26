package vip.creatio.cloader.msg.json;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

//My first time using this strange, weird but useful "abstract class".
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class Json implements JsonBase {

    public abstract JsonType.ContentType getContentType();

    public abstract String getContentJSON();

    private String color = null;
    private String font = null;

    private Boolean bold = null;
    private Boolean italic = null;
    private Boolean underlined = null;
    private Boolean strikethrough = null;
    private Boolean obfuscated = null;

    private String insertion = null;

    private Clickable clickevent = null;
    private Hoverable hoverevent = null;

    private JsonList extra = new JsonList();

    protected Json(String color, String font, Boolean bold, Boolean italic, Boolean underlined,
                Boolean strikethrough, Boolean obfuscated, String insertion, Clickable clickevent,
                Hoverable hoverevent, JsonList extra) {
        this.color = color;
        this.font = font;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.insertion = insertion;
        this.clickevent = clickevent;
        this.hoverevent = hoverevent;
        this.extra = extra;
    }

    protected Json() {}

    @Override
    public JsonType getType() {
        return JsonType.PARAGRAPH;
    }

    //Convert json paragraph to string
    public String getJSON() {
        StringBuilder text = new StringBuilder('{' + getContentJSON());
        if (color != null) text.append(",\"color\":\"").append(color).append('"');
        if (font != null) text.append(",\"font\":\"").append(font).append('"');
        if (bold != null) text.append(",\"bold\":").append(bold.toString());
        if (italic != null) text.append(",\"italic\":").append(italic.toString());
        if (underlined != null) text.append(",\"underlined\":").append(underlined.toString());
        if (strikethrough != null) text.append(",\"strikethrough\":").append(strikethrough.toString());
        if (obfuscated != null) text.append(",\"obfuscated\":").append(obfuscated.toString());
        if (insertion != null) text.append(",\"insertion\":\"").append(insertion).append('"');
        if (clickevent != null) text.append(',').append(clickevent.getJSON());
        if (hoverevent != null) text.append(',').append(hoverevent.getJSON());
        if (!extra.isEmpty()) text.append(",\"extra\":").append(extra.getJSON());
        text.append('}');
        return text.toString();
    }

    public static Json empty() {
        return new PlainText();
    }

    public static Json fromJSON(String rawtext) {
        return deserialize(StringUtil.parseMap(rawtext));
    }

    //Extract json from string
    @SuppressWarnings("unchecked")
    public static Json deserialize(Map<String, Object> map) {
        Json j = null;

        try {
            //Content deserialize
            if (map.get("text") != null) {
                j = new PlainText();
                ((PlainText) j).setText((String) map.get("text"));
            } else if (map.get("translate") != null) {
                j = new TranslatedText();
                ((TranslatedText) j).setTranslate((String) map.get("translate"));
                if (map.get("with") != null) {
                    if (map.get("with") instanceof ArrayList) {
                        List<Object> list = (ArrayList<Object>) map.get("with");
                        List<JsonBase> l = new ArrayList<>();
                        for (Object s : list) {
                            if (s instanceof Map) l.add(deserialize((Map<String, Object>) s));
                            else if (s instanceof List) l.add(new JsonList((List<Object>) s));
                            else l.add(JsonBase.toBase((String) s));
                        }
                        ((TranslatedText) j).setArgs(l);
                    }
                }
            } else if (map.get("score") != null) {
                j = new ScoreboardValue();
                if (map.get("score") instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) map.get("score");
                    ((ScoreboardValue) j).setEntry((String) m.get("name"));
                    ((ScoreboardValue) j).setObjective((String) m.get("objective"));
                    if (m.get("value") != null) ((ScoreboardValue) j).setValue((String) m.get("value"));
                }
            } else if (map.get("selector") != null) {
                j = new EntityNames();
                ((EntityNames) j).setSelector((String) map.get("selector"));
            } else if (map.get("keybind") != null) {
                j = new Keybind();
                ((Keybind) j).setKeybingNode((String) map.get("keybind"));
            }/* else if (map.get("nbt") != null) {
                j = new NBTValue();
                if (map.get("entity") != null) {
                    ((NBTValue) j).setMode(NBTMode.ENTITY);
                    ((NBTValue) j).setPath((String) map.get("nbt"));
                    ((NBTValue) j).setSelector((String) map.get("entity"));
                } else if (map.get("storage") != null) {
                    ((NBTValue) j).setMode(NBTMode.STORAGE);
                    ((NBTValue) j).setPath((String) map.get("nbt"));
                    ((NBTValue) j).setSelector((String) map.get("storage"));
                } else if (map.get("block") != null) {
                    ((NBTValue) j).setMode(NBTMode.BLOCK);
                    ((NBTValue) j).setPath((String) map.get("nbt"));
                    ((NBTValue) j).setSelector((String) map.get("block"));
                }
                if (map.get("interpret") != null) {
                    ((NBTValue) j).setInterpret((Boolean) map.get("interpret"));
                }
            }*/

            assert j != null;

            //Attribute deserialize
            if (map.get("color") != null) j.color((String) map.get("color"));
            if (map.get("font") != null) j.font((String) map.get("font"));
            if (map.get("bold") != null) j.bold((Boolean) map.get("bold"));
            if (map.get("italic") != null) j.italic((Boolean) map.get("italic"));
            if (map.get("underlined") != null) j.underlined((Boolean) map.get("underlined"));
            if (map.get("strikethrough") != null) j.strikethrough((Boolean) map.get("strikethrough"));
            if (map.get("obfuscated") != null) j.obfuscated((Boolean) map.get("obfuscated"));
            if (map.get("insertion") != null) j.insertion((String) map.get("insertion"));

            //ClickEvent deserialize
            if (map.get("clickEvent") != null) {
                if (map.get("clickEvent") instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) map.get("clickEvent");
                    if (m.get("action") != null && m.get("value") != null) {
                        if (((String) m.get("action")).equalsIgnoreCase("run_command")) j.setClickEvent(new Clickable.RunCommand((String) m.get("value")));
                        else if (((String) m.get("action")).equalsIgnoreCase("open_url")) j.setClickEvent(new Clickable.OpenURL((String) m.get("value")));
                        else if (((String) m.get("action")).equalsIgnoreCase("suggest_command")) j.setClickEvent(new Clickable.SuggestCommand((String) m.get("value")));
                        else if (((String) m.get("action")).equalsIgnoreCase("change_page")) j.setClickEvent(new Clickable.ChangePage(Integer.parseInt((String) m.get("value"))));
                        else if (((String) m.get("action")).equalsIgnoreCase("copy_to_clipboard")) j.setClickEvent(new Clickable.CopyToClipboard((String) m.get("value")));
                    }
                }
            }

            //HoverEvent deserialize
            if (map.get("hoverEvent") != null) {
                if (map.get("hoverEvent") instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) map.get("hoverEvent");
                    if (m.get("action") != null && m.get("value") != null) {
                        if (((String) m.get("action")).equalsIgnoreCase("show_text")) {
                            if (m.get("value") instanceof ArrayList) {
                                List<Object> list = (List<Object>) map.get("value");
                                List<JsonBase> l = new ArrayList<>();
                                for (Object s : list) {
                                    l.add(toBase(s.toString()));
                                }
                                j.setHoverEvent(new Hoverable.ShowText(l));
                            }
                        }/* else if (((String) m.get("action")).equalsIgnoreCase("show_item")) {
                            j.setHoverEvent(new Hoverable.ShowItem(new NBTItem(new NBTCompound((String) m.get("value")))));
                        } else if (((String) m.get("action")).equalsIgnoreCase("show_entity")) {
                            j.setHoverEvent(new Hoverable.ShowEntity((String) m.get("value")));
                        }*/
                    }
                }
            }
        } catch (Exception ignored) {}

        return j;
    }

    /*
     * Convert raw text to single line text
     * Everything that uses client resources will be wiped, etc font, hover/click event, insertion and local translation.
     * Only PlainText and TranslatedText are accepted.
     */
    String singleLine(TextColor... inherit) {
        if (getContentType() == JsonType.ContentType.PlainText || getContentType() == JsonType.ContentType.TranslatedText) {
            StringBuilder sb = new StringBuilder();
            String a;
            boolean b = false, u = false, s = false, i = false, o = false, r = false;
            List<TextColor> inh = new ArrayList<>();

            if (inherit != null) {
                for (TextColor ih : inherit) {
                    switch (ih.getColorCode()) {
                        case "§l":
                            b = true;
                            break;
                        case "§o":
                            i = true;
                            break;
                        case "§n":
                            u = true;
                            break;
                        case "§m":
                            s = true;
                            break;
                        case "§k":
                            o = true;
                            break;
                    }
                }
            }

            if (color != null) {
                inh.add(new TextColor(this.color));
                sb.insert(0, TextColor.hexToVanilla(this.color));
                r = true;
            }
            if (bold != null) {
                if (!r && b && !bold) {
                    r = true;
                }
                if (bold) {
                    sb.append("§l");
                    inh.add(TextColor.BOLD);
                }
            }
            if (underlined != null) {
                if (!r && u && !underlined) {
                    r = true;
                }
                if (underlined && !u) {
                    sb.append("§n");
                    inh.add(TextColor.UNDERLINE);
                }
            }
            if (strikethrough != null) {
                if (!r && s && !strikethrough) {
                    r = true;
                }
                if (strikethrough && !s) {
                    sb.append("§m");
                    inh.add(TextColor.STRIKETHROUGH);
                }
            }
            if (italic != null) {
                if (!r && i && !italic) {
                    r = true;
                }
                if (italic && !i) {
                    sb.append("§o");
                    inh.add(TextColor.ITALIC);
                }
            }

            if (obfuscated != null) {
                if (!r && o && !obfuscated) {
                    r = true;
                }
                if (obfuscated && !i) {
                    sb.append("§k");
                    inh.add(TextColor.OBFUSCATED);
                }
            }

            if (r && color == null) sb.insert(0, "§r");

            switch (getContentType()) {
                case PlainText:
                    sb.append(((PlainText) this).content);
                    break;
                case TranslatedText:
                    sb.append(((TranslatedText) this).content);
                    int ii = 1;
                    a = sb.toString();
                    for (JsonBase n : ((TranslatedText) this).args.listAll()) {
                        if (ii == 1) a = a.replaceAll("%s", n.toSingleLine());
                        a = a.replaceAll("%" + i + "\\$s", n.toSingleLine());
                        ii++;
                    }
                    a = a.replaceAll("%[0-9]\\$s", "");
                    sb = new StringBuilder(a);
                    break;
            }

            if (extra != null) {
                TextColor[] inhr = inh.toArray(new TextColor[0]);
                for (JsonBase jb : extra.listAll()) {
                    sb.append(jb.toSingleLine(inhr));
                    inhr = jb.getInherited();
                }
            }
            return sb.toString();
        }
        throw new RuntimeException("Invalid Json Type: " + getContentType().name() + " cannot be converted to single line.");
    }

    //Convert singleLine form text to Json (for chat packet, effective but just a wrapper json, basically useless for editing)
    public static Json wrap(String single) {
        return new PlainText(single);
    }

    /*
     * SingleLine parsing
     * This will always be created in extra JsonList, the main Json is just a warpper.
     */
    public static Json fromSingleLine(String single) {
        Json j = new PlainText();

        String[] element = single.split(TextColor.colorCodePattern);
        for (String s : element) {
            if (s.length() >= 1) {
                j.addExtra(new PlainText(s));
            }
        }

        int i = 0;
        Matcher mt = TextColor.colorCode.matcher(single);
        while (mt.find()) {
            String s = mt.group(0).replaceAll("§", "");
            if (s.charAt(0) == 'x') {
                ((Json) j.getExtra().get(i)).color = '#' + s.substring(2, 7);
            } else {
                for (int ii = 0; ii < s.length(); ii++) {
                    switch (s.charAt(ii)) {
                        case '1':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_BLUE.getCode();
                            break;
                        case '2':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_GREEN.getCode();
                            break;
                        case '3':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_AQUA.getCode();
                            break;
                        case '4':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_RED.getCode();
                            break;
                        case '5':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_PURPLE.getCode();
                            break;
                        case '6':
                            ((Json) j.getExtra().get(i)).color = TextColor.GOLD.getCode();
                            break;
                        case '7':
                            ((Json) j.getExtra().get(i)).color = TextColor.GRAY.getCode();
                            break;
                        case '8':
                            ((Json) j.getExtra().get(i)).color = TextColor.DARK_GRAY.getCode();
                            break;
                        case '9':
                            ((Json) j.getExtra().get(i)).color = TextColor.BLUE.getCode();
                            break;
                        case '0':
                            ((Json) j.getExtra().get(i)).color = TextColor.BLACK.getCode();
                            break;
                        case 'a':
                            ((Json) j.getExtra().get(i)).color = TextColor.GREEN.getCode();
                            break;
                        case 'b':
                            ((Json) j.getExtra().get(i)).color = TextColor.AQUA.getCode();
                            break;
                        case 'c':
                            ((Json) j.getExtra().get(i)).color = TextColor.RED.getCode();
                            break;
                        case 'd':
                            ((Json) j.getExtra().get(i)).color = TextColor.LIGHT_PURPLE.getCode();
                            break;
                        case 'e':
                            ((Json) j.getExtra().get(i)).color = TextColor.YELLOW.getCode();
                            break;
                        case 'f':
                            ((Json) j.getExtra().get(i)).color = TextColor.WHITE.getCode();
                            break;
                        case 'l':
                            ((Json) j.getExtra().get(i)).bold = true;
                            break;
                        case 'k':
                            ((Json) j.getExtra().get(i)).obfuscated = true;
                            break;
                        case 'n':
                            ((Json) j.getExtra().get(i)).underlined = true;
                            break;
                        case 'm':
                            ((Json) j.getExtra().get(i)).strikethrough = true;
                            break;
                        case 'o':
                            ((Json) j.getExtra().get(i)).italic = true;
                            break;
                        case 'r':
                            ((Json) j.getExtra().get(i)).reset();
                            break;
                    }
                }
            }
            i++;
        }
        return j;
    }

    public TextColor[] getInherited() {
        List<TextColor> lts = new ArrayList<>();
        if (color != null) lts.add(new TextColor(color));
        if (bold != null) lts.add(TextColor.BOLD);
        if (italic != null) lts.add(TextColor.ITALIC);
        if (strikethrough != null) lts.add(TextColor.STRIKETHROUGH);
        if (underlined != null) lts.add(TextColor.UNDERLINE);
        if (obfuscated != null) lts.add(TextColor.OBFUSCATED);
        return lts.toArray(new TextColor[0]);
    }

    //Reset all format
    public Json reset() {
        this.font = null;
        this.bold = false;
        this.color = null;
        this.italic = false;
        this.obfuscated = false;
        this.underlined = false;
        this.strikethrough = false;
        return this;
    }

    //Get data
    public String getColor() {return this.color;}
    public String getFont() {return this.font;}
    public Boolean isBold() {return this.bold;}
    public Boolean isItalic() {return this.italic;}
    public Boolean isUnderlined() {return this.underlined;}
    public Boolean isStrikethrough() {return this.strikethrough;}
    public Boolean isObfuscated() {return this.obfuscated;}
    public String getInsertion() {return this.insertion;}
    public Clickable getClickEvent() {return this.clickevent;}
    public Hoverable getHoverEvent() {return this.hoverevent;}
    public JsonList getExtra() {return this.extra;}

    //Set data
    public Json color(String color) {
        this.color = color;
        return this;
    }
    public Json font(String font) {
        this.font = font;
        return this;
    }
    public Json bold(Boolean isBold) {
        this.bold = isBold;
        return this;
    }
    public Json italic(Boolean isItalic) {
        this.italic = isItalic;
        return this;
    }
    public Json underlined(Boolean isUnderlined) {
        this.underlined = isUnderlined;
        return this;
    }
    public Json strikethrough(Boolean isStrikethrough) {
        this.strikethrough = isStrikethrough;
        return this;
    }
    public Json obfuscated(Boolean isObfuscated) {
        this.obfuscated = isObfuscated;
        return this;
    }
    public Json insertion(String insertion) {
        this.insertion = insertion;
        return this;
    }

    public Json onClick(Clickable event) {
        return setClickEvent(event);
    }
    public Json setClickEvent(Clickable event) {
        this.clickevent = event;
        return this;
    }

    public Json onHover(Hoverable event) {
        return setHoverEvent(event);
    }
    public Json setHoverEvent(Hoverable event) {
        this.hoverevent = event;
        return this;
    }

    public Json setExtra(JsonList extra) {
        this.extra = extra;
        return this;
    }
    public Json setExtra(JsonBase... extra) {
        this.extra = new JsonList(extra);
        return this;
    }
    public Json setExtra(Collection<JsonBase> extra) {
        this.extra = new JsonList(extra);
        return this;
    }
    public Json addExtra(JsonBase extra) {
        this.extra.add(extra);
        return this;
    }
    public Json addExtra(JsonBase... extra) {
        this.extra.add(extra);
        return this;
    }
    public Json addExtra(Collection<JsonBase> extra) {
        this.extra.add(extra);
        return this;
    }
    public Json removeExtra(JsonBase extra) {
        this.extra.remove(extra);
        return this;
    }
    public Json removeExtra(JsonBase... extra) {
        this.extra.remove(extra);
        return this;
    }
    public Json removeExtra(Collection<JsonBase> extra) {
        this.extra.remove(extra);
        return this;
    }
    public Json removeExtra(int Index) {
        this.extra.remove(Index);
        return this;
    }

    @Override
    public Json clone() {
        try {
            return (Json) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String toString() {
        return this.getJSON();
    }

    //Plain text
    public static class PlainText extends Json {

        String content;

        public PlainText() {
            this.content = "";
        }

        public PlainText(@Nullable String text) {
            this.content = text;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.PlainText;
        }

        public PlainText setText(String text) {
            this.content = text;
            return this;
        }

        public String getContentJSON() {
            return "\"text\":\"" + StringUtil.escape(this.content) + "\"";
        }
    }

    //Translated Text, good for using placeholder and generate translated message!
    public static class TranslatedText extends Json {

        String content;
        JsonList args;

        public TranslatedText() {
            this.content = "";
        }

        public TranslatedText(@NotNull String translate) {
            this.content = translate;
        }

        public TranslatedText(@NotNull String translate, @NotNull JsonBase... args) {
            this.content = translate;
            this.args = new JsonList(args);
        }
        public TranslatedText(@NotNull String translate, @NotNull Collection<JsonBase> args) {
            this.content = translate;
            this.args = new JsonList(args);
        }

        public TranslatedText(@NotNull String translate, @NotNull String... args) {
            this.args = new JsonList(args);
            this.content = translate;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.TranslatedText;
        }

        public TranslatedText setTranslate(String translate) {
            this.content = translate;
            return this;
        }

        public TranslatedText setArgs(JsonBase... args) {
            this.args = new JsonList(args);
            return this;
        }

        public TranslatedText setArgs(Collection<JsonBase> args) {
            this.args = new JsonList(args);
            return this;
        }

        public JsonList getArgs() {
            return this.args;
        }

        public String getContentJSON() {
            StringBuilder text = new StringBuilder("\"translate\":\"" + StringUtil.escape(this.content) + "\"");
            if (this.args != null) text.append(",\"with\":").append(args.getJSON());
            return text.toString();
        }
    }

    //Objective score from a entry, score can be fake.
    public static class ScoreboardValue extends Json {

        String content;
        String objective;
        String value = null;

        public ScoreboardValue() {
            this.content = "";
            this.objective = "";
        }

        public ScoreboardValue(@NotNull String entry, @NotNull String objective) {
            this.content = entry;
            this.objective = objective;
        }

        public ScoreboardValue(@NotNull String entry, @NotNull String objective, @Nullable String value) {
            this.content = entry;
            this.objective = objective;
            this.value = value;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.ScoreboardValue;
        }

        public ScoreboardValue setEntry(String entry) {
            this.content = entry;
            return this;
        }

        public ScoreboardValue setObjective(String obj) {
            this.objective = obj;
            return this;
        }

        public ScoreboardValue setValue(String value) {
            this.value = value;
            return this;
        }

        public String getObjective() {
            return this.objective;
        }

        public String getValue() {
            return this.value;
        }

        public String getContentJSON() {
            StringBuilder text = new StringBuilder("\"score\":{\"name\":\"" + StringUtil.escape(this.content) + "\",\"objective\":\"" + StringUtil.escape(this.objective) + "\"");
            if (this.value != null) {
                text.append(",\"value\":\"");
                text.append(this.value).append('"');
            }
            text.append('}');
            return text.toString();
        }
    }

    //Entity name
    public static class EntityNames extends Json {

        String content;

        public EntityNames() {
            this.content = "@s";
        }

        public EntityNames(@NotNull String selector) {
            this.content = selector;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.EntityNames;
        }

        public EntityNames setSelector(String selector) {
            this.content = selector;
            return this;
        }

        public String getContentJSON() {
            return "\"selector\":\"" + StringUtil.escape(this.content) + "\"";
        }
    }

    //Keybind of player
    public static class Keybind extends Json {

        String content;

        public Keybind() {
            this.content = "";
        }

        public Keybind(@NotNull String keybindNode) {
            this.content = keybindNode;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.Keybind;
        }

        public Keybind setKeybingNode(String node) {
            this.content = node;
            return this;
        }

        public String getContentJSON() {
            return "\"keybind\":\"" + StringUtil.escape(this.content) + "\"";
        }
    }

    /*//Block or entity nbt value
    public static class NBTValue extends Json {

        String content;
        String selector;
        boolean interpret;
        NBTMode mode;

        public NBTValue() {
            this.content = "";
            this.mode = ENTITY;
            this.selector = "@s";
            this.interpret = false;
        }

        //Mode can be "block", "entity" or "storage"
        public NBTValue(@NotNull String nbtPath, @NotNull String selector, @NotNull NBTMode mode, boolean interpret) {
            this.content = nbtPath;
            this.selector = selector;
            this.mode = mode;
            this.interpret = interpret;
        }

        public NBTValue(@NotNull String nbtPath, @NotNull String selector, @NotNull NBTMode mode) {
            this.content = nbtPath;
            this.selector = selector;
            this.mode = mode;
        }

        public JsonType.ContentType getContentType() {
            return JsonType.ContentType.NBTValue;
        }

        public NBTValue setPath(String nbtPath) {
            this.content = nbtPath;
            return this;
        }

        public String getSelector() {
            return this.selector;
        }

        public NBTValue setSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public boolean isInterpret() {
            return this.interpret;
        }

        public NBTValue setInterpret(Boolean isInterpret) {
            this.interpret = isInterpret;
            return this;
        }

        public NBTMode getMode() {
            return mode;
        }

        public void setMode(NBTMode mode) {
            this.mode = mode;
        }

        public String getContentJSON() {
            switch (mode) {
                case ENTITY:
                    return "\"nbt\":\"" + this.content + "\",\"entity\":\"" + this.selector + "\",\"interpret\":" + this.interpret;
                case BLOCK:
                    return "\"nbt\":\"" + this.content + "\",\"block\":\"" + this.selector + "\",\"interpret\":" + this.interpret;
                case STORAGE:
                    return "\"nbt\":\"" + this.content + "\",\"storage\":\"" + this.selector + "\",\"interpret\":" + this.interpret;
            }
            return null;
        }
    }*/
}
