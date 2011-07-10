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
package org.decojer.cavaj.util;

/**
 * 
 * @author Andr� Pankraz
 */
public interface MagicNumbers {

	// forth byte always 0x0A or 0x00 too?
	byte[] MAGIC_NUMBER_DEX = { (byte) 0x64, (byte) 0x65, (byte) 0x78 };

	byte[] MAGIC_NUMBER_JAVA = { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA,
			(byte) 0xBE };

	byte[] MAGIC_NUMBER_ZIP = { (byte) 0x50, (byte) 0x4B, (byte) 0x03,
			(byte) 0x04 };

}