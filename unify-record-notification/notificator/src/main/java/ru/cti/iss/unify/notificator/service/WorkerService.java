package ru.cti.iss.unify.notificator.service;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkerService {
    private final ScheduledExecutorService executorService;

    public WorkerService(int workerSize) {
        executorService = new ScheduledThreadPoolExecutor(workerSize);
    }

    public void execute(Runnable command) {
        executorService.execute(command);
    }

    public Future<?> execute(Runnable command, int delayInSec) {
        return executorService.schedule(command, delayInSec, TimeUnit.SECONDS);
    }

    public void close() {
        executorService.shutdown();
    }
}
