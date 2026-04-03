package at.jku.ssw.hocheneder.musicesolang.player;

import at.jku.ssw.hocheneder.musicesolang.compiler.MeasureStore;
import org.audiveris.proxymusic.ScorePartwise;

import javax.sound.midi.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;

public class MeasureSequencer implements IntConsumer, AutoCloseable, MeasureStore {

    private final Map<Integer, MeasureSequence> measures = new HashMap<>();
    private Synthesizer synth;
    private Sequencer sequencer;


    @Override
    public void accept(int id) {
        if (sequencer == null) {
            throw new IllegalStateException("Sequencer not initialized.");
        }
        if (measures.containsKey(id)) {
            MeasureSequence sequence = measures.get(id);
            try {
                sequence.play(sequencer);
            } catch (InvalidMidiDataException e) {
                throw new RuntimeException(e); //TODO
            }
        }
    }

    public void initialize() throws MidiUnavailableException {
        if (sequencer != null) {
            throw new IllegalStateException("Already initialized.");
        }

        synth = MidiSystem.getSynthesizer();
        synth.open();

        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synth.getReceiver());
    }

    @Override
    public void addMeasure(int id, ScorePartwise.Part.Measure measure) {
        if (sequencer != null) {
            throw new IllegalStateException("Already initialized. Measures can't be added anymore.");
        }
        measures.computeIfAbsent(id, i -> new MeasureSequence((measure)));
    }

    @Override
    public void close() {
        sequencer.close();
        synth.close();
    }
}
