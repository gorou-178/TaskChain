/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain.listener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TaskChainExceptionAdapter implements TaskChainExceptionListener{

	@Override
	public void execution(ExecutionException e, Runnable task) {
		
	}

	@Override
	public void interrupted(InterruptedException e, Runnable task) {
		
	}

	@Override
	public void timeout(TimeoutException e, Runnable task) {
		
	}
	
}
