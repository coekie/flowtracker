package com.coekie.flowtracker.tracker;

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

import java.util.Arrays;

/**
 * Represents a method call, that is a particular invocation at runtime, to both the caller and
 * callee.
 * Facilitates tracking of primitive values through calls, for return values and parameters.
 * The `Invocation` is stored in a `ThreadLocal` just before a method invocation, and retrieved at
 * the start of the implementation of the method.
 */
public class Invocation {
  private final String signature;

 /** Source of the returned primitive value */
  public TrackerPoint returnPoint;

  /** Tracks source for some primitive values in arguments. null for untracked arguments. */
  private TrackerPoint[] args;

  Invocation(String signature) {
    this.signature = signature;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg0(TrackerPoint trackerPoint) {
    return setArg(0, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg1(TrackerPoint trackerPoint) {
    return setArg(1, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg2(TrackerPoint trackerPoint) {
    return setArg(2, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg3(TrackerPoint trackerPoint) {
    return setArg(3, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg4(TrackerPoint trackerPoint) {
    return setArg(4, trackerPoint);
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  public Invocation setArg5(TrackerPoint trackerPoint) {
    return setArg(5, trackerPoint);
  }

  public Invocation setArg(int argNum, TrackerPoint trackerPoint) {
    if (trackerPoint == null) {
      return this;
    }

    if (args == null) {
      args = new TrackerPoint[argNum + 1];
    } else if (args.length < argNum + 1) {
      // alternative: take the number of arguments as constructor parameter
      args = Arrays.copyOf(args, Math.max(argNum + 1, args.length * 2));
    }
    args[argNum] = trackerPoint;

    return this;
  }

  /**
   * Called by a caller just before calling another method through which we want to track return or
   * parameter values.
   */
  public Invocation calling(Context context) {
    context.pendingInvocation = this;
    return this;
  }

  /**
   * Sets the source tracker of a returned value
   */
  public static void returning(Invocation invocation, TrackerPoint returnPoint) {
    if (invocation != null) {
      invocation.returnPoint = returnPoint;
    }
  }

  // invoked by instrumentation
  public static TrackerPoint getArgPoint(Invocation invocation, int argNum) {
    if (invocation == null) {
      return null;
    }
    TrackerPoint[] args = invocation.args;
    return args != null && args.length > argNum ? args[argNum] : null;
  }

  /**
   * Creates an {@link Invocation}. Can optionally be followed by calls to
   * {@link #setArg0(TrackerPoint)} and friends, and should be followed by {@link #calling(Context)}
   * before doing the actual call.
   */
  public static Invocation create(String signature) {
    return new Invocation(signature);
  }

  /**
   * Called inside the called method
   */
  public static Invocation start(Context context, String signature) {
    Invocation invocation = context.pendingInvocation;
    if (invocation != null) {
      context.pendingInvocation = null;
      // compare signatures to avoid getting different invocations mixed up.
      // e.g. there may be an instrumented caller A calling a non-instrumented method B,
      // that then calls another instrumented method C.
      // without this check we might incorrectly interpret that as A calling C.
      // (That could still happen if signatures match by coincidence, but the chance is much lower)
      if (signature.equals(invocation.signature)) {
        return invocation;
      }
    }
    return null;
  }

  /**
   * Like {@link #start(Context, String)}, but doesn't clear the invocation. Can be used to get the
   * invocation in a hook without breaking the real invocation instrumentation.
   */
  @SuppressWarnings("unused") // used in HookSpec.INVOCATION
  public static Invocation preStart(String signature) {
    Invocation invocation = context().pendingInvocation;
    if (invocation != null) {
      if (signature.equals(invocation.signature)) {
        return invocation;
      }
    }
    return null;
  }

  public static String signature(String name, String desc) {
    return name + " " + desc;
  }

  /**
   * Removes the current pending Invocation from the thread-local, to be restored later with
   * {@link #unsuspend(Invocation)}.
   * <p>
   * This is used to solve a problem caused by class loading and initialization triggered by a
   * method invocation.
   * The problem is that the loading and initialization can happen between when the caller calls
   * {@link #calling(Context)} and when the callee calls {@link #start(Context, String)}. And it may
   * involve other method calls that use Invocation.
   * For example, calling `SomeClass.foo()` may not immediately call `foo`, but first calls methods
   * in the ClassLoader, and transformers like flowtracker, and `SomeClass.&lt;clinit&gt;`.
   * Since we only keep track of one pending call, this means class loading would make us forget
   * about the pending call.
   * We solve that by, when class loading is triggered, removing the current pending call, and
   * restoring it when class loading has finished.
   */
  public static Invocation suspend() {
    Context context = context();
    Invocation invocation = context.pendingInvocation;
    context.pendingInvocation = null;
    return invocation;
  }

  /** @see #suspend() */
  public static void unsuspend(Invocation invocation) {
    context().pendingInvocation = invocation;
  }

  public static Invocation peekPending() {
    return context().pendingInvocation;
  }
}
