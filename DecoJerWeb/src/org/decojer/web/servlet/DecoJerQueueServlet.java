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
package org.decojer.web.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.decojer.DecoJer;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.DU;
import org.decojer.cavaj.model.TD;
import org.decojer.web.model.Upload;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

/**
 * DecoJer queue servlet.
 * 
 * @author Andr� Pankraz
 */
public class DecoJerQueueServlet extends HttpServlet {

	private static final BlobstoreService BLOBSTORE_SERVICE = BlobstoreServiceFactory
			.getBlobstoreService();

	private static final DatastoreService DATASTORE_SERVICE = DatastoreServiceFactory
			.getDatastoreService();

	private static Logger LOGGER = Logger.getLogger(DecoJerQueueServlet.class
			.getName());

	private static final MailService MAIL_SERVICE = MailServiceFactory
			.getMailService();

	private static final long serialVersionUID = -8624836355443861445L;

	private final FileService fileService = FileServiceFactory.getFileService();

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		final Key uploadKey = KeyFactory.createKey(Upload.KIND,
				req.getParameter("uploadKey"));

		final Upload upload;
		try {
			upload = new Upload(DATASTORE_SERVICE.get(uploadKey));
		} catch (final EntityNotFoundException e) {
			LOGGER.warning("Upload entity with Key '" + uploadKey
					+ "' not yet stored?");
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final BlobKey uploadBlobKey = upload.getUploadBlobKey();
		final InputStream uploadInputStream = new BufferedInputStream(
				new BlobstoreInputStream(uploadBlobKey));
		final BlobKey sourceBlobKey;

		final String filename = upload.getFilename();

		final DU du = DecoJer.createDu();
		TD td = du.read(uploadInputStream, filename, null);

		upload.setTds((long) du.getTds().size());
		if (td == null && upload.getTds().longValue() == 1L) {
			td = du.getTds().iterator().next().getValue();
		}

		if (td != null) {
			String source;
			try {
				final CU cu = DecoJer.createCu(td);
				source = DecoJer.decompile(cu);

				String sourcename = td.getSourceFileName();
				if (sourcename == null) {
					final int pos = filename.lastIndexOf('.');
					sourcename = (pos == -1 ? filename : filename.substring(0,
							pos)) + "java";
				}
				final AppEngineFile file = this.fileService.createNewBlobFile(
						"text/x-java-source", sourcename);
				final FileWriteChannel writeChannel = this.fileService
						.openWriteChannel(file, true);
				final Writer writer = Channels.newWriter(writeChannel, "UTF-8");
				writer.write(source);
				writer.close();
				writeChannel.closeFinally();

				if (upload.getSourceBlobKey() != null) {
					BLOBSTORE_SERVICE.delete(upload.getSourceBlobKey());
				}
				upload.setSourceBlobKey(this.fileService.getBlobKey(file));
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
				upload.setError(e.getMessage());
			}
		} else {
			final ByteArrayOutputStream sourceOutputStream = new ByteArrayOutputStream();
			try {
				DecoJer.decompile(du, sourceOutputStream);

				final int pos = filename.lastIndexOf('.');
				final String sourcename = (pos == -1 ? filename : filename
						.substring(0, pos)) + "_source.zip";
				final AppEngineFile file = this.fileService.createNewBlobFile(
						"application/java-archive", sourcename);
				final FileWriteChannel writeChannel = this.fileService
						.openWriteChannel(file, true);
				final OutputStream fileOutputStream = Channels
						.newOutputStream(writeChannel);
				// don't hold file open for too long (around max. 30 seconds),
				// else:
				// "Caused by: com.google.apphosting.api.ApiProxy$ApplicationException: ApplicationError: 10: Unknown",
				// don't use byte array directly, else file write request too
				// large
				// (BufferedOutputStream writes big data directly)
				IOUtils.copy(
						new ByteArrayInputStream(sourceOutputStream
								.toByteArray()), fileOutputStream);
				fileOutputStream.close();
				writeChannel.closeFinally();

				if (upload.getSourceBlobKey() != null) {
					BLOBSTORE_SERVICE.delete(upload.getSourceBlobKey());
				}
				upload.setSourceBlobKey(this.fileService.getBlobKey(file));
			} catch (final Exception e) {
				LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
				upload.setError(e.getMessage());
			}
		}
		final Transaction tx = DATASTORE_SERVICE.beginTransaction();
		try {
			DATASTORE_SERVICE.put(upload.getWrappedEntity());

			final String channelKey = req.getParameter("channelKey");
			if (channelKey != null) {
				// can currently not send directly from backend:
				// Open Issue 5123: Channel API Access from Backends
				// http://code.google.com/p/googleappengine/issues/detail?id=5123
				// ChannelServiceFactory.getChannelService().sendMessage(
				// new ChannelMessage(channelKey, "Decompiled '" + filename
				// + "'!"));
				QueueFactory.getQueue("frontendChannel").add(
						TaskOptions.Builder.withMethod(Method.GET).param(
								"channelKey", channelKey));
			}
		} finally {
			tx.commit();
		}
		sendEmail("Decompiled '" + filename + "'!");
	}

	private void sendEmail(final String textBody) {
		try {
			// sendToAdmin with or without "to" doesn't work for me in 1.5.4
			MAIL_SERVICE.send(new MailService.Message(
					"andrePankraz@decojer.org", "andrePankraz@gmail.com",
					"DecoJer worker", textBody));
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Could not send email!", e);
		}
	}

}