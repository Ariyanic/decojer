# Introduction #

Description of BlobStore functionality.

# Details #

## Upload ##

Multiple simultaneous uploads possible:
```
	<form action="<%=blobstoreService.createUploadUrl("/upload")%>"
		method="post" enctype="multipart/form-data">
		<input type="file" name="file1">
		<input type="file" name="file2">
		<input type="file" name="file3">
		<input type="submit" value="Submit">
	</form>
```

Servlet for _/upload_:
```
	public void doPost(final HttpServletRequest req,...)
		final Map<String, BlobKey> blobs = this.blobstoreService.getUploadedBlobs(req);
		...
		// no out allowed, only 1 final redirect!
		res.sendRedirect(...);
	}
```

## CPU Time and Logs for Upload ##

Creation Date (GMT US) is time on successful request end, before start of Upload Servlet:
```
	Content Type 		Size 		Creation Date
	application/x-msi 	38.9 MBytes 	2011-07-07 23:09:52 
```
From home for 39 MB (slow upload speed) upload time: 14 minutes (no 30 second rule, no size problems for 40 MB, started at 07:55:00).

Log for Upload Servlet shows 200 ms billable CPU time for 39 MB write! Okay ... buyed:
```
2011-07-08 08:09:54.691 /upload 302 784ms 209cpu_ms 8api_cpu_ms 0kb Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0,gzip(gfe),gzip(gfe),gzip(gfe)

0.1.0.30 - - [07/Jul/2011:23:09:54 -0700] "POST /upload HTTP/1.1" 302 44 "http://www.decojer.org/" "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0,gzip(gfe),gzip(gfe),gzip(gfe)" "www.decojer.org" ms=784 cpu_ms=209 api_cpu_ms=8 cpm_usd=0.091920
```

From company (fast upload speed) upload time: 38 seconds.

No blobs or log entries for interrupted uploads.

## Blob Info ##

Metadata like Filename via BlobInfo requestable:
http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/blobstore/BlobInfo.html

But better to request connected datastore entity `__BlobInfo__`:

```
Class: com.google.appengine.api.datastore.Entity
AppId: s~decojer
Kind: __BlobInfo__
Namespace: 
Key: __BlobInfo__("AMIfv95gf-PWTSzU4kVzZCFm39hth1owEoAvsJtsAGnGtoVZdO8uGxAZfNujO8i2q6A0cSh_HQEpf6t6u24emXEMxbCMRHtQtCWgIDZ4DA4L6_TLSxL4Ag8VX6fBNx0-6ladI9SoVllEfJWALcBp9i7x97V9TYP0GA")
Parent: null
Props: {filename=test.jar, creation=Tue Jul 05 21:49:28 UTC 2011, md5_hash=b670791e84d4d61d39177989ad851d9c, content_type=application/octet-stream, size=13424}
```

Property md5\_hash already created through uploader (currently not local)! Good for finding duplicates!

```
	// get associated blob info entity __BlobInfo__ from BlobKey
	Entity blobInfoEntity = datastoreService.get(KeyFactory.createKey("__BlobInfo__", blobKey.getKeyString()))

	// get blob from associated blob info entity
	final BlobKey blobKey = new BlobKey(blobInfoEntity.getKey().getName());
	// e.g. read stream or serve()
	BlobstoreInputStream blobstoreInputStream = new BlobstoreInputStream(blobKey);
```

On production it's not possible to put (update) blob info entities, hence no own properties!