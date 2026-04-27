package at.jku.ssw.hocheneder.musicesolang.interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteCodeIO {

    public static int[] loadCode(InputStream in) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(in.readAllBytes());
        IntBuffer intBuf = buf.asIntBuffer();
        int[] code = new int[buf.array().length/4];
        intBuf.get(code);
        return code;
    }

    public static void writeCode(int[] code, OutputStream out) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(code.length * 4);
        buf.asIntBuffer().put(code);

        out.write(buf.array());
    }

}
