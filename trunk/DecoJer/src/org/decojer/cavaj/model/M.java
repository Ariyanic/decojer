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

import org.decojer.cavaj.model.code.CFG;

/**
 * Method.
 * 
 * @author André Pankraz
 */
public abstract class M {

	public static final String CONSTRUCTOR_NAME = "<init>";

	public static final String INITIALIZER_NAME = "<clinit>";

	/**
	 * Check access flag.
	 * 
	 * @param af
	 *            access flag
	 * @return {@code true} - is access flag
	 */
	public abstract boolean check(final AF af);

	/**
	 * Create method declaration for this method.
	 * 
	 * @return method declaration
	 */
	public abstract MD createMd();

	/**
	 * Get control flow graph.
	 * 
	 * @return control flow graph
	 */
	public CFG getCfg() {
		return getMd().getCfg();
	}

	/**
	 * Get descriptor. Unique in owner context.
	 * 
	 * @return descriptor
	 */
	public abstract String getDescriptor();

	/**
	 * Get decompilation unit or null (for primitive and special types).
	 * 
	 * @return decompilation unit or null
	 */
	public DU getDu() {
		return getT().getDu();
	}

	/**
	 * Get method declaration.
	 * 
	 * @return method declaration
	 */
	public abstract MD getMd();

	/**
	 * Get name.
	 * 
	 * @return name
	 */
	public abstract String getName();

	/**
	 * Get method parameter annotations or {@code null}.
	 * 
	 * @return method parameter annotations or {@code null}
	 */
	public A[][] getParamAss() {
		return getMd().getParamAss();
	}

	/**
	 * Get parameter name for index.
	 * 
	 * Dalvik provides this information directly, the JVM indirect via the local variable table.
	 * Could also be extracted from JavaDoc etc.
	 * 
	 * @param i
	 *            index (starts with 0, double/long params count as 1)
	 * @return parameter name
	 */
	public String getParamName(final int i) {
		return getMd().getParamName(i);
	}

	/**
	 * get parameter types.
	 * 
	 * @return parameter types
	 */
	public abstract T[] getParamTs();

	/**
	 * Get receiver-type (this) for none-static methods.
	 * 
	 * @return receiver-type
	 */
	public abstract T getReceiverT();

	/**
	 * Get return type.
	 * 
	 * @return return type
	 */
	public abstract T getReturnT();

	/**
	 * Get owner type.
	 * 
	 * @return owner type
	 */
	public abstract T getT();

	/**
	 * Get throws types or {@code null}.
	 * 
	 * @return throws types or {@code null}
	 */
	public T[] getThrowsTs() {
		return getMd().getThrowsTs();
	}

	/**
	 * Get type parameters.
	 * 
	 * @return type parameters
	 */
	public T[] getTypeParams() {
		return getMd().getTypeParams();
	}

	/**
	 * Is constructor?
	 * 
	 * @return {@code true} - is constructor
	 */
	public abstract boolean isConstructor();

	/**
	 * Is dynamic?
	 * 
	 * @return {@code true} - is dynamic
	 */
	public abstract boolean isDynamic();

	/**
	 * Is initializer?
	 * 
	 * @return {@code true} - is constructor
	 */
	public boolean isInitializer() {
		return false;
	}

	/**
	 * Is static method?
	 * 
	 * @return {@code true} - is static method
	 */
	public abstract boolean isStatic();

	/**
	 * Is synthetic method?
	 * 
	 * @return {@code true} - is synthetic method
	 */
	public abstract boolean isSynthetic();

	/**
	 * Is method with final varargs parameter?
	 * 
	 * @return {@code true} - is method with final varargs parameter
	 */
	public abstract boolean isVarargs();

	/**
	 * Set access flags.
	 * 
	 * @param accessFlags
	 *            access flags
	 */
	public void setAccessFlags(final int accessFlags) {
		getMd().setAccessFlags(accessFlags);
	}

	/**
	 * Set annotation default value..
	 * 
	 * @param annotationDefaultValue
	 *            annotation default value
	 */
	public void setAnnotationDefaultValue(final Object annotationDefaultValue) {
		getMd().setAnnotationDefaultValue(annotationDefaultValue);
	}

	/**
	 * Set annotations.
	 * 
	 * @param as
	 *            annotations
	 */
	public void setAs(final A[] as) {
		getMd().setAs(as);
	}

	/**
	 * Set control flow graph.
	 * 
	 * @param cfg
	 *            control flow graph
	 */
	public void setCfg(final CFG cfg) {
		getMd().setCfg(cfg);
	}

	/**
	 * Method must be deprecated (from Deprecated attribute, marked via Javadoc @deprecate).
	 */
	public void setDeprecated() {
		assert false;
	}

	/**
	 * Set method parameter annotations.
	 * 
	 * @param paramAss
	 *            method parameter annotations
	 */
	public void setParamAss(final A[][] paramAss) {
		getMd().setParamAss(paramAss);
	}

	/**
	 * Set parameter name.
	 * 
	 * Dalvik provides this information directly, the JVM indirect via the local variable table.
	 * Could also be extracted from JavaDoc etc.
	 * 
	 * @param i
	 *            index
	 * @param name
	 *            parameter name
	 */
	public void setParamName(final int i, final String name) {
		getMd().setParamName(i, name);
	}

	/**
	 * Set raw method for modified method.
	 * 
	 * For type annotation application.
	 * 
	 * @param rawM
	 *            raw method for modified method
	 */
	public void setRawM(final M rawM) {
		assert false;
	}

	/**
	 * Set receiver type (this) for none-static methods.
	 * 
	 * @param receiverT
	 *            receiver type
	 * @return {@code true} - success
	 */
	public boolean setReceiverT(final T receiverT) {
		return getMd().setReceiverT(receiverT);
	}

	/**
	 * Set return type.
	 * 
	 * @param returnT
	 *            return type
	 */
	public void setReturnT(final T returnT) {
		assert false; // overwrite in ClassM
	}

	/**
	 * Set signature.
	 * 
	 * @param signature
	 *            signature
	 */
	public void setSignature(final String signature) {
		getMd().setSignature(signature);
	}

	/**
	 * Method must be static or dynamic (from usage, e.g. invoke).
	 * 
	 * @param f
	 *            {@code true} - is static
	 */
	public abstract void setStatic(final boolean f);

	/**
	 * Method must be synthetic (from synthetic attribute).
	 */
	public void setSynthetic() {
		assert false;
	}

	/**
	 * Set owner type (for applying type annotations).
	 * 
	 * @param t
	 *            owner type
	 */
	public void setT(final T t) {
		assert false; // overwrite in QualifiedM
	}

	/**
	 * Set throws types or {@code null}.
	 * 
	 * @param throwsTs
	 *            throws types or {@code null}
	 */
	public void setThrowsTs(final T[] throwsTs) {
		getMd().setThrowsTs(throwsTs);
	}

}