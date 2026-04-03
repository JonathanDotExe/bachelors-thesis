package at.jku.ssw.hocheneder.musicesolang.decompiler;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import at.jku.ssw.hocheneder.musicesolang.music.NoteUtil;
import jakarta.xml.bind.JAXBElement;
import org.audiveris.proxymusic.*;

import javax.xml.namespace.QName;
import java.lang.String;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

import static at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler.BASE;
import static at.jku.ssw.hocheneder.musicesolang.music.NoteUtil.NOTE_VALUES;

public class MusicDecompiler {

    private int[] code;
    private int count = 1;
    private Map<Integer, DecompilerLabel> labels = new TreeMap<>();
    private Map<Integer, ScorePartwise.Part.Measure> measures = new TreeMap<>();



    public MusicDecompiler(int[] code) {
        this.code = code;
    }

    public ScorePartwise generate(boolean randomize) {
        Random random = new Random();
        Step root = Step.C;

        ScorePartwise score = new ScorePartwise();
        //Partlist
        ScorePart scorePart = new ScorePart();
        scorePart.setId("P1");
        PartName name = new PartName();
        name.setValue("P1");
        scorePart.setPartName(name);
        score.setPartList(new PartList());
        score.getPartList().getPartGroupOrScorePart().add(scorePart);

        //Part
        ScorePartwise.Part part = new ScorePartwise.Part();
        part.setId(scorePart);

        //Measures
        for (int i = 0; i < code.length; i++) {
            ScorePartwise.Part.Measure measure = new ScorePartwise.Part.Measure();
            while (i < code.length && code[i] < 0) { //ignore measure id markings
                measures.put(i, measure);
                i++;
            }

            if (i < code.length) {
                measures.put(i, measure);
                measure.setNumber(count + "");

                int op = code[i];
                int arg = 0;
                if (Code.OpCode.hasArg(op)) {
                    arg = code[++i];
                }

                if (op == Code.OpCode.JMP_x) { //Handle jump
                    DecompilerLabel label = labels.computeIfAbsent(arg + i + 1, c -> new DecompilerLabel());
                    label.sourceHere(measure);
                    arg = 0; //Dont write argument
                }

                if (randomize) {
                    op += random.nextInt(3) * Code.OpCode.OPCODES.size();
                }

                Step[] steps = toSteps(root, op, arg);

                //Create notes
                for (Step step : steps) {
                    Note note = new Note();
                    Pitch pitch = new Pitch();
                    pitch.setStep(step);
                    note.setPitch(pitch);
                    pitch.setOctave(4);

                    NoteType type = new NoteType();
                    type.setValue("eighth");
                    note.setType(type);

                    note.setVoice("1");
                    note.setDuration(BigDecimal.ONE);

                    measure.getNoteOrBackupOrForward().add(note);
                }

                count++;
                part.getMeasure().add(measure);
            }
        }

        //Buffer measure
        ScorePartwise.Part.Measure bufferMeasure = new ScorePartwise.Part.Measure();
        measures.put(code.length, bufferMeasure);
        bufferMeasure.setNumber(count++ + "");
        part.getMeasure().add(bufferMeasure);

        //Resolve labels
        int id = 0;
        for (Map.Entry<Integer, DecompilerLabel> entry : labels.entrySet()) {
            if (measures.containsKey(entry.getKey())) {
                ScorePartwise.Part.Measure measure = measures.get(entry.getKey());
                //Insert barline
                Barline barline = new Barline();
                BarStyleColor barStyleColor = new BarStyleColor();
                barStyleColor.setValue(BarStyle.HEAVY_HEAVY);
                barline.setBarStyle(barStyleColor);
                barline.setLocation(RightLeftMiddle.LEFT);

                measure.getNoteOrBackupOrForward().addFirst(barline);

                //Fixup
                entry.getValue().fixup(root, id);
            }
            else {
                throw new IllegalArgumentException("Invalid jump in code.");
            }
            id++;
        }

        //Normalize note values
        for (ScorePartwise.Part.Measure measure : part.getMeasure()) {
            int amount = findNoteCount(measure);
            if (amount == 0) {
                // Insert rest
                Note note = new Note();
                note.setRest(new Rest());

                NoteType type = new NoteType();
                type.setValue("whole");
                note.setType(type);

                note.setVoice("1");
                note.setDuration(BigDecimal.ONE);

                measure.getNoteOrBackupOrForward().add(note);
            }
            else {
                int subdivision = NoteUtil.findNextPowerOf2(amount);

                if (!NOTE_VALUES.containsKey(subdivision)) {
                    throw new IllegalStateException("Invalid amount of subdivisions " + subdivision);
                }

                //Set subdivisions
                Stream<Note> notes = measure.getNoteOrBackupOrForward().stream().filter(o -> o instanceof Note)
                        .map(o -> (Note) o)
                        .filter(n -> "1".equals(n.getVoice()) && BigInteger.ONE.equals(n.getStaff()) && n.getPitch() != null);

                notes.forEach(n -> {
                    NoteType type = new NoteType();
                    type.setValue(NOTE_VALUES.get(subdivision));
                    n.setType(type);
                });

                //Breaks
                int stepsLeft = subdivision - amount;
                int index = measure.getNoteOrBackupOrForward().size();
                while (stepsLeft > 0) {
                    int nextSub = NoteUtil.findPrevPowerOf2(stepsLeft);
                    int value = subdivision / nextSub;
                    if (!NOTE_VALUES.containsKey(value)) {
                        throw new IllegalStateException("Invalid amount of subdivisions " + subdivision);
                    }

                    // Insert rest
                    Note note = new Note();
                    note.setRest(new Rest());

                    NoteType type = new NoteType();
                    type.setValue(NOTE_VALUES.get(value));
                    note.setType(type);

                    note.setVoice("1");
                    note.setDuration(BigDecimal.ONE);

                    measure.getNoteOrBackupOrForward().add(index, note); // => smallest rest first

                    stepsLeft -= nextSub;
                }
            }
        }

        //Attributes
        Attributes attributes = new Attributes();
        Key key = new Key();
        key.setFifths(BigInteger.ZERO);
        attributes.getKey().add(key);

        Time time = new Time();
        time.getTimeSignature().add(new JAXBElement<>(new QName("beats"), java.lang.String.class, "4"));
        time.getTimeSignature().add(new JAXBElement<>(new QName("beat-type"), java.lang.String.class, "4"));
        attributes.getTime().add(time);

        part.getMeasure().getFirst().getNoteOrBackupOrForward().addFirst(attributes);


        score.getPart().add(part);

        return score;
    }

    private int findNoteCount(ScorePartwise.Part.Measure measure) {
        return (int) measure.getNoteOrBackupOrForward().stream().filter(o -> o instanceof Note)
                .map(o -> (Note) o)
                .filter(n -> "1".equals(n.getVoice()) && BigInteger.ONE.equals(n.getStaff()) && n.getChord() == null).count(); // only primary voice
    }

    private static Step[] toSteps(Step root, int op, int arg) {
        List<Step> steps = new LinkedList<>();

        //Opcode
        steps.add(byteToStep(root, op / BASE));
        steps.add(byteToStep(root, op % BASE));
        //Arg
        while (arg > 0) {
            steps.add(2, byteToStep(root, arg % BASE));
            arg /= BASE;
        }

        return steps.toArray(Step[]::new);
    }

    private static Step[] argToSteps(Step root, int arg) {
        List<Step> steps = new LinkedList<>();

        //Arg
        while (arg > 0) {
            steps.addFirst(byteToStep(root, arg % BASE));
            arg /= BASE;
        }

        return steps.toArray(Step[]::new);
    }

    private static Step byteToStep(Step root, int b) {
        int diff = b + root.ordinal();
        if (diff >= BASE) {
            diff -= BASE;
        }
        return Step.values()[diff];
    }

    class DecompilerLabel {
        private List<ScorePartwise.Part.Measure> sourceMeasures = new ArrayList<>();

        public void sourceHere(ScorePartwise.Part.Measure measure) {
            sourceMeasures.add(measure);
        }

        public void fixup(Step root, int index) {
            Step[] steps = argToSteps(root, index);

            for (ScorePartwise.Part.Measure measure : sourceMeasures) {
                //Create notes
                for (Step step : steps) {
                    Note note = new Note();
                    Pitch pitch = new Pitch();
                    pitch.setStep(step);
                    note.setPitch(pitch);
                    pitch.setOctave(4);

                    NoteType type = new NoteType();
                    type.setValue("eighth");
                    note.setType(type);

                    note.setVoice("1");
                    note.setDuration(BigDecimal.ONE);

                    measure.getNoteOrBackupOrForward().add(note);
                }
            }
        }

    }
}
