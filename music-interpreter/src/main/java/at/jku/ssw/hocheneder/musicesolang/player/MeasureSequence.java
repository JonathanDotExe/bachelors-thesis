package at.jku.ssw.hocheneder.musicesolang.player;

import at.jku.ssw.hocheneder.musicesolang.music.NoteUtil;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.NoteType;
import org.audiveris.proxymusic.ScorePartwise;

import javax.sound.midi.*;
import java.util.Iterator;

public class MeasureSequence {

    private final Sequence sequence;

    public MeasureSequence(ScorePartwise.Part.Measure measure) {
        sequence = measureToSequence(measure);
    }

    public void play(Sequencer sequencer) throws InvalidMidiDataException {
        sequencer.setSequence(sequence);
        sequencer.setTickPosition(0);
        sequencer.setTempoInBPM(240);
        sequencer.start();

        while (sequencer.isRunning()) { //await end
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                //Ignore
            }
        }
    }

    private static Sequence measureToSequence(ScorePartwise.Part.Measure measure) {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 1024 / 4);
            Track track = sequence.createTrack();
            int ticks = 0;
            int lastDur = 0;
            Iterator<Note> iter = measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Note)
                    .map(o -> (Note) o)
                    .filter(n -> n.getVoice().equals("1")) // FIXME: consider multiple voices and staffs);
                    .iterator();
            while (iter.hasNext()) {
                Note n = iter.next();
                if (n.getChord() == null) {
                    ticks += lastDur;
                }
                NoteType type = n.getType();
                int noteValue = type != null ? NoteUtil.REVERSE_NOTE_VALUES.getOrDefault(type.getValue(), 4) : 4;
                lastDur = 1024 / noteValue;
                if (n.getPitch() != null) { //Note
                    int pitch = NoteUtil.pitchToMidiNote(n.getPitch());
                    ShortMessage on = new ShortMessage();
                    on.setMessage(ShortMessage.NOTE_ON, 0, pitch, 127);
                    ShortMessage off = new ShortMessage();
                    off.setMessage(ShortMessage.NOTE_OFF, 0, pitch, 127);

                    track.add(new MidiEvent(on, ticks));
                    track.add(new MidiEvent(off, ticks + lastDur));
                }
            }
            //Place CC at end so it finishes bar with rests
            ticks += lastDur;
            ShortMessage cc = new ShortMessage();
            cc.setMessage(ShortMessage.CONTROL_CHANGE, 0, 1, 127);
            track.add(new MidiEvent(cc, ticks));
            return sequence;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

}
