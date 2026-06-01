package at.jku.ssw.hocheneder.musicesolang.interpreter;

import java.util.*;

//Author: Christoph Pichler (abgewandelt)
public class Code {

    public static final class OpCode {

        public static final int JMP_x = 0;      // 1, 1 = C, C
        public static final int IS_NEG = 1;     // 1, 2 = C, D
        public static final int NOT = 2;        // 1, 3 = C, E
        public static final int LOAD_x = 3;     // 1, 4 = C, F
        public static final int STORE_x = 4;    // 1, 5 = C, G
        public static final int CONST_x = 5;    // 1, 6 = C, A
        public static final int DUP = 6;        // 1, 7 = C, B
        public static final int POP = 7;        // 2, 1 = D, C
        public static final int ADD = 8;        // 2, 2 = D, D
        public static final int NEG = 9;        // 2, 3 = D, E
        public static final int MUL = 10;       // 2, 4 = D, F
        public static final int DIV = 11;       // 2, 5 = D, G
        public static final int REM = 12;       // 2, 6 = D, A
        public static final int IN = 13;        // 2, 7 = D, B
        public static final int OUT = 14;       // 3, 1 = E, C
        public static final int OUT_INT = 15;   // 3, 1 = E, D

        public static final Map<Integer, String> OPCODES = Map.ofEntries(
                Map.entry(JMP_x, "JMP"),
                Map.entry(IS_NEG, "IS_NEG"),
                Map.entry(NOT, "NOT"),
                Map.entry(LOAD_x, "LOAD"),
                Map.entry(STORE_x, "STORE"),
                Map.entry(CONST_x, "CONST"),
                Map.entry(DUP, "DUP"),
                Map.entry(POP, "POP"),
                Map.entry(ADD, "ADD"),
                Map.entry(NEG, "NEG"),
                Map.entry(MUL, "MUL"),
                Map.entry(DIV, "DIV"),
                Map.entry(REM, "REM"),
                Map.entry(IN, "IN"),
                Map.entry(OUT, "OUT"),
                Map.entry(OUT_INT, "OUT_INT")
        );

        private OpCode() {

        }

        public static boolean hasArg(int opCode) {
            return switch (opCode) {
                //Argument to the opcode
                case CONST_x, JMP_x, LOAD_x, STORE_x -> true;
                default -> false;
            };
        }

        public static boolean isValidOpCode(int opCode) {
            return OPCODES.containsKey(opCode);
        }

    }

    private final Vector<Integer> code = new Vector<>();
    private final Map<String, Label> labels = new TreeMap<>();
    private int nextLabel = 0;

    public void add(int... ops) {
        for (int op : ops) {
            code.add(op);
        }
    }

    public int length() {
        return code.size();
    }

    public int[] getCode() {
        labels.values().forEach(Label::fixup);
        return code.stream().mapToInt(i -> i).toArray();
    }

    public Label createLabel() {
        if (labels.containsKey("l" + nextLabel)) {
            return labels.get("l" + nextLabel++);
        }
        Label l = new Label();
        labels.put("l" + nextLabel++, l);
        return l;
    }

    public Label getLabel(String id) {
        if (labels.containsKey(id)) {
            return labels.get(id);
        }

        Label l = new Label();
        labels.put(id, l);
        return l;
    }

    public Label getLabel(int id) {
        return getLabel("l" + id);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        boolean arg = false;
        for (int op : code) {
            if (arg) {
                str.append(op);
                str.append(System.lineSeparator());
                arg = false;
            }
            else {
                if (op >= 0 ) { //Ignore measure marking
                    str.append(OpCode.OPCODES.getOrDefault(op, "INVALID"));
                    if (OpCode.hasArg(op)) {
                        str.append(" ");
                        arg = true;
                    } else {
                        str.append(System.lineSeparator());
                    }
                }
            }
        }

        return str.toString();
    }

    public static String opToString(int[] code, int index) {
        int op = code[index];
        StringBuilder str = new StringBuilder();
        str.append(OpCode.OPCODES.getOrDefault(op, "INVALID"));
        if (OpCode.hasArg(op)) {
            str.append(" ");
            str.append(code[index + 1]);
        }
        return str.toString();
    }

    public class Label {
        private List<Integer> sourceAddr = new ArrayList<>();
        private int targetAddr = -1;

        public void targetHere() {
            targetAddr = code.size();
        }

        public void targetHere(int here) {
            targetAddr = here;
        }

        public void sourceHere() {
            sourceAddr.add(code.size() + 1);
        }

        public void fixup() {
            if (targetAddr < 0) {
                throw new IllegalStateException("Label not resolved!");
            }
            for (int src : sourceAddr) {
                int prev = code.set(src, targetAddr - src - 1);
                if (prev != 0) {
                    throw new IllegalStateException("Override a jump entry");
                }
            }
        }
    }
}
