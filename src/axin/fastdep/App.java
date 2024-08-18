package axin.fastdep;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson2.JSON;

import axin.fastdep.model.Config;
import axin.fastdep.model.Manifest;
import axin.fastdep.util.FileUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rocket-handler", mixinStandardHelpOptions = true, version = "rocket-handler v1.0.0",
description = "<rocket-handler>一款与Rocket配套的补丁增量离线部署工具, author:haojiaxin")
public class App implements Callable<Integer> {
	
	private static final String DEFAULT_UNIX_CONF_FILE = "/etc/rocket-handler.conf";
	
	private static final String DEFAULT_WIN_CONF_FILE = "rocket-handler.conf";
	
	private static final String ENTRY_FOLDER = "entry";
	
	private static final String MANIFEST_FILE = "manifest.json";
	
	
	
	@Option(names = {"-i", "--install"}, description = "Rocket生成的补丁包文件, 一般为zip格式")
    private File zipFile;
	
    @Option(names = {"-c", "--config"}, description = "配置文件，默认为 win: <user_home>\\rocket-handler.conf, unix: /etc/rocket-handler.conf")
    private File confFile;
    
    //配置
    private Config config;
    
    private Manifest manifest;
    
    private File patchFolder; 
    

	@Override
	public Integer call() throws Exception {
		if(zipFile == null) {
			throw new RuntimeException("Error: 请使用 -i or -install 指定zip补丁包文件！");
		}
		
		if(confFile == null) {
			initDefaultConfFile();
		}
		println("配置文件 => " + confFile.getPath());
		
		parseConfig();
		
		println("安装["+ config.getProjectName() +"]补丁 => " + config.getPublishRoot());
		
		//询问是否安装
		println("读取补丁包 => ...");
		patchFolder = FileUtil.unzip(zipFile);
		if(!patchFolder.exists()) {
			throw new RuntimeException("Error: 补丁包读取失败, 未能成功解压补丁包！");
		}
		
		println("读取manifest => ...");
		parseManifest();
		System.out.println(manifest);
		
		
		

		
		
		
		
		
		
		return 0;
	}
	
	
	private void parseManifest() {
		File file = new File(patchFolder,MANIFEST_FILE);
		if(!file.exists()) {
			throw new RuntimeException("Error: manifest file parse error:  manifest file is not exists!");
		}
		
		
        try {
            String content = FileUtils.readFileToString(file, "UTF-8");
            manifest = JSON.parseObject(content, Manifest.class);
            if(manifest == null) {
            	throw new RuntimeException("Error: manifest file parse error:  parse result is null!");
            }
            
            if(config.getProjectName() == null) {
            	throw new RuntimeException("Error: manifest file error:  the project_name is null!");
            }
        } catch (IOException e) {
        	e.printStackTrace();
        	throw new RuntimeException("Error: manifest file parse error: " + e.getMessage());
        }
	}
	
	
	private void parseConfig() {
        try {
            String content = FileUtils.readFileToString(confFile, "UTF-8");
            config = JSON.parseObject(content, Config.class);
            if(config == null) {
            	throw new RuntimeException("Error: config file parse error:  parse result is null!");
            }
            
            if(config.getProjectName() == null) {
            	throw new RuntimeException("Error: config file error:  the project_name is null!");
            }
            
            if(config.getPublishRoot() == null) {
            	throw new RuntimeException("Error: config file error:  the publish_root is null!");
            }
            
        } catch (IOException e) {
        	e.printStackTrace();
        	throw new RuntimeException("Error: config file parse error: " + e.getMessage());
        }
		
	}



	private void initDefaultConfFile() {
		if(confFile == null) {
			String os = System.getProperty("os.name");
			if(os != null && os.toLowerCase().startsWith("windows")) {
				String userDir = System.getProperty("user.home");
				File file = new File(userDir,DEFAULT_WIN_CONF_FILE);
				if(!file.exists()) {
					throw new RuntimeException("Error: config file " + file.getPath() + " is not exists!");
				}
				
			}else {
				File file = new File(DEFAULT_UNIX_CONF_FILE);
				if(!file.exists()) {
					throw new RuntimeException("Error: config file " + DEFAULT_UNIX_CONF_FILE + " is not exists!");
				}
			}
		}
	}
	
	private void newLine() {
		System.out.println();
	}
	
	private void print(String str) {
		System.out.print(str);
	}
	
	private void print(Object obj) {
		print(String.valueOf(obj));
	}
	
	private void println(String str) {
		System.out.println(str);
	}
	
	private void println(Object obj) {
		println(String.valueOf(obj));
	}
	
	
	
	
    public static void main(String... args) {
    	try {
    		 int exitCode = new CommandLine(new App()).execute(args);
    		 System.exit(exitCode);
		} catch (Exception e) {
			 e.printStackTrace();
			 System.exit(1);
		}
       
        
    }

}
