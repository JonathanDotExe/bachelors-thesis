package at.jku.ssw.hocheneder.musicesolang.generator;

import at.jku.ssw.hocheneder.musicesolang.compiler.MusicCompiler;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;
import org.audiveris.proxymusic.*;
import org.audiveris.proxymusic.opus.Score;

public class MusicCodeGenerator {

        public ScorePartwise generate(int[] code) {
            Step root = Step.C;

            ScorePartwise score = new ScorePartwise();
            ScorePartwise.Part part = new ScorePartwise.Part();
            part.setId("P1");

            //Measures
            int count = 1;
            for (int i = 0; i < code.length; i++) {
                if (code[i] >= 0) { //ignore measure id markings
                    ScorePartwise.Part.Measure measure = new ScorePartwise.Part.Measure();
                    measure.setNumber(count + "");

                    int op = code[i];
                    int arg = 0;
                    if (Code.OpCode.hasArg(op)) {
                        arg = code[++i];
                    }

                    Step[] steps = MusicCompiler.toSteps(root, op, arg);

                    //Create notes
                    for (Step step : steps) {
                        Note note = new Note();
                        Pitch pitch = new Pitch();
                        pitch.setStep(step);
                        note.setPitch(pitch);

                        NoteType type = new NoteType();
                        type.setValue("eigth");
                        note.setType(type); //TODO dynamically scale size


                        measure.getNoteOrBackupOrForward().add(note);
                        // TODO generate label jumps
                    }

                    count++;
                    part.getMeasure().add(measure);
                }
            }


            score.getPart().add(part);

            return score;
        }

}
