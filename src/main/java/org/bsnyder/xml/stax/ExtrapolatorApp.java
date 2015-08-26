package org.bsnyder.xml.stax;

import org.apache.commons.cli.*;

public class ExtrapolatorApp {

    private String dirname;

    public static void main(String[] args) {
        ExtrapolatorApp app = new ExtrapolatorApp();
        app.handleArgs(args);
        // TODO: Call the Extrapolator
    }

    private void handleArgs(String[] args) {
        Options options = defineOptions();
        parseArgs(options, args);
    }

    private void parseArgs(Options options, String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            this.dirname = cmd.getOptionValue('d');
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private Options defineOptions() {
        Options opts = new Options();

        Option dirnameOption = Option.builder("d")
                .longOpt("dirname")
                .required(true)
                .desc("The directory where the IDocs live")
                .build();
        opts.addOption(dirnameOption);
        return opts;
    }

}
