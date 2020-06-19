package cn.itcast.hibernate.log4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLog4j {
	
	private static final Logger log = LoggerFactory.getLogger(TestLog4j.class);
	
	public static void main(String[] args) {
		
		log.trace("这是一个栈信息");
		
		if(log.isDebugEnabled()){
			log.debug("这是一个调试信息");
			log.info("这是一个普通信息");
			log.warn("这是一个警告信息");
			log.error("这是一个错误信息");
		}
	}

}
