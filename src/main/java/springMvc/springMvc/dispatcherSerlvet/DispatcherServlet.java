package springMvc.springMvc.dispatcherSerlvet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import springMvc.springMvc.annotation.Controller;
import springMvc.springMvc.annotation.Dao;
import springMvc.springMvc.annotation.Qualiftier;
import springMvc.springMvc.annotation.RequestMapping;
import springMvc.springMvc.annotation.Service;
import springMvc.springMvc.base.BaseController;

@WebServlet(name="dispatcherSerlvet",urlPatterns="/*",loadOnStartup=1,
initParams={@WebInitParam(name = "base-package", value = "springMvc.springMvc")})
public class DispatcherServlet extends HttpServlet {

	//扫描的基包
	private String basePackage="";
	
	//基包下面所有的带包路径权限定类名
	private List<String> packageNames=new ArrayList<String>();
	
	//注解实例化  注解上的名称:实例化的对象
	private Map<String,Object> instanceMap=new HashMap<String,Object>();
	
	//文件路径:注解的名称
	private Map<String,String> nameMap=new HashMap<String, String>();
	
	//URL地址和方法的映射关系 springMvc就是方法调用链
	private Map<String,Method> urlMethodMap=new HashMap<String, Method>();
	
	//method和文件路径的映射关系 主要是通过method找到该方法的对象利用反射执行
	private Map<Method,String> methodPakageMap=new HashMap<Method, String>();
	
	@Override
	public void init(ServletConfig config) throws ServletException{
		basePackage=config.getInitParameter("base-package");
		try {
			//扫描基包放入类名称容器
			scanBasePackage(basePackage);
			
			instance(packageNames);
			
			springIOC();
			
			handlerUrlMethodMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//扫描基包，基包是X.Y.Z的形式，而URL是X/Y/Z的形式，需要转换
	private void scanBasePackage(String basePackage){
		URL url=this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));
		File basePackageFile=new File(url.getPath());
		System.out.println("scan:"+basePackageFile);
		File[] childFiles=basePackageFile.listFiles();
		for(File file:childFiles){
			if(file.isDirectory()){
				scanBasePackage(basePackage+"."+file.getName());
			}else if (file.isFile()){
				packageNames.add(basePackage+"."+file.getName().split("\\.")[0]);
			}
		}
	}
	
	//完成注解标注的类的实例化，以及和注解名称的映射
	private void instance(List<String> packageNames) throws ClassNotFoundException,IllegalAccessException,InstantiationException{
		if(packageNames.size()<1)
			return ;
		for(String string:packageNames){
			Class c= Class.forName(string);
			if(c.isAnnotationPresent(Controller.class)){
				Controller controller=(Controller) c.getAnnotation(Controller.class);
				String controllerName=controller.value();
				instanceMap.put(controllerName, c.newInstance());
				nameMap.put(string, controllerName);
				System.out.println("controller:"+string+",value:"+controllerName);
			}else if(c.isAnnotationPresent(Service.class)){
				Service service=(Service) c.getAnnotation(Service.class);
				String serviceName=service.value();
				instanceMap.put(serviceName, c.newInstance());
				nameMap.put(string, serviceName);
				System.out.println("service:"+string+",value:"+serviceName);
			}else if(c.isAnnotationPresent(Dao.class)){
				Dao dao=(Dao) c.getAnnotation(Dao.class);
				String daoName=dao.value();
				instanceMap.put(daoName, c.newInstance());
				nameMap.put(string, daoName);
				System.out.println("service:"+string+",value:"+daoName);
			}
		}
	}
	
	//依赖注入
	private void springIOC() throws ClassNotFoundException,IllegalAccessException{
		for(Map.Entry<String,Object> entry:instanceMap.entrySet()){
			Field[] fields=entry.getValue().getClass().getDeclaredFields();
			for(Field field:fields){
				if(field.isAnnotationPresent(Qualiftier.class)){
					String name=field.getAnnotation(Qualiftier.class).value();
					field.setAccessible(true);
					field.set(entry.getValue(), instanceMap.get(name));
				}
			}
		}
	}
	
	//url映射处理
	private void handlerUrlMethodMap() throws ClassNotFoundException{
		if(packageNames.size()<1)
			return ;
		for(String name:packageNames){
			Class c= Class.forName(name);
			if(c.isAnnotationPresent(Controller.class)){
				Method[] methods=c.getMethods();
				StringBuffer baseUrl=new StringBuffer();
				if(c.isAnnotationPresent(RequestMapping.class)){
					RequestMapping request=(RequestMapping) c.getAnnotation(RequestMapping.class);
					baseUrl.append(request.value());
				}
				for(Method method:methods){
					if(method.isAnnotationPresent(RequestMapping.class)){
						RequestMapping request=(RequestMapping) method.getAnnotation(RequestMapping.class);
						baseUrl.append(request.value());
						
						urlMethodMap.put(baseUrl.toString(), method);
						methodPakageMap.put(method, name);
					}
				}
 			}
		}
	}
	
	//重写doget/dopost方法
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
	    doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String uri=req.getRequestURI();
        String contextPath=req.getContextPath();
        String path=uri.replace(contextPath,"");
        Method method=urlMethodMap.get(path);
        if(method!=null){
        	String pakageName=methodPakageMap.get(method);
        	String controllName=nameMap.get(pakageName);
        	BaseController controller=(BaseController) instanceMap.get(controllName);
        	try {
        		method.setAccessible(true);
        		method.invoke(controller);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
        }
	}
}
