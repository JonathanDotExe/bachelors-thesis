package at.jku.ssw.hocheneder.musicesolang.compiler;

import at.jku.ssw.hocheneder.musicesolang.player.MeasureSequence;
import org.audiveris.proxymusic.ScorePartwise;

public interface MeasureStore {

    void addMeasure(int id, ScorePartwise.Part.Measure measure);

}
