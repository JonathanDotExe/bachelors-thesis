package at.jku.ssw.hocheneder.musicesolang.compiler;


import at.jku.ssw.hocheneder.musicesolang.interpreter.Interpreter;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MusicCompiler {

    private static final int BASE = Step.values().length;

    private ScorePartwise score;

    public MusicCompiler(InputStream in) throws IOException, Marshalling.UnmarshallingException {
        try (in) {
            score = (ScorePartwise) Marshalling.unmarshal(in);
        }
    }

    public int[] compile() {
        ScorePartwise.Part part = score.getPart().getFirst();
        //For now: always assume C major
        Step root = Step.C;
        List<Integer> code = new ArrayList<>();

        //Measures = commands
        for (ScorePartwise.Part.Measure measure : part.getMeasure()) {
            //Notes => bytes
            Iterator<Note> notes = measure.getNoteOrBackupOrForward().stream().filter(o -> o instanceof Note).map(o -> (Note) o).iterator();
            try {
                //Arg
                int op = nextNumber(notes, root, 2);
                switch (op) {
                    case Interpreter.CONST_x:
                    case Interpreter.JMP_x:
                    case Interpreter.LOAD_x:
                    case Interpreter.STORE_x:
                        int arg = nextNumber(notes, root, 8);
                        code.add(op);
                        code.add(arg);
                    default:
                        code.add(op);
                }

            } catch (InvalidTokenException e) {
                e.printStackTrace();
            }
        }

        return code.stream().mapToInt(i->i).toArray();
    }

    private static int toDigit(Step note, Step root) {
        int diff = note.ordinal() - root.ordinal();
        if (diff < 0) {
            diff += BASE;
        }
        return diff;
    }

    private static int nextDigit(Iterator<Note> notes, Step root) throws InvalidTokenException {
        if (notes.hasNext()) {
            return toDigit(notes.next().getPitch().getStep(), root);
        }
        throw new InvalidTokenException("Invalid instruction, expected another note.");
    }

    private static int nextNumber(Iterator<Note> notes, Step root, int digits) throws InvalidTokenException {
        int num = 0;
        for (int i = digits - 1; i >=0; i--) {
            num *= BASE;
            num += nextDigit(notes, root);
        }
        return num;
    }

}