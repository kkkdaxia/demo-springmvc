package springMvc.springMvc.service;

import springMvc.springMvc.annotation.Service;

@Service("testService")
public class TestService {

	public void testIndex() {
		System.out.println("testService_testIndex");
	}

}
