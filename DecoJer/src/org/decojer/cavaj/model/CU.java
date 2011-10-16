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

import java.util.ArrayList;
import java.util.List;

import org.decojer.DecoJerException;
import org.decojer.cavaj.util.TypeNameManager;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Compilation Unit. Can contain multiple type declarations, but only one with type name equal to
 * source file name can be public.
 * 
 * @author Andr� Pankraz
 */
public class CU implements PD {

	// all sub type declarations
	private final List<TD> allTds = new ArrayList<TD>();

	// Eclipse compilation unit
	private final CompilationUnit compilationUnit;

	private boolean ignoreSynthetic = true;

	private String sourceFileName;

	// start type declaration
	private final TD startTd;

	private boolean startTdOnly;

	// sub type declarations
	private final List<TD> tds = new ArrayList<TD>();

	private final TypeNameManager typeNameManager = new TypeNameManager(this);

	/**
	 * Constructor.
	 * 
	 * @param startTd
	 *            start type declaration
	 */
	public CU(final TD startTd) {
		assert startTd != null;

		this.startTd = startTd;

		// initializes Eclipse AST
		final ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(new char[0]);
		this.compilationUnit = (CompilationUnit) parser.createAST(null);
		this.compilationUnit.recordModifications();

		final String packageName = getStartTd().getT().getPackageName();
		if (packageName != null && packageName.length() != 0) {
			setPackageName(packageName);
		}
	}

	/**
	 * Add type declaration and add all parents.
	 * 
	 * @param td
	 *            type declaration
	 * @return true - success (or allready included)
	 */
	public boolean addTd(final TD td) {
		assert td != null;

		if (td.getPd() != null) {
			return false;
		}

		final String typeName = td.getT().getName();

		int pos = typeName.indexOf('$');
		if (pos == -1) {
			pos = typeName.length();
		}

		final TD rootTd = getStartTd().getT().getDu().getTd(typeName.substring(0, pos));
		if (rootTd == null || rootTd.getPd() != null && rootTd.getPd() != this) {
			return false;
		}

		TD pd = rootTd;
		final List<TD> allTds = new ArrayList<TD>();

		while (pos != typeName.length()) {
			pos = typeName.indexOf('$', pos + 1);
			if (pos == -1) {
				pos = typeName.length();
			}
			final TD bd = getStartTd().getT().getDu().getTd(typeName.substring(0, pos));
			if (bd == null) {
				return false;
			}
			// get possibly allready set parent
			PD pbd = bd.getPd();
			if (pbd instanceof FD) {
				pbd = ((FD) pbd).getTd();
			}
			if (pbd instanceof MD) {
				pbd = ((MD) pbd).getTd();
			}
			if (pbd != null) {
				if (pbd != pd) {
					throw new DecoJerException("Type declaration '" + bd.getT().getName()
							+ "' already belongs to other parent type declaration '" + pd + "'!");
				}
			} else {
				bd.setPd(pd);
				pd.getBds().add(bd);
				allTds.add(bd);
			}
			pd = bd;
		}

		if (rootTd.getPd() == null) {
			rootTd.setPd(this);
			getTds().add(rootTd);
			getAllTds().add(rootTd);
		}
		getAllTds().addAll(allTds);
		return true;
	}

	/**
	 * Add Eclipse type declaration.
	 * 
	 * @param typeDeclaration
	 *            Eclipse type declaration
	 * @return true - success
	 */
	@SuppressWarnings("unchecked")
	public boolean addTypeDeclaration(final AbstractTypeDeclaration typeDeclaration) {
		assert typeDeclaration != null;

		return getCompilationUnit().types().add(typeDeclaration);
	}

	/**
	 * Clear all generated data after read.
	 */
	public void clear() {
		for (final TD td : getAllTds()) {
			td.clear();
		}
		this.tds.clear();
		this.allTds.clear();
		this.typeNameManager.clear();
	}

	/**
	 * Create source code.
	 * 
	 * @return source code
	 */
	public String createSourceCode() {
		final Document document = new Document();
		final TextEdit edits = this.compilationUnit.rewrite(document, null);
		try {
			edits.apply(document);
		} catch (final MalformedTreeException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		} catch (final BadLocationException e) {
			throw new DecoJerException("Couldn't create source code!", e);
		}
		String sourceCode = document.get();

		packageAnnotationBug: if (this.compilationUnit.getPackage() != null
				&& this.compilationUnit.getPackage().annotations().size() > 0) {
			// bugfix for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=361071
			// see TrJvmStruct2JavaAst.transform(TD)
			final int pos = sourceCode.indexOf("package ");
			if (pos < 2) {
				break packageAnnotationBug;
			}
			final char ch = sourceCode.charAt(pos - 1);
			if (Character.isWhitespace(ch)) {
				break packageAnnotationBug;
			}
			sourceCode = sourceCode.substring(0, pos - 1) + "\n" + sourceCode.substring(pos);
		}
		// build class decompilation comment
		final StringBuilder sb = new StringBuilder(sourceCode);
		sb.append("\n\n/*\n").append(" * Generated by DecoJer ").append(0.9)
				.append(", a Java-bytecode decompiler.\n")
				.append(" * DecoJer Copyright (C) 2009-2011 Andr� Pankraz. All Rights Reserved.\n")
				.append(" *\n");
		final int version = this.startTd.getVersion();
		if (version == 0) {
			sb.append(" * Dalvik File");
		} else {
			sb.append(" * Class File Version: ").append(version).append(" (Java ");
			if (version <= 48) {
				sb.append("1.");
			}
			sb.append(version - 44).append(')');
		}
		sb.append('\n');
		if (this.startTd.getSourceFileName() != null) {
			sb.append(" * Source File Name: ").append(this.startTd.getSourceFileName())
					.append('\n');
		}
		sb.append(" */");
		return sb.toString();
	}

	/**
	 * Get all sub type declarations.
	 * 
	 * @return all sub type declarations, not null
	 */
	public List<TD> getAllTds() {
		return this.allTds;
	}

	/**
	 * Get Eclipse abstract syntax tree.
	 * 
	 * @return Eclipse abstract syntax tree, not null
	 */
	public AST getAst() {
		return getCompilationUnit().getAST();
	}

	/**
	 * Get Eclipse compilation unit.
	 * 
	 * @return Eclipse compilation unit, not null
	 */
	public CompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}

	/**
	 * Get source file name.
	 * 
	 * @return source file name
	 */
	public String getSourceFileName() {
		return this.sourceFileName;
	}

	/**
	 * Get start type declaration.
	 * 
	 * @return start type declaration, not null
	 */
	public TD getStartTd() {
		return this.startTd;
	}

	/**
	 * Get type declaration with name or full name.
	 * 
	 * @param name
	 *            name or full name
	 * @return type declaration or null
	 */
	public TD getTd(final String name) {
		String n = name;
		if (n.indexOf('.') == -1) {
			n = getStartTd().getT().getPackageName() + '.' + n;
		}
		for (final TD td : getAllTds()) {
			if (n.equals(td.getT().getName())) {
				return td;
			}
		}
		return null;
	}

	/**
	 * Get sub type declarations.
	 * 
	 * @return sub type declarations, not null
	 */
	public List<TD> getTds() {
		return this.tds;
	}

	protected TypeNameManager getTypeNameManager() {
		return this.typeNameManager;
	}

	/**
	 * Get predicate, if ignore synthetic type declarations, methods or fields.
	 * 
	 * @return true - ignore synthetic type declarations, methods or fields
	 */
	public boolean isIgnoreSynthetic() {
		return this.ignoreSynthetic;
	}

	/**
	 * Get predicate, if decompile start type declaration only.
	 * 
	 * @return true - decompile start type declaration only
	 */
	public boolean isStartTdOnly() {
		return this.startTdOnly;
	}

	private void setPackageName(final String packageName) {
		assert packageName != null && packageName.length() != 0;

		final PackageDeclaration newPackageDeclaration = getAst().newPackageDeclaration();
		newPackageDeclaration.setName(getAst().newName(packageName));
		this.compilationUnit.setPackage(newPackageDeclaration);
		this.typeNameManager.setPackageName(packageName);
	}

	/**
	 * Set source file name.
	 * 
	 * @param sourceFileName
	 *            source file name
	 */
	public void setSourceFileName(final String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	/**
	 * Decompile start type declaration only.
	 */
	public void startTdOnly() {
		this.startTdOnly = true;
		this.ignoreSynthetic = false;
		getStartTd().setPd(this);
		getTds().add(getStartTd());
		getAllTds().add(getStartTd());
	}

}