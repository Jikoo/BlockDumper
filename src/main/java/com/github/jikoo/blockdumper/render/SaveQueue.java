package com.github.jikoo.blockdumper.render;

import com.github.jikoo.blockdumper.BlockDumperMod;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public final class SaveQueue {

  private static final @NotNull Queue<WindowToPng> QUEUE = new ConcurrentLinkedQueue<>();
  private static final @NotNull AtomicReference<SaveThread> SAVE_THREAD = new AtomicReference<>();

  private static final AtomicLong metric = new AtomicLong();
  private static final AtomicInteger count = new AtomicInteger();

  public static void add(@NotNull WindowToPng data) {
    QUEUE.add(data);
    notifyOrStartSave();
  }

  public static int getPendingRenders() {
    return QUEUE.size();
  }

  public static void kill() {
    QUEUE.clear();
    SaveThread saveThread = SAVE_THREAD.get();
    if (saveThread != null && saveThread.isAlive()) {
      synchronized (saveThread) {
        saveThread.hasWaited = true;
        saveThread.interrupt();
      }
    }
  }

  private static void notifyOrStartSave() {
    // Get active thread.
    SaveThread active = SAVE_THREAD.get();

    if (active != null) {
      if (active.isAlive()) {
        // Sync on active thread so we own the monitor.
        synchronized (active) {
          // Notify thread in case it's waiting for more save data.
          active.notify();
        }
      }
      return;
    }

    // Start a new thread.
    SaveThread saveThread = new SaveThread();
    SAVE_THREAD.set(saveThread);
    saveThread.start();
  }

  private static class SaveThread extends Thread {
    private boolean hasWaited = false;
    @Override
    public void run() {
      while (true) {
        // Fetch next queue entry.
        WindowToPng next = QUEUE.poll();

        if (next == null) {
          // Nothing in the queue.
          if (hasWaited) {
            // If we already waited for more results, finish running - we're likely done.
            break;
          }

          try {
            // TODO make this less messy/use a latch
            Thread.sleep(1000L);
          } catch (InterruptedException e) {
            SAVE_THREAD.set(null);
            BlockDumperMod.LOGGER.error("Interrupted while waiting for more files!", e);
            return;
          }

          // We've now waited up to a second, less if notified. Onwards!
          hasWaited = true;
          continue;
        }

        // If we had an actual result, reset wait.
        hasWaited = false;

        try {
          long nathen = System.nanoTime();
          next.save();
          long nanow = System.nanoTime();
          metric.addAndGet(nanow - nathen);
          count.incrementAndGet();
        } catch (IOException e) {
          // We specifically do not reset save thread here - if a file cannot be saved, likely none
          // can be. One and done error.
          BlockDumperMod.LOGGER.error("Caught exception trying to write {}", next.location());
          BlockDumperMod.LOGGER.trace("Error", e);
          return;
        }

      }

      // Run complete, unset save thread.
      SAVE_THREAD.set(null);

      long imgioCount = metric.getAndSet(0L);
      int saveCount = count.getAndSet(0);

      BlockDumperMod.LOGGER.info("Average {} for {} images ({} total)", imgioCount / saveCount, saveCount, imgioCount);

    }

  }

  private SaveQueue() {}

}
