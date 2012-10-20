package com.rabbitmq.jms.client;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Used to signal completion of an asynchronous operation.
 */
public class Completion {

    private final FutureBoolean fb = new FutureBoolean();

    /**
     * Signal completion.
     */
    public void setComplete() {
        this.fb.setComplete();
    }

    /**
     * Wait (forever) until completion is signalled.
     *
     * @throws InterruptedException if thread is interrupted while waiting.
     */
    public void waitUntilComplete() throws InterruptedException {
        this.fb.get();
    }

    /**
     * Wait for a time limit until completion is signalled. Returns normally if completion is signalled before timeout
     * or interruption.
     *
     * @param timeout time to wait (in units).
     * @param unit units of time for timeout.
     * @throws TimeoutException if timed out before completion is signalled.
     * @throws InterruptedException if thread is interrupted while waiting.
     */
    public void waitUntilComplete(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        this.fb.get(timeout, unit);
    }

    private class FutureBoolean {
        private final Object lock = new Object();
        private volatile boolean completed = false;

        public Boolean get() throws InterruptedException {
            try {
                return get(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                throw new IllegalStateException("Impossible timeout.", e);
            }
        }

        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (this.completed) return true;
            long remainingTime = unit.toNanos(timeout);
            synchronized (this.lock) {
                while (!this.completed && remainingTime > 0) {
                    long startTime = System.nanoTime();
                    TimeUnit.NANOSECONDS.timedWait(this.lock, remainingTime);
                    remainingTime -= (System.nanoTime() - startTime);
                }
                if (this.completed)
                    return true;
                else
                    throw new TimeoutException();
            }
        }

        void setComplete() {
            this.completed = true;
            synchronized (this.lock) {
                this.lock.notifyAll();
            }
        }
    }
}
