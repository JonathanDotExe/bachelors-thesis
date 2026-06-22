package at.jku.ssw.hocheneder.musicesolang.interpreter;

import at.jku.ssw.hocheneder.musicesolang.interpreter.coco.Parser;
import at.jku.ssw.hocheneder.musicesolang.interpreter.coco.Scanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

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

    public static void writeCode(Code code, OutputStream out) throws IOException {
        try (PrintStream pr = new PrintStream(out)) {
            pr.print(code.toString());
        }
    }

}
