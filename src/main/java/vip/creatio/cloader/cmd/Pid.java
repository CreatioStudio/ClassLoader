package vip.creatio.cloader.cmd;

import vip.creatio.cloader.msg.Message;
import org.bukkit.command.PluginCommand;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class Pid {

    private static final String name = "pid";
    private static final String description = "Check Server PID";
    private static final List<String> aliases = Collections.emptyList();

    //No default constructor
    private Pid() {}

    public static PluginCommand register() {
        PluginCommand cmd = CommandRegister.create(name);
        cmd.setDescription(description);
        cmd.setAliases(aliases);
        cmd.setUsage("");
        cmd.setPermission(null);
        cmd.setPermissionMessage(null);
        cmd.setExecutor((sender, command, label, args) -> {
            if (sender.isOp()) {
                Message.sendStatic("MAIN.PID", sender, ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
            } else {
                Message.sendStatic("MAIN.NO_PERM", sender);
                return false;
            }
            return true;
        });
        return cmd;
    }
}
