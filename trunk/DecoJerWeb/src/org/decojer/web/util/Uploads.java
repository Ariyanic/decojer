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
package org.decojer.web.util;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.decojer.DecoJer;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.web.model.Upload;

import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.channel.ChannelServiceFactory;

/**
 * Uploads.
 * 
 * @author Andr� Pankraz
 */
public class Uploads {

	private static Logger LOGGER = Logger.getLogger(Uploads.class.getName());

	public static void addUpload(final HttpServletRequest req,
			final Upload upload) {
		List<Upload> uploads = getUploads(req.getSession());
		if (uploads == null) {
			uploads = new ArrayList<Upload>();
		} else {
			// to list end
			uploads.remove(upload);
		}
		uploads.add(upload);
		req.getSession().setAttribute("uploads", uploads); // trigger update
	}

	public static String getChannelKey(final HttpSession httpSession) {
		return httpSession.getId();
	}

	public static String getChannelToken(final HttpSession httpSession) {
		String channelToken = (String) httpSession.getAttribute("channelToken");
		if (channelToken == null) {
			channelToken = ChannelServiceFactory.getChannelService()
					.createChannel(getChannelKey(httpSession));
			httpSession.setAttribute("channelToken", channelToken);
		}
		return channelToken;
	}

	public static List<Upload> getUploads(final HttpSession httpSession) {
		return (List<Upload>) httpSession.getAttribute("uploads");
	}

	public static String getUploadsHtml(final HttpServletRequest req,
			final HttpSession httpSession) {
		final List<Upload> uploads = getUploads(httpSession);
		if (uploads == null || uploads.size() == 0) {
			return "";
		}
		final boolean channel = false;
		final StringBuilder sb = new StringBuilder("<ul>");
		for (int i = 0; i < uploads.size(); ++i) {
			final Upload upload = uploads.get(i);
			sb.append("<li><a href='/decompile?u=").append(i)
					.append("' target='_blank'>").append(upload.getFilename())
					.append("</a>");
			if (upload.getTds() > 1) {
				sb.append(" (").append(upload.getTds()).append(" classes)");
			} else {
				sb.append(" (<a href='/?u=").append(i).append("'>View</a>)");
			}
			sb.append("</li>");
		}
		sb.append("</ul>");

		sb.append(
				"<script type='text/javascript' src='/_ah/channel/jsapi'></script>'")
				.append("<script>")
				.append("  onMessage = function(msg) { window.location.reload(); };")
				.append("  channel = new goog.appengine.Channel('")
				.append(getChannelToken(httpSession))
				.append("');")
				.append("  socket = channel.open();")
				.append("  socket.onopen = function() { $('#progressbar').progressbar({ value: 1 }); };")
				.append("  socket.onmessage = onMessage;") //
				.append("</script>");

		int u;
		try {
			u = Integer.parseInt(req.getParameter("u"));
			if (u >= uploads.size()) {
				return sb.toString();
			}
		} catch (final NumberFormatException e) {
			u = uploads.size() - 1;
		}
		final Upload upload = uploads.get(u);
		if (!upload.getFilename().endsWith(".class")) {
			return sb.toString();
		}
		try {
			final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
					upload.getUploadBlobKey());
			final DU du = DecoJer.createDu();
			final TD td = du.read(upload.getFilename(),
					new BufferedInputStream(blobstoreInputStream), null);
			final CU cu = DecoJer.createCu(td);
			final String source = DecoJer.decompile(cu);
			sb.append("<hr /><pre class=\"brush: java\">")
					.append(source.replace("<", "&lt;"))
					.append("</pre><script type=\"text/javascript\">SyntaxHighlighter.all()</script>");
		} catch (final Throwable e) {
			LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
		}
		return sb.toString();
	}

}