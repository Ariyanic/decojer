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
package org.decojer.cavaj.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;

import org.decojer.cavaj.model.fields.FD;
import org.decojer.cavaj.model.methods.MD;
import org.decojer.cavaj.model.types.TD;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.common.collect.Lists;

/**
 * Container for Declarations.
 * 
 * @author André Pankraz
 */
public abstract class Container {

	private final static Logger LOGGER = Logger.getLogger(Container.class.getName());

	/**
	 * All body declarations: inner type / method / field declarations.
	 */
	@Getter
	private final List<Declaration> bds = new ArrayList<Declaration>(4);

	protected void _getAllTds(final List<TD> tds) {
		for (final Declaration bd : this.bds) {
			if (bd instanceof TD) {
				tds.add((TD) bd);
			}
			bd._getAllTds(tds);
		}
	}

	/**
	 * Add body declaration.
	 * 
	 * @param bd
	 *            bode declaration
	 */
	public void addBd(final Declaration bd) {
		if (bd.getParent() != null) {
			if (bd.getParent() != this) {
				LOGGER.warning("Cannot change parent declaration for '" + bd + "' from '"
						+ bd.getParent() + "' to '" + this + "'!");
			}
			return;
		}
		bd.setParent(this);
		this.bds.add(bd);
	}

	/**
	 * Add type declaration.
	 * 
	 * @param td
	 *            type declaration
	 */
	public void addTd(final TD td) {
		addBd(td);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		for (final Declaration bd : this.bds) {
			bd.clear();
		}
	}

	public List<TD> getAllTds() {
		final List<TD> tds = Lists.newArrayList();
		_getAllTds(tds);
		return tds;
	}

	public Declaration getBdForDeclaration(final ASTNode node) {
		for (final Declaration bd : getBds()) {
			// could also work with polymorphism here...but why pollute subclasses with helper
			if (bd instanceof FD) {
				if (((FD) bd).getFieldDeclaration() == node) {
					return bd;
				}
			} else if (bd instanceof MD) {
				if (((MD) bd).getMethodDeclaration() == node) {
					return bd;
				}
			} else if (bd instanceof TD) {
				if (((TD) bd).getTypeDeclaration() == node) {
					return bd;
				}
			}
			final Declaration retBd = bd.getBdForDeclaration(node);
			if (retBd != null) {
				return retBd;
			}
		}
		return null;
	}

	public List<Element> getDeclarations() {
		final List<Element> declarations = Lists.newArrayList();
		for (final Declaration bd : getBds()) {
			declarations.add(bd.getElement());
		}
		return declarations;
	}

	public Element getElement() {
		if (this instanceof TD) {
			return ((TD) this).getT();
		}
		if (this instanceof MD) {
			return ((MD) this).getM();
		}
		if (this instanceof FD) {
			return ((FD) this).getF();
		}
		return null;
	}

	/**
	 * Get static initializer method declaration.
	 * 
	 * @return static initializer method declaration
	 */
	public MD getInitializer() {
		for (final Declaration bd : getBds()) {
			if (!(bd instanceof MD)) {
				continue;
			}
			final MD md = (MD) bd;
			if (md.isInitializer()) {
				return md;
			}
		}
		return null;
	}

	public abstract String getName();

}