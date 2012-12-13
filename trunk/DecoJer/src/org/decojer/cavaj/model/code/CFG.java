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
package org.decojer.cavaj.model.code;

import java.util.List;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;

import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.MD;
import org.decojer.cavaj.model.T;
import org.decojer.cavaj.model.TD;
import org.decojer.cavaj.model.code.R.Kind;
import org.decojer.cavaj.model.code.ops.Op;
import org.eclipse.jdt.core.dom.Block;

/**
 * Control flow graph.
 * 
 * @author André Pankraz
 */
public final class CFG {

	private final static Logger LOGGER = Logger.getLogger(CFG.class.getName());

	/**
	 * AST method block.
	 */
	@Getter
	@Setter
	private Block block;

	@Getter
	@Setter
	private Exc[] excs;

	private Frame[] frames;

	/**
	 * Array with immediate dominators, index is postorder.
	 */
	private BB[] iDoms;

	/**
	 * Max stack size.
	 */
	@Getter
	private final int maxStack;

	@Getter
	private final MD md;

	@Getter
	@Setter
	private Op[] ops;

	/**
	 * Array with postordered basic blocks.
	 */
	@Getter
	@Setter
	private List<BB> postorderedBbs;

	/**
	 * Register count (max locals).
	 */
	@Getter
	private final int regs;

	@Getter
	@Setter
	private BB startBb;

	private V[][] vss;

	/**
	 * Constructor.
	 * 
	 * @param md
	 *            method declaration
	 * @param regs
	 *            register count (max locals)
	 * @param maxStack
	 *            max stack size
	 */
	public CFG(final MD md, final int regs, final int maxStack) {
		assert md != null;
		assert regs >= 0 : regs;
		assert maxStack >= 0 : maxStack;

		this.md = md;
		this.regs = regs;
		this.maxStack = maxStack;
	}

	/**
	 * Add local variable.
	 * 
	 * Only basic checks, compare later with method parameters.
	 * 
	 * @param reg
	 *            register
	 * @param var
	 *            local variable
	 */
	public void addVar(final int reg, final V var) {
		assert var != null;

		V[] vars = null;
		if (this.vss == null) {
			this.vss = new V[reg + 1][];
		} else if (reg >= this.vss.length) {
			final V[][] newVarss = new V[reg + 1][];
			System.arraycopy(this.vss, 0, newVarss, 0, this.vss.length);
			this.vss = newVarss;
		} else {
			vars = this.vss[reg];
		}
		if (vars == null) {
			vars = new V[1];
		} else {
			final V[] newVars = new V[vars.length + 1];
			System.arraycopy(vars, 0, newVars, 0, vars.length);
			vars = newVars;
		}
		vars[vars.length - 1] = var;
		this.vss[reg] = vars;
	}

	/**
	 * Calculate iDoms.
	 */
	public void calculateIDoms() {
		int b = this.postorderedBbs.size();
		this.iDoms = new BB[b--];
		this.iDoms[b] = this.postorderedBbs.get(b);
		boolean changed = true;
		while (changed) {
			changed = false;
			// start with rootNode, means this.postorderBBNodes.length - 1
			for (; b-- > 0;) {
				BB iDomNew = null;
				for (final E in : this.postorderedBbs.get(b).getIns()) {
					final BB pred = in.getStart();
					if (this.iDoms[pred.getPostorder()] == null) {
						continue;
					}
					if (iDomNew == null) {
						iDomNew = pred;
						continue;
					}
					iDomNew = intersectIDoms(pred, iDomNew);
				}
				if (this.iDoms[b] == iDomNew) {
					continue;
				}
				this.iDoms[b] = iDomNew;
				changed = true;
			}
		}
	}

	/**
	 * Clear CFG.
	 */
	public void clear() {
		this.block = null;
		this.frames = null;
		this.iDoms = null;
		this.postorderedBbs = null;
		this.startBb = null;
	}

	/**
	 * Get compilation unit.
	 * 
	 * @return compilation unit
	 */
	public CU getCu() {
		return getTd().getCu();
	}

	/**
	 * Get local variable (from debug info).
	 * 
	 * @param reg
	 *            register
	 * @param pc
	 *            pc
	 * 
	 * @return local variable (from debug info)
	 */
	public V getDebugV(final int reg, final int pc) {
		if (this.vss == null || reg >= this.vss.length) {
			return null;
		}
		final V[] vs = this.vss[reg];
		if (vs == null) {
			return null;
		}
		for (int i = vs.length; i-- > 0;) {
			final V v = vs[i];
			if (v.validIn(pc)) {
				return v;
			}
		}
		return null;
	}

	/**
	 * Get decompilation unit.
	 * 
	 * @return decompilation unit
	 */
	public DU getDu() {
		return getTd().getDu();
	}

	/**
	 * Get frame for PC.
	 * 
	 * @param pc
	 *            PC
	 * @return frame
	 */
	public Frame getFrame(final int pc) {
		return this.frames[pc];
	}

	/**
	 * Get local variable (from frame).
	 * 
	 * @param reg
	 *            register
	 * @param pc
	 *            PC
	 * @return local variable (from frame)
	 */
	public V getFrameVar(final int reg, final int pc) {
		return getDebugV(reg, pc); // hack TODO this.frames[pc].get(reg);
	}

	/**
	 * Get immediate dominator (IDom) for basic block.
	 * 
	 * @param bb
	 *            basic block
	 * @return immediate domminator (IDom) for basic block
	 */
	public BB getIDom(final BB bb) {
		return this.iDoms[bb.getPostorder()];
	}

	/**
	 * Get input frame for basic block.
	 * 
	 * @param bb
	 *            basic block
	 * @return input frame
	 */
	public Frame getInFrame(final BB bb) {
		return this.frames[bb.getPc()];
	}

	/**
	 * Get input frame for operation.
	 * 
	 * @param op
	 *            operation
	 * @return input frame
	 */
	public Frame getInFrame(final Op op) {
		return this.frames[op.getPc()];
	}

	/**
	 * Get output frame for operation. Doesn't (and must not) work for control flow statements.
	 * 
	 * @param op
	 *            operation
	 * @return output frame
	 */
	public Frame getOutFrame(final Op op) {
		return this.frames[op.getPc() + 1];
	}

	/**
	 * Get type declaration.
	 * 
	 * @return type declaration
	 */
	public TD getTd() {
		return getMd().getTd();
	}

	/**
	 * Initialize frames. Create first frame from method parameters.
	 */
	public void initFrames() {
		this.frames = new Frame[this.ops.length];
		final Frame frame = new Frame(this);
		for (int reg = getRegs(); reg-- > 0;) {
			final V v = getDebugV(reg, 0);
			if (v != null) {
				frame.store(reg, new R(0, v.getT(), Kind.CONST));
			}
		}
		this.frames[0] = frame;
	}

	private BB intersectIDoms(final BB b1, final BB b2) {
		BB finger1 = b1;
		BB finger2 = b2;
		while (finger1 != finger2) {
			while (finger1.getPostorder() < finger2.getPostorder()) {
				finger1 = this.iDoms[finger1.getPostorder()];
			}
			while (finger2.getPostorder() < finger1.getPostorder()) {
				finger2 = this.iDoms[finger2.getPostorder()];
			}
		}
		return finger1;
	}

	/**
	 * Are frames initialized?
	 * 
	 * @return {@code true} - frames are initialized
	 */
	public boolean isFrames() {
		return this.frames != null;
	}

	/**
	 * Should transformer ignore this?
	 * 
	 * @return {@code true} - ignore this
	 */
	public boolean isIgnore() {
		return this.ops == null || this.ops.length == 0 || this.block == null;
	}

	/**
	 * Is line information available?
	 * 
	 * @return {@code true} - line information is available
	 */
	public boolean isLineInfo() {
		return this.startBb.isLineInfo();
	}

	/**
	 * New basic block.
	 * 
	 * @param opPc
	 *            first operation PC
	 * 
	 * @return basic block
	 */
	public BB newBb(final int opPc) {
		return new BB(this, opPc);
	}

	/**
	 * Post process local variables, e.g. extract method parameter. Better in read step than in
	 * transformator for generic base model. Dalvik has parameter names separately encoded.
	 */
	public void postProcessVars() {
		if (this.vss == null) {
			this.vss = new V[this.regs][];
		} else if (this.regs < this.vss.length) {
			LOGGER.warning("Register count less than biggest register with local variable info!");
		} else if (this.regs > this.vss.length) {
			final V[][] newVarss = new V[this.regs][];
			System.arraycopy(this.vss, 0, newVarss, 0, this.vss.length);
			this.vss = newVarss;
		}
		final T[] paramTs = this.md.getParamTs();
		if (this.md.getTd().isDalvik()) {
			// Dalvik...function parameters right aligned
			int reg = this.regs;
			for (int i = paramTs.length; i-- > 0;) {
				final T paramT = paramTs[i];
				if (paramT.isWide()) {
					--reg;
				}
				// parameter name was encoded in extra debug info, copy names
				// and parameter types to local vars
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					LOGGER.warning("Found local variable info for method parameter '" + reg + "'!");
				}
				this.vss[reg] = new V[] { new V(paramT, this.md.getParamName(i), 0, this.ops.length) };
			}
			if (!this.md.isStatic()) {
				final V[] vs = this.vss[--reg];
				if (vs != null) {
					LOGGER.warning("Found local variable info for method parameter '" + reg
							+ "' (this)!");
				}
				this.vss[reg] = new V[] { new V(this.md.getTd().getT(), "this", 0, this.ops.length) };
			}
			return;
		}
		// JVM...function parameters left aligned
		int reg = 0;
		if (!this.md.isStatic()) {
			final V[] vs = this.vss[reg];
			if (vs != null) {
				if (vs.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "' (this)!");
				}
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(this.md.getTd().getT(), "this", 0,
						this.ops.length) };
			}
		}
		for (int i = 0; i < paramTs.length; ++i) {
			final T paramT = paramTs[i];
			final V[] vs = this.vss[reg];
			if (vs != null) {
				if (vs.length > 1) {
					LOGGER.warning("Found multiple local variable info for method parameter '"
							+ reg + "'!");
				}
				this.md.setParamName(i, vs[0].getName());
				++reg;
			} else {
				this.vss[reg++] = new V[] { new V(paramT, this.md.getParamName(i), 0,
						this.ops.length) };
			}
			if (paramT.isWide()) {
				++reg;
			}
		}
	}

	/**
	 * Set frame for PC.
	 * 
	 * @param pc
	 *            PC
	 * @param frame
	 *            frame
	 */
	public void setFrame(final int pc, final Frame frame) {
		this.frames[pc] = frame;
	}

	@Override
	public String toString() {
		return getMd().toString() + " (ops: " + this.ops.length + ", regs: " + this.regs + ")";
	}

}