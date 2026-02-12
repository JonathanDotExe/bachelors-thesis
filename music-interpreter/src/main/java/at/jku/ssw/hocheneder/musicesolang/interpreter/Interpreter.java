package at.jku.ssw.hocheneder.musicesolang.interpreter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

//Author: Christoph Pichler
public class Interpreter {
	private static void interpret(int... code) {
		Interpreter interpreter = new Interpreter(code);
		try {
			interpreter.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final int[] code;
	private int pc;
	private final int[] eStack;
	private int ePos;
	private final int[] data;

	private Interpreter(int[] code) {
		this.code = code;
		this.pc = 0;
		this.eStack = new int[100];
		this.ePos = 0;
		this.data = new int[100];
	}

	private void run() throws IOException {
		while (pc >= 0 && pc < code.length) {
			switch (code[pc++]) {
			case ADD:
				push(pop() + pop());
				break;
			case CONST_x:
				push(code[pc++]);
				break;
			case DIV:
				int denom = pop();
				push(pop() / denom);
				break;
			case DUP:
				int val = pop();
				push(val);
				push(val);
				break;
			case IS_NEG:
				push(pop() < 0 ? 1 : 0);
				break;
			case JMP_x:
				if (pop() != 0) {
					pc += code[pc];
				}
				pc++;
				break;
			case LOAD_x:
				push(data[code[pc++]]);
				break;
			case MUL:
				push(pop() * pop());
				break;
			case NEG:
				push(-pop());
				break;
			case NOT:
				push(pop() == 0 ? 1 : 0);
				break;
			case POP:
				pop();
				break;
			case REM:
				denom = pop();
				push(pop() % denom);
				break;
			case STORE_x:
				data[code[pc++]] = pop();
				break;
			case OUT:
				System.out.print((char) (pop()));
				break;
			case IN:
				push(System.in.read());
				break;
			case OUT_INT:
				System.out.print(pop());
				break;
			default:
				throw new IllegalArgumentException("unknown opcode: " + code[pc]);
			}
			;
		}
	}

	private int pop() {
		return eStack[--ePos];
	}

	private void push(int x) {
		eStack[ePos++] = x;
	}

	public static void main(String[] args) {
		/* the current program prints all divisors of N */
		final int N = 60;
		Code code = new Code();
		code.add(CONST_x, N, STORE_x, 0); // int base = N
		code.add(CONST_x, 1, STORE_x, 1); // int i=1
		Code.Label loopStart = code.createLabel();
		loopStart.targetHere();
		code.add(LOAD_x, 0, LOAD_x, 1, NEG, ADD, IS_NEG); // if(base < i)
		Code.Label jumpToEnd = code.createLabel();
		jumpToEnd.sourceHere();
		code.add(JMP_x, 0); // exit
		code.add(LOAD_x, 0, LOAD_x, 1, REM); // load 1, push(base%i),
		// if(base%i!=0)
		Code.Label skipPrint = code.createLabel();
		skipPrint.sourceHere();
		code.add(JMP_x, 0); // skip printing
		code.add(LOAD_x, 1, OUT_INT); // print(i)
		code.add(CONST_x, 10, OUT);// println
		skipPrint.targetHere();
		code.add(CONST_x, 1, LOAD_x, 1, ADD, STORE_x, 1); // i++
		code.add(CONST_x, 1);
		loopStart.sourceHere();
		code.add(JMP_x, 0);// back to 10
		jumpToEnd.targetHere();
		final int[] resolvedCode = code.getCode();
		System.err.println("Code: " + Arrays.toString(resolvedCode));
		interpret(resolvedCode);
	}

	public static final int JMP_x = 0;
	public static final int IS_NEG = 1;
	public static final int NOT = 2;
	public static final int LOAD_x = 3;
	public static final int STORE_x = 4;
	public static final int CONST_x = 5;
	public static final int DUP = 6;
	public static final int POP = 7;
	public static final int ADD = 8;
	public static final int NEG = 9;
	public static final int MUL = 10;
	public static final int DIV = 11;
	public static final int REM = 12;
	public static final int IN = 13;
	public static final int OUT = 14;
	public static final int OUT_INT = 15;

	static class Code {
		final Vector<Integer> code = new Vector<>();
		private final List<Label> labels = new ArrayList<>();

		void add(int... ops) {
			for (int op : ops) {
				code.add(op);
			}
		}

		int[] getCode() {
			labels.forEach(Label::fixup);
			return code.stream().mapToInt(i -> i).toArray();
		}

		Label createLabel() {
			Label l = new Label();
			labels.add(l);
			return l;
		}

		class Label {
			private int sourceAddr = -1;
			private int targetAddr = -1;

			void targetHere() {
				targetAddr = code.size();
			}

			void sourceHere() {
				sourceAddr = code.size() + 1;
			}

			void fixup() {
				if (sourceAddr < 0 || targetAddr < 0) {
					throw new IllegalStateException("Label not resolved!");
				}
				int prev = code.set(sourceAddr, targetAddr - sourceAddr - 1);
				if (prev != 0) {
					throw new IllegalStateException("Override a jump entry");
				}
			}
		}
	}
}
