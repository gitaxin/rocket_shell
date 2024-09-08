package axin.fastdep.model;

import java.io.Serializable;
import java.util.List;

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
	
	private List<Env> envs;
	
	@JSONField(name="cache_max_days")
	private Integer cacheMaxDays;

	
	
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


	public Integer getCacheMaxDays() {
		return cacheMaxDays;
	}

	public void setCacheMaxDays(Integer cacheMaxDays) {
		this.cacheMaxDays = cacheMaxDays;
	}

	public List<Env> getEnvs() {
		return envs;
	}

	public void setEnvs(List<Env> envs) {
		this.envs = envs;
	}

	@Override
	public String toString() {
		return "Config [name=" + name + ", projectName=" + projectName + ", envs=" + envs + ", cacheMaxDays="
				+ cacheMaxDays + "]";
	}

	
	
	
	

	
}
