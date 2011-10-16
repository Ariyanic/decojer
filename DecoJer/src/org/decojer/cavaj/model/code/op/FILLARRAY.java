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
package org.decojer.cavaj.model.code.op;

/**
 * Operation 'FILLARRAY'.
 * 
 * @author Andr� Pankraz
 */
public class FILLARRAY extends Op {

	private Object[] values;

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
	public FILLARRAY(final int pc, final int opcode, final int line) {
		super(pc, opcode, line);
	}

	@Override
	public Optype getOptype() {
		return Optype.FILLARRAY;
	}

	/**
	 * Get values.
	 * 
	 * @return values
	 */
	public Object[] getValues() {
		return this.values;
	}

	/**
	 * Set values.
	 * 
	 * @param values
	 *            values
	 */
	public void setValues(final Object[] values) {
		this.values = values;
	}

}