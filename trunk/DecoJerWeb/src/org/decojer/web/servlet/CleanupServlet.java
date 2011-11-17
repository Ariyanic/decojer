/*
 * $Id: UploadServlet.java 57 2011-07-09 12:51:55Z andrePankraz@googlemail.com $
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
package org.decojer.web.servlet;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultList;

/**
 * http://worker.decojer.appspot.com/admin/cleanup
 * 
 * @author Andr� Pankraz
 */
public class CleanupServlet extends HttpServlet {

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private final static int PAGE_SIZE = 10;

	private static final long serialVersionUID = -6567596163814017159L;

	@Override
	public void doGet(final HttpServletRequest req, final HttpServletResponse res)
			throws ServletException, IOException {
		final ServletOutputStream out = res.getOutputStream();
		out.println("<ul>");

		final HashSet<String> stat = new HashSet<String>();
		long size = 0;

		final Query q = new Query("__BlobInfo__");
		final PreparedQuery pq = DATASTORE_SERVICE.prepare(q);
		final FetchOptions fetchOptions = FetchOptions.Builder.withLimit(PAGE_SIZE);

		while (true) {
			final QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
			for (final Entity entity : results) {
				final String md5Hash = (String) entity.getProperty("md5_hash");
				size += (Long) entity.getProperty("size");

				if (stat.contains(md5Hash)) {
					out.println("<li>" + md5Hash + "</li>");
					continue;
				}
				stat.add(md5Hash);
			}

			if (results.size() < PAGE_SIZE || results.getCursor() == null) {
				break;
			}
			fetchOptions.startCursor(results.getCursor());
		}

		/*
		 * final byte[] base91Decode = IOUtils.base91Decode(key); final byte[] md5bytes = new
		 * byte[16]; System.arraycopy(base91Decode, 0, md5bytes, 0, 16); final String md5 =
		 * IOUtils.hexEncode(md5bytes); final long size = new DataInputStream(new
		 * ByteArrayInputStream(base91Decode, 16, 8)) .readLong(); System.out.println("TEST: " + md5
		 * + " : " + size);
		 */

		out.println("</ul><p>Sumsize: " + size + " Nr: " + stat.size() + "</p>");
		out.flush();
	}

}