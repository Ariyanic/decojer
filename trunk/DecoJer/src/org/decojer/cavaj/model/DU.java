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
package org.decojer.cavaj.model;

import java.util.HashMap;
import java.util.Map;

import org.decojer.DecoJerException;

/**
 * Decompilation unit.
 * 
 * @author Andr� Pankraz
 */
public class DU {

	private final Map<String, TD> tds = new HashMap<String, TD>();

	private final Map<String, T> ts = new HashMap<String, T>();

	/**
	 * Add type.
	 * 
	 * @param name
	 *            type name
	 * @param descriptor
	 *            descriptor
	 * @return type
	 */
	public T addT(final String name, final String descriptor) {
		assert name != null;
		assert descriptor != null;

		final T t = getT(name);
		if (t.getDescriptor() == null) {
			t.setDescriptor(descriptor);
		} else if (!t.getDescriptor().equals(descriptor)) {
			throw new DecoJerException(
					"Double definition of type with different descriptor!");
		}
		return t;
	}

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		assert td != null;

		this.tds.put(td.getName(), td);
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name
	 * @return type declaration
	 */
	public T getT(final String name) {
		assert name != null;

		T t = this.ts.get(name);
		if (t == null) {
			t = new T(this, name);
		}
		return t;
	}

	/**
	 * Get type declaration.
	 * 
	 * @param name
	 *            name
	 * @return type declaration
	 */
	public TD getTd(final String name) {
		return this.tds.get(name);
	}

}