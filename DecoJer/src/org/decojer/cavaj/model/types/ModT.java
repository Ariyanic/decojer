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

import java.util.List;
import java.util.Map;

import lombok.Getter;

import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.Element;
import org.decojer.cavaj.model.methods.M;

/**
 * Modifying type.
 * 
 * The general rule here should be, that this modification is optional from the view of the type
 * system and that getSimpleName() and getEnclosingT() don't really change.
 * 
 * @author André Pankraz
 */
public abstract class ModT extends T {

	@Getter
	private T rawT;

	protected ModT(final T rawT) {
		setRawT(rawT);
	}

	@Override
	public void addTypeDeclaration(final T t) {
		getRawT().addTypeDeclaration(t);
	}

	@Override
	public boolean eraseTo(final T t) {
		return getRawT().eraseTo(t);
	}

	@Override
	public T getBoundT() {
		return getRawT().getBoundT();
	}

	@Override
	public T getComponentT() {
		// modified type is also array, iff raw type is array
		return getRawT().getComponentT();
	}

	@Override
	public Element getDeclarationOwner() {
		return getRawT().getDeclarationOwner();
	}

	@Override
	public List<Element> getDeclarations() {
		return getRawT().getDeclarations();
	}

	@Override
	public DU getDu() {
		return getRawT().getDu();
	}

	@Override
	public T getElementT() {
		// modified type is also array, iff raw type is array
		return getRawT().getElementT();
	}

	@Override
	public M getEnclosingM() {
		return getRawT().getEnclosingM();
	}

	@Override
	public T getEnclosingT() {
		return getRawT().getEnclosingT();
	}

	@Override
	public T[] getInterfaceTs() {
		return getRawT().getInterfaceTs();
	}

	@Override
	public int getKind() {
		return getRawT().getKind();
	}

	@Override
	public Map<String, Object> getMember() {
		return getRawT().getMember();
	}

	@Override
	public String getName() {
		return getRawT().getName();
	}

	@Override
	public T getQualifierT() {
		// modified type is also qualified, iff raw type is qualified
		return getRawT().getQualifierT();
	}

	@Override
	public T getSuperT() {
		return getRawT().getSuperT();
	}

	@Override
	public TD getTd() {
		return getRawT().getTd();
	}

	@Override
	public T[] getTypeArgs() {
		return getRawT().getTypeArgs();
	}

	@Override
	public int getVersion() {
		return getRawT().getVersion();
	}

	@Override
	public boolean isArray() {
		// modified type is also array, iff raw type is array
		return getRawT().isArray();
	}

	@Override
	public boolean isAssignableFrom(final T t) {
		return getRawT().isAssignableFrom(t);
	}

	@Override
	public boolean isDeclaration() {
		return getRawT().isDeclaration();
	}

	@Override
	public boolean isInterface() {
		return getRawT().isInterface();
	}

	@Override
	public boolean isPrimitive() {
		return getRawT().isPrimitive();
	}

	@Override
	public boolean isQualified() {
		// modified type is also qualified, iff raw type is qualified
		return getRawT().isQualified();
	}

	@Override
	public boolean isRef() {
		return getRawT().isRef();
	}

	@Override
	public boolean isSynthetic() {
		return getRawT().isSynthetic();
	}

	@Override
	public boolean isUnresolvable() {
		return getRawT().isUnresolvable();
	}

	@Override
	public void setBoundT(final T boundT) {
		// for annotation application
		getRawT().setBoundT(boundT);
	}

	@Override
	public void setComponentT(final T componentT) {
		// modified type is also array, iff raw type is array
		getRawT().setComponentT(componentT);
	}

	@Override
	public void setEnclosingT(final T enclosingT) {
		getRawT().setEnclosingT(enclosingT);
	}

	@Override
	public void setInterface(final boolean f) {
		getRawT().setInterface(f);
	}

	@Override
	public void setQualifierT(final T qualifierT) {
		// modified type is also qualified, iff raw type is qualified
		getRawT().setQualifierT(qualifierT);
	}

	@Override
	public void setRawT(final T rawT) {
		assert rawT != null;

		this.rawT = rawT;
	}

	@Override
	public void setSynthetic() {
		getRawT().setSynthetic();
	}

}