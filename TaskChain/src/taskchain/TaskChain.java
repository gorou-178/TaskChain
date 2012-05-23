/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain;

import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskChain {

	private final static Logger logger = Logger.getLogger(TaskChain.class.getName());
	private ScheduledExecutorService executor;
	private Map<Future, Callable> callableFutureMap;
	private Map<Future, Runnable> runnableFutureMap;

	public TaskChain(int poolSize) {
		this.executor = Executors.newScheduledThreadPool(poolSize);
		this.callableFutureMap = new ConcurrentHashMap<Future, Callable>();
		this.runnableFutureMap = new ConcurrentHashMap<Future, Runnable>();
	}

	public TaskChain asyncRun(Runnable task) {
		if (isShutdown()) {
			return this;
		}
		return asyncRun(task, null);
	}

	public TaskChain asyncCall(final Callable task) {
		if (isShutdown()) {
			return this;
		}
		executor.execute(new Runnable() {
			public void run() {
				Future future = executor.submit(task);
				callableFutureMap.put(future, task);
			}
		});
		return this;
	}

	public Future getFuture(Callable task) {
		for (Map.Entry<Future, Callable> entry : callableFutureMap.entrySet()) {
			if (entry.getValue() == task) {
				//timerAsyncRun( new ScheduleTaskRecyclerThread( task ), 1, TimeUnit.SECONDS );
				Future future = entry.getKey();
				callableFutureMap.remove(task);
				return future;
			}
		}
		return null;
	}

	public Future getFuture(Runnable task) {
		for (Map.Entry<Future, Runnable> entry : runnableFutureMap.entrySet()) {
			if (entry.getValue() == task) {
				//timerAsyncRun( new TaskRecyclerThread( task ), 1, TimeUnit.SECONDS );
				Future future = entry.getKey();
				runnableFutureMap.remove(task);
				return future;
			}
		}
		return null;
	}

	public TaskChain asyncRun(final Runnable task, final Runnable callback) {
		if (isShutdown()) {
			return this;
		}
		executor.execute(new Runnable() {
			public void run() {
				task.run();
				if (callback != null) {
					logger.info(" => callback call");
					callback.run();
				}
			}
		});
		return this;
	}

	public TaskChain asyncRun(final Runnable task, final long timeout, final TimeUnit timeUnit, final Runnable callback) {
		if (isShutdown()) {
			return this;
		}
		executor.execute(new Runnable() {
			public void run() {
				try {
					Future future = executor.submit(task);
					future.get(timeout, timeUnit);
					callback.run();
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, null, e);
				} catch (ExecutionException e) {
					logger.log(Level.WARNING, null, e);
				} catch (TimeoutException e) {
					logger.log(Level.WARNING, null, e);
				}
			}
		});
		return this;
	}

	public TaskChain syncRun(Runnable task) {
		if (isShutdown()) {
			return this;
		}
		try {
			Future future = executor.submit(task);
			future.get();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, null, e);
		} catch (ExecutionException e) {
			logger.log(Level.WARNING, null, e);
		}
		return this;
	}

	public TaskChain syncRun(Runnable task, long timeout, TimeUnit timeUnit) {
		if (isShutdown()) {
			return this;
		}
		try {
			Future future = executor.submit(task);
			future.get(timeout, timeUnit);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, null, e);
		} catch (ExecutionException e) {
			logger.log(Level.WARNING, null, e);
		} catch (TimeoutException e) {
			logger.log(Level.WARNING, null, e);
		}
		return this;
	}

	public TaskChain timerAsyncRun(final Runnable task, long delay, TimeUnit timeUnit) {
		if (isShutdown()) {
			return this;
		}
		logger.info("timer run schedule...");
		executor.schedule(new Runnable() {
			public void run() {
				logger.info("timer run execute");
				executor.execute(task);
			}
		}, delay, timeUnit);
		return this;
	}

	public TaskChain timerSyncRun(final Runnable task, final long delay, final TimeUnit timeUnit) {
		if (isShutdown()) {
			return this;
		}
		logger.info("timer run schedule...");
		executor.execute(new Runnable() {
			public void run() {
				try {
					ScheduledFuture future = executor.schedule(new Runnable() {
						public void run() {
							logger.info("timer run execute");
							executor.execute(task);
						}
					}, delay, timeUnit);
					future.get();
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, null, e);
				} catch (ExecutionException e) {
					if (!isShutdown()) {
						logger.log(Level.WARNING, null, e);
					}
				}
			}
		});
		return this;
	}

	public TaskChain timerSyncRun(final Runnable task, final long delay, final long timeout, final TimeUnit timeUnit) {
		if (isShutdown()) {
			return this;
		}
		logger.info("timer run schedule...");
		executor.execute(new Runnable() {
			public void run() {
				try {
					ScheduledFuture future = executor.schedule(new Runnable() {
						public void run() {
							logger.info("timer run execute");
							executor.execute(task);
						}
					}, delay, timeUnit);
					future.get(timeout, timeUnit);
				} catch (InterruptedException e) {
					logger.log(Level.WARNING, null, e);
				} catch (ExecutionException e) {
					if (!isShutdown()) {
						logger.log(Level.WARNING, null, e);
					}
				} catch (TimeoutException e) {
					logger.log(Level.WARNING, null, e);
				}
			}
		});
		return this;
	}

	public TaskChain scheduleAsyncRun(Runnable task, long delay, long interval, TimeUnit timeUnit) {
		if (isShutdown()) {
			return this;
		}
		Future future = executor.scheduleAtFixedRate(task, delay, interval, timeUnit);
		runnableFutureMap.put(future, task);
		return this;
	}

	public void shutdown() {
		if (!isShutdown()) {
			executor.shutdown();
		}
	}

	public void shutdown(long timeout, TimeUnit timeUnit) {
		try {
			shutdown();
			if ( !executor.awaitTermination(timeout, timeUnit) ) {
				shutdownNow();
			}
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, null, e);
		}
		shutdownNow();
	}

	public void shutdownNow() {
		if ( !isShutdown()) {
			executor.shutdownNow();
		}
	}

	public void shutdownNow(long timeout, TimeUnit timeUnit) {
		try {
			shutdownNow();
			executor.awaitTermination(timeout, timeUnit);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, null, e);
		}
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public static void main(String[] args) {
		
		Runnable taskA = new Runnable() {
			public void run() {
				try {
					logger.log(Level.INFO, "taskA: start");
					Thread.sleep(1000);
					logger.log(Level.INFO, "taskA: end");
				} catch (InterruptedException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		};
		
		Runnable taskB = new Runnable() {
			public void run() {
				try {
					logger.log(Level.INFO, "taskB: start");
					Thread.sleep(5000);
					logger.log(Level.INFO, "taskB: end");
				} catch (InterruptedException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		};
		
		Runnable taskC = new Runnable() {
			public void run() {
				try {
					logger.log(Level.INFO, "taskC: start");
					Thread.sleep(3000);
					logger.log(Level.INFO, "taskC: end");
				} catch (InterruptedException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		};

		TaskChain tc = new TaskChain(10);

		/**
		 * 1.非同期でタスクB
		 * 2.同期でタスクC
		 * 3.3秒後にタスクA
		 * 4.非同期でタスクA。終了後にタスクC
		 * 5.シャットダウン(タイムアウト10秒)
		 */
		tc.asyncRun(taskB)
		   .syncRun(taskC)
		   .timerAsyncRun(taskA, 3, TimeUnit.SECONDS)
		   .asyncRun(taskA, taskC)
		   .shutdown(10, TimeUnit.SECONDS);

	}
}