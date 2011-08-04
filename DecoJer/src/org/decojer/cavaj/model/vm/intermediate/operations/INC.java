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
package org.decojer.cavaj.model.vm.intermediate.operations;

import org.decojer.cavaj.model.vm.intermediate.Opcode;
import org.decojer.cavaj.model.vm.intermediate.Operation;

public class INC extends Operation {

	private final int type;

	private final int varIndex;

	private final int constValue;

	public INC(final int opPc, final int opCode, final int opLine,
			final int type, final int varIndex, final int constValue) {
		super(opPc, opCode, opLine);
		this.type = type;
		this.varIndex = varIndex;
		this.constValue = constValue;
	}

	public int getConstValue() {
		return this.constValue;
	}

	@Override
	public int getInStackSize() {
		return 0;
	}

	@Override
	public int getOpcode() {
		return Opcode.INC;
	}

	public int getType() {
		return this.type;
	}

	public int getVarIndex() {
		return this.varIndex;
	}

}