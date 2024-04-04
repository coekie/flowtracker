package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.ArrayLoadHook;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.util.Config;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
public class WeaverInitializer {
  public static void initialize(Instrumentation inst, Config config) throws Exception {
    // avoid ClassCircularityErrors: Make sure these hook classes are loaded before we start
    // transforming
    ArrayLoadHook.class.getName();
    TrackerRepository.class.getName();

    AsmTransformer transformer = new AsmTransformer(config);
    inst.addTransformer(transformer, true);

    // retransform classes that have already been loaded
    List<Class<?>> toTransform = new ArrayList<>();
    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
      if (transformer.shouldRetransformOnStartup(loadedClass, inst)) {
        toTransform.add(loadedClass);
      }
    }
    try {
      inst.retransformClasses(toTransform.toArray(new Class<?>[0]));
    } catch (Throwable t) {
      System.err.println("Failed to retransform");
      throw t;
    }
  }
}
