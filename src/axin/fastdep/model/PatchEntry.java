package axin.fastdep.model;

import java.io.Serializable;

/**
 * @author haojiaxin <https://www.yuque.com/xinblog>
 * @date 2024年8月18日 17:44:18
 */
public class PatchEntry implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private String digest;
	
	private String fileLastTime;
	
	private String relativePath;
	
	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getFileLastTime() {
		return fileLastTime;
	}

	public void setFileLastTime(String fileLastTime) {
		this.fileLastTime = fileLastTime;
	}
	
	

}
