package at.jku.ssw.hocheneder.musicesolang.interpreter;

import at.jku.ssw.hocheneder.musicesolang.generator.MusicCodeGenerator;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.util.Marshalling;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.IntConsumer;

import static at.jku.ssw.hocheneder.musicesolang.interpreter.Code.OpCode.*;

//Author: Christoph Pichler (abgewandelt)
public class Interpreter {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Interpreter.class);

	public static void interpret(IntConsumer measureCallback, int... code) {
		Interpreter interpreter = new Interpreter(code, measureCallback);
		try {
			interpreter.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void interpret(int... code) {
		interpret(i -> {}, code);
	}

	private final int[] code;
	private int pc;
	private final int[] eStack;
	private int ePos;
	private final int[] data;
	private final IntConsumer measureCallback;

	private Interpreter(int[] code, IntConsumer measureCallback) {
		this.code = code;
		this.measureCallback = measureCallback;
		this.pc = 0;
		this.eStack = new int[100];
		this.ePos = 0;
		this.data = new int[100];
	}

	private void run() throws IOException {
		while (pc >= 0 && pc < code.length) {
			if (code[pc] < 0) {
				//Measure
				int measure = -code[pc++];
                log.debug("Measure {}", measure);
				measureCallback.accept(measure);
			}
			else {
				//Opcode
				log.debug(Code.opToString(code, pc));
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
				log.debug(eStackToString());
			}
		}
	}

	private int pop() {
		return eStack[--ePos];
	}

	private void push(int x) {
		eStack[ePos++] = x;
	}

	private String eStackToString() {
		StringBuilder str = new StringBuilder();
		str.append("Expression Stack: ");
		for (int i = 0; i < ePos; i++) {
			str.append(eStack[i]);
		}
		str.append(System.lineSeparator());
		return str.toString();
	}

	public static void main(String[] args) throws IOException, Marshalling.MarshallingException {
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

		// Compile code
		ScorePartwise score = new MusicCodeGenerator().generate(resolvedCode);
		try (OutputStream out = new FileOutputStream("output.xml")) {
			Marshalling.marshal(score, out, false, 4);
		}
	}

}
