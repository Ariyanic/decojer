/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
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
package org.decojer.cavaj.model.code.ops;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation 'SWITCH'.
 * 
 * @author André Pankraz
 */
public class SWITCH extends Op {

	@Getter
	@Setter
	@Nullable
	private int[] caseKeys;

	@Getter
	@Setter
	@Nullable
	private int[] casePcs;

	@Getter
	@Setter
	private int defaultPc;

	/**
	 * Constructor.
	 * 
	 * @param pc
	 *            pc
	 * @param opcode
	 *            operation code
	 * @param line
	 *            line number
	 */
	public SWITCH(final int pc, final int opcode, final int line) {
		super(pc, opcode, line);
	}

	@Override
	public int getInStackSize() {
		return 1;
	}

	@Override
	public Optype getOptype() {
		return Optype.SWITCH;
	}

}