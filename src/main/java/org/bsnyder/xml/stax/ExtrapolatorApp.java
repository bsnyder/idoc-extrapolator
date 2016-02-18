package org.bsnyder.xml.stax;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

public class ExtrapolatorApp {

    private String extrapolatorType;
    private String inputDirectoryName;
    private int totalNumberOfFilesToCreate;

    public static void main(String[] args) throws Exception {
        ExtrapolatorApp app = new ExtrapolatorApp();
        Options options = app.defineOptions();
        if (args != null && args.length > 0) {
            app.parseArgs(args, options);
            app.extrapolate();
        } else {
            app.printHelp(options);
        }
    }

    private void extrapolate() throws IOException {
        Extrapolator ex = null;
        if ("artmas".equalsIgnoreCase(extrapolatorType)) {
            ex = new ArtmasExtrapolator(inputDirectoryName, totalNumberOfFilesToCreate);
        } else if ("matmas".equalsIgnoreCase(extrapolatorType)) {
            ex = new MatmasExtrapolator(inputDirectoryName, totalNumberOfFilesToCreate);
        }
        ex.extrapolate();
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ExtrapolatorApp", options);

    }

    private void parseArgs(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            this.extrapolatorType = cmd.getOptionValue('t');
            this.inputDirectoryName = cmd.getOptionValue('d');
            this.totalNumberOfFilesToCreate = new Integer(cmd.getOptionValue("f")).intValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private Options defineOptions() {
        return new Options()
                .addOption("t", true, "The type of Extrapolator (supported types: Artmas or Matmas)")
                .addOption("d", true, "The input directory where the IDocs live")
                .addOption("f", true, "The total number of files to create");
    }

}
