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
For enabling boolean options, the `=value` part can be omitted.
For a complete list of configuration options, see below.

As an example for using it with maven, this command was used to generate the PetClinic demo:
```
./mvnw integration-test -Dtest=PetClinicIntegrationTests -DargLine="-javaagent:$FT_JAR=webserver=false;trackCreation;snapshotOnExit=spring-petclinic-snapshot.zip $FT_JVMOPTS"
```

## Source Code

FlowTracker doesn't require the source code of the application you are observing, but the results will look a bit prettier if the source code is available.
If you're using maven, you can download the sources of your dependencies using `mvn dependency:sources`.
FlowTracker looks for the sources of a dependency `foo.jar` in a file `foo-sources.jar`, which is the convention that maven follows.
If FlowTracker can't find the source code, it will display the code decompiled with the Vineflower decompiler.

## Configuration

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
  There's a recommended base of classes to instrument or not (of the JDK itself) that can (and is highly recommended to) be referred to using %base.
  Mostly useful to exclude classes that break when instrumented. e.g. `%base,-exclude.this.package.*,+*` (Default: `%base,+*`)
* `breakStringInterning`: Instrumented classes where it is ok to break String interning.
  Same syntax as `filter`.
  To track Strings that appear as String literals in the code, FlowTrackers undoes the interning of Strings that the JVM does.
  That can cause some libraries that depend on the interning to break.
  Those can be excluded with this option.
  (Default: `%base,+*`)
* `eager`: Classes where we are more eager to track primitive values.
  Same syntax as `filter`.
  This makes the heuristics for which method arguments and return values to track, track all `int` values.
  If you see that tracking of an interesting part is getting lost in a method call taking or returning an int, adding it here may improve results.
  This comes at the cost of some performance.
  (Default: none)
* `hideInternals`: Disable tracking for some uninteresting internal operations that add noise, such as reading of .class files by ClassLoaders (Default: true)

These options are meant for FlowTracker development/debugging only:
* `webmodule`: Can be used to disable the web module, including support for snapshots. Intended for testing only, use `webserver` instead (Default: true)
* `exitOnError`: Exit the JVM as soon as any FlowTracker error happens (Default: false)
* `debugRecursion`: Helps debug StackOverflowErrors due to recursion accidentally introduced by instrumentation (Default: false)
* `verify`: Verify instrumented bytecode using ASM CheckClassAdapter (Default: false)
* `dumpByteCode`: Dump instrumented class files to this path (Default: none)
* `dumpText`: Dump instrumented classes in text form to this path, including comments for instructions added by FlowTracker (Default: none)
* `dumpTextPrefix`: When `dumpText` is enabled, only dump classes whose name starts with this prefix (Default: none)
* `dynamicFallback`: When a `PointerTracker` for a stored value is null, fall back to pointing to the code location of the store.
   This can be helpful in debugging why a value wasn't tracked.

## Compatibility

FlowTracker should work on Oracle or other OpenJDK based JDKs.
It has been tested on JDK 11, 17, 21 and 22.