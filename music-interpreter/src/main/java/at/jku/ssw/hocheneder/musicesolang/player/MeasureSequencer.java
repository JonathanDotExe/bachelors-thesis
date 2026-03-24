package at.jku.ssw.hocheneder.musicesolang.player;

import org.audiveris.proxymusic.ScorePartwise;

import javax.sound.midi.*;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MeasureSequencer implements IntConsumer, AutoCloseable {

    private Map<Integer, MeasureSequence> measures = new HashMap<>();
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

        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();

        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synth.getReceiver());
    }

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
