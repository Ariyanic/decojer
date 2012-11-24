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
package org.decojer.cavaj.model.types;

import lombok.Getter;

import org.decojer.cavaj.model.T;

/**
 * Parameterized type.
 * 
 * @since JVM 5
 * 
 * @author André Pankraz
 */
public final class ParamT extends ModT {

	/**
	 * Type argument.
	 * 
	 * We don't follow the often used {@code WildcardType} paradigma. Wildcards are only allowed in
	 * the context of parameterized types and aren't useable as standalone Types.
	 * 
	 * @author André Pankraz
	 */
	@Getter
	public static final class TypeArg {

		public enum Kind {

			MATCH,

			UNBOUND,

			SUBCLASS_OF,

			SUPER_OF

		}

		public static TypeArg subclassOf(final T t) {
			assert t != null;

			return new TypeArg(t, Kind.SUBCLASS_OF);
		}

		public static TypeArg superOf(final T t) {
			assert t != null;

			return new TypeArg(t, Kind.SUPER_OF);
		}

		private final Kind kind;

		private final T t;

		public TypeArg() {
			this(null, Kind.UNBOUND);
		}

		public TypeArg(final T t) {
			this(t, Kind.MATCH);

			assert t != null;
		}

		private TypeArg(final T t, final Kind kind) {
			this.t = t;
			this.kind = kind;
		}

		@Override
		public String toString() {
			switch (this.kind) {
			case MATCH:
				return this.t.toString();
			case SUBCLASS_OF:
				return "? extends " + this.t;
			case SUPER_OF:
				return "? super " + this.t;
			case UNBOUND:
				return "?";
			}
			return "???";
		}

	}

	private static String toString(final T genericT, final TypeArg[] typeArgs) {
		final StringBuilder sb = new StringBuilder(genericT.getName()).append('<');
		for (final TypeArg typeArg : typeArgs) {
			sb.append(typeArg).append(',');
		}
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}

	/**
	 * Type arguments for matching type parameters.
	 */
	@Getter
	private final TypeArg[] typeArgs;

	/**
	 * Constructor.
	 * 
	 * @param genericT
	 *            generic type with matching type parameters
	 * @param typeArgs
	 *            type arguments for matching type parameters
	 */
	public ParamT(final T genericT, final TypeArg[] typeArgs) {
		super(toString(genericT, typeArgs), genericT);

		this.typeArgs = typeArgs;
	}

	/**
	 * Get generic type.
	 * 
	 * @return generic type
	 */
	public T getGenericT() {
		return getRawT();
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		if (super.isAssignableFrom(t)) {
			return true;
		}
		if (t == null) {
			return false;
		}
		return getRawT().isAssignableFrom(t);
	}

	@Override
	public T signatureExtend(final T reducedT) {
		return getGenericT().equals(reducedT) ? this : null;
	}

}