package axin.fastdep.model;

import java.io.Serializable;
import java.util.List;

import com.alibaba.fastjson2.annotation.JSONField;
/**
 * @author haojiaxin <https://www.yuque.com/xinblog>
 * @date 2024年8月18日 17:41:36
 */
public class Manifest implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@JSONField(name="manifest_version")
	private String manifestVersion;
	
	@JSONField(name="project_name")
	private String projectName;
	
	private List<PatchEntry> entry;

	public String getManifestVersion() {
		return manifestVersion;
	}

	public void setManifestVersion(String manifestVersion) {
		this.manifestVersion = manifestVersion;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public List<PatchEntry> getEntry() {
		return entry;
	}

	public void setEntry(List<PatchEntry> entry) {
		this.entry = entry;
	}

	@Override
	public String toString() {
		return "Manifest [manifestVersion=" + manifestVersion + ", projectName=" + projectName + ", entry=" + entry
				+ "]";
	}
	
	
	
	

}
