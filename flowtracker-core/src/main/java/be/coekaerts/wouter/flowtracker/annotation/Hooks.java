package be.coekaerts.wouter.flowtracker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Repeated annotation holder for {@link Hook} */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hooks {
  Hook[] value();
}
