
package at.jku.ssw.hocheneder.musicesolang.interpreter.coco;

import static at.jku.ssw.hocheneder.musicesolang.interpreter.Code.OpCode.OPCODES_REV;
import at.jku.ssw.hocheneder.musicesolang.interpreter.Code;

public class Parser {
	public static final int _EOF = 0;
	public static final int _number = 1;
	public static final int _ident = 2;
	public static final int maxT = 5;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;
	public Code code;

	

	public Parser(Scanner scanner, Code code) {
		this.scanner = scanner;
		this.code = code;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void PlainCode() {
		if (la.kind == 2 || la.kind == 4) {
			if (la.kind == 4) {
				Label();
			} else {
				Instruction();
			}
		}
		while (la.kind == 3) {
			Get();
			while (la.kind == 3) {
				Get();
			}
			if (la.kind == 4) {
				Label();
			} else if (la.kind == 2) {
				Instruction();
			} else SynErr(6);
		}
	}

	void Label() {
		Expect(4);
		Expect(2);
		code.getLabel(t.val).targetHere(); 
	}

	void Instruction() {
		boolean arg = false; 
		int op = OpCode();
		code.add(op); 
		if (la.kind == 1 || la.kind == 2) {
			if (la.kind == 1) {
				Get();
				code.add(Integer.parseInt(t.val)); arg = true;
			} else {
				Get();
			}
			if (op == Code.OpCode.JMP_x) { code.getLabel(t.val).sourceHereRel(-1); code.add(0); } else {SemErr("Number expected for opcode " + OPCODES_REV.get(op));} 
		}
		if (Code.OpCode.hasArg(op) && !arg) {
		SemErr("Argument expected for opcode " + OPCODES_REV.get(op));
		} else if (!Code.OpCode.hasArg(op) && arg) {
		SemErr("No argument expected for opcode " + OPCODES_REV.get(op));
		} 
	}

	int  OpCode() {
		int  op;
		Expect(2);
		if (OPCODES_REV.containsKey(t.val.toUpperCase())) { op = OPCODES_REV.get(t.val.toUpperCase()); } else { op = -1; SemErr("Unknown opcode: " + t.val); } 
		return op;
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		PlainCode();
		Expect(0);

		scanner.buffer.Close();
	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "number expected"; break;
			case 2: s = "ident expected"; break;
			case 3: s = "\"\\n\" expected"; break;
			case 4: s = "\":\" expected"; break;
			case 5: s = "??? expected"; break;
			case 6: s = "invalid PlainCode"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
