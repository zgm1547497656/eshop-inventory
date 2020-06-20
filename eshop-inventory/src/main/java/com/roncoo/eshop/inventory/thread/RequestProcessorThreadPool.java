package com.roncoo.eshop.inventory.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.roncoo.eshop.inventory.request.Request;
import com.roncoo.eshop.inventory.request.RequestQueue;

public class RequestProcessorThreadPool {
	/**
	 * 线程池
	 */
	private ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	public RequestProcessorThreadPool() {
		RequestQueue requestQueue = RequestQueue.getInstance();
		for(int i = 0; i < 10; i++) {
			//每一个队列最多存100个数据
			ArrayBlockingQueue<Request> queue = new ArrayBlockingQueue<Request>(100);
			requestQueue.addQueue(queue);  
			threadPool.submit(new RequestProcessorThread(queue));  
		}
	}

	/**
	 * 
	 * 静态内部类的方式，去初始化单例
	 * @author Administrator
	 *
	 */
	private static class Singleton {
		
		private static RequestProcessorThreadPool instance;
		
		static {
			instance = new RequestProcessorThreadPool();
		}
		
		public static RequestProcessorThreadPool getInstance() {
			return instance;
		}
		
	}
	public static RequestProcessorThreadPool getInstance() {
		return Singleton.getInstance();
	}
	/**
	 * 初始化
	 */
	public static void init() {
		getInstance();
	}
	
}
