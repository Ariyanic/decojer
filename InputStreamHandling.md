# Introduction #

DecoJer.decompile(stream)

# Details #

  * Java Classfiles start with magic number "CA FE BA BE"
  * JAR is like ZIP with special file structure (like META-INF folder)
  * ZIP Files (JAR, EAR, WAR) start with "50 4B 03 04", ZIP Entries begin with this signature, than Zip Entry local informations
  * http://www.onicos.com/staff/iz/formats/zip.html
  * Zip Central Directory on file end, not necessary to read entries
  * Zip embedded in Zip often used (e.g. EAR, WAR)
  * Dalvik: APK is like ZIP with special file structure, contains DEX, quick win: http://code.google.com/p/dex2jar/

  * Java build-in: JarFile / ZipFile and JarInputStream / ZipInputStream
  * Jar-Versions expect META-INF
  * InputStream doesn't read Zip Central Directory, only Zip Entry local informations

  * MD5 on the fly possible with Java build-in: DigestInputStream

## Zip Order ##

Any order of ZIP entries possible, test:
```
			final ZipOutputStream os = new ZipOutputStream(new FileOutputStream(new File("...")));

			os.putNextEntry(new ZipEntry("test/test"));
			os.write("TEST".getBytes());

			os.putNextEntry(new ZipEntry("test2"));
			os.write("TEST2".getBytes());

			os.putNextEntry(new ZipEntry("test/test3"));
			os.write("TEST3".getBytes());

			os.putNextEntry(new ZipEntry("test/")); // is possible...

			os.finish();
```

And read:
```
			final ZipInputStream zip = new ZipInputStream(new FileInputStream(new File("...")));

			System.out.println("Mark Supported: " + zip.markSupported());

			for (ZipEntry zipEntry = zip.getNextEntry(); zipEntry != null; zipEntry = zip
					.getNextEntry()) {
				System.out.println("ZIP Entry: " + zipEntry);
			}
```

results in:
```
Mark Supported: false
ZIP Entry: test/test
ZIP Entry: test2
ZIP Entry: test/test3
ZIP Entry: test/
```

Duplicate names on write ("test3" => "test") result in:
```
java.util.zip.ZipException: duplicate entry: test/test
	at java.util.zip.ZipOutputStream.putNextEntry(ZipOutputStream.java:175)
	at org.decojer.ZipTests.main(ZipTests.java:58)
ZIP Entry: test/test
ZIP Entry: test2
```

## File Check Order ##
  * http://www.java2s.com/Tutorial/Java/0180__File/UnzipusingtheZipInputStream.htm
  * static long LOCSIG = 0x04034b50L;	// "PK\003\004"

  * markSupported()? mark() => check ZIP magic
    * YES => open JarFile via Google FileService http://code.google.com/intl/de-DE/appengine/docs/java/javadoc/index.html?com/google/appengine/api/blobstore/package-summary.html
    * NO => reset() and new TD(is) => new javassist.bytecode.ClassFile() => OK or throws IOException("bad magic number...)