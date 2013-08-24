package be.coekaerts.wouter.flowtracker.agent;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class FlowTrackAgent {
	private static final Map<String, String> config = new HashMap<String, String>();

	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("FlowTrackAgent.premain");

		try {
			initProperties(agentArgs);

      // make the instrumented JDK classes find the hook class
			inst.appendToBootstrapClassLoaderSearch(new JarFile(getConfig("core")));

      // do not track our own initialization
      Class.forName("be.coekaerts.wouter.flowtracker.tracker.Trackers")
          .getMethod("suspendOnCurrentThread").invoke(null);

      ClassLoader classLoader = createSpiderClassLoader();
			inst.addTransformer(createTransformer(classLoader), true);

			// TODO avoid hardcoding of list of classes to retransform here
			inst.retransformClasses(String.class);
			inst.retransformClasses(InputStreamReader.class);
			inst.retransformClasses(Arrays.class);
			inst.retransformClasses(StringBuilder.class.getSuperclass()); // AbstractStringBuilder is not public

      initWeb(classLoader);

      // initialization done, unsuspend tracking
      Class.forName("be.coekaerts.wouter.flowtracker.tracker.Trackers")
          .getMethod("unsuspendOnCurrentThread").invoke(null);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
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

  /**
   * Creates the classloader used to load the weaver and the web interface.
   * Those are loaded with their dependencies in a separate class loader to avoid polluting the
   * classpath of the application.
   */
  private static ClassLoader createSpiderClassLoader() throws Exception {
    List<URL> urls = new ArrayList<URL>();
    for (String path : getConfig("spiderClasspath").split(",")) {
      urls.add(new File(path).toURI().toURL());
    }
    return new URLClassLoader(urls.toArray(new URL[urls.size()]));
  }

	private static void initProperties(String agentArgs) {
		if (agentArgs == null) {
			throw new RuntimeException("You must provide arguments to the agent");
		}
		for (String arg : agentArgs.split(";")) {
			String[] keyAndValue = arg.split("=", 2);
			config.put(keyAndValue[0], keyAndValue[1]);
		}
	}

	private static String getConfig(String name) {
		// TODO default config
		String result = config.get(name);
		if (result == null) {
			throw new RuntimeException("You must provide '" + name + "' as argument to the agent");
		}
		return result;
	}
}
