/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  Andr� Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.cavaj.reader.smali;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.decojer.cavaj.model.AF;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.F;
import org.decojer.cavaj.model.M;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.vm.intermediate.CompareType;
import org.decojer.cavaj.model.vm.intermediate.DataType;
import org.decojer.cavaj.model.vm.intermediate.Exc;
import org.decojer.cavaj.model.vm.intermediate.Operation;
import org.decojer.cavaj.model.vm.intermediate.operations.ADD;
import org.decojer.cavaj.model.vm.intermediate.operations.ALOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.AND;
import org.decojer.cavaj.model.vm.intermediate.operations.DIV;
import org.decojer.cavaj.model.vm.intermediate.operations.GET;
import org.decojer.cavaj.model.vm.intermediate.operations.GOTO;
import org.decojer.cavaj.model.vm.intermediate.operations.INVOKE;
import org.decojer.cavaj.model.vm.intermediate.operations.JCMP;
import org.decojer.cavaj.model.vm.intermediate.operations.JCND;
import org.decojer.cavaj.model.vm.intermediate.operations.JSR;
import org.decojer.cavaj.model.vm.intermediate.operations.LOAD;
import org.decojer.cavaj.model.vm.intermediate.operations.MUL;
import org.decojer.cavaj.model.vm.intermediate.operations.NEW;
import org.decojer.cavaj.model.vm.intermediate.operations.OR;
import org.decojer.cavaj.model.vm.intermediate.operations.PUSH;
import org.decojer.cavaj.model.vm.intermediate.operations.RETURN;
import org.decojer.cavaj.model.vm.intermediate.operations.STORE;
import org.decojer.cavaj.model.vm.intermediate.operations.SUB;
import org.decojer.cavaj.model.vm.intermediate.operations.SWITCH;
import org.decojer.cavaj.model.vm.intermediate.operations.XOR;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.CodeItem.EncodedTypeAddrPair;
import org.jf.dexlib.CodeItem.TryItem;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.Instruction10t;
import org.jf.dexlib.Code.Format.Instruction11n;
import org.jf.dexlib.Code.Format.Instruction11x;
import org.jf.dexlib.Code.Format.Instruction12x;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Format.Instruction21s;
import org.jf.dexlib.Code.Format.Instruction21t;
import org.jf.dexlib.Code.Format.Instruction22b;
import org.jf.dexlib.Code.Format.Instruction22s;
import org.jf.dexlib.Code.Format.Instruction22t;
import org.jf.dexlib.Code.Format.Instruction23x;
import org.jf.dexlib.Code.Format.Instruction35c;
import org.jf.dexlib.Debug.DebugInstructionIterator;

/**
 * Read code item.
 * 
 * @author Andr� Pankraz
 */
public class ReadCodeItem {

	private final static Logger LOGGER = Logger.getLogger(ReadCodeItem.class
			.getName());

	private final DU du;

	private MD md;

	final ArrayList<Operation> operations = new ArrayList<Operation>();

	private final HashMap<Integer, Integer> pc2index = new HashMap<Integer, Integer>();

	private final HashMap<Integer, ArrayList<Object>> pc2unresolved = new HashMap<Integer, ArrayList<Object>>();

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadCodeItem(final DU du) {
		assert du != null;

		this.du = du;
	}

	private int getPcIndex(final int pc) {
		final Integer index = this.pc2index.get(pc);
		if (index != null) {
			return index;
		}
		final int unresolvedIndex = -1 - this.pc2unresolved.size();
		this.pc2index.put(pc, unresolvedIndex);
		return unresolvedIndex;
	}

	private ArrayList<Object> getPcUnresolved(final int pc) {
		ArrayList<Object> unresolved = this.pc2unresolved.get(pc);
		if (unresolved == null) {
			unresolved = new ArrayList<Object>();
			this.pc2unresolved.put(pc, unresolved);
		}
		return unresolved;
	}

	/**
	 * Init and set method declaration.
	 * 
	 * @param md
	 *            method declaration
	 * @param codeItem
	 *            code item
	 */
	public void initAndVisit(final MD md, final CodeItem codeItem) {
		this.md = md;

		this.operations.clear();
		this.pc2index.clear();
		this.pc2unresolved.clear();

		final M m = md.getM();

		HashMap<Integer, Integer> opLines = null;

		final DebugInfoItem debugInfo = codeItem.getDebugInfo();
		if (debugInfo != null) {
			final StringIdItem[] parameterNames = debugInfo.getParameterNames();
			if (parameterNames != null && parameterNames.length > 0) {
				for (int i = parameterNames.length; i-- > 0;) {
					if (parameterNames[i] == null) {
						// could happen, e.g. synthetic methods, inner <init>
						// with outer type param
						continue;
					}
					m.setParamName(i, parameterNames[i].getStringValue());
				}
			}
			final ReadDebugInfo readDebugInfo = new ReadDebugInfo(md);
			DebugInstructionIterator.DecodeInstructions(debugInfo,
					codeItem.getRegisterCount(), readDebugInfo);
			opLines = readDebugInfo.getOpLines();
		}

		// init CFG with start BB
		final CFG cfg = new CFG(md, codeItem.getRegisterCount(), -1);
		md.setCFG(cfg);
		md.postProcessVars();

		final Instruction[] instructions = codeItem.getInstructions();

		// 2 free to use work register, 3 parameter
		// static: (5 register)
		// work_register1...work_register_2...param1...param2...param3
		// dynamic: (6 register)
		// work_register1...work_register_2...this...param1...param2...param3

		for (int opPc = 0, opLine = -1, i = 0; i < instructions.length; ++i) {
			visitPc(opPc);

			final Instruction instruction = instructions[i];

			final int opCode = instruction.opcode.value;
			if (opLines != null && opLines.containsKey(opPc)) {
				// opLine remains constant with increasing opPc till new info is
				// available
				opLine = opLines.get(opPc);
			}

			System.out.println("I" + opPc + " (" + opLine + "): "
					+ instruction.opcode + "     "
					+ instruction.getClass().getName());

			int type = -1;
			int iValue = 0;
			final int lValue = 0;
			final Object oValue = null;

			switch (instruction.opcode) {
			/*******
			 * ADD *
			 *******/
			case ADD_DOUBLE:
				type = DataType.T_DOUBLE;
				// fall through
			case ADD_FLOAT:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case ADD_INT:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case ADD_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A + B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new ADD(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case ADD_DOUBLE_2ADDR:
				type = DataType.T_DOUBLE;
				// fall through
			case ADD_FLOAT_2ADDR:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case ADD_INT_2ADDR:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case ADD_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A + B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new ADD(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case ADD_INT_LIT8: {
				// B := A + literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new ADD(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case ADD_INT_LIT16: {
				// B := A + literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new ADD(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			/*********
			 * ALOAD *
			 *********/
			case AGET:
				type = DataType.T_INT;
				// fall through
			case AGET_BOOLEAN:
				if (type < 0) {
					type = DataType.T_BOOLEAN;
				}
				// fall through
			case AGET_BYTE:
				if (type < 0) {
					type = DataType.T_BYTE;
				}
				// fall through
			case AGET_CHAR:
				if (type < 0) {
					type = DataType.T_CHAR;
				}
				// fall through
			case AGET_OBJECT:
				if (type < 0) {
					type = DataType.T_AREF;
				}
				// fall through
			case AGET_SHORT:
				if (type < 0) {
					type = DataType.T_SHORT;
				}
				// fall through
			case AGET_WIDE:
				if (type < 0) {
					type = DataType.T_LONG; // TODO and T_DOUBLE, as multi?
				}
				{
					// A := B[C]
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterB())); // TODO array type?
					this.operations.add(new LOAD(opPc, opCode, opLine,
							DataType.T_INT, instr.getRegisterC()));

					this.operations.add(new ALOAD(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			/*******
			 * AND *
			 *******/
			case AND_INT:
				type = DataType.T_INT;
				// fall through
			case AND_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A & B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new AND(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case AND_INT_2ADDR:
				type = DataType.T_INT;
				// fall through
			case AND_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A & B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new AND(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case AND_INT_LIT8: {
				// B := A & literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new AND(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case AND_INT_LIT16: {
				// B := A & literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new AND(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			/**********
			 * ASTORE *
			 **********/
			case APUT:
			case APUT_BOOLEAN:
			case APUT_BYTE:
			case APUT_CHAR:
			case APUT_OBJECT:
			case APUT_SHORT:
			case APUT_WIDE: {
				// B[C] := A
				final Instruction23x instr = (Instruction23x) instruction;
				System.out
						.println("  A: " + instr.getRegisterA() + "  B: "
								+ instr.getRegisterB() + "  C: "
								+ instr.getRegisterC());
				break;
			}
			case CONST_4: {
				// A := literal
				final Instruction11n instr = (Instruction11n) instruction;
				System.out.println("  refItem: " + instr.getLiteral() + "  A: "
						+ instr.getRegisterA());
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, (int) instr.getLiteral()));
				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				break;
			}
			case CONST_16:
			case CONST_WIDE_16: /* long */{
				// A := literal
				final Instruction21s instr = (Instruction21s) instruction;
				System.out.println("  refItem: " + instr.getLiteral() + "  A: "
						+ instr.getRegisterA());
				break;
			}
			case CONST_STRING: {
				// A := refItem
				final Instruction21c instr = (Instruction21c) instruction;
				System.out.println("  refItem: " + instr.getReferencedItem()
						+ "  A: " + instr.getRegisterA());
				final StringIdItem stringIdItem = (StringIdItem) instr
						.getReferencedItem();
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_STRING, stringIdItem.getStringValue()));
				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_STRING, instr.getRegisterA()));
				break;
			}
			/*******
			 * DIV *
			 *******/
			case DIV_DOUBLE:
				type = DataType.T_DOUBLE;
				// fall through
			case DIV_FLOAT:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case DIV_INT:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case DIV_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A / B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new DIV(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case DIV_DOUBLE_2ADDR:
				type = DataType.T_DOUBLE;
				// fall through
			case DIV_FLOAT_2ADDR:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case DIV_INT_2ADDR:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case DIV_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A / B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new DIV(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case DIV_INT_LIT8: {
				// B := A / literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new DIV(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case DIV_INT_LIT16: {
				// B := A / literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new DIV(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			/********
			 * GOTO *
			 ********/
			case GOTO: {
				final Instruction10t instr = (Instruction10t) instruction;
				final GOTO op = new GOTO(opPc, opCode, opLine);
				final int targetPc = opPc + instr.getTargetAddressOffset();
				final int pcIndex = getPcIndex(targetPc);
				op.setTargetPc(pcIndex);
				if (pcIndex < 0) {
					getPcUnresolved(targetPc).add(op);
				}
				this.operations.add(op);
				break;
			}
			/********
			 * JCMP *
			 ********/
			case IF_EQ:
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
				// fall through
			case IF_GE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case IF_GT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case IF_LE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case IF_LT:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case IF_NE:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				{
					// IF A cond B JMP offset
					final Instruction22t instr = (Instruction22t) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterB()));

					final JCMP op = new JCMP(opPc, opCode, opLine, type, iValue);
					final int targetPc = opPc + instr.getTargetAddressOffset();
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
				}
				break;
			/********
			 * JCND *
			 ********/
			case IF_EQZ:
				type = DataType.T_INT;
				iValue = CompareType.T_EQ;
				// fall through
			case IF_GEZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GE;
				}
				// fall through
			case IF_GTZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_GT;
				}
				// fall through
			case IF_LEZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LE;
				}
				// fall through
			case IF_LTZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_LT;
				}
				// fall through
			case IF_NEZ:
				if (type < 0) {
					type = DataType.T_INT;
					iValue = CompareType.T_NE;
				}
				{
					// IF A cond 0 JMP offset
					final Instruction21t instr = (Instruction21t) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterA()));

					final JCND op = new JCND(opPc, opCode, opLine, type, iValue);
					final int targetPc = opPc + instr.getTargetAddressOffset();
					final int pcIndex = getPcIndex(targetPc);
					op.setTargetPc(pcIndex);
					if (pcIndex < 0) {
						getPcUnresolved(targetPc).add(op);
					}
					this.operations.add(op);
				}
				break;
			case INT_TO_BYTE:
			case INT_TO_CHAR:
			case INT_TO_DOUBLE:
			case INT_TO_FLOAT:
			case INT_TO_LONG:
			case INT_TO_SHORT: {
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.println("  A: " + instr.getRegisterA() + "  B: "
						+ instr.getRegisterB());
				break;
			}
			/**********
			 * INVOKE *
			 **********/
			case INVOKE_DIRECT:
				// constructor or supermethod callout
			case INVOKE_INTERFACE:
			case INVOKE_STATIC:
			case INVOKE_VIRTUAL: {
				final Instruction35c instr = (Instruction35c) instruction;
				System.out.print("  " + instr.getReferencedItem() + " : "
						+ instr.getRegCount());
				System.out.print("  (D: " + instr.getRegisterD());
				System.out.print("  E: " + instr.getRegisterE());
				System.out.print("  F: " + instr.getRegisterF());
				System.out.print("  G: " + instr.getRegisterG());
				System.out.println("  A: " + instr.getRegisterA() + ")");
				if (instruction.opcode != Opcode.INVOKE_STATIC) {

				}
				final MethodIdItem methodIdItem = (MethodIdItem) instr
						.getReferencedItem();
				final T invokeT = this.du.getDescT(methodIdItem
						.getContainingClass().getTypeDescriptor());
				if (instruction.opcode == Opcode.INVOKE_INTERFACE) {
					invokeT.markAf(AF.INTERFACE);
				}
				final M invokeM = invokeT.getM(methodIdItem.getMethodName()
						.getStringValue(), methodIdItem.getPrototype()
						.getPrototypeString());
				if (instruction.opcode == Opcode.INVOKE_STATIC) {
					invokeM.markAf(AF.STATIC);
				}
				if (instr.getRegCount() > 0) {
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterD()));
				}
				if (instr.getRegCount() > 1) {
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterE()));
				}
				if (instr.getRegCount() > 2) {
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterF()));
				}
				if (instr.getRegCount() > 3) {
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterG()));
				}
				if (instr.getRegCount() > 4) {
					this.operations.add(new LOAD(opPc, opCode, opLine, -1,
							instr.getRegisterA()));
				}
				this.operations.add(new INVOKE(opPc, opCode, opLine, invokeM,
						instruction.opcode == Opcode.INVOKE_DIRECT));
				break;
			}
			case MOVE: {
				final Instruction12x instr = (Instruction12x) instruction;
				System.out.print("  A: " + instr.getRegisterA());
				System.out.println("  B: " + instr.getRegisterB());
				this.operations.add(new LOAD(opPc, opCode, opLine, -1, instr
						.getRegisterA()));
				this.operations.add(new STORE(opPc, opCode, opLine, -1, instr
						.getRegisterB()));
				break;
			}
			case MOVE_RESULT:
			case MOVE_RESULT_OBJECT:
			case MOVE_RESULT_WIDE: {
				// A := resultRegister
				final Instruction11x instr = (Instruction11x) instruction;
				System.out.println("  A: " + instr.getRegisterA());
				break;
			}
			/*******
			 * MUL *
			 *******/
			case MUL_DOUBLE:
				type = DataType.T_DOUBLE;
				// fall through
			case MUL_FLOAT:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case MUL_INT:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case MUL_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A * B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new MUL(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case MUL_DOUBLE_2ADDR:
				type = DataType.T_DOUBLE;
				// fall through
			case MUL_FLOAT_2ADDR:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case MUL_INT_2ADDR:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case MUL_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A * B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new MUL(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case MUL_INT_LIT8: {
				// B := A * literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new MUL(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case MUL_INT_LIT16: {
				// B := A * literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new MUL(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			/*******
			 * NEW *
			 *******/
			case NEW_INSTANCE: {
				// A := new referencedItem
				final Instruction21c instr = (Instruction21c) instruction;

				final T t = this.du.getDescT(((TypeIdItem) instr
						.getReferencedItem()).getTypeDescriptor());
				this.operations.add(new NEW(opPc, opCode, opLine, t));
				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_AREF, instr.getRegisterA()));
				break;
			}
			/*******
			 * OR *
			 *******/
			case OR_INT:
				type = DataType.T_INT;
				// fall through
			case OR_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A | B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new OR(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case OR_INT_2ADDR:
				type = DataType.T_INT;
				// fall through
			case OR_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A | B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new OR(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case OR_INT_LIT8: {
				// B := A | literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations
						.add(new OR(opPc, opCode, opLine, DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case OR_INT_LIT16: {
				// B := A | literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations
						.add(new OR(opPc, opCode, opLine, DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			/**********
			 * RETURN *
			 **********/
			case RETURN:
			case RETURN_OBJECT:
			case RETURN_WIDE: {
				// return A
				final Instruction11x instr = (Instruction11x) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine, -1, instr
						.getRegisterA()));
				cfg.getStartBb().addOperation(
						new RETURN(opPc, opCode, opLine, DataType.T_AREF));
				break;
			}
			case RETURN_VOID: {
				cfg.getStartBb().addOperation(
						new RETURN(opPc, opCode, opLine, DataType.T_VOID));
				break;
			}
			case SGET_OBJECT: {
				final Instruction21c instr = (Instruction21c) instruction;
				System.out.println("  " + instr.getReferencedItem() + "  A: "
						+ instr.getRegisterA());
				final FieldIdItem fieldIdItem = (FieldIdItem) instr
						.getReferencedItem();
				final T fieldRefT = this.du.getDescT(fieldIdItem
						.getContainingClass().getTypeDescriptor());
				final T fieldValueT = this.du.getDescT(fieldIdItem
						.getFieldType().getTypeDescriptor());
				final F f = fieldRefT.getF(fieldIdItem.getFieldName()
						.getStringValue(), fieldValueT);
				f.markAf(AF.STATIC);
				this.operations.add(new GET(opPc, opCode, opLine, f));
				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_AREF, instr.getRegisterA()));
				break;
			}
			/*******
			 * SUB *
			 *******/
			case SUB_DOUBLE:
				type = DataType.T_DOUBLE;
				// fall through
			case SUB_FLOAT:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case SUB_INT:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case SUB_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A - B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new SUB(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case SUB_DOUBLE_2ADDR:
				type = DataType.T_DOUBLE;
				// fall through
			case SUB_FLOAT_2ADDR:
				if (type < 0) {
					type = DataType.T_FLOAT;
				}
				// fall through
			case SUB_INT_2ADDR:
				if (type < 0) {
					type = DataType.T_INT;
				}
				// fall through
			case SUB_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A - B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new SUB(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			/*******
			 * XOR *
			 *******/
			case XOR_INT:
				type = DataType.T_INT;
				// fall through
			case XOR_LONG:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// C := A ^ B
					final Instruction23x instr = (Instruction23x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new XOR(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterC()));
				}
				break;
			case XOR_INT_2ADDR:
				type = DataType.T_INT;
				// fall through
			case XOR_LONG_2ADDR:
				if (type < 0) {
					type = DataType.T_LONG;
				}
				{
					// A := A ^ B
					final Instruction12x instr = (Instruction12x) instruction;

					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterA()));
					this.operations.add(new LOAD(opPc, opCode, opLine, type,
							instr.getRegisterB()));

					this.operations.add(new XOR(opPc, opCode, opLine, type));

					this.operations.add(new STORE(opPc, opCode, opLine, type,
							instr.getRegisterA()));
				}
				break;
			case XOR_INT_LIT8: {
				// B := A ^ literal
				final Instruction22b instr = (Instruction22b) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new XOR(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			case XOR_INT_LIT16: {
				// B := A ^ literal
				final Instruction22s instr = (Instruction22s) instruction;

				this.operations.add(new LOAD(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterA()));
				this.operations.add(new PUSH(opPc, opCode, opLine,
						DataType.T_INT, instr.getLiteral()));

				this.operations.add(new XOR(opPc, opCode, opLine,
						DataType.T_INT));

				this.operations.add(new STORE(opPc, opCode, opLine,
						DataType.T_INT, instr.getRegisterB()));
				break;
			}
			}
			opPc += instruction.getSize(opPc);
		}
		cfg.setOperations(this.operations.toArray(new Operation[this.operations
				.size()]));

		final TryItem[] tryItems = codeItem.getTries();
		if (tryItems != null && tryItems.length > 0) {
			final ArrayList<Exc> excs = new ArrayList<Exc>();
			// preserve order
			for (int i = 0; i < tryItems.length; ++i) {
				final TryItem tryItem = tryItems[i];
				for (final EncodedTypeAddrPair handler : tryItem.encodedCatchHandler.handlers) {
					final T catchT = this.du.getDescT(handler.exceptionType
							.getTypeDescriptor());
					final Exc exc = new Exc(catchT);
					exc.setStartPc(tryItem.getStartCodeAddress());
					exc.setEndPc(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength());
					exc.setHandlerPc(handler.getHandlerAddress());
					excs.add(exc);
				}
				if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() != -1) {
					final Exc exc = new Exc(null);
					exc.setStartPc(tryItem.getStartCodeAddress());
					exc.setEndPc(tryItem.getStartCodeAddress()
							+ tryItem.getTryLength());
					exc.setHandlerPc(tryItem.encodedCatchHandler
							.getCatchAllHandlerAddress());
					excs.add(exc);
				}
			}
			cfg.setExcs(excs.toArray(new Exc[excs.size()]));
		}
	}

	private void visitPc(final int pc) {
		final Integer pcIndex = this.pc2index.put(pc, this.operations.size());
		if (pcIndex == null) {
			// fresh new label, never referenced before
			return;
		}
		if (pcIndex > 0) {
			// visited before but is known?!
			LOGGER.warning("Pc '" + pc + "' is not unique, has old opPc '"
					+ this.operations.size() + "'!");
			return;
		}
		final int labelUnknownIndex = pcIndex;
		// unknown and has forward reference
		for (final Object o : this.pc2unresolved.get(pc)) {
			if (o instanceof GOTO) {
				((GOTO) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JCMP) {
				((JCMP) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JCND) {
				((JCND) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof JSR) {
				((JSR) o).setTargetPc(this.operations.size());
				continue;
			}
			if (o instanceof SWITCH) {
				final SWITCH op = (SWITCH) o;
				if (labelUnknownIndex == op.getDefaultPc()) {
					op.setDefaultPc(this.operations.size());
				}
				final int[] keyTargets = op.getCasePcs();
				for (int i = keyTargets.length; i-- > 0;) {
					if (labelUnknownIndex == keyTargets[i]) {
						keyTargets[i] = this.operations.size();
					}
				}
				continue;
			}
			// cannot happen for Exc / Var here
		}
	}

}