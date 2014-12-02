package be.coekaerts.wouter.flowtracker.util;

public class ShutdownSuspender {
  private static boolean suspendShutdown;

  public synchronized static boolean isSuspendShutdown() {
    return suspendShutdown;
  }

  public synchronized static void setSuspendShutdown(boolean suspendShutdown) {
    ShutdownSuspender.suspendShutdown = suspendShutdown;
    ShutdownSuspender.class.notifyAll();
  }

  public static void initShutdownHook(boolean initialSuspendShutdown) {
    suspendShutdown = initialSuspendShutdown;
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override public void run() {
        synchronized (ShutdownSuspender.class) {
          while (suspendShutdown) {
            try {
              ShutdownSuspender.class.wait();
            } catch (InterruptedException e) {
              return;
            }
          }
        }
      }
    });
  }
}
