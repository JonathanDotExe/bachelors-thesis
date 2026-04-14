package at.jku.ssw.hocheneder.musicesolang;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Interpreter;
import at.jku.ssw.hocheneder.musicesolang.player.MeasureSequencer;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;
import org.audiveris.proxymusic.util.Marshalling;

import javax.sound.midi.MidiUnavailableException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class Main {

    private static final String PLAY_OPTION = "play";
    private static final String VERBOSE_OPTION = "verbose";
    private static final String PART_OPTION = "part";
    private static final String PLAIN_OPTION = "plain";
    private static final String NO_MEASURES_OPTION = "no-measures";
    private static final String OUTPUT_OPTION = "output";
    private static final String HELP_OPTION = "help";

    private static final String PLAY_SHORT_OPTION = "P";
    private static final String VERBOSE_SHORT_OPTION = "v";
    private static final String PART_SHORT_OPTION = "p";
    private static final String PLAIN_SHORT_OPTION = "t";
    private static final String NO_MEASURES_SHORT_OPTION = "X";
    private static final String OUTPUT_SHORT_OPTION = "o";
    private static final String HELP_SHORT_OPTION = "h";



    public static void main(String[] args) throws IOException, Marshalling.UnmarshallingException, MidiUnavailableException {
        if (args.length == 0) {
            System.out.println("Error, no command specified. Use command help for more information.");
            return;
        }

        HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();

        String[] optionArr = Arrays.copyOfRange(args, 1, args.length);

        Options runOptions = new Options();
        runOptions.addOption(Option.builder(PLAY_SHORT_OPTION)
                .longOpt(PLAY_OPTION)
                .desc("plays the given music xml file in sync")
                .hasArg()
                .get()
        );
        runOptions.addOption(Option.builder(VERBOSE_SHORT_OPTION)
                .longOpt(VERBOSE_OPTION)
                .desc("shows debug messages with each instruction")
                .get()
        );
        runOptions.addOption(Option.builder(HELP_SHORT_OPTION)
                .longOpt(HELP_OPTION)
                .desc("shows the usage and available options of this command")
                .get()
        );

        Options interpretOptions = new Options();
        interpretOptions.addOption(Option.builder(PLAY_SHORT_OPTION)
                .longOpt(PLAY_OPTION)
                .desc("plays the given music xml file in sync")
                .get()
        );
        interpretOptions.addOption(Option.builder(VERBOSE_SHORT_OPTION)
                .longOpt(VERBOSE_OPTION)
                .desc("shows debug messages with each instruction")
                .get()
        );
        interpretOptions.addOption(Option.builder(PART_SHORT_OPTION)
                .longOpt(PART_OPTION)
                .desc("specifies the part id to use in the mxl file")
                .hasArg()
                .get()
        );
        interpretOptions.addOption(Option.builder(HELP_SHORT_OPTION)
                .longOpt(HELP_OPTION)
                .desc("shows the usage and available options of this command")
                .get()
        );

        Options compileOptions = new Options();
        compileOptions.addOption(Option.builder(NO_MEASURES_SHORT_OPTION)
                .longOpt(NO_MEASURES_OPTION)
                .desc("doesn't encode measure markings for playback")
                .get()
        );
        compileOptions.addOption(Option.builder(PLAIN_SHORT_OPTION)
                .longOpt(PLAIN_OPTION)
                .desc("uses plaintext bytecode format")
                .get()
        );
        compileOptions.addOption(Option.builder(OUTPUT_SHORT_OPTION)
                .longOpt(OUTPUT_OPTION)
                .desc("specifies the output file")
                .hasArg()
                .get()
        );
        compileOptions.addOption(Option.builder(PART_SHORT_OPTION)
                .longOpt(PART_OPTION)
                .desc("specifies the part id to use in the mxl file")
                .hasArg()
                .get()
        );
        compileOptions.addOption(Option.builder(HELP_SHORT_OPTION)
                .longOpt(HELP_OPTION)
                .desc("shows the usage and available options of this command")
                .get()
        );

        Options decompileOptions = new Options();
        decompileOptions.addOption(Option.builder(OUTPUT_SHORT_OPTION)
                .longOpt(OUTPUT_OPTION)
                .desc("specifies the output file")
                .hasArg()
                .get()
        );
        decompileOptions.addOption(Option.builder(HELP_SHORT_OPTION)
                .longOpt(HELP_OPTION)
                .desc("shows the usage and available options of this command")
                .get()
        );


        CommandLineParser parser = new DefaultParser();

        switch (args[0]) {
            case "run" -> {
                try {
                    CommandLine line = parser.parse(runOptions, optionArr);

                    if (line.hasOption(HELP_OPTION)) {
                        formatter.printHelp("cmd run <file>", "runs bytecode or plaintext bytecode file", runOptions, "", false);
                        return;
                    }

                    System.out.println("Run not implemented yet.");
                } catch (ParseException e) {
                    System.out.println(e.getMessage() + ". Use cmd run -h for a list of available options.");
                }
            }
            case "interpret" -> {
                try {
                    CommandLine line = parser.parse(interpretOptions, optionArr);

                    if (line.hasOption(HELP_OPTION)) {
                        formatter.printHelp("cmd interpret <file>", "compiles music xml to bytecode in memory and runs it", interpretOptions, "", false);
                        return;
                    }

                    if (line.getArgs().length == 0) {
                        System.out.println("No input file specified.");
                        return;
                    }

                    String filename = line.getArgs()[0];
                    System.out.println("Loading file " + filename);
                    try (InputStream input = new FileInputStream(filename);
                         MeasureSequencer sequencer = new MeasureSequencer();) {
                        boolean play = line.hasOption(PLAY_OPTION);
                        boolean verbose = line.hasOption(VERBOSE_OPTION);
                        if (verbose) {
                            //TODO  verbose logging
                        }

                        String part = line.getOptionValue(PLAIN_OPTION);
                        if (part == null || part.isBlank()) {
                            part = "P1";
                        }

                        MusicCompiler compiler = new MusicCompiler(input);

                        System.out.println("Compiling using part " + part +" ...");
                        Code code = compiler.compile(play, sequencer, part);
                        System.out.println("Compilation successful.");
                        System.out.println("Generated code:");
                        System.out.println(code);

                        System.out.println();
                        System.out.println("Executing ... ");
                        System.out.println();

                        if (play) {
                            sequencer.initialize();
                            Interpreter.interpret(sequencer, code.getCode());
                        }
                        else {
                            Interpreter.interpret(code.getCode());
                        }

                        System.out.println();
                        System.out.println("Finished");
                    }
                } catch (ParseException e) {
                    System.out.println(e.getMessage() + ". Use cmd interpret -h for a list of available options.");
                }
            }
            case "compile" -> {
                try {
                    CommandLine line = parser.parse(compileOptions, optionArr);

                    if (line.hasOption(HELP_OPTION)) {
                        formatter.printHelp("cmd compile <file>", "compiles musicxml to bytecode", compileOptions, "", false);
                        return;
                    }

                    if (line.getArgs().length == 0) {
                        System.out.println("No input file specified.");
                        return;
                    }

                    System.out.println("Compile not implemented yet.");
                } catch (ParseException e) {
                    System.out.println(e.getMessage() + ". Use cmd compile -h for a list of available options.");
                }
            }
            case "decompile" -> {
                try {
                    CommandLine line = parser.parse(decompileOptions, optionArr);

                    if (line.hasOption(HELP_OPTION)) {
                        formatter.printHelp("cmd decompile <file>", "decompiles bytecode to musicxml", decompileOptions, "", false);
                        return;
                    }

                    if (line.getArgs().length == 0) {
                        System.out.println("No input file specified.");
                        return;
                    }

                    System.out.println("Compile not implemented yet.");
                } catch (ParseException e) {
                    System.out.println(e.getMessage() + ". Use cmd decompile -h for a list of available options.");
                }
            }
            case "help" -> {
                System.out.println("The following subcommands are supported:");
                System.out.println("\thelp\t\t\t\tprints all available commands and their explanations");
                System.out.println("\trun <file>\t\t\truns bytecode or plaintext bytecode file");
                System.out.println("\tinterpret <file>\tcompiles music xml to bytecode in memory and runs it");
                System.out.println("\tcompile <file>\t\tcompiles musicxml to bytecode");
                System.out.println("\tdecompile <file>\tdecompiles bytecode to musicxml");

                System.out.println();
                System.out.println("Use the -h option with any command to get a more detailed overview of it's usage.");
            }
            default -> {
                System.out.println("Unrecognized command, use cmd help to see a list of all available commands.");
            }
        }
    }

}
