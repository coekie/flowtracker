package com.coekie.flowtracker.agent;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.CoreInitializer;
import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.util.Config;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.jar.JarFile;

public class FlowTrackAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      JarFile agentJar = getAgentJar();
      // Put flowtracker-core on the bootstrap classpath, to make instrumented JDK classes find the
      // hook classes.
      inst.appendToBootstrapClassLoaderSearch(agentJar);
      new Phase2().premain2(agentArgs, inst, agentJar);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /** Returns the jar that this class is running in */
  static JarFile getAgentJar() throws IOException {
    URL url = Thread.currentThread().getContextClassLoader().getResource("flowtracker-spider");
    if (url == null) {
      throw new IllegalStateException("Failed to find our own jar");
    }
    String path = url.getPath();
    if (!"jar".equals(url.getProtocol()) || !path.startsWith("file:/")) {
      throw new IllegalStateException("FlowTracker not launched from jar file: " + url);
    }
    return new JarFile(path.substring("file:".length(), path.indexOf("!")));
  }

  /**
   * Second phase of starting of the agent. This is in a separate class, so that FlowTrackAgent
   * itself doesn't have references to classes from flowtracker-core that need to be put on the
   * bootstrap classpath _before_ they get loaded.
   */
  public static class Phase2 {
    public void premain2(String agentArgs, Instrumentation inst, JarFile agentJar)
        throws Exception {
      Config config = Config.initialize(agentArgs);
      ClassLoader spiderClassLoader = createSpiderClassLoader(inst, agentJar, config);

      CoreInitializer.preInitialize(config);

      // do not track our own initialization
      Context context = context();
      context.suspend();

      spiderClassLoader
          .loadClass("com.coekie.flowtracker.weaver.WeaverInitializer")
          .getMethod("initialize", Instrumentation.class, Config.class)
          .invoke(null, inst, config);

      CoreInitializer.initialize(config, agentJar);

      // initialization done, unsuspend tracking
      context.unsuspend();

      initWeb(spiderClassLoader, config);
      CoreInitializer.postInitialize(config);
    }

    /**
     * Initialize the spiderClassLoader used to load the weaver and the web interface.
     * Those are loaded with their dependencies in a separate class loader to avoid polluting the
     * classpath of the application.
     */
    public ClassLoader createSpiderClassLoader(Instrumentation inst, JarFile agentJar,
        Config config) throws IOException {
      return new SpiderClassLoader(agentJar, config);
    }

    // NICE: generic plugin system would be cleaner
    private static void initWeb(ClassLoader classLoader, Config config) throws Exception {
      // don't load web module when "webmodule" is turned off, e.g. for unit tests
      if (!config.getBoolean("webmodule", true)) {
        return;
      }

      Class<?> clazz = classLoader.loadClass("com.coekie.flowtracker.web.WebModule");
      Constructor<?> constructor = clazz.getConstructor(Config.class);
      constructor.newInstance(config);
    }
  }
}
