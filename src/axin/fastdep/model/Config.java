package axin.fastdep.model;

import java.io.Serializable;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * @author haojiaxin <https://www.yuque.com/xinblog>
 * @date 2024年8月18日 16:50:02
 */
public class Config implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String name;
	
	@JSONField(name="project_name")
	private String projectName;
	
	@JSONField(name="publish_root")
	private String publishRoot;

	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	
	

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getPublishRoot() {
		return publishRoot;
	}

	public void setPublishRoot(String publishRoot) {
		this.publishRoot = publishRoot;
	}

	@Override
	public String toString() {
		return "Config [name=" + name + ", projectName=" + projectName + ", publishRoot=" + publishRoot + "]";
	}
	
	

	
}
