package vip.creatio.cloader.cmd;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import vip.creatio.accessor.Func;
import vip.creatio.accessor.Reflection;
import vip.creatio.cloader.bukkit.CLoader;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.jetbrains.annotations.NotNull;
import vip.creatio.common.ReflectUtil;

import java.util.*;

public class CommandRegister {

    private static final SimpleCommandMap COMMAND_MAP =
            ReflectUtil.invoke(Bukkit.getServer(), "getCommandMap");
    private static final Map<String, Command> KNOWN_COMMAND =
            ReflectUtil.get(SimpleCommandMap.class, "knownCommands");

    private final Map<String, Command> REGISTERED_COMMANDS = new HashMap<>();
    private final String FALLBACK_PREFIX;

    public static final List<String> COMPLETER_SMALL_INT = Collections.unmodifiableList(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
    public static final List<String> COMPLETER_EMPTY = Collections.unmodifiableList(new ArrayList<>());

    public CommandRegister(String FALLBACK_PREFIX) {
        this.FALLBACK_PREFIX = FALLBACK_PREFIX;
    }

    //Command register
    public void init() {
        register(Main.register());
        register(Dos.register());
        register(Test.register());
        register(Eval.register());
        register(Pid.register());
    }

    public void register(Command cmd) {
        COMMAND_MAP.register(cmd.getName(), FALLBACK_PREFIX, cmd);
        REGISTERED_COMMANDS.put(cmd.getName(), cmd);
    }

    public void unregister(Command cmd) {
        unregister(cmd.getName());
    }

    public void registerAll(Collection<Command> commands) {
        for (Command c : commands) {
            register(c);
        }
    }

    public void unregister(String lable) {
        Command cmd = REGISTERED_COMMANDS.get(lable);
        cmd.unregister(COMMAND_MAP);
        for (String s : cmd.getAliases()) {
            KNOWN_COMMAND.remove(s);
            KNOWN_COMMAND.remove(FALLBACK_PREFIX + ':' + s);
            REGISTERED_COMMANDS.remove(s);
            REGISTERED_COMMANDS.remove(FALLBACK_PREFIX + ':' + s);
        }
        KNOWN_COMMAND.remove(cmd.getName());
        KNOWN_COMMAND.remove(FALLBACK_PREFIX + ':' + cmd.getName());
        REGISTERED_COMMANDS.remove(lable);
        REGISTERED_COMMANDS.remove(FALLBACK_PREFIX + ':' + lable);
    }

    public void clearCommands() {
        for (String key : new HashSet<>(REGISTERED_COMMANDS.keySet())) {
            unregister(key);
        }
    }

    public Command getCommand(String name) {
        return REGISTERED_COMMANDS.get(name);
    }

    public Collection<Command> getCommands() {
        return Collections.unmodifiableCollection(REGISTERED_COMMANDS.values());
    }

    public static SimpleCommandMap getCommandMap() {
        return COMMAND_MAP;
    }

    private final static Func<PluginCommand> CONSTRUCT_PLUGIN_CMD = Reflection.constructor(PluginCommand.class, String.class, Plugin.class);
    public static PluginCommand create(@NotNull String name) {
        return CONSTRUCT_PLUGIN_CMD.invoke(name, CLoader.getInstance());
    }
}
