package at.jku.ssw.hocheneder.musicesolang;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Interpreter;
import at.jku.ssw.hocheneder.musicesolang.player.MeasureSequencer;
import org.audiveris.proxymusic.util.Marshalling;

import javax.sound.midi.MidiUnavailableException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) throws IOException, Marshalling.UnmarshallingException, MidiUnavailableException {
        String filename = args.length > 0 ? args[0] : "output.xml";
        System.out.println("Loading file " + filename);
        try (InputStream input = new FileInputStream(filename);
             MeasureSequencer sequencer = new MeasureSequencer();) {
            MusicCompiler compiler = new MusicCompiler(input);

            System.out.println("Compiling ...");
            Code code = compiler.compile(true, sequencer);
            System.out.println("Compilation successful.");
            System.out.println("Generated code:");
            System.out.println(code);

            System.out.println();
            System.out.println("Executing ... ");
            System.out.println();

            sequencer.initialize();

            Interpreter.interpret(sequencer, code.getCode());

            System.out.println();
            System.out.println("Finished");
        }
    }

}
