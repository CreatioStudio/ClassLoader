package vip.creatio.cloader.cmd;

import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import vip.creatio.basic.cmd.Argument;
import vip.creatio.basic.cmd.ArgumentTypes;
import vip.creatio.basic.cmd.CommandRegister;
import vip.creatio.basic.tools.FormatMsgManager;
import vip.creatio.cloader.bukkit.CLoader;
import vip.creatio.cloader.ccl.RuntimeExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class CommandSystem {

    private static final String name = "system";
    private static final String description = "Executing OS terminal commands.";
    private static final List<String> aliases = Arrays.asList("terminal", "cmd", "term");
    private static final FormatMsgManager msg = CLoader.getMsgSender();

    private static final Map<String, String> PREFIX = Map.of("%prefix%", msg.getSingle("MAIN.TERM.PREFIX"));

    //No default constructor
    private CommandSystem() {}

    public static void register(CommandRegister register) {
        LiteralCommandNode<?> node = Argument.of(name)
                .then(Argument.arg("Command", ArgumentTypes.ofGreedyString())
                        .executes(c -> execute(c.getSender(), c.getArgument("Command", String.class))))
                .fallbacksNoPermission(c -> msg.sendStatic(c, "MAIN.NO_PERM"))
                .fallbacksInvalidInput((c, e) -> msg.sendStatic(c.getSender(), "COMMAND.USAGE.DOS", c.getLabel()))
                .requires(c -> (c instanceof ConsoleCommandSender) || ((c instanceof Player) && c.isOp() && CLoader.getInstance().allowPlayersExecuteDos()))
                .restricted(true)
                .build();
        register.register(node, description, aliases);
    }

    private static void execute(CommandSender sender, String cmdLine) {
        CLoader.getInstance().getThreadPool().execute(() -> {

            //Mention other operators as well as console
            //TODO: make this!
                                /*if (sender instanceof Player)
                                {
                                    List<Player> op = Message.getOnlineOp();
                                    op.remove(sender);
                                    msg.sendStatic(
                                            op,
                                            "MAIN.TERM.EXECUTED_MENTION",
                                            sender.getName(),
                                            sb.toString());
                                }*/

            //Executing
            RuntimeExecutor exe = CLoader.getInstance().getRuntimeExecutor();
            try {
                //if process continues
                if (CLoader.getInstance().getRuntimeExecutor().isProcessing()) {
                    msg.sendStatic(PREFIX, sender, "MAIN.TERM.INPUT", cmdLine);
                    exe.input(sender, cmdLine);
                }
                else {
                    msg.sendStatic(PREFIX, sender, "MAIN.TERM.EXECUTED", cmdLine);
                    exe.getNew(sender, cmdLine);
                }
            } catch (IOException e) {
                msg.sendStatic(PREFIX, sender, "MAIN.TERM.ERROR", e.getLocalizedMessage());
            }
        });
    }
}
