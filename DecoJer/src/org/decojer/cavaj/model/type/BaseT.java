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
package org.decojer.cavaj.model.type;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Base Type (Primitives and Artificial / Internal VM Types).
 * 
 * @author Andr� Pankraz
 */
public class BaseT extends T {

	@Getter
	private final int kind;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Type Name
	 * @param kind
	 *            Type Kind
	 */
	public BaseT(final String name, final int kind) {
		super(name);

		this.kind = kind;
	}

	@Override
	public boolean isMulti() {
		int nr = getKind() - (getKind() >> 1 & 0x55555555);
		nr = (nr & 0x33333333) + (nr >> 2 & 0x33333333);
		nr = (nr + (nr >> 4) & 0x0F0F0F0F) * 0x01010101 >> 24;

		assert nr > 0;

		return nr > 1;
	}

	@Override
	public boolean isPrimitive() {
		// not always true - consider REF/RET multitypes
		return (getKind() & PRIMITIVE.getKind()) != 0;
	}

	@Override
	public boolean isRef() {
		// not always false - consider REF/RET multitypes
		return (getKind() & REF.getKind()) != 0;
	}

	/**
	 * Is wide type?
	 * 
	 * @return true - is wide type
	 */
	@Override
	public boolean isWide() {
		return (getKind() & WIDE.getKind()) != 0;
	}

}