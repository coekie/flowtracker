package be.coekaerts.wouter.flowtracker.weaver;

import java.lang.instrument.Instrumentation;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
public class WeaverInitializer {
  public static void initialize(Instrumentation inst, Map<String, String> config) throws Exception {
    AsmTransformer transformer = new AsmTransformer(config);
    inst.addTransformer(transformer, true);

    // retransform classes that have already been loaded
    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
      if (transformer.shouldRetransformOnStartup(loadedClass, inst)) {
        try {
          inst.retransformClasses(loadedClass);
        } catch (Throwable t) {
          System.err.println("Failed to retransform " + loadedClass);
          throw t;
        }
      }
    }
  }
}
