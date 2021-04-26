package vip.creatio.cloader.msg;

import vip.creatio.cloader.bukkit.ClassLoader;
import vip.creatio.cloader.bukkit.Config;
import vip.creatio.cloader.ccl.module.ClassFile;
import vip.creatio.cloader.msg.json.Json;
import vip.creatio.cloader.msg.json.JsonBase;
import vip.creatio.cloader.msg.json.JsonText;
import vip.creatio.cloader.msg.json.TextColor;
import vip.creatio.cloader.reflect.ReflectionClass;
import vip.creatio.cloader.reflect.ReflectionConstructor;
import vip.creatio.cloader.reflect.ReflectionMethod;
import vip.creatio.cloader.reflect.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedReturnTypes", "unused"})
public class Message {

    public static final Prefix MAIN_PREFIX = new Prefix("MAIN.FORMAT.PREFIX");
    public static final Prefix DOS_PREFIX = new Prefix("MAIN.DOS.PREFIX");
    public static final Prefix EVAL_PREFIX = new Prefix("MAIN.EVAL.PREFIX");

    static FileConfiguration language;
    private static final Pattern hex = Pattern.compile("\\{(#[0-9a-fA-F]{6}?)}");
    private static final Pattern pattern = Pattern.compile("^#([0-9a-fA-F]{6})$");
    private static final Pattern hexCode = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern var = Pattern.compile("%([0-9]{1,2}?)%");
    private static final JsonBase ENDL = new JsonText("\n");
    private static String WARN = "&e";
    private static String ERROR = "&c";
    private static String NORMAL = "&7";
    private static String SUCCESS = "&a";
    private static String HIGHLIGHT = "&6";


    private static final PrintStream sysStream = System.out;


    private static final Field playerConnection;
    private static final Map<Player, Object> CONNECTION_MAP = new HashMap<>();
    private static final String[] NULL = new String[0];

    private static final Map<String, String[]> MSG_BUFFER = new HashMap<>();

    static {
        try {
            playerConnection = ReflectionClass.EntityPlayer.c.getDeclaredField("playerConnection");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        ClassLoader.getInstance().getLogger().setFilter((record) -> !record.getMessage().contains("which is not a depend"));
    }

    //No default constructor
    private Message() {}

    public static void initialization(JavaPlugin plugin, String lang) {
        MSG_BUFFER.clear();
        Config.updateConfig(plugin, "lang/" + lang + ".yml", -1);
        language = Config.load(plugin, "lang/" + lang + ".yml");
        WARN = charReplace(language.getString("MAIN.FORMAT.WARN"));
        ERROR = charReplace(language.getString("MAIN.FORMAT.ERROR"));
        NORMAL = charReplace(language.getString("MAIN.FORMAT.NORMAL"));
        SUCCESS = charReplace(language.getString("MAIN.FORMAT.SUCCESS"));
        HIGHLIGHT = charReplace(language.getString("MAIN.FORMAT.HIGHLIGHT"));
    }



    /** Get all online Operators */
    public static List<Player> getOnlineOp() {
        List<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) players.add(p);
        }
        return players;
    }


    public static void switchPrintln(CommandSender sender) {
        System.setOut(new SenderPrintStream(sender));
    }

    public static void initPrintln() {
        System.setOut(sysStream);
    }

    //Log debug message
    public static void debug(String msg) {
        debug(Level.SEVERE, msg);
    }

    public static void debug(Level level, String msg) {
        Bukkit.getLogger().log(level, charReplace("&6[&c&lClassLoader - Debug&6] " + msg));
    }



    //Log internal message
    public static void internal(String msg) {
        internal(Level.SEVERE, msg);
    }
    public static void internal(Level level, String msg) {
        Bukkit.getLogger().log(level, charReplace("&6[&e&lClassLoader &8- &c&lInternal&6] " + msg));
    }



    //Send to player
    public static void send(Player player, String... msg) {
        send((CommandSender) player, msg);
    }

    public static void send(Player player, Json... json) {
        send((CommandSender) player, json);
    }




    //Send to console, player or commandblock
    public static void send(CommandSender sender, String... msg) {
        if (sender instanceof ConsoleCommandSender || sender == null) {
            send(Level.INFO, msg);
            return;
        }

        Json json = new Json.PlainText("");
        for (int i = 0; i < msg.length - 1; i++) {
            json.addExtra(Json.wrap(charReplace(msg[i])), ENDL);
        }
        json.addExtra(Json.wrap(charReplace(msg[msg.length - 1])));

        if (sender instanceof Player) {
            new ChatPacket(json, null)
                    .send((Player) sender);
        }
        if (sender instanceof BlockCommandSender) {
            sender.sendMessage(json.toSingleLine());
        }
    }
    public static void send(CommandSender sender, Json... json) {
        if (sender instanceof ConsoleCommandSender) {
            send(Level.INFO, json);
            return;
        }

        Json j = new Json.PlainText("");
        for (int i = 0; i < json.length - 1; i++) {
            j.addExtra(json[i], ENDL);
        }
        j.addExtra(json[json.length - 1]);

        if (sender instanceof Player) {
            new ChatPacket(j, null)
                    .send((Player) sender);
        }
        if (sender instanceof BlockCommandSender) {
            sender.sendMessage(j.toSingleLine());
        }
    }




    //Send to console
    public static void send(Level level, String... message) {
        for (String s : message) {
            Bukkit.getLogger().log(level, StringUtil.wipeHex(charReplace(s)));
        }
    }
    public static void send(Level level, Json... json) {
        for (Json j : json) {
            Bukkit.getLogger().log(level, charReplace(j.toSingleLine()));
        }
    }




    public static String listFileCrafter0(int max, File... items) {
        return listItemCrafter0(File::getName, max, fromPath("MAIN.FILE.FILE")[0], items);
    }

    public static String listClassCrafter0(int max, Class<?>... classes) {
        return listItemCrafter0(Class::getTypeName, max, fromPath("MAIN.FILE.CLASS")[0], classes);
    }

    public static String listClassCrafter1(int max, ClassFile... classes) {
        return listItemCrafter0(ClassFile::getClassName, max, fromPath("MAIN.FILE.CLASS")[0], classes);
    }

    @SafeVarargs
    public static <T> String listItemCrafter0(Messager<T> msg, int max, String alias, T... items) {
        if (items.length > max) return items.length + alias;
        if (items.length == 0) return 0 + alias;
        boolean flag = false;
        StringBuilder sb = new StringBuilder();
        for (T item : items) {
            if (flag) sb.append(", ");
            flag = true;
            sb.append(msg.getMessage(item));
        }
        return sb.toString();
    }

    public interface Messager<T> {
        String getMessage(T item);
    }




    /** Get raw message */
    public static String getStatic(String path) {
        return language.getString(path);
    }



    //Send static msg to single player
    public static void sendStatic(String path, @NotNull CommandSender player) {
        sendStatic0(MAIN_PREFIX, Level.INFO, path, player);
    }

    public static void sendStatic(String path, @NotNull CommandSender player, String... vars) {
        sendStatic0(MAIN_PREFIX, Level.INFO, path, player, vars);
    }

    public static void sendStatic(Prefix prefix, String path, @NotNull CommandSender player) {
        sendStatic0(prefix, Level.INFO, path, player);
    }

    public static void sendStatic(Prefix prefix, String path, @NotNull CommandSender player, String... vars) {
        sendStatic0(prefix, Level.INFO, path, player, vars);
    }



    //Send static msg to multiple players
    public static void sendStatic(String path, @NotNull Collection<Player> players) {
        sendStatic1(MAIN_PREFIX, Level.INFO, path, players);
    }

    public static void sendStatic(String path, @NotNull Collection<Player> players, String... vars) {
        sendStatic1(MAIN_PREFIX, Level.INFO, path, players, vars);
    }

    public static void sendStatic(Prefix prefix, String path, @NotNull Collection<Player> players) {
        sendStatic1(prefix, Level.INFO, path, players);
    }

    public static void sendStatic(Prefix prefix, String path, @NotNull Collection<Player> players, String... vars) {
        sendStatic1(prefix, Level.INFO, path, players, vars);
    }



    //Send to console
    public static void sendStatic(Level level, String path) {
        sendStatic0(MAIN_PREFIX, level, path, null);
    }

    public static void sendStatic(Level level, String path, String... vars) {
        sendStatic0(MAIN_PREFIX, level, path, null, vars);
    }


    //private impl.
    private static void sendStatic0(Prefix prefix,
                                    Level level,
                                    String path,
                                    @Nullable CommandSender sender,
                                    @Nullable String... vars)
    {
        String[] msg = fromPath(prefix, path, vars);
        send(sender, msg);
    }
    private static void sendStatic1(Prefix prefix,
                                    Level level,
                                    String path,
                                    @NotNull Collection<Player> senders,
                                    @Nullable String... vars)
    {
        String[] msg = fromPath(prefix, path, vars);
        for (Player s : senders) {
            send((CommandSender) s, msg);
        }
    }


    //Get String array of message from given path
    public static String[] fromPath(@NotNull String path) {
        return fromPath(MAIN_PREFIX, path);
    }

    public static String[] fromPath(@NotNull Prefix prefix, @NotNull String path) {
        return fromPath(prefix, path, NULL);
    }

    public static String[] fromPath(@NotNull String path, @Nullable String... vars) {
        return fromPath(MAIN_PREFIX, path, vars);
    }

    public static String[] fromPath(@NotNull Prefix prefix, @NotNull String path, @Nullable String... vars) {
        String[] arr = MSG_BUFFER.get(path);
        if (arr == null) {
            Object var = language.get(path);
            var = (var == null) ? path : var;
            if (var instanceof List) {
                List<String> temp = new ArrayList<>();
                for (String s : (List<String>) var) {
                    temp.add(StringUtil.backTrim(varReplace(prefix, s, vars)));
                }
                arr = temp.toArray(new String[0]);
            } else {
                arr = new String[]{StringUtil.backTrim(varReplace(prefix, var.toString(), vars))};
            }
        }
        return arr;
    }

    //Placeholder replacement
    public static String charReplace(String message) {
        if (message == null || message.equals("")) return "";

        message = message.replace('&', '§');
        message = StringUtil.replaceAll(message, "/§", "&");
        message = StringUtil.replaceAll(message, "%w%", WARN);
        message = StringUtil.replaceAll(message, "%e%", ERROR);
        message = StringUtil.replaceAll(message, "%n%", NORMAL);
        message = StringUtil.replaceAll(message, "%s%", SUCCESS);
        message = StringUtil.replaceAll(message, "%h%", HIGHLIGHT);
        message = StringUtil.replaceAll(message, "\\n", "\n");

        Matcher mt = hex.matcher(message);
        while (mt.find()) {
            message = StringUtil.replaceAll(
                    message,
                    StringUtil.replaceAll(mt.group(0),"\\{", "\\\\{"),
                    TextColor.hexToColorCode(mt.group(1))
            );
        }
        return message;
    }

    //Wipe hex color code for console messages
    public static String colorCodeToHex(String ori) {
        Matcher mt = hexCode.matcher(ori);
        if (mt.find()) {
            return '#' + ori.substring(3, 15).replace('§', (char) 0x0);
        }
        throw new RuntimeException("No such color: " + ori);
    }

    private static String varReplace(Prefix prefix, String message, String... vars) {
        message = charReplace(StringUtil.replaceAll(message, "%prefix%", prefix.getPrefix()));
        Matcher mt = var.matcher(message);
        while(mt.find()) {
            int i = Integer.parseInt(mt.group(1));
            if (vars.length > i)
                message = StringUtil.replaceAll(message,mt.group(0), charReplace(vars[i]));
        }
        return message;
    }

    private static class SenderPrintStream extends PrintStream {

        private final CommandSender sender;

        SenderPrintStream(CommandSender sender) {
            super(new OutputStream() {
                @Override
                public void write(int b) {}
            });
            this.sender = sender;
        }

        private void newLine() {
            sender.sendMessage("\n");
        }

        @Override
        public void print(boolean b) {
            sender.sendMessage(b ? "true" : "false");
        }

        @Override
        public void print(char c) {
            sender.sendMessage(String.valueOf(c));
        }

        @Override
        public void print(int i) {
            sender.sendMessage(String.valueOf(i));
        }

        @Override
        public void print(long l) {
            sender.sendMessage(String.valueOf(l));
        }

        @Override
        public void print(float f) {
            sender.sendMessage(String.valueOf(f));
        }

        @Override
        public void print(double d) {
            sender.sendMessage(String.valueOf(d));
        }

        @Override
        public void print(char[] s) {
            sender.sendMessage(new String(s));
        }

        @Override
        public void print(String s) {
            if (s == null) {
                s = "null";
            }
            sender.sendMessage(s);
        }

        @Override
        public void print(Object obj) {
            sender.sendMessage(String.valueOf(obj));
        }
    }

    //private chat packet for json message
    private static class ChatPacket {

        private final Object original;

        ChatPacket(String json, @Nullable UUID senderUuid) {
            original = ReflectionConstructor.PacketPlayOutChat.run(
                    ReflectionUtils.getNmsChatComponent(json),
                    ReflectionClass.ChatMessageType.c.getEnumConstants()[1],
                    senderUuid);
        }

        ChatPacket(Json json, @Nullable UUID senderUuid) {
            original = ReflectionConstructor.PacketPlayOutChat.run(
                    ReflectionUtils.getNmsChatComponent(json.toString()),
                    ReflectionClass.ChatMessageType.c.getEnumConstants()[1],
                    senderUuid);
        }

        ChatPacket(String json) {
            this(json, null);
        }

        ChatPacket(Json json) {
            this(json, null);
        }

        Object getOriginal() {
            return original;
        }

        void send(Player p) {
            try {
                CONNECTION_MAP.putIfAbsent(p, playerConnection.get(ReflectionUtils.getNmsPlayer(p)));
                ClassLoader.getInstance().getThreadPool().execute(() ->
                        ReflectionMethod
                                .PlayerConnection_sendPacket
                                .invoke(CONNECTION_MAP.get(p), getOriginal()));
            } catch (IllegalAccessException e) {
                Message.internal("&4Failed to send chat packet for &6&l" + p.getName());
                e.printStackTrace();
            }
        }
    }

    public static class Prefix {
        private String prefix;
        private final String path;

        public Prefix(String path) {
            this.path = path;
        }

        public String getPrefix() {
            if (prefix == null)
                prefix = charReplace(language.getString(path));
            return prefix;
        }

        public String getPath() {
            return path;
        }
    }
}
