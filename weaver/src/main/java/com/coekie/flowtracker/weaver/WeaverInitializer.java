package com.coekie.flowtracker.weaver;

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

import com.coekie.flowtracker.hook.ArrayLoadHook;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.util.Config;
import com.coekie.flowtracker.weaver.flow.InvocationArgStore;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Type;

@SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackerAgent
public class WeaverInitializer {
  public static void initialize(Instrumentation inst, Config config) throws Exception {
    // avoid ClassCircularityErrors: Make sure these hook classes are loaded before we start
    // transforming
    ArrayLoadHook.class.getName();
    TrackerRepository.class.getName();

    InvocationArgStore.initialize(config);

    AsmTransformer transformer = new AsmTransformer(config);
    inst.addTransformer(transformer, true);

    // keep track of classes that are getting transformed while we are initializing
    Set<String> transformed = new HashSet<>();
    transformer.transformListener = transformed::add;

    // retransform classes that have already been loaded
    List<Class<?>> toTransform = new ArrayList<>();
    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
      if (transformer.shouldRetransformOnStartup(loadedClass, inst)) {
        toTransform.add(loadedClass);
      }
    }
    doRetransform(inst, toTransform);
    transformer.firstRoundDone = true;

    // second round of retransformations, to retransforms classes that were loaded while handling
    // the first round, but didn't get transformed yet.
    transformer.transformListener = null;
    toTransform.clear();
    for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
      if (!transformed.contains(Type.getInternalName(loadedClass)) &&
          transformer.shouldRetransformOnStartup(loadedClass, inst)) {
        toTransform.add(loadedClass);
      }
    }
    doRetransform(inst, toTransform);
  }

  private static void doRetransform(Instrumentation inst, List<Class<?>> toTransform)
      throws Exception {
    try {
      inst.retransformClasses(toTransform.toArray(new Class<?>[0]));
    } catch (Throwable t) {
      System.err.println("Failed to retransform");
      throw t;
    }

    // alternative, in case transforming all at once fails: transforming one by one.
    // leaving this code in, commented out, for easier debugging when needed.
//    for (Class<?> aClass : toTransform) {
//      try {
//        inst.retransformClasses(new Class<?>[]{aClass});
//      } catch (Throwable t2) {
//        System.err.println("Failed to retransform " + aClass);
//        t2.printStackTrace();
//      }
//    }
  }
}
