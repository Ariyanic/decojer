# Introduction #

  * Bytecode Viewer: http://www.ej-technologies.com/products/jclasslib/overview.html

DecoJer uses multiple ByteCode Libraries:

> ## JVM ByteCode ##
  * JBoss Javassist
  * ObjectWeb ASM

> ## Dalvik ByteCode ##
  * smali (dexlib)
  * dex2jar (dex-reader)

Blog: http://blog.decojer.org/2011/07/virtual-machine-bytecode-reading-dom.html

# Javassist #

  * Version: 3.15.0-GA
  * Download project as archive from: http://sourceforge.net/projects/jboss/files/Javassist/
  * svn co http://anonsvn.jboss.org/repos/javassist/trunk

# ASM #

  * Version: 4.0
  * Download project as archive from: http://asm.ow2.org/download/index.html
  * svn co svn://svn.forge.objectweb.org/svnroot/asm/trunk

# smali #

  * Sublib: dexlib
  * Version: 1.3.3-dev
  * http://code.google.com/p/smali/
  * git clone https://code.google.com/p/smali/

> ## Checkout with Eclipse ##
  * install m2e, EGit
  * Clone a Git Repository... -> URI: https://code.google.com/p/smali/ -> Directory: D:\Data\Decomp\workspace\smali
  * Git Repositories: "Working directory/dexlib" -> Import Projects... -> Import as general project
  * Navigator: "dexlib" -> Configure -> Convert to Maven Project
  * # dexlib/mvn clean install

> ## Patch for smali ##

  * Can currently only read files. Small dirty patch:

  * in _/prj-smali/dexlib/src/main/java/org/jf/dexlib/DexFile.java_

between
```
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (zipFile != null) {
                zipFile.close();
            }
        }
        ReadContext readContext = new ReadContext();
```
insert
```
        }
        read(in);
    }

    public DexFile(final Input in, boolean preserveSignedRegisters, boolean skipInstructions) {
            this(preserveSignedRegisters, skipInstructions);
            read(in);
    }

    private void read(final Input in) {
        ReadContext readContext = new ReadContext();
```

# dex2jar #

  * Sublib: dexreader
  * Version: 1.9-snapshot
  * http://code.google.com/p/dex2jar/
  * hg clone https://code.google.com/p/dex2jar/

# Eclipse Source #

  * Clone from Git:
    * git://git.eclipse.org/gitroot/jdt/eclipse.jdt.core.git
    * git://git.eclipse.org/gitroot/jdt/eclipse.jdt.ui.git
  * for both: _master_ and _R3\_7\_maintenance_, checkout _R3\_7\_maintenance_
  * Import existing projects:
    * org.eclipse.jdt.core
    * org.eclipse.jdt.ui
  * observe bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=361071