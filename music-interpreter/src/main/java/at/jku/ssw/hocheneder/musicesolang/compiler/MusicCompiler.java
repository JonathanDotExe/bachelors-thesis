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
            Iterator<Note> notes = new NoteIterator(measure);
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

    /**
     * only pos numbers
     *
     * @param notes
     * @param root
     * @param digits
     * @return
     * @throws InvalidTokenException
     */
    private static int nextNumber(Iterator<Note> notes, Step root, int digits) throws InvalidTokenException {
        int num = 0;
        for (int i = digits - 1; i >=0; i--) {
            num *= BASE;
            num += nextDigit(notes, root);
        }
        return num;
    }

    /**
     * Pos/neg numbers
     *
     * @param notes
     * @param root
     * @return
     */
    private static int nextNumber(Iterator<Note> notes, Step root) {
        int num = 0;
        boolean first = true;
        int fac = 1;
        while (notes.hasNext()) {
            Note note = notes.next();
            num *= BASE;
            num += toDigit(note.getPitch().getStep(), root);
            if (first && note.getGrace() != null) {
                fac = -1;
            }
            first = false;
        }
        return num * fac;
    }

    private static class NoteIterator implements Iterator<Note> {

        private final Iterator<Note> base;
        private Note next;

        public NoteIterator(ScorePartwise.Part.Measure measure) {
            base = measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Note)
                    .map(o -> (Note) o)
                    .filter(n -> n.getVoice().equals("1")) // only primary voice
                    .iterator();
            if (base.hasNext()) {
                next = base.next();
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Note next() {
            Note curr = next;
            // Get next
            if (base.hasNext()) {
                next = base.next();
            }
            else {
                next = null;
            }
            //Find top note in Chord
            while (next != null && next.getChord() != null) {
                if (NoteUtil.PITCH_COMPARATOR.compare(next.getPitch(), curr.getPitch()) > 0) {
                    curr = next;
                }
                if (base.hasNext()) {
                    next = base.next();
                }
                else {
                    next = null;
                }
            }
            return curr;
        }
    }

}