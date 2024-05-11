package com.coekie.flowtracker.weaver;

import org.objectweb.asm.ClassVisitor;

/** Creates adapters by wrapping a {@link ClassVisitor} */
public interface Transformer {
  /** Wrap the given {@link ClassVisitor} in an adapter. */
  ClassVisitor transform(String className, ClassVisitor cv);

  /** Combine two ClassAdapterFactories */
  static Transformer and(Transformer first, Transformer andThen) {
    if (first == null) return andThen;
    if (andThen == null) return first;
    return (className, cv) -> andThen.transform(className, first.transform(className, cv));
  }
}
