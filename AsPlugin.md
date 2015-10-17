# Steps #

  * org.decojer.editor\_1.0.0.201301021546.jar into dropins

  * options.txt:
```
org.eclipse.equinox.p2.core/debug=true
org.eclipse.equinox.p2.core/reconciler=true
```

  * 
```
eclipse -clean -consoleLog -debug options.txt
```