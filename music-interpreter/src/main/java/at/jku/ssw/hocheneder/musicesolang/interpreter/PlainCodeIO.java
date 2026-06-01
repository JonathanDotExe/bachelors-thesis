package at.jku.ssw.hocheneder.musicesolang.interpreter;

import at.jku.ssw.hocheneder.musicesolang.interpreter.coco.Parser;
import at.jku.ssw.hocheneder.musicesolang.interpreter.coco.Scanner;

import java.io.IOException;
import java.io.InputStream;

public class PlainCodeIO {

    public static int[] loadCode(InputStream in) throws IOException {
        try (in) {
            Code code = new Code();
            Scanner scanner = new Scanner(in);
            Parser parser = new Parser(scanner, code);
            parser.Parse();

            return code.getCode();
        }
    }

}
