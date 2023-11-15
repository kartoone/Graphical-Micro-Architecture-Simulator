package com.arm.legv8simulator.client.instruction;

import com.arm.legv8simulator.client.cpu.ControlUnitConfiguration;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * An <code>Instruction</code> object is used to represent each LEGv8 instruction in the user's program.
 * <p>
 * The <code>CPU</code> class executes the operation defined by each <code>Instruction</code>
 * 
 * @see CPU
 * 
 * @author Jonathan Wright, 2016
 */
public class Instruction {
	
	/**
	 * @param mnemonic			the instruction mnemonic
	 * @param args				the array of arguments for this instruction i.e. register indices and immediates
	 * @param editorLineNumber	the line in the code editor of this instruction
	 * @param controlSignals	the control signals required to execute this instruction
	 * 
	 * @see Mnemonic
	 * @see ControlUnitConfiguration
	 * @see CPU
	 */
	public Instruction(Mnemonic mnemonic, int[] args, int editorLineNumber, 
			ControlUnitConfiguration controlSignals) {
		this.mnemonic = mnemonic;
		this.args = args;
		this.editorLineNumber = editorLineNumber;
		this.controlSignals = controlSignals;
		rootLogger = Logger.getLogger("");
	}
	
	/**
	 * @return	the instruction mnemonic
	 */
	public Mnemonic getMnemonic() {
		return mnemonic;
	}
	
	/**
	 * @return	the array of arguments for this instruction i.e. registers and immediates
	 */
	public int[] getArgs() {
		return args;
	}
	
	/**
	 * @return	the line in the code editor of this instruction
	 */
	public int getLineNumber() {
		return editorLineNumber;
	}
	
	/**
	 * @return	the control signals required to execute this instruction
	 */
	public ControlUnitConfiguration getControlSignals() {
		return controlSignals;
	}

	public int assemble(int instructionIndex) {
		Mnemonic m = getMnemonic();
		int word = 0;
		switch (m.type) {
		case MNEMONIC_RRR :
			word = assembleRRR(this);
			break;
		case MNEMONIC_RRI : 
			if (m.equals(Mnemonic.LSL) || m.equals(Mnemonic.LSR)) {
				word = assembleShift(this);
			} else {
				word = assembleRRI(this);
			}
			break;
		case MNEMONIC_RISI :
			word = assembleRISI(this);
			break;
		case MNEMONIC_RM :
			if (m.equals(Mnemonic.STUR) || m.equals(Mnemonic.STURW) || m.equals(Mnemonic.STURH) || m.equals(Mnemonic.STURB)) {
				word = assembleRM(this);
			} else {
				word = assembleRM(this);
			}
			break;
		case MNEMONIC_RRM :
			word = assembleRRM(this);
			break;
		case MNEMONIC_RL :
			word = assembleRL(this, instructionIndex);
			break;
		case MNEMONIC_L :
			word = assembleL(this, instructionIndex);
			break;
		default:
		}
		return word;
	}

	private int assembleRRR(Instruction ins) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getRegBinary(ins.getArgs()[2]));
		instruction.append(getImmBinary(0, 6, false));
		instruction.append(getRegBinary(ins.getArgs()[1]));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return (int)Long.parseLong(instruction.toString(), 2);
	}
	
	private int assembleShift(Instruction ins) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getImmBinary(0, 5, false));
		instruction.append(getImmBinary(ins.getArgs()[2], 6, false));
		instruction.append(getRegBinary(ins.getArgs()[1]));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleRRI(Instruction ins) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getImmBinary(ins.getArgs()[2], 12, false));
		instruction.append(getRegBinary(ins.getArgs()[1]));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleRM(Instruction ins) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getImmBinary(ins.getArgs()[2], 12, true));
		instruction.append("00");
		instruction.append(getRegBinary(ins.getArgs()[1]));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleRRM(Instruction ins) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getRegBinary(ins.getArgs()[0]));
		instruction.append(getImmBinary(31, 6, false));
		instruction.append(getRegBinary(ins.getArgs()[2]));
		instruction.append(getRegBinary(ins.getArgs()[1]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleRISI(Instruction ins) {
		String shift = "";
		switch (ins.getArgs()[2]) {
			case 0 : shift = "00";
			break;
			case 16 : shift = "01";
			break;
			case 32 : shift = "10";
			break;
			case 48 : shift = "11";
			break;
		}
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode + shift);
		instruction.append(getImmBinary(ins.getArgs()[1], 16, false));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleRL(Instruction ins, int instructionIndex) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getImmBinary(ins.getArgs()[1]-instructionIndex, 19, true));
		instruction.append(getRegBinary(ins.getArgs()[0]));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	private int assembleL(Instruction ins, int instructionIndex) {
		StringBuilder instruction = new StringBuilder("");
		instruction.append(ins.getMnemonic().opcode);
		instruction.append(getImmBinary(ins.getArgs()[0]-instructionIndex, 26, true));
		return Integer.parseInt(instruction.toString(), 2);
	}
	
	// these two functions are copied exactly as is from the SingleCycleVis class ... they shouldn't be there. they should be over here.
	private String getRegBinary(int regNum) {
		String regBinary = Integer.toBinaryString(regNum);
		while (regBinary.length() < 5) {
			regBinary = "0" + regBinary;
		}
		return regBinary;
	}
	
	private String getImmBinary(int value, int numBits, boolean signed) {
		String immBinary = null;
		if (signed) {
			if (value < 0) {
				immBinary = Integer.toBinaryString(value & 0x0fffffff);
				while (immBinary.length() > numBits) {
					immBinary = immBinary.substring(1);
				}
				return immBinary;
			}
		}
		immBinary = Integer.toBinaryString(value);
		while (immBinary.length() < numBits) {
			immBinary = "0" + immBinary;
		}
		return immBinary;
	}
		
	private Mnemonic mnemonic;
	private int[] args;
	private int editorLineNumber;
	private ControlUnitConfiguration controlSignals;
	protected Logger rootLogger;
}
