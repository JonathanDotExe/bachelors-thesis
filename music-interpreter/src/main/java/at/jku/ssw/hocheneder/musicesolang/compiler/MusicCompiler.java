package at.jku.ssw.hocheneder.musicesolang.compiler;


import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.music.NoteUtil;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.util.Marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.math.BigInteger;
import java.util.*;

public class MusicCompiler {

    public static final int BASE = Step.values().length;

    private final ScorePartwise score;

    public MusicCompiler(InputStream in) throws IOException, Marshalling.UnmarshallingException {
        try (in) {
            score = (ScorePartwise) Marshalling.unmarshal(in);
        }
    }

    public Code compile() {
        return compile(false, null, "P1");
    }

    public Code compile(boolean writeMeasure, MeasureStore store, String partId) {
        Optional<ScorePartwise.Part> opt = score.getPart().stream().filter(p -> partId.equals(((ScorePart) p.getId()).getId())).findFirst();

        if (opt.isEmpty()) {
            throw new RuntimeException("Part with the given id " + partId + " does not exist.");
        }

        ScorePartwise.Part part = opt.get(); //get part with id
        //For now: always assume C major
        Step root = Step.C;
        Code code = new Code();

        Set<Integer> labels = new TreeSet<>();

        //Measures = commands
        int measureId = 1;
        Map<String, Integer> numToId = new HashMap<>();
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
                //TODO what if multiple attributes?
            }
            int startPc = code.length();
            //Encode measure
            if (writeMeasure) {
                code.add(-measureId);
            }
            numToId.put(measure.getNumber(), measureId);

            //Notes => bytes
            Iterator<Note> notes = new NoteIterator(measure);
            try {
                int op = nextNumber(notes, root, 2) % Code.OpCode.OPCODES.size();
                //Arg
                if (Code.OpCode.hasArg(op)) {
                    int arg = nextNumber(notes, root);
                    if (op == Code.OpCode.JMP_x) { //Jump source
                        code.getLabel(arg).sourceHere();
                        code.add(op, 0);
                    }
                    else {
                        code.add(op, arg);
                    }
                } else if (Code.OpCode.isValidOpCode(op)){
                    code.add(op);
                }
                else {
                    System.out.println("Invalid opcode encountered: " + op + ". Ignoring ...");
                }

            } catch (InvalidTokenException e) {
                e.printStackTrace();
            }
            int endPc = code.length();
            //Labels
            measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Barline)
                    .map(o -> (Barline) o)
                    .forEach(b -> {
                        if (b.getBarStyle() != null &&
                                b.getRepeat() == null &&
                                (b.getBarStyle().getValue() == BarStyle.HEAVY_HEAVY || b.getBarStyle().getValue() == BarStyle.LIGHT_LIGHT)) {
                            if (b.getLocation() == null || b.getLocation() == RightLeftMiddle.RIGHT) {
                                labels.add(endPc);
                            }
                            else if (b.getLocation() == RightLeftMiddle.LEFT) {
                                labels.add(startPc);
                            }
                        }
                    });

            measureId++;
        }

        //Add labels
        for (int l : labels) {
            Code.Label label = code.createLabel();
            label.targetHere(l);
        }

        // Store measures
        // Note: measures that do not occur in the main line are not stored/played by the sequencer
        if (store != null) {
            for (ScorePartwise.Part p : score.getPart()) {
                for (ScorePartwise.Part.Measure measure : p.getMeasure()) {
                    if (numToId.containsKey(measure.getNumber())) {
                        store.addMeasure(numToId.get(measure.getNumber()), measure);
                    }
                }
            }
        }

        return code;
    }

    public static int toDigit(Step note, Step root) {
        int diff = note.ordinal() - root.ordinal();
        if (diff < 0) {
            diff += BASE;
        }
        return diff;
    }

    private static int nextDigit(Iterator<Note> notes, Step root) throws InvalidTokenException {
        while (notes.hasNext()) {
            Note next = notes.next();
            if (next.getGrace() == null) {  // ignore grace notes
                return toDigit(next.getPitch().getStep(), root);
            }
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
            if (note.getGrace() != null) { //ignore grace notes
                if (first) {
                    fac = -1;
                }
            }
            else {
                num *= BASE;
                num += toDigit(note.getPitch().getStep(), root);
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
                    .filter(n -> "1".equals(n.getVoice()) && BigInteger.ONE.equals(n.getStaff()) && n.getPitch() != null) // only primary voice
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