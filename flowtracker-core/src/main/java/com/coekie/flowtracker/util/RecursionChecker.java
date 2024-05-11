package com.coekie.flowtracker.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper to debug some nasty recursions problems caused by our instrumentation.
 * Without help those can be hard to debug, because they can also break the ability to print errors,
 * StackOverflowErrors are created without stacktraces, and we've had a couple heisenbugs where they
 * only happen if there is no debugger attached.
 * <p>
 * This isn't actually added in a lot of places yet; add it in suspected places when needed.
 */
public class RecursionChecker implements AutoCloseable {
  /**
   * Counts how many ongoing calls there are. For simplicity, we don't even care if they're on the
   * same thread or not.
   */
  private static final AtomicInteger counter = new AtomicInteger();

  private static final RecursionChecker instance = new RecursionChecker();

  private static boolean enabled = false;

  public static boolean enabled() {
    return enabled;
  }

  public static void initialize(Config config) {
    enabled = config.getBoolean("debugRecursion", false);
    if (enabled) {
      // make sure everything to do Thread.sleep (which we might do in fail()) is already
      // initialized
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  // use in try-with-resources around suspected code blocks
  public static RecursionChecker check() {
    before();
    return instance;
  }

  @Override
  public void close() {
    after();
  }

  public static void before() {
    if (enabled) {
      int value = counter.incrementAndGet();
      if (value > 20) {
        fail();
      }
    }
  }

  public static void after() {
    if (enabled) {
      counter.decrementAndGet();
    }
  }

  private static void fail() {
    counter.set(-100000); // don't fail again
    try {
      new Error("RecursionChecker!").printStackTrace();
    } catch (Throwable t) {}
    // sleep, so we get the chance to take a stack dump with jstack, or attach a debugger
    try {
      Thread.sleep(1000000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
