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
package org.decojer.cavaj.reader.dex2jar;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;

import com.googlecode.dex2jar.visitors.DexClassVisitor;
import com.googlecode.dex2jar.visitors.DexFileVisitor;

/**
 * Dex2jar read file visitor.
 * 
 * @author Andr� Pankraz
 */
public class ReadDexFileVisitor implements DexFileVisitor {

	private final DU du;

	private final ReadDexClassVisitor readDexClassVisitor;

	private String selectorPrefix;

	private String selectorMatch;

	private TD selectorTd;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexFileVisitor(final DU du) {
		assert du != null;

		this.du = du;
		this.readDexClassVisitor = new ReadDexClassVisitor(du);
	}

	/**
	 * Get type declaration for selector.
	 * 
	 * @return type declaration for selector
	 */
	public TD getSelectorTd() {
		return this.selectorTd;
	}

	/**
	 * Init.
	 * 
	 * @param selector
	 *            selector
	 */
	public void init(final String selector) {
		if (selector != null && selector.endsWith(".class")) {
			this.selectorMatch = "L"
					+ selector.substring(selector.charAt(0) == '/' ? 1 : 0, selector.length() - 6)
					+ ";";
			final int pos = this.selectorMatch.lastIndexOf('/');
			if (pos != -1) {
				this.selectorPrefix = this.selectorMatch.substring(0, pos + 1);
			}
		} else {
			this.selectorMatch = null;
			this.selectorPrefix = null;
		}
		this.selectorTd = null;
	}

	@Override
	public DexClassVisitor visit(final int access_flags, final String className,
			final String superClass, final String[] interfaceNames) {
		// Lorg/apache/commons/logging/impl/WeakHashtable; :
		// Ljava/util/Hashtable; : null
		System.out
				.println("## visit ## " + className + " : " + superClass + " : " + interfaceNames);

		if (this.selectorPrefix != null
				&& (!className.startsWith(this.selectorPrefix) || className.indexOf('/',
						this.selectorPrefix.length()) != -1)) {
			return null;
		}

		final TD td = (TD) this.du.getDescT(className);
		td.setAccessFlags(access_flags);
		td.setSuperT(this.du.getDescT(superClass));
		if (interfaceNames != null && interfaceNames.length > 0) {
			final T[] interfaceTs = new T[interfaceNames.length];
			for (int i = interfaceNames.length; i-- > 0;) {
				interfaceTs[i] = this.du.getDescT(interfaceNames[i]);
			}
			td.setInterfaceTs(interfaceTs);
		}

		if (className.equals(this.selectorMatch)) {
			this.selectorTd = td;
		}

		this.du.addTd(td);

		this.readDexClassVisitor.init(td);
		return this.readDexClassVisitor;
	}

	@Override
	public void visitEnd() {
		// nothing
	}

}