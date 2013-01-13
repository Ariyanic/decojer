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
package org.decojer.cavaj.readers.asm;

import java.lang.annotation.RetentionPolicy;
import java.util.logging.Logger;

import lombok.Getter;

import org.decojer.cavaj.model.A;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.FD;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.types.ClassT;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM read class visitor.
 * 
 * @author André Pankraz
 */
public class ReadClassVisitor extends ClassVisitor {

	private final static Logger LOGGER = Logger.getLogger(ReadClassVisitor.class.getName());

	private A[] as;

	private final DU du;

	private final ReadAnnotationMemberVisitor readAnnotationMemberVisitor;

	private final ReadFieldVisitor readFieldVisitor;

	private final ReadMethodVisitor readMethodVisitor;

	@Getter
	private TD td;

	/**
	 * Constructor.
	 * 
	 * @param du
	 *            decompilation unit
	 */
	public ReadClassVisitor(final DU du) {
		super(Opcodes.ASM4);

		assert du != null;

		this.du = du;
		this.readAnnotationMemberVisitor = new ReadAnnotationMemberVisitor(du);
		this.readFieldVisitor = new ReadFieldVisitor(du);
		this.readMethodVisitor = new ReadMethodVisitor(du);
	}

	/**
	 * Init.
	 */
	public void init() {
		this.as = null;
		this.td = null;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName, final String[] interfaces) {
		final ClassT t = (ClassT) this.du.getT(name);
		if (t.getTd() != null) {
			LOGGER.warning("Type '" + t + "' already read!");
			throw new AlreadyReadException();
		}
		this.td = t.createTd();
		this.td.setAccessFlags(access);
		this.td.setSuperT(this.du.getT(superName));
		if (interfaces != null && interfaces.length > 0) {
			final T[] interfaceTs = new T[interfaces.length];
			for (int i = interfaces.length; i-- > 0;) {
				interfaceTs[i] = this.du.getT(interfaces[i]);
			}
			this.td.setInterfaceTs(interfaceTs);
		}
		this.td.setSignature(signature);

		// fix ASM bug: mixup of minor and major (which is 196653),
		// only JDK 1.1 class files use a minor number (45.3),
		// JDK 1.1 - JDK 1.3 create this version without a target option
		this.td.setVersion(version & 0xffff);
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
		if (this.as == null) {
			this.as = new A[1];
		} else {
			final A[] newAs = new A[this.as.length + 1];
			System.arraycopy(this.as, 0, newAs, 0, this.as.length);
			this.as = newAs;
		}
		this.as[this.as.length - 1] = this.readAnnotationMemberVisitor.init(desc,
				visible ? RetentionPolicy.RUNTIME : RetentionPolicy.CLASS);
		return this.readAnnotationMemberVisitor;
	}

	@Override
	public void visitAttribute(final Attribute attr) {
		if ("Scala".equals(attr.type) || "ScalaSig".equals(attr.type)) {
			this.td.setScala();
			return;
		}
		LOGGER.warning("Unknown class attribute tag '" + attr.type + "'!");
	}

	@Override
	public void visitEnd() {
		if (this.as != null) {
			this.td.setAs(this.as);
		}
		this.td.resolve();
	}

	@Override
	public FieldVisitor visitField(final int access, final String name, final String desc,
			final String signature, final Object value) {
		final FD fd = this.td.createFd(name, desc);

		fd.setAccessFlags(access);
		fd.setSignature(signature);

		fd.setValue(value);

		this.readFieldVisitor.init(fd);
		return this.readFieldVisitor;
	}

	@Override
	public void visitInnerClass(final String name, final String outerName, final String innerName,
			final int access) {
		final ClassT innerT = (ClassT) this.du.getT(name);
		if (outerName != null) {
			// set enclosing first for better inner name check
			innerT.setEnclosingT(this.du.getT(outerName));
		}
		innerT.setInnerInfo(innerName, access);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc,
			final String signature, final String[] exceptions) {
		final MD md = this.td.createMd(name, desc);

		md.setAccessFlags(access);
		if (exceptions != null && exceptions.length > 0) {
			final T[] throwsTs = new T[exceptions.length];
			for (int i = exceptions.length; i-- > 0;) {
				throwsTs[i] = this.du.getT(exceptions[i]);
			}
			md.setThrowsTs(throwsTs);
		}
		md.setSignature(signature);

		this.readMethodVisitor.init(md);
		return this.readMethodVisitor;
	}

	@Override
	public void visitOuterClass(final String owner, final String name, final String desc) {
		final ClassT enclosingT = (ClassT) this.du.getT(owner);
		if (name == null) {
			this.td.getT().setEnclosingT(enclosingT);
		} else {
			this.td.getT().setEnclosingM(enclosingT.getM(name, desc));
		}
	}

	@Override
	public void visitSource(final String source, final String debug) {
		if (debug != null) {
			// TODO need an example, really useful in the wild?
			// JVM spec: 4.7.11 The SourceDebugExtension Attribute
			LOGGER.warning("### visitSource debug? ###: " + debug);
		}
		this.td.setSourceFileName(source);
	}

}