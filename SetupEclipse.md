# Introduction #

The project **DecoJer** currently exists of 3 subprojects.
  * DecoJer
  * DecoJerTest
  * DecoJerWeb

These subprojects are separately checked in (using **Subversion**) and preconfigured for the **Eclipse** IDE.

The following details give an overview of the necessary steps to check out the source code and to get the project running in Eclipse.

# Details #

## Eclipse & Plugins ##

  * Prepare Eclipse:
    * download **Eclipse for RCP and RAP Developers** (Version 3.7.2 or 4.4) from http://www.eclipse.org/downloads/
    * extract Eclipse
    * install **Lombok**:
      * download from http://projectlombok.org/download.html
      * start and select Eclipse folder
    * set Eclipse encoding (important for Plugin export) and increase memory settings in settings.xml, should now look like:
```
...
-vmargs
-Dosgi.requiredJavaVersion=1.7
-Xms1g
-Xmx2g
-javaagent:lombok.jar
-Xbootclasspath/a:lombok.jar
```
    * start and _Check for Updates_ -> update if necessary
  * Install New Software...:
    * Install **Subversive**:
      * Work with, choose: Luna - http://download.eclipse.org/releases/luna
        * for Eclipse 3.7.2: Subversion compatible to new 1.7 repository: http://download.eclipse.org/technology/subversive/0.7/update-site/
      * select _Collaboration_ / _Subversive SVN Team Provider_
      * after Installation and Eclipse Restart; open SVN Perspective and then should appear the Subversive Connector Installer: Choose e.g. Native JavaHL 1.7.x for Win64
    * Install **ZEST**
      * Work with, choose: Luna - http://download.eclipse.org/releases/luna
      * select _Modeling_ / _Graphical Editing Framework Zest Visualization Toolkit SDK_
    * Install **AST View**:
      * Work with, add: http://www.eclipse.org/jdt/ui/update-site
      * select _AST View 1.1.9 for Eclipse Luna (4.4) and later_ / _AST View_
    * Install **TestNG**:
      * Work with, add: http://beust.com/eclipse
      * select _TestNG_ / _TestNG_

## Checkout Source Code ##

![http://decojer.googlecode.com/svn/wiki/img/eclipse_svn_repository_location.png](http://decojer.googlecode.com/svn/wiki/img/eclipse_svn_repository_location.png)

  * add Subversion Repository Location https://decojer.googlecode.com/svn/
    * checkout DecoJer, optionally: DecoJerTest, DecoJerWeb

  * because we are heaviliy dependant from Eclipse JDT - download plugin sources from http://download.eclipse.org/eclipse/downloads/drops4/R-4.2.2-201302041200/download.php?dropFile=org.eclipse.jdt.source-4.2.2.zip and copy jdt-stuff into plugins

## Errors ##

  * SWT Errors, e.g. Colors not found: change target platform settings in workspace: http://stackoverflow.com/questions/4498018/eclipse-plugin-dependency-on-swt-classes-not-being-resolved
    * Window / Preferences / Plug-in Development / Target Platform / Running Platform (Active) / Edit... / Environment / Architecture: x86\_64

## Additional Plugins for Dalvik ##

  * Install Android SDK
  * Install Eclipse Android Plugins: http://developer.android.com/sdk/eclipse-adt.html#installing
  * Android/Preferences - add SDK

  * View DecoJerTest/dex/classes.dex as dump:
    * add as project nature: com.android.ide.eclipse.adt.AndroidNature
    * put classes.dex into bin/
    * on project context-menu: Android Tools/Display dex bytecode

## Additional Plugins for DecoJerWeb ##

  * Install Web Development Plugins:
    * Install **Google Plugin for Eclipse**:
      * Add Update Manager: http://dl.google.com/eclipse/plugin/4.2
      * for Eclipse 3.7.2: http://dl.google.com/eclipse/plugin/3.7
      * select items:
        * _Google Plugin for Eclipse (required)_ / _Google Plugin for Eclipse 4.2_
        * _SDK_ / _Google App Engine Java SDK 1.7.x_
      * (see under http://code.google.com/intl/de-DE/eclipse/docs/download.html)
    * Install **Eclipse WTP Plugins** (we use Eclipse for RCP!)
      * choose Update Manager: Indigo - http://download.eclipse.org/releases/indigo
      * select _Web, XML, Java EE and OSGi Enterprise Development_/
      * _Eclipse Java Web Developer Tools_
      * _Eclipse Web Developer Tools_
      * _JavaScript Development Tools_

  * Eclipse and Jasper / JSP / JSTL
    * Eclipse has embedded Jasper which uses Eclipse AST API, rename: eclipse-SDK-3.7.1-win32\plugins\com.google.appengine.eclipse.sdkbundle\_1.5.5.r37v201110112027\appengine-java-sdk-1.5.5\lib\tools\jsp\repackaged-appengine-jasper-jdt-6.0.29.bak
    * GAE has JSTL installed, cannot put it in WEB-INF/lib, but Eclipse cannot find it under above tools folder -> again seperately in lib only to make Eclipse happy now
    * don't use HTML 5 with JSP XML mode (is only better for XHTML)

  * for GAE Deployment, this has to be added to eclipse.ini (must be JDK7!):
```
-vm
C:\Program Files\Java\jdk1.7.0_80\bin\javaw
```