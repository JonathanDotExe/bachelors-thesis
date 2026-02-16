package at.jku.ssw.hocheneder.musicesolang.music;

import org.audiveris.proxymusic.Step;

public class NoteUtil {

    public static Step fifthsToMajorScaleRootIgnoreAlter(int fifths) {
        switch (fifths) {
            case 0:
                return Step.C;
            case 1:
                return Step.G;
            case 2:
                return Step.D;
            case 3:
                return Step.A;
            case 4:
                return Step.E;
            case 5:
                return Step.B;
            case 6:
                return Step.F; //#
            case -1:
                return Step.F;
            case -2:
                return Step.B; //b
            case -3:
                return Step.E; //b
            case -4:
                return Step.A; //b
            case -5:
                return Step.D; //b
            case -6:
                return Step.G; //b
        }
        throw new IllegalArgumentException("Not a valid amount of fifths");
    }

}
