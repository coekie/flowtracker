package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.ClassVisitor;

/** Creates adapters by wrapping a {@link ClassVisitor} */
public interface ClassAdapterFactory {
  /** Wrap the given {@link ClassVisitor} in an adapter. */
  ClassVisitor createClassAdapter(ClassVisitor cv);

  /** Combine two ClassAdapterFactories */
  static ClassAdapterFactory and(ClassAdapterFactory first, ClassAdapterFactory andThen) {
    if (first == null) return andThen;
    if (andThen == null) return first;
    return cv -> andThen.createClassAdapter(first.createClassAdapter(cv));
  }
}
