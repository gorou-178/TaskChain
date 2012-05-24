/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain.listener;

import java.util.concurrent.TimeoutException;

public interface TimeoutExceptionListener {
	void timeout(TimeoutException e, Runnable task);
}
