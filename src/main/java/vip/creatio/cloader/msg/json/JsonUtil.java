package vip.creatio.cloader.msg.json;

import vip.creatio.cloader.exception.CommandNumberFormatException;
import vip.creatio.cloader.msg.Message;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

import static vip.creatio.cloader.msg.Message.fromPath;

public class JsonUtil {

    //No default constructor
    private JsonUtil() {}

    public static Json craftBottom(@Nullable String prevCommand, @Nullable String sufCommand) {
        Json json = new Json.PlainText();
        if (prevCommand == null) json.addExtra(new Json.PlainText("  §7<<< "));
        else json.addExtra(new Json.PlainText("  §2§l<<< ").setClickEvent(new Clickable.RunCommand(prevCommand)));
        json.addExtra(new Json.PlainText("§3|"));
        if (sufCommand == null) json.addExtra(new Json.PlainText(" §7>>>"));
        else json.addExtra(new Json.PlainText(" §2§l>>>").setClickEvent(new Clickable.RunCommand(sufCommand)));
        return json;
    }

    public static <T> Json[] listItems(int page,
                                       int pgSize,
                                       Collection<T> src,
                                       String nullHolder,
                                       String title,
                                       Listable<T> sortItem,
                                       Bottom bottom) {
        return listItems(page, pgSize, src, s -> s.size() == 0, nullHolder, title, sortItem, bottom);
    }

    /** Generic list crafter */
    public static <T> Json[] listItems(int page,
                                       int pgSize,
                                       Collection<T> src,
                                       Predicate<Collection<T>> nullLogic,
                                       String nullHolder,
                                       String title,
                                       Listable<T> sortItem,
                                       Bottom bottom) {
        if (nullLogic.test(src)) return new Json[]{new Json.PlainText(Message.fromPath("MAIN.LIST.NOT_FOUND", nullHolder)[0])};

        page--;
        int i = pgSize * page;

        if (page < 0) throw new CommandNumberFormatException("" + page, CommandNumberFormatException.NumberFormat.Positive);

        if (i >= src.size()) return new Json[]{new Json.PlainText(Message.fromPath("MAIN.LIST.OUT_OF_BOUND",
                "" + (page + 1),
                "" + (int) Math.ceil(src.size() / (double) pgSize))[0])};

        Json[] text = new Json[Math.min((src.size() - i), pgSize) + 3];

        text[0] = new Json.PlainText(Message.fromPath("MAIN.LIST.HEADER", title)[0]);

        int set = 0;

        for (T values : src) {
            set++;
            if (set < i + 1) continue;
            text[set - i] = sortItem.get(set, values);
            if (set >= i + pgSize) break;
        }

        set -= i;

        text[set + 1] = bottom.get(page, set < 15);
        text[set + 2] = new Json.PlainText(Message.fromPath("MAIN.LIST.FOOTER", Integer.toString(src.size()))[0]);
        return text;
    }

    public static Json dyePath(String path) {
        String[] str = new String[2];
        str[0] = path.substring(0, path.lastIndexOf("/") + 1);
        str[1] = path.substring(path.lastIndexOf("/") + 1);
        return new Json.PlainText(str[0]).color("#FCFCFC").addExtra(new Json.PlainText(str[1]).color("yellow"));
    }

    /** Listable interface for listItem() */
    public interface Listable<T> {
        Json get(int index, T item);

        static String fill(int index) {
            String str = "" + index;
            return (str.length() > 5) ? str : StringUtil.repeat(" ", 5 - str.length()) + str;
        }
    }

    /** Bottom crafting */
    public interface Bottom {
        Json get(int page, boolean latest);
    }

    public static final class StandardBottom implements Bottom {

        private final String cmd;

        public StandardBottom(String cmd) {
            this.cmd = cmd;
        }

        @Override
        public Json get(int page, boolean latest) {
            String prev = (page == 0) ? null : cmd + page;
            String next = (latest) ? null : cmd + (page + 2);
            return craftBottom(prev, next);
        }
    }
}
