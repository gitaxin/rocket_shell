package axin.fastdep.model;

import java.io.Serializable;

import com.alibaba.fastjson2.annotation.JSONField;

public class Env implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String name;
	
	@JSONField(name="publish_root")
	private String publishRoot;
	
	@JSONField(name="is_default")
	private boolean isDefault;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPublishRoot() {
		return publishRoot;
	}

	public void setPublishRoot(String publishRoot) {
		this.publishRoot = publishRoot;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public String toString() {
		return "Env [name=" + name + ", publishRoot=" + publishRoot + ", isDefault=" + isDefault + "]";
	}
	
	
	
	

}
