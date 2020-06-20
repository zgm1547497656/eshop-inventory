package com.roncoo.eshop.inventory.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.roncoo.eshop.inventory.model.ProductInventory;
import com.roncoo.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.roncoo.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import com.roncoo.eshop.inventory.request.Request;
import com.roncoo.eshop.inventory.service.ProductInventoryService;
import com.roncoo.eshop.inventory.service.RequestAsyncProcessService;
import com.roncoo.eshop.inventory.vo.Response;

@Controller
public class ProductInventoryController {

	@Resource
	private RequestAsyncProcessService requestAsyncProcessService;
	@Resource
	private ProductInventoryService productInventoryService;
	
	/**
	 * 更新商品库存
	 */
	@RequestMapping("/updateProductInventory")
	@ResponseBody
	public Response updateProductInventory(ProductInventory productInventory) {
		
		Response response = null;
		try {
			Request request = new ProductInventoryDBUpdateRequest(
					productInventory, productInventoryService);
			requestAsyncProcessService.process(request);
			response = new Response(Response.SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			response = new Response(Response.FAILURE);
		}
		
		return response;
	}
	
	/**
	 * 获取商品库存
	 */
	@RequestMapping("/getProductInventory")
	@ResponseBody
	public ProductInventory getProductInventory(Integer productId) {
	
		ProductInventory productInventory = null;
		
		try {
			Request request = new ProductInventoryCacheRefreshRequest(productId, productInventoryService, false);
			requestAsyncProcessService.process(request);
			
			long startTime = System.currentTimeMillis();
			long endTime = 0L;
			long waitTime = 0L;
			// 等待超过200ms没有从缓存中获取到结果
			while(true) {
				if(waitTime > 200) {// 控制在200ms就可以了
					break;
				}
				// 尝试去redis中读取一次商品库存的缓存数据
				productInventory = productInventoryService.getProductInventoryCache(productId);
				
				// 如果读取到了结果，那么就返回
				if(productInventory != null) {
					return productInventory;
				} else {// 如果没有读取到结果，那么等待一段时间
					Thread.sleep(20);
					endTime = System.currentTimeMillis();
					waitTime = endTime - startTime;
				}
			}
			
			// 直接尝试从数据库中读取数据
			productInventory = productInventoryService.findProductInventory(productId);
			if(productInventory != null) {
				// 将缓存刷新一下
				// 这个过程，实际上是一个读操作的过程，但是没有放在队列中串行去处理，还是有数据不一致的问题
				request = new ProductInventoryCacheRefreshRequest(
						productId, productInventoryService, true);
				requestAsyncProcessService.process(request);
				
				// 代码会运行到这里，只有三种情况：
				// 1、就是说，上一次也是读请求，数据刷入了redis，但是redis LRU算法给清理掉了，标志位还是false
				// 2、可能在200ms内，就是读请求在队列中一直积压着，没有等待到它执行（最坏的可能）
				// 3、数据库里本身就没有，缓存穿透，穿透redis，请求到达mysql库
				
				return productInventory;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new ProductInventory(productId, -1L);  
	}
	
}
