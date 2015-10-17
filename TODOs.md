http://worker256.decojer.appspot.com/admin/home.jsp

```

DecTestExceptions.simpleFinally()

  Catch-Follow missing
  finally - throw r1 too much


			// TODO we have another important potential follow, which is the first unhandled
			// post-member!!! important for finally and not-returning catches! gather info here


```

# TODOs #

  * backpropagate real array type instead of "null"-REF to PC 5 for array read in 278
    * com.ibm.icu.util.VTimeZone.parseRRULE
  * speed up enclosing method analysis
    * find NEWs on the fly during reading?
    * improve finding circles with declaration owner (ClassT, enclosingMethod finder and reading), more then 1 level
  * paramAss can contain null in inner array, why?
  * Automatically add casting for incompatible (narrowing casts):
    * bool -> int etc.
    * oracle.jdbc.proxy.oracle$1jdbc$1replay$1driver$1NonTxnReplayableArray$2java$1sql$1Array$$$Proxy.setDelegate():
> // works: Comparable c = (Comparable) new Object();
> weblogic.cluster.singleton._RemoteLeasingBasisImpl\_Stub.renewLeases():
> // works: Serializable s = (Serializable) new HashSet();
    * cannot assign '[R10](https://code.google.com/p/decojer/source/detail?r=10).MO229: byte' to 'char':
      * java.lang.RuntimeException: File: C:\Users\andre\.m2\repository\io\undertow\undertow-core\1.0.0.Beta17\undertow-core-1.0.0.Beta17.jar
      * io.undertow.client.http.HttpResponseParser$$generated.handleHttpVersion(Ljava/nio/ByteBuffer;Lio/undertow/client/http/ResponseParseState;Lio/undertow/client/http/HttpResponseBuilder;)V (ops: 581, regs:
  * endless loop in createSyncStruct() if struct.memberAdd woudln't stopp now
    * activate assert in Struct-add
    * check com.mysql.jdbc.PreparedStatement.executePreparedBatchAsMultiStatement in D:\Data\Decomp\workspace\DecoJerTest\test\_bytecode\_closed\cae-live-httpcache-webapp-16.war
  * variable analysis
    * special recognition of this and params for invalid debug vars
    * handling of wrong debugV currently only in DataFlowAnalysis: STORE for spring org.springframework.aop.framework.Cglib2AopProxy.getCallbacks()
  * remove assert field ".$assertionsDisabled" and static init
  * assign-to with "maybe"
  * cascading DUs like class loaders, check package/folders, root can have META-INF
    * jaxb-xjc-2.0.5.jar contains multiple versions of the same code
    * are conflicting, e.g. CodeWraiter interface / abstract class in newer
    * currently excluded from TestDU.read()
  * what to do with wrong type signatures? happens in valid byte code!
    * multiple bugs in scala lib signatures, wrong handling of type parameter :super:interface
    * java.lang.RuntimeException: File: D:\Oracle\JDeveloper\jdeveloper\webcenter\modules\oracle.wcps.sal-tools\_1.0.0.0\scala-library-2.9.1.jar_

  * Infinity Loop in Eclipse-CodeFormatter: https://bugs.eclipse.org/bugs/show_bug.cgi?id=432593
  * optimize stream reader -> use more byte arrays down to bytecode libs
    * check for this org.eclipse.m2e.maven.runtime\_1.4.0.20130531-2315.jar
    * copy zip entries because JDK 7 has nested ZIP bugs: only gets 3 bytes (CAFEBA..) instead of 4 (e.g. CharStreams$2 in com.google.common.io.CharStreams.newReaderSupplier)
  * inner classes synthetic initializer for eclosing.this, comment in PUT indicates, data flowe analysis must be solved first:
    * we want to rewrite this$0 here, but for this constructor methods have to be handled first to mark such fields for consumption?
    * fieldInit is false for synthetic this$0 initializer in constructor, also not sufficient check for conditionals, alternative via data flow analysis?!
  * java.lang.IllegalArgumentException: Invalid string literal : >"\0\0\0,\0X\0?\0°\0Ü\0C\0J\0Š\0?\0?\0G\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?\0?"<
> at org.eclipse.jdt.core.dom.StringLiteral.setEscapedValue(StringLiteral.java:187)
> at org.eclipse.jdt.core.dom.StringLiteral.setLiteralValue(StringLiteral.java:327)
> at org.decojer.cavaj.utils.Expressions.newLiteral2(Expressions.java:295)

  * JDK 8
    * Eclipse AST for JDK 8: http://www.oracle.com/technetwork/articles/java/lambda-1984522.html
      * JDK 8 Update from: http://download.eclipse.org/eclipse/updates/4.3-P-builds/
        * http://wiki.eclipse.org/JDT_Core/Java8
        * http://wiki.eclipse.org/JDT/Eclipse_Java_8_Support_%28BETA%29
      * ASTView not working for new stuff
    * JLS8 API
      * changes for Type Annotations:
        * AnnotatableType & NameQualifiedTypeName
        * ArrayTypes are not recursive anymore, have dimension attribute and elementType instead of componentType
        * method: thrownExceptionTypes instead of thrownExceptions (names)
    * Method References, Default & Static Interface Methods, Lambdas, Repeatable Annotations OK
    * Type Annotations OK, but:
      * Receiver must get fully parameterized reference
      * check more examples: http://www.mscharhag.com/2014/02/java-8-type-annotations.html
      * Eclipse Bugs: https://bugs.eclipse.org/bugs/show_bug.cgi?id=426616
      * JDK Bug: Issue here: https://bugs.openjdk.java.net/issues/?jql=&startIndex=50
      * Eclipse and JDK Bug: http://mail.openjdk.java.net/pipermail/type-annotations-dev/2014-February/001590.html
      * Examples:
        * http://www.tutego.de/blog/javainsel/2012/02/erster-draft-fr-annotations-on-java-types-jsr-308/
      * http://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
    * Inner classes buggy in Eclipse: https://bugs.eclipse.org/bugs/show_bug.cgi?id=426854
  * other IDEs
    * IntelliJ Plugin
      * http://bjorn.tipling.com/how-to-make-an-intellij-idea-plugin-in-30-minutes
    * Netbeans Plugin

# NOTICES #
Eclipse Editor
  * open JAR: Tree View
  * open JAR: Bytecode View
  * open File@JAR: Switchable to Tree View
  * Save Source

Dalvik
  * initial temporary 0 and 1 are typeless constants (no bool backpropagation)...currently i havn't a good solution for this in the generic code


Web

05.01.12

Blob Stats:

Double Hashes: ffaf5932fcd667088159617ce2072d38, 371ce66ec6edf334cd03a5c36f9f1958, ce83ef281361c29d7826c782cb475ee6, ffaf5932fcd667088159617ce2072d38, eda20137ad51c89953c56027fa1381bd, 5f079ab1450158d894d3dc332aa846f6, 54bbc08b6d3ce2f0ef4243f33c60940f

Number: 126619

Size: 38335640414

```
/_ah/queue/decoJer?uploadKey=a74%40_)7FCDy%4018%7DI(AqN%3EW%40A&channelKey=ggpy0c3MqGZ3QkpLo6pcHQ
=>

SELECT * FROM UPLOAD where __key__= KEY('UPLOAD','a74@_)7FCDy@18}I(AqN>W@A')
```

Download big Blob file:
```
http://www.decojer.org/download/test.tst?u=AMIfv973hPWhetoX_Et1Gn8__ATu5hRHkKaWRnKDrXZfkeR1QSe13v84Sp05AOTtMsS1xeNtquTqv24jl21VJ_l1C7hOKvkT0ILOHS1SXdx3D1nf8L69rSPa1e64euvPAQbgzw4MtGOV9ehmTnrFOW1pe1aNpB8kxQ
```


TODOs

md.setParamName besser als START\_LOCAL mit pc 0?

check:
> // Compiled from TimelinePropertyBuilder.java (version 1.6 : 50.0, super bit)
// Signature: Lorg/pushingpixels/trident/TimelinePropertyBuilder<TT;>.AbstractFieldInfo<Ljava/lang/Object;>;
private class org.pushingpixels.trident.TimelinePropertyBuilder$GenericFieldInfo extends org.pushingpixels.trident.TimelinePropertyBuilder$AbstractFieldInfo {


Aug 19, 2012 10:52:00 PM org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts transform
WARNING: Stack underflow in 'org.eclipse.jdt.internal.formatter.comment.HTMLEntity2JavaReader.entity2Text(Ljava/lang/String;)Ljava/lang/String; (ops: 50, regs: 3)':
BB 2 (l 79, pc 30)
Ops: [RETURN](RETURN.md)
Aug 19, 2012 10:52:00 PM org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts rewriteHandler
WARNING: First operation in handler isn't STORE: BB 1 (l 80, pc 31)
Ops: [POP, GOTO 42]
Aug 19, 2012 10:52:00 PM org.decojer.cavaj.transformers.TrCfg2JavaExpressionStmts transform
WARNING: Stack underflow in 'org.eclipse.jdt.internal.formatter.comment.HTMLEntity2JavaReader.entity2Text(Ljava/lang/String;)Ljava/lang/String; (ops: 50, regs: 3)':
BB 1 (l 80, pc 31)
Ops: [POP, GOTO 42]


Bug with split for same-BB-backlinks:

com.itextpdf.text.pdf.PRTokeniser.nextToken()Z (ops: 466, regs: 6)

BB 84 (l 489, pc 440)
Stmts: [outBuf.append((char)ch);
, ch=this.file.read();
, if (!com.itextpdf.text.pdf.PRTokeniser.delims[+ "1"](ch.md)) {
}
]

  * JSR adress -> stack-adress/STORE/RET register-address - better STORE handling for handler
  * exception-ends sometimes earlier (RETURN, PUSH/RETURN)...stack underflow...pull incoming nodes?
  * switch case reordering doesn't work anymore...should mark list unmodifiable? or better rely on E.isSwitch...
  * must more agressively inline or i cannot recognize while() loops with expressions like (--i > 2)
  * 
    * load, iinc => i++
    * iinc, load => --i
    * load...dup / store => (i += 10)

  * Maven Model 3.0.4 API direkt nutzen oder Maven Aether, welches auch Dependencies und Parents etc. auflöst
  * must store register type independantly from register read types, later merge could lower type in hierarchy, merge & store goes top
  * read types: store most specific, read goes bottom, forget nothing

Facts

  * exceptions have changed a lot over versions, especially finally:

  * (GOTO -> JSR)...1.3 (first JSR then GOTO) -> 1.4 (GOTOs not in any-catch - splitted) -> 1.5 (no JSR, inlined finally-code copies)...
  * since 1.5 any-catch (JDK, not Eclipse-compiler) has a strange self-catch, last remark:
http://stackoverflow.com/questions/6386917/strange-exception-table-entry-produced-by-suns-javac
  * Eclipse compiler has yet another form
  * JSR-elimination-cost: http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.104.7844
  * Exceptions-Ends sometimes earlier if operation cannot generate exception. e.g. RETURN or PUSH -> RETURN

  * jdk1.6.6 bytecode: all exception-handlers special ops like STORE, JSR, GOTO have the try-line as line number
  * 1.6.0\_32 and 1.7.0\_04 changed something at inner classes bytecode
  * Glossar:

  * Catch -> Handler
  * Switch -> Case
  * Cond -> Branch


Web

  * Upload JAR: Create / check Maven dependencies
  * Upload POM: Check / Show Dependencies
  * Abonnement: POM-Update-Notifications

Blob: Save Version Diffs for space saving if > 30% down

1 GB => 10 cent / month
100 GB are 10 Euro / month

Obfuscator

  * http://www.dzone.com/links/r/java_obfuscate_proguard_maven_configuration_skele.html