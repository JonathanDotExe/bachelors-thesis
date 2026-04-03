package at.jku.ssw.hocheneder.musicesolang.player;

import at.jku.ssw.hocheneder.musicesolang.music.NoteUtil;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.NoteType;
import org.audiveris.proxymusic.ScorePartwise;

import javax.sound.midi.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

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
        // FIXME: consider staffs
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 1024 / 4);
            // Track
            Map<VoiceKey, VoiceTrack> tracks = new HashMap<>();
            measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Note)
                    .map(o -> new VoiceKey(((Note) o).getVoice(), ((Note) o).getStaff().intValue()))
                    .distinct()
                    .forEach(v -> {
                        tracks.put(v, new VoiceTrack(sequence.createTrack()));
                    });

            // Iterate notes
            Iterator<Note> iter = measure.getNoteOrBackupOrForward().stream()
                    .filter(o -> o instanceof Note)
                    .map(o -> (Note) o)
                    .iterator();

            while (iter.hasNext()) {
                Note n = iter.next();
                VoiceTrack track = tracks.get(new VoiceKey(n.getVoice(), n.getStaff().intValue()));
                if (n.getChord() == null) {
                    track.ticks += track.lastDur;
                }
                NoteType type = n.getType();
                int noteValue = type != null ? NoteUtil.REVERSE_NOTE_VALUES.getOrDefault(type.getValue(), 4) : 4;
                track.lastDur = 1024 / noteValue;
                if (n.getPitch() != null) { //Note
                    int pitch = NoteUtil.pitchToMidiNote(n.getPitch());
                    ShortMessage on = new ShortMessage();
                    on.setMessage(ShortMessage.NOTE_ON, 0, pitch, 127);
                    ShortMessage off = new ShortMessage();
                    off.setMessage(ShortMessage.NOTE_OFF, 0, pitch, 127);

                    track.track.add(new MidiEvent(on, track.ticks));
                    track.track.add(new MidiEvent(off, track.ticks + track.lastDur));
                }
            }
            //Place CC at end so it finishes bar with rests
            for (VoiceTrack track : tracks.values()) {
                track.ticks += track.lastDur;
                ShortMessage cc = new ShortMessage();
                cc.setMessage(ShortMessage.CONTROL_CHANGE, 0, 1, 127);
                track.track.add(new MidiEvent(cc, track.ticks));
            }
            return sequence;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static class VoiceTrack {

        public int ticks = 0;
        public int lastDur = 0;
        public final Track track;

        private VoiceTrack(Track track) {
            this.track = track;
        }
    }

    private static class VoiceKey {

        public final String voice;
        public final int part;

        private VoiceKey(String voice, int part) {
            this.voice = voice;
            this.part = part;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            VoiceKey voiceKey = (VoiceKey) o;
            return part == voiceKey.part && Objects.equals(voice, voiceKey.voice);
        }

        @Override
        public int hashCode() {
            return Objects.hash(voice, part);
        }
    }


}
