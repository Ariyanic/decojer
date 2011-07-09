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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.decojer.DecoJer;
import org.decojer.PackageClassStreamProvider;
import org.decojer.cavaj.model.CU;
import org.decojer.cavaj.model.PF;
import org.decojer.cavaj.model.TD;
import org.decojer.web.stream.StatClassVisitor;
import org.decojer.web.util.EntityUtils;
import org.decojer.web.util.IOUtils;
import org.decojer.web.util.Messages;
import org.objectweb.asm.ClassReader;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * 
 * @author Andr� Pankraz
 */
public class UploadServlet extends HttpServlet {

	private static Logger LOGGER = Logger.getLogger(UploadServlet.class
			.getName());

	private static final long serialVersionUID = -6567596163814017159L;

	private static int MAGIC_NUMBER_MAX_LENGTH = 4;

	private static byte[] MAGIC_NUMBER = new byte[MAGIC_NUMBER_MAX_LENGTH];

	private static final byte[] MAGIC_NUMBER_DEX_CLASS = { (byte) 0x64,
			(byte) 0x65, (byte) 0x78, (byte) 0x0A };

	private static final byte[] MAGIC_NUMBER_JAVA_CLASS = { (byte) 0xCA,
			(byte) 0xFE, (byte) 0xBA, (byte) 0xBE };

	private static final byte[] MAGIC_NUMBER_ZIP = { (byte) 0x50, (byte) 0x4B,
			(byte) 0x03, (byte) 0x04 };

	private final BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();

	private final DatastoreService datastoreService = DatastoreServiceFactory
			.getDatastoreService();

	@Override
	public void doPost(final HttpServletRequest req,
			final HttpServletResponse res) throws ServletException, IOException {

		// read uploaded blob
		final Map<String, BlobKey> blobs = this.blobstoreService
				.getUploadedBlobs(req);
		BlobKey blobKey = blobs.get("file");
		if (blobKey == null) {
			Messages.addMessage(req, "File was empty!");
			res.sendRedirect("/");
			return;
		}

		final List<BlobKey> toDelete = new ArrayList<BlobKey>();

		// transaction only for transaction level SERIALIZABLE, always commit
		Transaction tx = this.datastoreService.beginTransaction();
		try {
			final Entity blobInfoEntity = EntityUtils.getBlobInfoEntity(
					this.datastoreService, blobKey);

			final String md5Hash = (String) blobInfoEntity
					.getProperty("md5_hash");

			final Query duplicateQuery = new Query("__BlobInfo__");
			duplicateQuery.addFilter("md5_hash", Query.FilterOperator.EQUAL,
					md5Hash);
			final List<Entity> duplicateEntities = this.datastoreService
					.prepare(duplicateQuery).asList(
							FetchOptions.Builder.withLimit(10));
			LOGGER.warning("Duplicate entities for hash '" + md5Hash + "': "
					+ duplicateEntities.size());
			if (duplicateEntities.size() > 1) {
				// find oldest...
				Entity oldestEntity = null;
				Date oldestDate = null;
				Date newestDate = null;
				int uploadedSum = 0;
				for (final Entity duplicateEntity : duplicateEntities) {
					final Date creationDate = (Date) duplicateEntity
							.getProperty("creation");
					Date updatedDate = (Date) duplicateEntity
							.getProperty("updated");
					if (updatedDate == null) {
						updatedDate = creationDate;
					}
					final Integer uploaded = (Integer) duplicateEntity
							.getProperty("uploaded");
					uploadedSum += uploaded == null ? 1 : uploaded.intValue();
					// init
					if (oldestDate == null) {
						oldestEntity = duplicateEntity;
						oldestDate = creationDate;
						newestDate = updatedDate;
						continue;
					}
					// one must die now...
					if (newestDate.compareTo(updatedDate) < 0) {
						newestDate = updatedDate;
					}
					Entity dieEntity;
					if (oldestDate.compareTo(creationDate) < 0) {
						dieEntity = duplicateEntity;
					} else {
						dieEntity = oldestEntity;
						oldestDate = creationDate;
						oldestEntity = duplicateEntity;
					}
					// change and delete newer entry
					toDelete.add(new BlobKey(dieEntity.getKey().getName()));
				}
				blobKey = new BlobKey(oldestEntity.getKey().getName());
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Problems with database operations.", e);
			Messages.addMessage(req, "Internal system problem!");
			res.sendRedirect("/");
			return;
		} finally {
			if (tx.isActive()) {
				tx.commit();
			}
		}

		// after transaction, entity group!
		if (!toDelete.isEmpty()) {
			this.blobstoreService.delete(toDelete.toArray(new BlobKey[toDelete
					.size()]));
			Messages.addMessage(req, "Duplicate! Delete " + toDelete.size()
					+ " entries!");
		}
		// before transaction, entity group!
		final BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(
				blobKey);

		// transaction only for transaction level SERIALIZABLE, always commit
		tx = this.datastoreService.beginTransaction();
		try {
			// TODO
			final MessageDigest subDigest = MessageDigest.getInstance("MD5");
			visitStream(blobstoreInputStream, "TEST", subDigest);
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Problems with stream visitor.", e);
			Messages.addMessage(req, "Internal system problem!");
			res.sendRedirect("/");
			return;
		} finally {
			if (tx.isActive()) {
				tx.commit();
			}
		}

		try {
			final PackageClassStreamProvider packageClassStreamProvider = new PackageClassStreamProvider(
					null);
			packageClassStreamProvider.addClassStream(blobKey.getKeyString(),
					new DataInputStream(new BlobstoreInputStream(blobKey)));
			final PF pf = DecoJer.createPF(packageClassStreamProvider);
			final Entry<String, TD> next = pf.getTds().entrySet().iterator()
					.next();
			final CU cu = DecoJer.createCU(next.getValue());
			final String source = DecoJer.decompile(cu);

			Messages.addMessage(req,
					"This is currently only a pre-alpha test-version!");
			Messages.addMessage(req, "<pre>" + source + "</pre>");
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, "Problems with decompilation.", e);
			Messages.addMessage(req, "Decompilation problems!");
			res.sendRedirect("/");
			return;
		}
		res.sendRedirect("/");
	}

	private void visitStream(final InputStream is, final String name,
			final MessageDigest digest) throws IOException,
			NoSuchAlgorithmException {
		final int read = is.read(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);
		if (read < MAGIC_NUMBER_MAX_LENGTH) {
			// System.out.println("  MN too small: " + read);
			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_JAVA_CLASS, MAGIC_NUMBER)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);

			// rough test and statistics
			final ClassReader classReader = new ClassReader(
					digest == null ? pis : new DigestInputStream(pis, digest));
			final StatClassVisitor statClassVisitor = new StatClassVisitor();
			classReader.accept(statClassVisitor, ClassReader.SKIP_FRAMES);

			final String md5Hash = IOUtils.toHexString(digest.digest());
			LOGGER.warning("  Javaclass: " + name + md5Hash);
			final Entity entity = new Entity("Class");
			entity.setProperty("name", statClassVisitor.className);
			entity.setProperty("md5_hash", md5Hash);
			entity.setProperty("content", new Blob(classReader.b));
			this.datastoreService.put(entity);
			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_DEX_CLASS, MAGIC_NUMBER)) {
			LOGGER.warning("  Dex: " + name);

			// use http://code.google.com/p/dex2jar/

			return;
		}
		if (Arrays.equals(MAGIC_NUMBER_ZIP, MAGIC_NUMBER)) {
			final PushbackInputStream pis = new PushbackInputStream(is, 4);
			pis.unread(MAGIC_NUMBER, 0, MAGIC_NUMBER_MAX_LENGTH);

			final MessageDigest subDigest = MessageDigest.getInstance("MD5");

			final ZipInputStream zip = new ZipInputStream(digest == null ? pis
					: new DigestInputStream(pis, digest));
			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				try {
					visitStream(zip, zipEntry.getName(), subDigest);
				} catch (final Exception e) {
					LOGGER.log(Level.WARNING, "Zip entry problems: ", e);
				}
			}
			LOGGER.warning("  Zip: " + name
					+ IOUtils.toHexString(digest.digest()));
			return;
		}
		// System.out.println("  Unknown!");
		return;
	}

}