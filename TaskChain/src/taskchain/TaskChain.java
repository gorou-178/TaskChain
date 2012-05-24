/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import taskchain.listener.TaskChainExceptionAdapter;
import taskchain.listener.TaskChainExceptionListener;

public class TaskChain {

	private final static Logger logger = Logger.getLogger(TaskChain.class.getName());
	private ScheduledExecutorService executor;
	private Map<Future, Callable> callableFutureMap;
	private Map<Future, Runnable> runnableFutureMap;
	private List<TaskChainExceptionListener> exceptionListeners;

	public TaskChain(){
		this(Runtime.getRuntime().availableProcessors());
	}
	
	public TaskChain(int poolSize) {
		this.executor = Executors.newScheduledThreadPool(poolSize);
		this.callableFutureMap = new ConcurrentHashMap<Future, Callable>();
		this.runnableFutureMap = new ConcurrentHashMap<Future, Runnable>();
		this.exceptionListeners = new ArrayList<TaskChainExceptionListener>();
	}

	public TaskChain asyncRun(Runnable task) {
		if(task == null){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		return asyncRun(task, null);
	}

	public TaskChain asyncCall(final Callable task) {
		if (task == null){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Future future = executor.submit(task);
				callableFutureMap.put(future, task);
			}
		});
		return this;
	}

	public Future getFuture(Callable task) {
		if (task == null){
			return null;
		}
		
		for (Map.Entry<Future, Callable> entry : callableFutureMap.entrySet()) {
			if (entry.getValue() == task) {
				Future future = entry.getKey();
				callableFutureMap.remove(future);
				return future;
			}
		}
		return null;
	}

	public Future getFuture(Runnable task) {
		if (task == null){
			return null;
		}
		
		for (Map.Entry<Future, Runnable> entry : runnableFutureMap.entrySet()) {
			if (entry.getValue() == task) {
				Future future = entry.getKey();
				runnableFutureMap.remove(future);
				return future;
			}
		}
		return null;
	}

	public TaskChain asyncRun(final Runnable task, final Runnable callback) {
		if (task == null){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		executor.execute(new Runnable() {
			@Override
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
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (timeout <= 0){
			return asyncRun(task, callback);
		}
		
		if (isShutdown()) {
			return this;
		}
		
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Future future = executor.submit(task);
					future.get(timeout, timeUnit);
					if(callback != null){
						callback.run();
					}
				} catch (InterruptedException e) {
					notifyInterrupted(e, task);
				} catch (ExecutionException e) {
					notifyExecution(e, task);
				} catch (TimeoutException e) {
					notifyTimeout(e, task);
				}
			}
		});
		return this;
	}

	public TaskChain syncRun(Runnable task) {
		if (task == null){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		try {
			Future future = executor.submit(task);
			future.get();
		} catch (InterruptedException e) {
			notifyInterrupted(e, task);
		} catch (ExecutionException e) {
			notifyExecution(e, task);
		}
		return this;
	}

	public TaskChain syncRun(Runnable task, long timeout, TimeUnit timeUnit) {
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (timeout <= 0){
			return syncRun(task);
		}
		
		if (isShutdown()) {
			return this;
		}
		
		try {
			Future future = executor.submit(task);
			future.get(timeout, timeUnit);
		} catch (InterruptedException e) {
			notifyInterrupted(e, task);
		} catch (ExecutionException e) {
			notifyExecution(e, task);
		} catch (TimeoutException e) {
			notifyTimeout(e, task);
		}
		return this;
	}

	public TaskChain timerAsyncRun(final Runnable task, long delay, TimeUnit timeUnit) {
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (delay <= 0){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		logger.info("timer run schedule...");
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				logger.info("timer run execute");
				executor.execute(task);
			}
		}, delay, timeUnit);
		return this;
	}

	public TaskChain timerSyncRun(final Runnable task, final long delay, final TimeUnit timeUnit) {
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (delay <= 0){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		logger.info("timer run schedule...");
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					ScheduledFuture future = executor.schedule(new Runnable() {
						@Override
						public void run() {
							logger.info("timer run execute");
							executor.execute(task);
						}
					}, delay, timeUnit);
					future.get();
				} catch (InterruptedException e) {
					notifyInterrupted(e, task);
				} catch (ExecutionException e) {
					notifyExecution(e, task);
				}
			}
		});
		return this;
	}

	public TaskChain timerSyncRun(final Runnable task, final long delay, final long timeout, final TimeUnit timeUnit) {
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (timeout <= 0){
			return timerSyncRun(task, delay, timeUnit);
		}
		
		if (isShutdown()) {
			return this;
		}
		
		logger.info("timer run schedule...");
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					ScheduledFuture future = executor.schedule(new Runnable() {
						@Override
						public void run() {
							logger.info("timer run execute");
							executor.execute(task);
						}
					}, delay, timeUnit);
					future.get(timeout, timeUnit);
				} catch (InterruptedException e) {
					notifyInterrupted(e, task);
				} catch (ExecutionException e) {
					notifyExecution(e, task);
				} catch (TimeoutException e) {
					notifyTimeout(e, task);
				}
			}
		});
		return this;
	}

	public TaskChain scheduleAsyncRun(Runnable task, long delay, long interval, TimeUnit timeUnit) {
		if (task == null){
			return this;
		}
		
		if (timeUnit == null){
			return this;
		}
		
		if (delay < 0){
			return this;
		}
		
		if (interval <= 0){
			return this;
		}
		
		if (isShutdown()) {
			return this;
		}
		
		Future future = executor.scheduleAtFixedRate(task, delay, interval, timeUnit);
		runnableFutureMap.put(future, task);
		return this;
	}

	public void shutdown() {
		if ( !isShutdown()) {
			executor.shutdown();
		}
	}

	public List<Runnable> shutdown(long timeout, TimeUnit timeUnit) {
		try {
			shutdown();
			if ( !executor.awaitTermination(timeout, timeUnit) ) {
				return shutdownNow();
			}
		} catch (InterruptedException e) {
			notifyInterrupted(e, null);
			return shutdownNow();
		}
		return Collections.EMPTY_LIST;
	}

	public List<Runnable> shutdownNow() {
		if ( !isShutdown()) {
			return executor.shutdownNow();
		}
		return Collections.EMPTY_LIST;
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}
	
	public boolean isTerminated() {
		return executor.isTerminated();
	}
	
	public void addTaskChainExceptionListener(TaskChainExceptionListener listener){
		if (listener != null) {
			this.exceptionListeners.add(listener);
		}
	}
	
	public void removeTaskChainExceptionListener(TaskChainExceptionListener listener){
		if (listener != null && exceptionListeners.contains(listener)) {
			this.exceptionListeners.remove(listener);
		}
	}
	
	private void notifyExecution(ExecutionException e, Runnable task){
		for (TaskChainExceptionListener listener: exceptionListeners) {
			listener.execution(e, task);
		}
	}
	
	private void notifyInterrupted(InterruptedException e, Runnable task){
		for (TaskChainExceptionListener listener: exceptionListeners) {
			listener.interrupted(e, task);
		}
	}
	
	private void notifyTimeout(TimeoutException e, Runnable task){
		for (TaskChainExceptionListener listener: exceptionListeners) {
			listener.timeout(e, task);
		}
	}

	public static void main(String[] args) {
		
		Runnable taskA = new Runnable() {
			@Override
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
			@Override
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
			@Override
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
		tc.addTaskChainExceptionListener(new TaskChainExceptionAdapter(){
			@Override
			public void interrupted(InterruptedException e, Runnable task) {
				logger.log(Level.INFO, "interrupted: ", e);
			}
			@Override
			public void timeout(TimeoutException e, Runnable task) {
				logger.log(Level.INFO, "timeout: ", e);
			}
		});
		/**
		 * 1.非同期でタスクB
		 * 2.同期でタスクC
		 * 3.3秒後にタスクA
		 * 4.非同期でタスクA。終了後にタスクC
		 * 5.シャットダウン(タイムアウト10秒)
		 */
		tc.asyncRun(taskB)
		   .syncRun(taskC, 1, TimeUnit.SECONDS)
		   .timerAsyncRun(taskA, 3, TimeUnit.SECONDS)
		   .asyncRun(taskA, taskC)
		   .shutdown(2, TimeUnit.SECONDS);

	}
}