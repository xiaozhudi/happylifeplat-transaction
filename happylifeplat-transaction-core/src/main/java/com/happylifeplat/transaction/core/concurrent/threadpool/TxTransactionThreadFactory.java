package com.happylifeplat.transaction.core.concurrent.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Description: .</p>
 * <p>Company: 深圳市旺生活互联网科技有限公司</p>
 * <p>Copyright: 2015-2017 happylifeplat.com All Rights Reserved</p>
 * 线程池工厂帮助类
 * @author yu.xiao@happylifeplat.com
 * @version 1.0
 * @date 2017/7/17 20:56
 * @since JDK 1.8
 */
public class TxTransactionThreadFactory implements ThreadFactory {

  private static final  Logger log = LoggerFactory.getLogger(TxTransactionThreadFactory.class);

  private final AtomicLong threadNumber = new AtomicLong(1);

  private final String namePrefix;

  private static volatile boolean daemon;

  private static final ThreadGroup threadGroup = new ThreadGroup("txTransaction");

  public static ThreadGroup getThreadGroup() {
    return threadGroup;
  }

  public static ThreadFactory create(String namePrefix, boolean daemon) {
    return new TxTransactionThreadFactory(namePrefix, daemon);
  }

  public static boolean waitAllShutdown(int timeoutInMillis) {
    ThreadGroup group = getThreadGroup();
    Thread[] activeThreads = new Thread[group.activeCount()];
    group.enumerate(activeThreads);
    Set<Thread> alives = new HashSet<Thread>(Arrays.asList(activeThreads));
    Set<Thread> dies = new HashSet<Thread>();
    log.info("Current ACTIVE thread count is: {}", alives.size());
    long expire = System.currentTimeMillis() + timeoutInMillis;
    while (System.currentTimeMillis() < expire) {
      classify(alives, dies, thread -> !thread.isAlive() || thread.isInterrupted() || thread.isDaemon());
      if (alives.size() > 0) {
        log.info("Alive txTransaction threads: {}", alives);
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
          // ignore
        }
      } else {
        log.info("All txTransaction threads are shutdown.");
        return true;
      }
    }
    log.warn("Some txTransaction threads are still alive but expire time has reached, alive threads: {}",
        alives);
    return false;
  }

  private static interface ClassifyStandard<T> {
    boolean satisfy(T thread);
  }

  private static <T> void classify(Set<T> src, Set<T> des, ClassifyStandard<T> standard) {
    Set<T> set = new HashSet<>();
    for (T t : src) {
      if (standard.satisfy(t)) {
        set.add(t);
      }
    }
    src.removeAll(set);
    des.addAll(set);
  }

  private TxTransactionThreadFactory(String namePrefix, boolean daemon) {
    this.namePrefix = namePrefix;
    this.daemon = daemon;
  }


  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(threadGroup, runnable,//
        threadGroup.getName() + "-" + namePrefix + "-" + threadNumber.getAndIncrement());
    thread.setDaemon(daemon);
    if (thread.getPriority() != Thread.NORM_PRIORITY) {
      thread.setPriority(Thread.NORM_PRIORITY);
    }
    return thread;
  }
}
