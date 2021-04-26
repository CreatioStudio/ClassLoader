package vip.creatio.cloader.cmd;

import vip.creatio.clib.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.RuntimeExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class Dos {

    private static final String name = "dos";
    private static final String description = "Executing OS terminal  commands.";
    private static final List<String> aliases = Arrays.asList("terminal", "tcmd", "term");
    private static final FormatMsgManager msg = CLoader.getMsgSender();

    //No default constructor
    private Dos() {}

    public static PluginCommand register() {
        PluginCommand cmd = CommandRegister.create(name);
        cmd.setDescription(description);
        cmd.setAliases(aliases);
        cmd.setUsage("");
        cmd.setPermission(null);
        cmd.setPermissionMessage(null);
        cmd.setExecutor((sender, command, label, args) -> {

            if ((sender instanceof ConsoleCommandSender)
                    || ((sender instanceof Player) && sender.isOp() && CLoader.getInstance().allowPlayersExecuteDos()))
            {
                //Var check
                if (args.length < 1) {
                    msg.sendStatic(sender, "COMMAND.USAGE.DOS", command.getName());
                    return true;
                }

                CLoader.getInstance().getThreadPool().execute(() ->
                {
                    //Craft input cmd
                    StringBuilder sb = new StringBuilder(args[0]);
                    for (int i = 1; i < args.length; i++)
                    {
                        sb.append(' ').append(args[i]);
                    }

                    //Mention other operators as well as console
                    //TODO: make this!
                    /*if (sender instanceof Player)
                    {
                        List<Player> op = Message.getOnlineOp();
                        op.remove(sender);
                        msg.sendStatic(
                                op,
                                "MAIN.DOS.EXECUTED_MENTION",
                                sender.getName(),
                                sb.toString());
                    }*/

                    //Executing
                    RuntimeExecutor exe = CLoader.getInstance().getRuntimeExecutor();
                    try
                    {
                        //if process continues
                        if (CLoader.getInstance().getRuntimeExecutor().isProcessing())
                        {
                            msg.sendStatic(sender, "MAIN.DOS.INPUT", sb.toString());
                            exe.input(sender, sb.toString());
                        }
                        else
                        {
                            msg.sendStatic(sender, "MAIN.DOS.EXECUTED", sb.toString());
                            exe.getNew(sender, sb.toString());
                        }
                    }
                    catch (IOException e)
                    {
                        msg.sendStatic(sender, "MAIN.DOS.ERROR", e.getLocalizedMessage());
                    }
                });

            }
            else
            {
                if (sender.isOp())
                {
                    msg.sendStatic(sender, "MAIN.CONSOLE_ONLY");
                }
                else
                {
                    msg.sendStatic(sender, "MAIN.NO_PERM");
                }
            }
            return true;
        });
        return cmd;
    }
}
