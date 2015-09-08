package org.bsnyder.xml.stax;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;

public class ExtrapolatorApp {

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
        Extrapolator ex = new Extrapolator(inputDirectoryName, totalNumberOfFilesToCreate);
        ex.extrapolate();
    }

    private void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("extrapolator", options);

    }

    private void parseArgs(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            this.inputDirectoryName = cmd.getOptionValue('d');
            this.totalNumberOfFilesToCreate = new Integer(cmd.getOptionValue("f")).intValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private Options defineOptions() {
        return new Options()
                .addOption("d", true, "The input directory where the IDocs live")
                .addOption("f", true, "The total number of files to create");

        /*
        Option intputDirectoryNameOption = Option.builder("d")
                .longOpt("inputDirectoryName")
                .required(true)
                .desc("The directory where the IDocs live")
                .build();
        opts.addOption(intputDirectoryNameOption);
        Option totalNumberOfFilesToCreateOption = Option.builder("f")
                .longOpt("totalNumberOfFilesToCreate")
                .required(true)
                .desc("The total number of files to create")
                .build();
        opts.addOption(totalNumberOfFilesToCreateOption);
        */
    }

}
