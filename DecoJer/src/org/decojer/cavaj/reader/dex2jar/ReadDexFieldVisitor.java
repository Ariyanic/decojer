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

import java.lang.annotation.RetentionPolicy;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.objectweb.asm.AnnotationVisitor;

import com.googlecode.dex2jar.visitors.DexFieldVisitor;

/**
 * Read DEX field visitor.
 * 
 * @author Andr� Pankraz
 */
public class ReadDexFieldVisitor implements DexFieldVisitor {

	private A[] as;

	private final DU du;

	private FD fd;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadDexFieldVisitor(final DU du) {
		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
	}

	/**
	 * Init and set field declaration.
	 * 
	 * @param fd
	 *            field declaration
	 */
	public void init(final FD fd) {
		this.fd = fd;
		this.as = null;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String name,
			final boolean visitable) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor.init(
				name, visitable ? RetentionPolicy.RUNTIME
						: RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.fd.setAs(this.as);
		}
	}

}