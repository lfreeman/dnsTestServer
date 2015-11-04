package commands;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.crsh.cli.Command;
import org.crsh.cli.Option;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;

public class date extends BaseCommand {

    public date() {

    }

    @Usage("show the current time")
    @Command
    public Object main(@Usage("the time format") @Option(names = {"f", "format"}) String format) {
        if (format == null)
            format = "EEE MMM d HH:mm:ss z yyyy";
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }
}
