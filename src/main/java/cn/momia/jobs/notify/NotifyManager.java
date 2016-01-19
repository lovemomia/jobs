package cn.momia.jobs.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NotifyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyManager.class);

    private Set<Notifier> notifiers = new HashSet<Notifier>();

    private Object signal = new Object();
    private ExecutorService executorService = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1000));
    private Queue<Notifier> tasksQueue = new LinkedList<Notifier>();

    public void setNotifiers(Set<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        consume();
                    } catch (InterruptedException e) {
                        LOGGER.error("InterruptedException", e);
                    }
                }
            }
        }).start();
    }

    public void produce() {
        synchronized (signal) {
            for (Notifier notifier : notifiers) {
                if (!tasksQueue.contains(notifier)) tasksQueue.offer(notifier);
            }

            LOGGER.info("notify new tasks");
            signal.notify();
        }
    }

    public void consume() throws InterruptedException {
        synchronized (signal) {
            if (tasksQueue.isEmpty()) {
                LOGGER.info("no tasks, waiting...");
                signal.wait();
            }

            final Notifier notifier = tasksQueue.poll();
            if (notifier == null) return;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    notifier.notifyUser();
                }
            });
        }
    }
}
