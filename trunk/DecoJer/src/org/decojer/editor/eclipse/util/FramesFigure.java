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
package org.decojer.editor.eclipse.util;

import org.decojer.cavaj.model.code.BB;
import org.decojer.cavaj.model.code.Frame;
import org.decojer.cavaj.model.code.V;
import org.decojer.cavaj.model.code.op.Op;
import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Insets;

/**
 * Frames Figure.
 * 
 * @author Andr� Pankraz
 */
public class FramesFigure extends Figure {

	private static final GridData GRID_DATA = new GridData(GridData.FILL_HORIZONTAL);

	private static final Border LEFT_BORDER = new AbstractBorder() {

		@Override
		public Insets getInsets(final IFigure figure) {
			return new Insets(0, 5, 0, 0);
		}

		@Override
		public void paint(final IFigure figure, final Graphics graphics, final Insets insets) {
			tempRect.setBounds(getPaintRectangle(figure, insets));
			tempRect.shrink(1, 0);
			graphics.drawLine(tempRect.getTopLeft(), tempRect.getBottomLeft());
		}

	};

	/**
	 * Constructor.
	 * 
	 * @param bb
	 *            basic block
	 */
	public FramesFigure(final BB bb) {
		final int maxLocals = bb.getCfg().getMaxLocals();
		int maxStack = 0;
		// don't use bb.getCfg().getMaxLocals()
		for (final Op operation : bb.getOps()) {
			final Frame frame = bb.getCfg().getInFrame(operation);
			if (frame == null) {
				continue;
			}
			if (maxStack < frame.getStackSize()) {
				maxStack = frame.getStackSize();
			}
		}

		final GridLayout gridLayout = new GridLayout(1 + maxLocals + maxStack, false);
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
		setLayoutManager(gridLayout);

		for (final Op operation : bb.getOps()) {
			add(new Label(operation.getClass().getSimpleName() + " "));
			final Frame frame = bb.getCfg().getInFrame(operation);
			if (frame == null) {
				continue;
			}
			final int regsSize = frame.getLocals();
			for (int index = 0; index < regsSize; ++index) {
				final V v = frame.get(index);
				final Label label = new Label(v == null ? "    " : v.toString());
				label.setBorder(LEFT_BORDER);
				add(label);
			}
			for (int index = maxStack; index-- > 0;) {
				final Label label = new Label(index >= frame.getStackSize()
						|| frame.getStack(index) == null ? "    " : frame.getStack(index)
						.toString());
				label.setBorder(LEFT_BORDER);
				add(label);
			}
		}

		for (int i = 0; i < maxStack; ++i) {
			add(new Label("s" + i), GRID_DATA, 0);
		}
		for (int i = maxLocals; i-- > 0;) {
			add(new Label("r" + i), GRID_DATA, 0);
		}
		add(new Label(""), 0);
	}

}