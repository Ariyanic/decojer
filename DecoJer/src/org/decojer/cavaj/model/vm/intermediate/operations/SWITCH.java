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

/**
 * Operation 'SWITCH'.
 * 
 * @author Andr� Pankraz
 */
public class SWITCH extends Operation {

	private int[] caseKeys;

	private int[] casePcs;

	private int defaultPc;

	public SWITCH(final int pc, final int code, final int line) {
		super(pc, code, line);
	}

	public int[] getCaseKeys() {
		return this.caseKeys;
	}

	public int[] getCasePcs() {
		return this.casePcs;
	}

	public int getDefaultPc() {
		return this.defaultPc;
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	@Override
	public int getOpcode() {
		return Opcode.SWITCH;
	}

	public void setCaseKeys(final int[] caseKeys) {
		this.caseKeys = caseKeys;
	}

	public void setCasePcs(final int[] casePcs) {
		this.casePcs = casePcs;
	}

	public void setDefaultPc(final int defaultPc) {
		this.defaultPc = defaultPc;
	}

}