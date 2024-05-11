package com.coekie.flowtracker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method will be called when {@link #method()} on {@link #target()} finishes.
 * This is used to generate GeneratedHookSpecs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Hooks.class)
public @interface Hook {
  String target();
  String method();
  String condition() default "";
  HookLocation location() default HookLocation.ON_RETURN;
}
