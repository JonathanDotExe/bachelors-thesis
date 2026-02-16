package at.jku.ssw.hocheneder.musicesolang.compiler;


import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.music.NoteUtil;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.util.Marshalling;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class MusicCompiler {

    private static final int BASE = Step.values().length;

    private final ScorePartwise score;

    public MusicCompiler(InputStream in) throws IOException, Marshalling.UnmarshallingException {
        try (in) {
            score = (ScorePartwise) Marshalling.unmarshal(in);
        }
    }

    public Code compile() {
        ScorePartwise.Part part = score.getPart().getFirst();
        //For now: always assume C major
        Step root = Step.C;
        Code code = new Code();

        //Measures = commands
        for (ScorePartwise.Part.Measure measure : part.getMeasure()) {
            //Root
            Optional<Attributes> attr = measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Attributes)
                    .map(o -> (Attributes) o)
                    .findFirst();
            if (attr.isPresent()) {
                List<Key> key = attr.get().getKey();
                if (!key.isEmpty()) {
                    root = NoteUtil.fifthsToMajorScaleRootIgnoreAlter(key.getFirst().getFifths().intValue());
                }
                //TODO altered key signatures?
            }
            //Notes => bytes
            Iterator<Note> notes = measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Note)
                    .map(o -> (Note) o)
                    .filter(n -> n.getVoice().equals("1")) // only primary voice
                    .iterator();
            try {
                int op = nextNumber(notes, root, 2);
                //Arg
                if (Code.OpCode.hasArg(op)) {
                    int arg = nextNumber(notes, root);
                    code.add(op, arg);
                } else if (Code.OpCode.isValidOpCode(op)){
                    code.add(op);
                }
                else {
                    System.out.println("Invalid opcode encountered: " + op + ". Ignoring ...");
                }

            } catch (InvalidTokenException e) {
                e.printStackTrace();
            }
        }

        return code;
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

    private static int nextNumber(Iterator<Note> notes, Step root) {
        int num = 0;
        while (notes.hasNext()) {
            num *= BASE;
            num += toDigit(notes.next().getPitch().getStep(), root);
        }
        return num;
    }

}