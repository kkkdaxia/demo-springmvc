package springMvc.springMvc.controller;

import springMvc.springMvc.annotation.Controller;
import springMvc.springMvc.annotation.Qualiftier;
import springMvc.springMvc.annotation.RequestMapping;
import springMvc.springMvc.base.BaseController;
import springMvc.springMvc.service.TestService;

@Controller("TestController")
@RequestMapping("/control")
public class TestController implements BaseController {

	@Qualiftier("testService")
	private TestService testService;
	
	@RequestMapping("/testIndex")
	public void testIndex(){
		System.out.println("testController_testIndex");
		testService.testIndex();
	}
	
}
