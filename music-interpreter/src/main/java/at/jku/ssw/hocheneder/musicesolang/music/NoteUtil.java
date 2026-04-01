package at.jku.ssw.hocheneder.musicesolang.music;

import at.jku.ssw.hocheneder.musicesolang.interpreter.Interpreter;
import org.audiveris.proxymusic.Pitch;
import org.audiveris.proxymusic.Step;

import java.util.Comparator;
import java.util.Map;

public class NoteUtil {


    public static final Map<Integer, String> NOTE_VALUES = Map.ofEntries(
            Map.entry(1, "whole"),
            Map.entry(2, "half"),
            Map.entry(4, "quarter"),
            Map.entry(8, "eighth"),
            Map.entry(16, "16th"),
            Map.entry(32, "32nd"),
            Map.entry(64, "64th"),
            Map.entry(128, "128th"),
            Map.entry(256, "256th"),
            Map.entry(512, "512th"),
            Map.entry(1024, "1024th")
    );

    public static final Map<String, Integer> REVERSE_NOTE_VALUES = Map.ofEntries(
            Map.entry("whole", 1),
            Map.entry("half", 2),
            Map.entry("quarter", 4),
            Map.entry("eighth", 8),
            Map.entry("16th", 16),
            Map.entry("32nd", 32),
            Map.entry("64th", 64),
            Map.entry("128th", 128),
            Map.entry("256th", 256),
            Map.entry("512th", 512),
            Map.entry("1024th", 1024)
    );

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
        int alter = p.getAlter() != null ? p.getAlter().intValue() : 0;
        return 60 + 12 * (p.getOctave() - 4) + toPitchOct(p.getStep()) + alter; //TODO consider decimal
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

    public static int findPrevPowerOf2(int num) {
        return 1 << (32 - Integer.numberOfLeadingZeros(num) - 1);
    }

    public static int findNextPowerOf2(int num) {
        int prev = findPrevPowerOf2(num);
        return num == prev ? num : prev * 2;
    }

}
