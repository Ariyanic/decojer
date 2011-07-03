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
package org.decojer.cavaj.transformer;

import org.decojer.cavaj.model.BD;
import org.decojer.cavaj.model.CFG;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.TD;

/**
 * Remove empty constructors in AST.
 * 
 * @author Andr� Pankraz
 */
public class TrRemoveEmptyConstructor {

	public static void transform(final TD td) {
		new TrRemoveEmptyConstructor(td).transform();
	}

	private final TD td;

	private TrRemoveEmptyConstructor(final TD td) {
		this.td = td;
	}

	private TD getTD() {
		return this.td;
	}

	private void transform() {
		for (final BD bd : getTD().getBds()) {
			if (!(bd instanceof MD)) {
				continue;
			}
			final CFG cfg = ((MD) bd).getCfg();
			transform(cfg);
		}
	}

	private void transform(final CFG cfg) {
		// TODO
	}

}