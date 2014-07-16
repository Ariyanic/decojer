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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.decojer.DecoJerException;
import org.decojer.cavaj.model.code.ops.GOTO;
import org.decojer.cavaj.model.code.ops.Op;
import org.decojer.cavaj.model.code.ops.RET;
import org.decojer.cavaj.model.code.structs.Struct;
import org.decojer.cavaj.model.types.T;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Basic block for CFG.
 *
 * @author André Pankraz
 */
public final class BB {

	@Getter
	@Nonnull
	private final CFG cfg;

	@Nonnull
	private final List<E> ins = Lists.newArrayListWithCapacity(2);

	@Nonnull
	private final List<Op> ops = Lists.newArrayList();

	/**
	 * Cond / loop-back / switch case and catch outs, pc-ordered after initial read.
	 *
	 * Pc-ordering at initial read automatically through read-order and some sorts at branches.
	 */
	@Nonnull
	private final List<E> outs = Lists.newArrayListWithCapacity(2);

	/**
	 * Must cache and manage first operation PC separately because operations are removed through
	 * Java statement transformations.
	 */
	@Getter
	private int pc;

	@Getter
	@Setter
	private int postorder;

	@Nonnull
	private final List<Statement> stmts = Lists.newArrayListWithCapacity(2);

	@Getter
	@Setter
	@Nullable
	private Struct struct;

	/**
	 * Stack expression number (stack size).
	 */
	@Getter
	private int top;

	@Nonnull
	private Expression[] vs;

	protected BB(@Nonnull final CFG cfg, final int pc) {
		this.cfg = cfg;
		assert pc >= 0 : pc;
		setPc(pc);
		this.vs = new Expression[getRegs()];
	}

	/**
	 * Add handler.
	 *
	 * @param handler
	 *            handler BB
	 * @param catchTs
	 *            catch types
	 * @return out edge
	 */
	public E addCatchHandler(@Nonnull final BB handler, @Nonnull final T[] catchTs) {
		return addSucc(handler, catchTs);
	}

	protected final void addIn(@Nonnull final E e) {
		e.setEnd(this); // necessary asserts are included here
		this.ins.add(e);
	}

	/**
	 * Add operation.
	 *
	 * @param op
	 *            operation
	 */
	public void addOp(final Op op) {
		this.ops.add(op);
	}

	protected final void addOut(@Nonnull final E e) {
		e.setStart(this); // necessary asserts are included here
		this.outs.add(e);
	}

	/**
	 * Add statement.
	 *
	 * @param stmt
	 *            statement
	 */
	public void addStmt(final Statement stmt) {
		this.stmts.add(stmt);
	}

	/**
	 * Add successor.
	 *
	 * @param succ
	 *            successor BB
	 * @param value
	 *            value
	 * @return out edge
	 */
	public final E addSucc(@Nonnull final BB succ, @Nullable final Object value) {
		final E e = new E(value);
		addOut(e); // add as last important for cleanupOuts
		succ.addIn(e);
		return e;
	}

	/**
	 * Add switch case.
	 *
	 * @param caseBb
	 *            case BB
	 * @param values
	 *            Integer values
	 * @return out edge
	 */
	public E addSwitchCase(@Nonnull final BB caseBb, @Nonnull final Object[] values) {
		return addSucc(caseBb, values);
	}

	/**
	 * Just one outgoing sequence or condition allowed.
	 *
	 * All public methods in CFG, BB, E have to be atomar and leave a consistent CFG state.
	 */
	private void cleanupOuts() {
		boolean foundSequence = false;
		boolean foundCondTrue = false;
		boolean foundCondFalse = false;
		for (int i = this.outs.size(); i-- > 0;) {
			final E out = this.outs.get(i);
			if (out.isSequence()) {
				if (foundSequence || foundCondFalse || foundCondTrue) {
					out.remove();
				} else {
					foundSequence = true;
				}
				continue;
			}
			if (out.isCondFalse()) {
				if (foundSequence || foundCondFalse) {
					out.remove();
				} else {
					foundCondFalse = true;
				}
				continue;
			}
			if (out.isCondTrue()) {
				if (foundSequence || foundCondTrue) {
					out.remove();
				} else {
					foundCondTrue = true;
				}
				continue;
			}
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof BB)) {
			return false;
		}
		return this.pc == ((BB) obj).pc;
	}

	/**
	 * Get local expression.
	 *
	 * @param i
	 *            index
	 * @return expression
	 */
	public Expression get(final int i) {
		return this.vs[i];
	}

	/**
	 * Get expression from expression statement at index.
	 *
	 * @param i
	 *            expression statement index
	 * @return expression
	 */
	@Nullable
	public Expression getExpression(final int i) {
		final Statement stmt = getStmt(i);
		if (!(stmt instanceof ExpressionStatement)) {
			return null;
		}
		return ((ExpressionStatement) stmt).getExpression();
	}

	/**
	 * Get (conditional) false out.
	 *
	 * @return false out
	 */
	@Nullable
	public E getFalseOut() {
		for (final E out : this.outs) {
			if (out.getValue() == Boolean.FALSE) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get (conditional) false successor.
	 *
	 * @return false successor
	 */
	@Nullable
	public BB getFalseSucc() {
		final E out = getFalseOut();
		return out == null ? null : out.getEnd();
	}

	/**
	 * Get final operation.
	 *
	 * @return final operation
	 */
	@Nullable
	public Op getFinalOp() {
		return this.ops.isEmpty() ? null : this.ops.get(this.ops.size() - 1);
	}

	/**
	 * Get final statement.
	 *
	 * @return final statement
	 */
	@Nullable
	public Statement getFinalStmt() {
		return this.stmts.isEmpty() ? null : this.stmts.get(this.stmts.size() - 1);
	}

	public String[][] getFrameInfos() {
		final int regs = getRegs();
		final int stackRegs = getStackRegs();
		final int ops = getOps();
		final String[][] frameInfos = new String[1 + ops][];
		final String[] header = new String[2 + getRegs() + getStackRegs()];
		header[0] = "PC";
		header[1] = "Operation";
		for (int j = 0; j < stackRegs; ++j) {
			header[2 + j] = "s" + j;
		}
		for (int j = 0; j < regs; ++j) {
			header[2 + stackRegs + j] = "r" + j;
		}
		frameInfos[0] = header;
		for (int i = 0; i < ops; ++i) {
			final String[] row = new String[header.length];
			frameInfos[1 + i] = row;
			final Op op = getOp(i);
			row[0] = Integer.toString(op.getPc());
			// align header
			if (header[0].length() < row[0].length()) {
				header[0] += Strings.repeat(" ", row[0].length() - header[0].length());
			}
			row[1] = op.toString();
			// align header
			if (header[1].length() < row[1].length()) {
				header[1] += Strings.repeat(" ", row[1].length() - header[1].length());
			}
			final Frame frame = getCfg().getInFrame(op);
			if (frame == null) {
				continue;
			}
			for (int j = 0; j < frame.getTop(); ++j) {
				final R r = frame.load(regs + j);
				if (r != null) {
					row[2 + j] = (frame.isAlive(j) ? "A " : "") + r.getSimpleName();
					// align header
					if (header[2 + j].length() < row[2 + j].length()) {
						header[2 + j] += Strings.repeat(" ",
								row[2 + j].length() - header[2 + j].length());
					}
				}
			}
			for (int j = 0; j < regs; ++j) {
				final R r = frame.load(j);
				if (r != null) {
					row[2 + stackRegs + j] = (frame.isAlive(j) ? "A " : "") + r.getSimpleName();
					// align header
					if (header[2 + stackRegs + j].length() < row[2 + stackRegs + j].length()) {
						header[2 + stackRegs + j] += Strings.repeat(
								" ",
								row[2 + stackRegs + j].length()
								- header[2 + stackRegs + j].length());
					}
				}
			}
		}
		return frameInfos;
	}

	public String getFrameInfosString() {
		final int stackRegs = getStackRegs();
		final String[][] frameInfos = getFrameInfos();
		final String[] header = frameInfos[0];
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < frameInfos.length; ++i) {
			final String[] row = frameInfos[i];
			sb.append("\n|");
			for (int j = 0; j < row.length; ++j) {
				String str = row[j];
				if (str == null) {
					str = "";
				}
				sb.append(str);
				sb.append(Strings.repeat(" ", header[j].length() - str.length()));
				if (j == 1 || j - 1 == stackRegs) {
					sb.append(" # ");
					continue;
				}
				sb.append(" | ");
			}
			if (i == 0) {
				sb.append('\n').append(Strings.repeat("-", sb.length() - 3));
			}
		}
		return sb.toString();
	}

	/**
	 * Get immediate dominator (IDom).
	 *
	 * @return immediate domminator (IDom)
	 */
	public BB getIDom() {
		return getCfg().getIDom(this);
	}

	public List<E> getIns() {
		assert !isRemoved();
		return Collections.unmodifiableList(this.ins);
	}

	/**
	 * Get first operation line.
	 *
	 * @return first operation line
	 */
	public int getLine() {
		if (this.pc < 0 || this.pc > getCfg().getOps().length) {
			return -1;
		}
		return getCfg().getOps()[this.pc].getLine();
	}

	/**
	 * Get operation at index.
	 *
	 * @param i
	 *            operation index
	 * @return operation
	 */
	public Op getOp(final int i) {
		return this.ops.get(i);
	}

	/**
	 * Get operations number.
	 *
	 * @return operations number
	 */
	public int getOps() {
		return this.ops.size();
	}

	public List<E> getOuts() {
		assert !isRemoved();
		return Collections.unmodifiableList(this.outs);
	}

	/**
	 * Get register number (locals).
	 *
	 * @return register number (locals)
	 */
	public int getRegs() {
		return getCfg().getRegs();
	}

	/**
	 * Get relevant in.
	 *
	 * @return relevant in
	 *
	 * @see E#getRelevantIn()
	 */
	@Nullable
	public E getRelevantIn() {
		// only single ins are relevant, including none-sequences like conditional edges
		return this.ins.size() != 1 ? null : this.ins.get(0).getRelevantIn();
	}

	/**
	 * Get relevant out.
	 *
	 * @return relevant out
	 *
	 * @see E#getRelevantEnd()
	 */
	@Nullable
	public E getRelevantOut() {
		final E out = getSequenceOut();
		// only sequence outs are relevant, don't follow error handlers etc.
		return out == null ? null : out.getRelevantOut();
	}

	/**
	 * Get sequence in.
	 *
	 * Relevant outs are sequence edges with relevant outs, relevant ins are single ins (any type)
	 * where the start BB is relevant and has only a single in.
	 *
	 * @return sequence in
	 */
	@Nullable
	public E getSequenceIn() {
		if (this.ins.size() != 1) {
			return null;
		}
		return this.ins.get(0);
	}

	/**
	 * Get sequence out.
	 *
	 * Relevant outs are sequence edges with relevant outs, relevant ins are single ins (any type)
	 * where the start BB is relevant and has only a single in.
	 *
	 * @return sequence out
	 */
	@Nullable
	public E getSequenceOut() {
		for (final E out : this.outs) {
			if (out.isSequence()) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get maximum stack register number.
	 *
	 * @return maximum stack register number
	 */
	public int getStackRegs() {
		int stackRegs = 0;
		for (int i = 0; i < getOps(); ++i) {
			final Frame frame = getCfg().getInFrame(getOp(i));
			if (frame == null) {
				break;
			}
			if (stackRegs < frame.getTop()) {
				stackRegs = frame.getTop();
			}
		}
		return stackRegs;
	}

	/**
	 * Get statement at index.
	 *
	 * @param i
	 *            statement index
	 * @return statement
	 */
	@Nullable
	public Statement getStmt(final int i) {
		final int size = this.stmts.size();
		return size <= i ? null : this.stmts.get(i);
	}

	/**
	 * Get statements numer.
	 *
	 * @return statements number
	 */
	public int getStmts() {
		return this.stmts.size();
	}

	/**
	 * Get switch default out.
	 *
	 * @return switch default out
	 */
	@Nullable
	public E getSwitchDefaultOut() {
		for (final E out : this.outs) {
			if (out.isSwitchDefault()) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get (conditional) true out.
	 *
	 * @return true out
	 */
	@Nullable
	public E getTrueOut() {
		for (final E out : this.outs) {
			if (Boolean.TRUE == out.getValue()) {
				return out;
			}
		}
		return null;
	}

	/**
	 * Get (conditional) true successor.
	 *
	 * @return true successor
	 */
	@Nullable
	public BB getTrueSucc() {
		final E out = getTrueOut();
		return out == null ? null : out.getEnd();
	}

	@Override
	public int hashCode() {
		return this.pc;
	}

	/**
	 * Is this BB before (or same like) given BB?
	 *
	 * @param bb
	 *            given BB
	 * @return {@code true} - this BB is before given BB, also for same BBs or given BB as
	 *         {@code null}
	 */
	public boolean isBefore(@Nullable final BB bb) {
		if (bb == null) {
			return true;
		}
		if (getLine() < bb.getLine()) {
			return true;
		}
		if (getLine() == bb.getLine() && getPc() <= bb.getPc()) {
			return true;
		}
		return false;
	}

	/**
	 * Is BB a catch handler?
	 *
	 * @return {@code true} - BB is a catch handler
	 */
	public boolean isCatchHandler() {
		for (final E in : this.ins) {
			if (in.isCatch()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is conditional BB? (e.g. if() or loop head)
	 *
	 * Exclude empty if-statements that can be created by rewriteConditionalConstants().
	 *
	 * @return {@code true} - is conditional BB
	 */
	public boolean isCond() {
		return getFinalStmt() instanceof IfStatement && getTrueOut() != null
				&& getFalseOut() != null;
	}

	/**
	 * Is line information available?
	 *
	 * @return {@code true} - line information is available
	 */
	public boolean isLineInfo() {
		return getLine() >= 0;
	}

	/**
	 * Is given BB a predecessor (or same)?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - given BB is a predecessor (or same)
	 */
	public boolean isPred(@Nonnull final BB bb) {
		final List<BB> checks = Lists.newArrayList(this);
		final Set<BB> checked = Sets.newHashSet();
		while (!checks.isEmpty()) {
			final BB check = checks.remove(0);
			if (check == bb) {
				return true;
			}
			checked.add(check);
			if (check.getPostorder() >= bb.getPostorder()) {
				// check cannot have BB as pred here, are above it in postordering
				continue;
			}
			for (final E in : this.ins) {
				if (in.isBack()) {
					continue;
				}
				final BB pred = in.getStart();
				if (checked.contains(pred) || checks.contains(pred)) {
					continue;
				}
				checks.add(pred);
			}
		}
		return false;
	}

	/**
	 * Is BB relevant?
	 *
	 * None-empty BBs which are not single GOTO operations (after CFG building) are relevant.
	 *
	 * We could exclude this BBs in CFG building, but may be they are an interesting info for
	 * decompiling structures.
	 *
	 * @return {@code true} - BB is empty
	 */
	public boolean isRelevant() {
		// important in-check for getRelevantOut()
		if (this.ins.size() != 1) {
			return true;
		}
		// for ops.isEmpty() -> later GOTO check
		if (!this.stmts.isEmpty() || !isStackEmpty()) {
			return true;
		}
		for (final Op op : this.ops) {
			if (!(op instanceof GOTO)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isRemoved() {
		return this.pc < 0;
	}

	/**
	 * Is sub-ret BB?
	 *
	 * @return {@code true} - is sub-ret BB
	 */
	public boolean isRet() {
		return getFinalOp() instanceof RET;
	}

	/**
	 * Is BB a sequence?
	 *
	 * @return {@code true} - BB is a sequence
	 */
	public boolean isSequence() {
		for (final E out : this.outs) {
			if (out.isSequence()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is stack empty?
	 *
	 * @return {@code true} - stack is empty
	 */
	public boolean isStackEmpty() {
		return this.top <= 0;
	}

	/**
	 * Is start BB?
	 *
	 * @return {@code true} - is start BB
	 */
	public boolean isStartBb() {
		return getCfg().getStartBb() == this;
	}

	/**
	 * Is given BB a successor (or same)?
	 *
	 * @param bb
	 *            BB
	 * @return {@code true} - given BB is a successor (or same)
	 */
	public boolean isSucc(@Nonnull final BB bb) {
		return !isPred(bb) || this == bb;
	}

	/**
	 * Copy content from predecessor BB.
	 *
	 * @param bb
	 *            predecessor BB
	 */
	public void joinPredBb(final BB bb) {
		this.ops.addAll(0, bb.ops);
		this.stmts.addAll(0, bb.stmts);
		if (bb.top > 0) {
			if (getRegs() + bb.top + this.top > this.vs.length) {
				final Expression[] newVs = new Expression[getRegs() + bb.top + this.top];
				System.arraycopy(this.vs, 0, newVs, 0, getRegs());
				System.arraycopy(this.vs, getRegs(), newVs, getRegs() + bb.top, this.top);
				this.vs = newVs;
			} else {
				// shift right
				System.arraycopy(this.vs, getRegs(), this.vs, getRegs() + bb.top, this.top);
			}
			System.arraycopy(bb.vs, getRegs(), this.vs, getRegs(), bb.top);
			this.top += bb.top;
		}
		setPc(bb.getPc());
		bb.moveIns(this);
	}

	/**
	 * Move in edges to target BB. Adjust CFG start BB.
	 *
	 * Is an atomic operation and removes this node.
	 *
	 * @param target
	 *            target BB
	 */
	public void moveIns(@Nonnull final BB target) {
		if (target == this) {
			assert false;
			return;
		}
		if (getCfg().getStartBb() == this) {
			getCfg().setStartBb(target);
		}
		for (final E in : this.ins) {
			assert in != null;
			target.addIn(in);
		}
		this.ins.clear(); // necessary, all incomings are relocated, don't remove!
		remove();
	}

	/**
	 * Move out egde to another index in outs list. For switch case rewrites.
	 *
	 * @param fromIndex
	 *            from index
	 * @param toIndex
	 *            to index
	 */
	public void moveOut(final int fromIndex, final int toIndex) {
		final E e = this.outs.remove(fromIndex);
		this.outs.add(toIndex, e);
	}

	/**
	 * Peek stack expression.
	 *
	 * @return expression
	 */
	@Nonnull
	public Expression peek() {
		if (this.top <= 0) {
			throw new DecoJerException(getCfg() + ": Stack is empty for: " + this);
		}
		final Expression e = this.vs[getRegs() + this.top - 1];
		assert e != null;
		return e;
	}

	/**
	 * Peek stack expression.
	 *
	 * @param i
	 *            reverse stack index
	 * @return expression
	 */
	@Nonnull
	public Expression peek(final int i) {
		if (this.top <= i) {
			throw new DecoJerException(getCfg() + ": Stack is empty for: " + this);
		}
		final Expression e = this.vs[getRegs() + this.top - 1];
		assert e != null;
		return e;
	}

	/**
	 * Pop stack expression.
	 *
	 * @return expression
	 */
	@Nonnull
	public Expression pop() {
		if (this.top <= 0) {
			throw new IndexOutOfBoundsException("Stack is empty!");
		}
		final Expression e = this.vs[getRegs() + --this.top];
		assert e != null;
		return e;
	}

	/**
	 * Push stack expression.
	 *
	 * @param v
	 *            expression
	 */
	public void push(@Nonnull final Expression v) {
		if (getRegs() + this.top >= this.vs.length) {
			final Expression[] newVs = new Expression[getRegs() + this.top + 1];
			System.arraycopy(this.vs, 0, newVs, 0, getRegs() + this.top);
			this.vs = newVs;
		}
		this.vs[getRegs() + this.top++] = v;
	}

	/**
	 * Push stack expression as first stack value.
	 *
	 * @param v
	 *            expression
	 */
	public void pushFirst(@Nonnull final Expression v) {
		if (getRegs() + this.top >= this.vs.length) {
			final Expression[] newVs = new Expression[getRegs() + this.top + 1];
			System.arraycopy(this.vs, 0, newVs, 0, getRegs());
			System.arraycopy(this.vs, getRegs(), newVs, getRegs() + 1, this.top);
			this.vs = newVs;
		}
		this.vs[getRegs()] = v;
		++this.top;
	}

	/**
	 * Remove BB from CFG.
	 */
	public void remove() {
		assert !isStartBb();
		for (int i = this.outs.size(); i-- > 0;) {
			final E e = this.outs.get(i);
			e.getEnd().removeIn(e);
			e.setStart(null);
		}
		this.outs.clear();
		for (int i = this.ins.size(); i-- > 0;) {
			final E e = this.ins.get(i);
			e.setEnd(null);
			e.getStart().removeOut(e);
		}
		this.ins.clear();
		getCfg().getPostorderedBbs().set(this.postorder, null);
		setPc(-1);
	}

	/**
	 * Remove final statement.
	 *
	 * @return statement
	 */
	@Nullable
	public Statement removeFinalStmt() {
		return this.stmts.isEmpty() ? null : this.stmts.remove(this.stmts.size() - 1);
	}

	protected void removeIn(final E e) {
		e.setEnd(null);
		this.ins.remove(e);
		if (this.ins.isEmpty() && !isStartBb()) {
			remove();
		}
	}

	/**
	 * Remove operation at index.
	 *
	 * @param i
	 *            operation index
	 * @return operation
	 */
	@Nullable
	public Op removeOp(final int i) {
		final int size = this.ops.size();
		return size <= i ? null : this.ops.remove(i);
	}

	protected void removeOut(final E e) {
		e.setStart(null);
		this.outs.remove(e);
	}

	/**
	 * Remove statement at index.
	 *
	 * @param i
	 *            statement index
	 * @return statement
	 */
	@Nullable
	public Statement removeStmt(final int i) {
		final int size = this.stmts.size();
		return size <= i ? null : this.stmts.remove(i);
	}

	/**
	 * Set local expression.
	 *
	 * @param i
	 *            index
	 * @param v
	 *            expression
	 */
	public void set(final int i, final Expression v) {
		this.vs[i] = v;
	}

	/**
	 * Set conditional successors.
	 *
	 * @param trueBb
	 *            true BB
	 * @param falseBb
	 *            false BB
	 */
	public void setConds(@Nonnull final BB trueBb, @Nonnull final BB falseBb) {
		// preserve pc-order as edge-order
		if (falseBb.getPc() < trueBb.getPc()) {
			// usual case, if not a direct branching
			addSucc(falseBb, Boolean.FALSE);
			addSucc(trueBb, Boolean.TRUE);
		} else {
			addSucc(trueBb, Boolean.TRUE);
			addSucc(falseBb, Boolean.FALSE);
		}
		cleanupOuts();
	}

	/**
	 * Set JSR successor.
	 *
	 * @param succ
	 *            JSR successor (Sub routine).
	 * @param sub
	 *            sub routine
	 * @return out edge
	 */
	public final E setJsrSucc(@Nonnull final BB succ, @Nonnull final Sub sub) {
		return addSucc(succ, sub);
	}

	protected void setPc(final int pc) {
		assert pc >= 0 || this.ins.isEmpty() && this.outs.isEmpty();
		this.pc = pc;
	}

	/**
	 * Set RET successor.
	 *
	 * @param succ
	 *            RET successor
	 * @param sub
	 *            sub routine
	 * @return out edge
	 */
	public final E setRetSucc(@Nonnull final BB succ, @Nonnull final Sub sub) {
		return addSucc(succ, sub);
	}

	/**
	 * Set successor.
	 *
	 * @param succ
	 *            successor
	 * @return out edge
	 */
	public final E setSucc(@Nonnull final BB succ) {
		final E e = addSucc(succ, null);

		cleanupOuts();
		return e;
	}

	/**
	 * Sort outs.
	 */
	public void sortOuts() {
		if (!getCfg().isLineInfo()) {
			return;
		}
		Collections.sort(this.outs, E.LINE_COMPARATOR);
	}

	/**
	 * Split off predecessor BB.
	 *
	 * Necessary for CFG building, we must preserve "this" for new found backloops into same BB.
	 *
	 * @param pc
	 *            new first operation PC for this BB
	 *
	 * @return new predecessor BB
	 */
	public BB splitPredBb(final int pc) {
		final BB bb = getCfg().newBb(getPc());
		// like moveIns(this), but without final remove
		if (getCfg().getStartBb() == this) {
			getCfg().setStartBb(bb);
		}
		for (final E in : this.ins) {
			assert in != null;
			bb.addIn(in);
		}
		this.ins.clear(); // necessary, all incomings are relocated, don't remove!
		bb.setSucc(this);
		setPc(pc);
		return bb;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("BB").append(getPc());
		if (getPostorder() > 0) {
			sb.append(" / ").append(getPostorder());
		}
		if (getLine() >= 0) {
			sb.append(" (l").append(getLine()).append(')');
		}
		if (this.ops.size() > 0) {
			sb.append("\nOps: ").append(this.ops);
		}
		if (this.stmts.size() > 0) {
			sb.append("\nStmts: ").append(this.stmts);
		}
		if (this.top > 0) {
			sb.append("\nStack: ").append(
					Arrays.toString(Arrays.copyOfRange(this.vs, getRegs(), getRegs() + this.top)));
		}
		return sb.toString();
	}

}