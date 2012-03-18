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
package org.decojer.web.controller;

import lombok.Getter;
import lombok.Setter;

import org.decojer.web.service.URLFetchService;

/**
 * Merge.
 * 
 * @author Andr� Pankraz
 */
public class URLFetch {

	@Getter
	@Setter
	public String url;

	private final URLFetchService urlFetchService = URLFetchService.getInstance();

	public String getFetchResult() {
		if (this.url == null) {
			return "";
		}
		final byte[] fetchContent = this.urlFetchService.fetchContent(this.url, true);
		if (fetchContent == null) {
			return "No result: " + this.url;
		}
		return new String(fetchContent);
	}

}