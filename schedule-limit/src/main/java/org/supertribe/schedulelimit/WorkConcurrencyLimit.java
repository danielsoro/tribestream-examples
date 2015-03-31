/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package org.supertribe.schedulelimit;

import com.tomitribe.tribestream.governance.api.ApplicationLimit;
import com.tomitribe.tribestream.governance.api.Concurrent;

import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Lock(LockType.READ)
public class WorkConcurrencyLimit {

    /**
     * Timer service, injected by the EJB container
     */
    @Resource
    private TimerService timerService;

    /**
     * Thread-safe counter to tracke the number of times the @Timeout method is called
     */
    private AtomicInteger timerCount = new AtomicInteger(0);

    /**
     * Starts the timer. The timer calls the @Timeout method every 500 ms.
     */
    public void start() {
        timerCount.set(0);
        timerService.createTimer(500, 500, "testtimer");
    }

    /**
     * Stops the timer and returns the number of times the @Timeout method was called
     * @return the number of times the @Timeout method was called
     */
    public int stop() {
        final Collection<Timer> timers = timerService.getTimers();
        for (Timer timer : timers) {
            if ("testtimer".equals(timer.getInfo())) {
                timer.cancel();
            }
        }

        return timerCount.get();
    }

    /**
     * Simulatates some work being done. This method can only be called twice at once
     */
    @Timeout
    @AccessTimeout(0)
    @ApplicationLimit(concurrent = @Concurrent(limit = 2))
    public void doWork() {
        final int count = timerCount.incrementAndGet();

        System.out.println("Work item " + count + " starting");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // interrupted, not much we can do
        }

        System.out.println("Work item " + count + " complete");
    }
}
