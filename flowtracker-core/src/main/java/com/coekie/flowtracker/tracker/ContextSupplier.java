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

import com.coekie.flowtracker.hook.Reflection;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Manages the storing of {@link Context} in the current thread.
 * This has multiple implementations, with the simplest being {@link WithThreadLocal} which stores
 * it in a {@link ThreadLocal}. The other implementations are insane ugly hacks that hijack fields
 * on the {@link Thread} class, as a form of optimized ThreadLocal, avoiding the cost of lookups
 * in the thread local map. That's because flowtracker accesses the context _very often_. It's still
 * questionable if that's worth it.
 */
abstract class ContextSupplier {
  abstract Context get();

  static ContextSupplier initSupplier() {
    if (Context.class.getClassLoader() != null) {
      // not running as an agent
      return new WithThreadLocal();
    } else if (threadHasTargetField()) {
      return new WithThreadTarget();
    } else {
      return new WithThreadInterruptLock();
    }
  }

  private static boolean threadHasTargetField() {
    try {
      Thread.class.getDeclaredField("target");
      return true;
    } catch (NoSuchFieldException e) {
      return false;
    }
  }

  private static class WithThreadLocal extends ContextSupplier {
    private static final ThreadLocal<Context> threadLocal = ThreadLocal.withInitial(Context::new);

    @Override
    Context get() {
      return threadLocal.get();
    }
  }

  /**
   * Store the Context in Thread.target. Since the thread is already running, that field can
   * more-or-less safely be changed.
   * This field was removed (replaced by Thread.FieldHolder.task) in later JDK versions.
   */
  private static class WithThreadTarget extends ContextSupplier {
    private static final VarHandle threadTargetHandle =
        Reflection.varHandle(Thread.class, "target", Runnable.class);

    @Override
    Context get() {
      Thread thread = Thread.currentThread();
      Runnable runnable = (Runnable) threadTargetHandle.get(thread);
      if (runnable instanceof Context) {
        return (Context) runnable;
      } else {
        Context context = new Context();
        threadTargetHandle.set(thread, (Runnable) context);
        return context;
      }
    }
  }

  /** Store the Context in Thread.interruptLock */
  private static class WithThreadInterruptLock extends ContextSupplier {
    private static final VarHandle threadInterruptLockHandle =
        Reflection.varHandle(Thread.class, "interruptLock", Object.class);
    private static final MethodHandle setThreadInterruptLock =
        Reflection.setter(Thread.class, "interruptLock", Object.class);

    @Override
    Context get() {
      Thread thread = Thread.currentThread();
      Object lock = threadInterruptLockHandle.get(thread);
      if (lock instanceof Context) {
        return (Context) lock;
      } else {
        Context context = new Context();
        try {
          setThreadInterruptLock.invokeExact(thread, (Object) context);
        } catch (Throwable t) {
          throw new Error(t);
        }
        return context;
      }
    }
  }
}
