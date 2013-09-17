package be.coekaerts.wouter.flowtracker.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import sun.net.www.protocol.file.FileURLConnection;

public class FlowTrackAgent {
	/** Configuration passed to the agent */
  private static final Map<String, String> config = new HashMap<String, String>();

	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("FlowTrackAgent.premain");

		try {
			initProperties(agentArgs);

      /**
       * Classloader used to load the weaver and the web interface.
       * Those are loaded with their dependencies in a separate class loader to avoid polluting the
       * classpath of the application.
       */
      ClassLoader spiderClassLoader = initClassLoaders(inst);

      // do not track our own initialization
      Class.forName("be.coekaerts.wouter.flowtracker.tracker.Trackers")
          .getMethod("suspendOnCurrentThread").invoke(null);

			inst.addTransformer(createTransformer(spiderClassLoader), true);

			// TODO avoid hardcoding of list of classes to retransform here
			inst.retransformClasses(String.class);
			inst.retransformClasses(InputStreamReader.class);
			inst.retransformClasses(OutputStreamWriter.class);
      inst.retransformClasses(URLConnection.class);
      inst.retransformClasses(FileURLConnection.class);
			inst.retransformClasses(Arrays.class);
      // AbstractStringBuilder is not public
			inst.retransformClasses(StringBuilder.class.getSuperclass());

      initShutdownHook();

      initWeb(spiderClassLoader);

      // initialization done, unsuspend tracking
      Class.forName("be.coekaerts.wouter.flowtracker.tracker.Trackers")
          .getMethod("unsuspendOnCurrentThread").invoke(null);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

  /**
   * Update the bootstrap classpath and initialize the spiderClassLoader used to load the weaver
   * and the web interface.
   * Those are loaded with their dependencies in a separate class loader to avoid polluting the
   * classpath of the application.
   */
  private static ClassLoader initClassLoaders(Instrumentation inst) throws IOException {
    // flowtracker-core jar, to be added to the bootstrap classpath
    JarFile coreJar = null;
    // all other flowtracker and dependencies jars
    List<URL> spiderClasspath = new ArrayList<URL>();

    if (! config.containsKey("core")) { // paths not explicitly configured
      // assume we're running from flowtracker-all jar with nested jars in it
      File expandDir = createExpandDir();
      JarFile jar = getThisJar();
      for (JarEntry entry : Collections.list(jar.entries())) {
        if (entry.getName().startsWith("flowtracker/") && entry.getName().endsWith(".jar")) {
          String subJarName = entry.getName().substring("flowtracker/".length());
          File outFile = new File(expandDir, subJarName);
          copy(jar.getInputStream(entry), outFile);
          if (subJarName.contains("flowtracker-core")) {
            coreJar = new JarFile(outFile);
          } else {
            spiderClasspath.add(outFile.toURI().toURL());
          }
        }
      }
      if (coreJar == null) {
        throw new IllegalStateException("Could not find flowtracker-core jar in "
            + jar.getName());
      }
    } else {
      coreJar = new JarFile(getConfig("core"));
      for (String path : getConfig("spiderClasspath").split(",")) {
        spiderClasspath.add(new File(path).toURI().toURL());
      }
    }

    // make the instrumented JDK classes find the hook class
    inst.appendToBootstrapClassLoaderSearch(coreJar);

    return new URLClassLoader(spiderClasspath.toArray(new URL[spiderClasspath.size()]));
  }

	private static ClassFileTransformer createTransformer(ClassLoader classLoader) throws Exception {
		Class<?> transformerClass = classLoader
        .loadClass("be.coekaerts.wouter.flowtracker.weaver.AsmTransformer");
		return (ClassFileTransformer) transformerClass.newInstance();
	}

  // NICE: generic plugin system would be cleaner
  private static void initWeb(ClassLoader classLoader) throws Exception {
    Class<?> clazz;
    try {
      clazz = classLoader.loadClass("be.coekaerts.wouter.flowtracker.web.WebModule");
    } catch (ClassNotFoundException e) {
      return; // ok, module not included
    }
    clazz.newInstance();
  }

	private static void initProperties(String agentArgs) {
		if (agentArgs != null) {
      for (String arg : agentArgs.split(";")) {
        String[] keyAndValue = arg.split("=", 2);
        config.put(keyAndValue[0], keyAndValue[1]);
      }
    }
	}

	private static String getConfig(String name) {
		String result = config.get(name);
		if (result == null) {
			throw new RuntimeException("You must provide '" + name + "' as argument to the agent");
		}
		return result;
	}

  /** Returns the jar that this class is running in */
  private static JarFile getThisJar() throws IOException {
    URL url = FlowTrackAgent.class.getResource("FlowTrackAgent.class");
    String path = url.getPath();
    if (!"jar".equals(url.getProtocol()) || !path.startsWith("file:/")) {
      throw new IllegalStateException("Flowtracker not launched from jar file: " + url);
    }
    return new JarFile(path.substring("file:".length(), path.indexOf("!")));
  }

  /** Create temporary directory that we expand ourselves into */
  private static File createExpandDir() {
    Random rnd = new Random();
    for (int counter = 0; counter < 1000; counter++) {
      File tempDir = new File(System.getProperty("java.io.tmpdir"), "flowtracker-" + Math.abs(
          rnd.nextInt()));
      if (tempDir.mkdir()) {
        tempDir.deleteOnExit();
        return tempDir;
      }
    }
    throw new RuntimeException("Could not create temporary directory");
  }

  private static void copy(InputStream in, File outFile) throws IOException {
    try {
      FileOutputStream out = new FileOutputStream(outFile);
      try {
        byte[] buf = new byte[1024];
        while (true) {
          int r = in.read(buf);
          if (r == -1) {
            break;
          }
          out.write(buf, 0, r);
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  private static void initShutdownHook() throws Exception {
    Class.forName("be.coekaerts.wouter.flowtracker.util.ShutdownSuspender")
        .getMethod("initShutdownHook", boolean.class)
        .invoke(null, "true".equals(config.get("suspendShutdown")));
  }
}
