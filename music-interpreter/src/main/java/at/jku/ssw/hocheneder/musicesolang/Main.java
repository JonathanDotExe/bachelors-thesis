package at.jku.ssw.hocheneder.musicesolang;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Interpreter;
import at.jku.ssw.hocheneder.musicesolang.player.MeasureSequencer;
import org.apache.commons.cli.*;
import org.audiveris.proxymusic.util.Marshalling;

import javax.sound.midi.MidiUnavailableException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Main {

    private static String PLAY_OPTION = "play";
    private static String VERBOSE_OPTION = "verbose";
    private static String PART_OPTION = "part";
    private static String PLAIN_OPTION = "plain";
    private static String NO_MEASURES_OPTION = "no-measures";
    private static String OUTPUT_OPTION = "output";



    public static void main(String[] args) throws IOException, Marshalling.UnmarshallingException, MidiUnavailableException {
        if (args.length == 0) {
            System.out.println("Error, no command specified. Use command help for more information.");
            return;
        }


        String[] optionArr = Arrays.copyOfRange(args, 1, args.length);

        Options runOptions = new Options();
        runOptions.addOption(Option.builder(PLAY_OPTION)
                .desc("plays the given music xml file in sync")
                .hasArg()
                .get()
        );
        runOptions.addOption(Option.builder(VERBOSE_OPTION)
                .desc("shows debug messages with each instruction")
                .get()
        );

        Options interpretOptions = new Options();
        interpretOptions.addOption(Option.builder(PLAY_OPTION)
                .desc("plays the given music xml file in sync")
                .get()
        );
        interpretOptions.addOption(Option.builder(VERBOSE_OPTION)
                .desc("shows debug messages with each instruction")
                .get()
        );
        interpretOptions.addOption(Option.builder(PART_OPTION)
                .desc("specifies the part id to use in the mxl file")
                .hasArg()
                .get()
        );

        Options compileOptions = new Options();
        compileOptions.addOption(Option.builder(NO_MEASURES_OPTION)
                .desc("doesn't encode measure markings for playback")
                .get()
        );
        compileOptions.addOption(Option.builder(PLAIN_OPTION)
                .desc("uses plaintext bytecode format")
                .get()
        );
        compileOptions.addOption(Option.builder(OUTPUT_OPTION)
                .desc("specifies the output file")
                .hasArg()
                .get()
        );
        compileOptions.addOption(Option.builder(PART_OPTION)
                .desc("specifies the part id to use in the mxl file")
                .hasArg()
                .get()
        );

        Options decompileOptions = new Options();
        decompileOptions.addOption(Option.builder(OUTPUT_OPTION)
                .desc("specifies the output file")
                .hasArg()
                .get()
        );


        CommandLineParser parser = new DefaultParser();

        switch (args[0]) {
            case "run" -> {
                try {
                    CommandLine line = parser.parse(runOptions, optionArr);

                    System.out.println("Run not implemented yet.");
                } catch (ParseException e) {
                    e.printStackTrace(); //TODO
                }
            }
            case "interpret" -> {
                try {
                    CommandLine line = parser.parse(interpretOptions, optionArr);

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
                    e.printStackTrace(); //TODO
                }
            }
            case "compile" -> {
                try {
                    CommandLine line = parser.parse(compileOptions, optionArr);

                    if (line.getArgs().length == 0) {
                        System.out.println("No input file specified.");
                        return;
                    }

                    System.out.println("Compile not implemented yet.");
                } catch (ParseException e) {
                    e.printStackTrace(); //TODO
                }
            }
            case "decompile" -> {
                try {
                    CommandLine line = parser.parse(decompileOptions, optionArr);

                    if (line.getArgs().length == 0) {
                        System.out.println("No input file specified.");
                        return;
                    }

                    System.out.println("Compile not implemented yet.");
                } catch (ParseException e) {
                    e.printStackTrace(); //TODO
                }
            }
            case "help" -> {

            }
        }
    }

}
