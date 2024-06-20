FlowTracker - https://github.com/coekie/flowtracker

To use FlowTracker, you must include it on the command line when launching the java program to watch.
FlowTracker requires a long list of JVM options, which you can get from running the jar with "jvmopts":

```
FT_JAR=path/to/flowtracker.jar
FT_JVMOPTS="$(java -jar $FT_JAR jvmopts)"

java -javaagent:$FT_JAR $FT_JVMOPTS
```

By default, FlowTracker starts a webserver on port 8011. Open http://localhost:8011/ in your browser.

FlowTracker can be configured using `-javaagent:$FT_JAR=option1=value1;option2=value2`.
Supported configuration options:
* `port`: Port to run the webserver on (Default: 8011)
* `webserver`: Run the webserver (Default: true)
* `suspendShutdown`: When the application ends, don't let the JVM exit yet. This allows you to look at FlowTracker for short-lived processes. An alternative way to do that is to use `snapshotOnExit` (Default: false)
* `snapshotOnExit`: Path to zip file to write when the JVM exits.
  The zip file will contain a dump of FlowTracker data, and a copy of the UI (html/js).
  The html needs to be loaded from a webserver (not directly from the local filesystem) because of browser security restrictions.
  The easiest way to do that it using java's builtin webserver.
  For example: `java -javaagent:$FT_JAR=snapshotOnExit=snapshot.zip ...; unzip snapshot.zip; cd snapshot; jwebserver`.
  Note that while the app is running you can grab such a snapshot from http://localhost:8011/snapshot/minimized or http://localhost:8011/snapshot/full.
  (Default: none)
* `snapshotOnExitMinimized`: Reduce the size of the snapshot that is produced on exit, by skipping origins that are not referred to from any sinks. That means you'll see fewer entries in the tree, but any interesting ones tracking-wise will still be included (Default: true)
* `trackCreation`: Every time a tracker is created that appears in the tree (most sinks and origins), collect a stacktrace dump. This stacktrace can be seen in the UI by clicking on the small button on the top right. This can be useful to see where in the application some input or output was triggered (Default: false)
* `logging`: Log FlowTracker info messages to stderr. Note that error logging is always enabled. (Default: false)
* `filter`: Specifies which classes to instrument. Comma-separated list of inclusions (starting with +) or exclusions (starting with -).
  There's a recommended base of classes to instrument or not (of the JDK itself) that can (and is highly recommended to) be referred to using %recommended.
  Mostly useful to exclude classes that break when instrumented. e.g. `%recommended,-exclude.this.package.*,+*` (Default: `%recommended,+*`)
* `breakStringInterning`: Similar to `filter`, instrumented classes where it is ok to break String interning.
  To track Strings that appear as String literals in the code, FlowTrackers undoes the interning of Strings that the JVM does.
  That can cause some libraries that depend on the interning to break.
  Those can be excluded with this option.
  (Default: `%recommended,+*`)
* hideInternals: Disable tracking for some uninteresting internal operations that add noise, such as reading of .class files by ClassLoaders (Default: true)

These options are meant for FlowTracker development/debugging only:
* `webmodule`: Can be used to disable the web module, including support for snapshots. Intended for testing only, use `webserver` instead (Default: true)
* `exitOnError`: Exit the JVM as soon as any FlowTracker error happens (Default: false)
* `debugRecursion`: Helps debug StackOverflowErrors due to recursion accidentally introduced by instrumentation (Default: false)
* `verify`: Verify instrumented bytecode using ASM CheckClassAdapter (Default: false)
* `dumpByteCode`: Dump instrumented class files to this path (Default: none)
* `dumpText`: Dump instrumented classes in text form to this path, including comments for instructions added by FlowTracker (Default: none)
* `dumpTextPrefix`: When `dumpText` is enabled, only dump classes whose name starts with this prefix (Default: none)