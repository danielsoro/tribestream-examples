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
import com.tomitribe.tribestream.governance.api.GovernanceUnit;
import com.tomitribe.tribestream.governance.api.Rate;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Path("rate")
@Singleton
@Lock(LockType.READ)
public class WorkRateLimit {

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
    @GET
    @Path("start")
    public void start() {
        timerCount.set(0);
        timerService.createTimer(500, 500, "testtimer");
    }

    /**
     * Stops the timer and returns the number of times the @Timeout method was called
     * @return the number of times the @Timeout method was called
     */
    @GET
    @Path("stop")
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
     * Simulatates some work being done. This method can only be called three times per minute.
     */
    @Timeout
    @ApplicationLimit(rate = @Rate(window = 1, unit = GovernanceUnit.MINUTES, limit = 3))
    public void doWork() {
        final int count = timerCount.incrementAndGet();

        System.out.println("Work item " + count + " starting");
        System.out.println("Work item " + count + " complete");
    }
}
