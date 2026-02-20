package at.jku.ssw.hocheneder.musicesolang.music;

import org.audiveris.proxymusic.Pitch;
import org.audiveris.proxymusic.Step;

import java.util.Comparator;

public class NoteUtil {

    public static final Comparator<Pitch> PITCH_COMPARATOR = Comparator
            .comparing(NoteUtil::pitchToMidiNote)
            .thenComparing(Pitch::getOctave)
            .thenComparing(p -> (p.getStep().ordinal() - 2) % Step.values().length)
            .thenComparing(Pitch::getAlter);

    public static Step fifthsToMajorScaleRootIgnoreAlter(int fifths) {
        return switch (fifths) {
            case 0 -> Step.C;
            case 1 -> Step.G;
            case 2 -> Step.D;
            case 3 -> Step.A;
            case 4 -> Step.E;
            case 5 -> Step.B;
            case 6 -> Step.F; //#
            case -1 -> Step.F;
            case -2 -> Step.B; //b
            case -3 -> Step.E; //b
            case -4 -> Step.A; //b
            case -5 -> Step.D; //b
            case -6 -> Step.G; //b
            default -> throw new IllegalArgumentException("Not a valid amount of fifths");
        };
    }


    public static int pitchToMidiNote(Pitch p) {
        return 60 + 12 * (p.getOctave() - 4) + toPitchOct(p.getStep()) + p.getAlter().intValue(); //TODO consider decimal
    }

    public static int toPitchOct(Step step) {
        switch (step) {
            case A -> {
                return 9;
            }
            case B -> {
                return 11;
            }
            case C -> {
                return 0;
            }
            case D -> {
                return 2;
            }
            case E -> {
                return 4;
            }
            case F -> {
                return 5;
            }
            case G -> {
                return 7;
            }
        }
        return 0;
    }
}
