package at.jku.ssw.hocheneder.musicesolang.decompiler;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import org.audiveris.proxymusic.*;

import java.math.BigDecimal;
import java.util.*;

import static at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler.BASE;

public class MusicDecompiler {

    private int[] code;
    private int count = 1;
    private Map<Integer, DecompilerLabel> labels = new TreeMap<>();
    private Map<Integer, ScorePartwise.Part.Measure> measures = new TreeMap<>();


    public MusicDecompiler(int[] code) {
        this.code = code;
    }

    public ScorePartwise generate() {
        Step root = Step.C;

        ScorePartwise score = new ScorePartwise();
        //Partlist
        ScorePart scorePart = new ScorePart();
        //scorePart.setId("P1");
        score.setPartList(new PartList());
        score.getPartList().getPartGroupOrScorePart().add(scorePart);

        //Part
        ScorePartwise.Part part = new ScorePartwise.Part();
        //part.setId("P1");

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

                Step[] steps = toSteps(root, op, arg);

                //Create notes
                for (Step step : steps) {
                    Note note = new Note();
                    Pitch pitch = new Pitch();
                    pitch.setStep(step);
                    note.setPitch(pitch);

                    NoteType type = new NoteType();
                    type.setValue("eigth");
                    note.setType(type); //TODO dynamically scale size

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

                measure.getNoteOrBackupOrForward().add(0, barline);

                //Fixup
                entry.getValue().fixup(root, id);
            }
            else {
                throw new IllegalArgumentException("Invalid jump in code.");
            }
            id++;
        }


        score.getPart().add(part);

        return score;
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
            steps.add(0, byteToStep(root, arg % BASE));
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

                    NoteType type = new NoteType();
                    type.setValue("eigth");
                    note.setType(type); //TODO dynamically scale size

                    note.setVoice("1");
                    note.setDuration(BigDecimal.ONE);

                    measure.getNoteOrBackupOrForward().add(note);
                }
            }
        }

    }
}
