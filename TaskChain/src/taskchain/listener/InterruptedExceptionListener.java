/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package taskchain.listener;

/**
 *
 * @author anaisatoshi
 */
public interface InterruptedExceptionListener {
	void interrupted(InterruptedException e, Runnable task);
}
