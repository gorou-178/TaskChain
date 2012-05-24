/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain.listener;

import java.util.concurrent.ExecutionException;

public interface ExecutionExceptionListener {
	void execution(ExecutionException e, Runnable task);
}
