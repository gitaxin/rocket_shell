package axin.fastdep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson2.JSON;

import axin.fastdep.model.Config;
import axin.fastdep.model.Manifest;
import axin.fastdep.model.PatchEntry;
import axin.fastdep.util.FileUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "rocket-handler", 
	mixinStandardHelpOptions = true, 
	version = "rocket-handler v1.0.0",
	description = "一款与Rocket配套的补丁增量部署工具, author:haojiaxin")
public class App implements Callable<Integer> {
	
	private static final String DEFAULT_UNIX_CONF_FILE = "/etc/rocket-handler.conf";
	
	private static final String DEFAULT_WIN_CONF_FILE = "rocket-handler.conf";
	
	private static final String ENTRY_FOLDER = "entry";
	
	private static final String MANIFEST_FILE = "manifest.json";
	
	
	
	@Parameters(paramLabel = "zipFile", description = "Rocket生成的补丁包文件, 一般为zip格式")
    private File zipFile;
	
	@Option(names = {"-i", "--install"}, description = "安装补丁包")
    private boolean install;
	
	@Option(names = {"-p", "--parse"}, description = "解析补丁包")
    private boolean parse;
	
    @Option(names = {"-c", "--config"}, description = "配置文件，默认为 win: <user_home>\\rocket-handler.conf, unix: /etc/rocket-handler.conf")
    private File confFile;
    
    //配置
    private Config config;
    
    private Manifest manifest;
    
    private File patchFolder; 
    
    private static final SimpleDateFormat SDF_YYYYMMDD = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    private static final SimpleDateFormat SDF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final SimpleDateFormat SDF_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
    
    private static final Pattern VersionDatePattern = Pattern.compile("^(?<year>\\d{4})(?<month>\\d{2})(?<day>\\d{2})$");
    
    private static final int DEFAULT_CACHE_MAX_DAYS = 5;
    
    private final String time = SDF_YYYYMMDDHHMMSS.format(new Date());
    
    private static BufferedWriter logWriter;
    private static BufferedWriter reportWriter;
    
    private static File logFile;
    private static File reportFile;
    
    private static final String LINE = "=================================";
    
    private static final String LINE_SIMPLE = "------------------------------";
    

	@Override
	public Integer call() throws Exception {
		if(zipFile == null) {
			throw new RuntimeException("Error: 请指定Rocket生成的补丁包文件！");
		}
		
		if(install) {
			return install();
		}else if(parse) {
			return parse();
		}else {
			log("[Error]-i, --install \t安装补丁包");
			log("[Error]-p, --parse \t解析补丁包");
			return 1;
		}
	}
	
	/**
	 * 安装文件
	 * @return
	 * @throws Exception
	 * @author haojiaxin <https://www.yuque.com/xinblog>
	 * @date 2024年9月7日 17:50:24
	 */
	private int install() throws Exception {
		if(confFile == null) {
			initDefaultConfFile();
		}
		log("[Info]使用配置文件 " + confFile.getPath());
		
		parseConfig();
		
		log("[Info]安装["+ config.getProjectName() +"]补丁到目录 " + config.getPublishRoot());
		
		if(!new File(config.getPublishRoot()).exists()) {
			log("[Error]安装目录 "+ config.getPublishRoot() +" 不存在！");
			return 1;
		}
		
		
		log("[Info]读取补丁包 ...");
		patchFolder = FileUtil.unzip(zipFile);
		if(!patchFolder.exists()) {
			throw new RuntimeException("[Error]补丁包读取失败, 未能成功解压补丁包！");
		}
		
		log("[Info]读取manifest ...");
		parseManifest();
		
		List<String> msgList = fillEntry();
		if(msgList.size() > 0) {
			log(msgList);
			return 0;
		}
		
		List<PatchEntry> entry = manifest.getEntry();
		
		if(entry == null || entry.size() == 0) {
			log("[Info]未读取到需要安装的文件！");
			return 0;
		}
		
		if(!manifest.getProjectName().equals(config.getProjectName())) {
			log("[Error]补丁包"+zipFile.getName()+"不能安装到当前环境" + config.getProjectName());
			return 1;
		}
		
		
		log(LINE);
		log(zipFile.getName() + " 补丁文件清单:");
		for (int i = 0; i < entry.size(); i++) {
			PatchEntry patchEntry = entry.get(i);
			log((i+1) + ".\t" + patchEntry.getFileLastTime() + "\t" + patchEntry.getRelativePath());
		}
		log(LINE);
		
		//询问是否安装
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		String q = "将安装项目【"+manifest.getProjectName()+"】的 "+entry.size()+" 个文件到目录 "+config.getPublishRoot()+"? (y/n):";
	   	 System.out.print(q);
	   	 String enter = scanner.nextLine();
	   	 while(enter == null || enter.trim().length() == 0) {
	   		System.out.print(q);
	   		 enter = scanner.nextLine();
	   	 }
	   	 if(!enter.equalsIgnoreCase("y")) {
	   		 return 0;
	   	 }
	   	
	   	int success = 0;
	   	int samed = 0;
	   	int created = 0;
	   	int updated = 0;
	   	log("[Info]开始安装....");
		for (int i = 0; i < entry.size(); i++) {
			PatchEntry patchEntry = entry.get(i);
			int backState = backFile(patchEntry);
			if(backState == 0) {
				//文件相同
				success++;
				samed++;
				patchEntry.setSuccess(true);
				patchEntry.setState("same");
				continue;
			}
			log("[Info]安装文件 -> " + patchEntry.getRelativePath());
			File targetFile = new File(config.getPublishRoot(),patchEntry.getRelativePath());
			FileUtils.copyFile(patchEntry.getFile(), targetFile);
			if(targetFile.exists()) {
				long time = SDF_YYYY_MM_DD.parse(patchEntry.getFileLastTime()).getTime();
				boolean modified = targetFile.setLastModified(time);
				if(!modified) {
					log("[Error]安装文件 -> 最后修改时间更新失败: " + patchEntry.getRelativePath());
				}
				
				try {
					String md5 = FileUtil.calculateMD5(targetFile);
					if(md5.equals(patchEntry.getDigest())) {
						success++;
						patchEntry.setSuccess(true);
						if(backState == -1){
						   created ++;
					       patchEntry.setState("created");
				        }else{
				           updated++;
				           patchEntry.setState("updated");
				        }
						
					}else {
						patchEntry.setSuccess(false);
						patchEntry.setMsg("安装异常:文件完整性异常");
					}
					
				}catch (Exception e) {
					patchEntry.setSuccess(false);
					patchEntry.setMsg("安装异常:文件完整性校验失败");
				}
				
				//删除多余备份文件
				removeBackFile(targetFile);
				
				
			}else {
				patchEntry.setSuccess(false);
				patchEntry.setMsg("安装异常:未找到安装后的文件");
			}
			
		}
		log("[Info]安装完成!");
		
		log(LINE);
		
		log("补丁安装报告：");
		report("补丁安装报告：");
		
		String format = "总:%s  成功:%s  新增:%s  更新:%s  相同:%s";
		String summary = String.format(format, entry.size(), success, created, updated, samed);
		
		log(summary);
		report(summary);
		
		log(LINE_SIMPLE);
		report(LINE_SIMPLE);
		
		String title = "序号\t状态\t类型\t文件";
		log(title);
		report(title);
		
		for (int i = 0; i < entry.size(); i++) {
			PatchEntry patchEntry = entry.get(i);
			String lineStr = (i+1) + ".\t" + patchEntry.getSuccess() + "\t" + patchEntry.getState() + "\t"+ patchEntry.getRelativePath()+ "\t" + patchEntry.getMsg();
			log(lineStr);
			report(lineStr);
		}
		
		log(LINE);
		
		return 0;
	}
	
	/**
	 * 解析文件
	 * @return
	 * @throws Exception
	 * @author haojiaxin <https://www.yuque.com/xinblog>
	 * @date 2024年9月7日 17:50:36
	 */
	private int parse() throws Exception{
		log("[Info]读取补丁包 ...");
		patchFolder = FileUtil.unzip(zipFile);
		if(!patchFolder.exists()) {
			throw new RuntimeException("[Error]补丁包读取失败, 未能成功解压补丁包！");
		}
		
		log("[Info]读取manifest ...");
		parseManifest();
		
		List<String> msgList = fillEntry();
		if(msgList.size() > 0) {
			log(msgList);
			return 0;
		}
		
		List<PatchEntry> entry = manifest.getEntry();
		
		if(entry == null || entry.size() == 0) {
			log("[Info]未读取到需要解析的文件！");
			return 0;
		}
		
	   	
		int success = 0;
	   	int error = 0;
	   
	   	log("[Info]开始解析....");
	   	
	   	File parseRoot = new File(patchFolder.getParent(),patchFolder.getName() + "_root");
	   	if(parseRoot.exists()) {
	   		FileUtils.deleteDirectory(parseRoot);
	   	}
	   	
		for (int i = 0; i < entry.size(); i++) {
			PatchEntry patchEntry = entry.get(i);
			File targetFile = new File(patchFolder.getParent(),patchFolder.getName() + "_root" + File.separator + patchEntry.getRelativePath());
			FileUtils.copyFile(patchEntry.getFile(), targetFile);
			if(targetFile.exists()) {
				success ++;
				log("[Info]解析文件成功 -> " + targetFile.getPath());
			}else {
				error ++;
				log("[Error]解析文件失败 -> " + patchEntry.getRelativePath());
			}
		}
		
		String format = "[Info]解析结果 -> 总:%s  成功:%s  失败:%s";
		String summary = String.format(format, entry.size(), success, error);
		log(summary);
		log("解压目录: " + parseRoot.getPath());
		
		return 0;
	}
	
	
	/**
	 * 
	 * @param patchEntry
	 * @return -1 :无需备份  1-已备份 0-相同
	 * @author haojiaxin <https://www.yuque.com/xinblog>
	 * @date 2024年9月1日 11:25:52
	 */
	private int backFile(PatchEntry patchEntry) {
		
		File file = new File(config.getPublishRoot(),patchEntry.getRelativePath());
		if(!file.exists()) {
			return -1;
		}
		
		try {
			String md5 = FileUtil.calculateMD5(file);
			if(md5.equals(patchEntry.getDigest())) {
				return 0;
			}
			
		}catch (Exception e) {
		}
		
		String ymd = SDF_YYYYMMDD.format(new Date());
		File newFile = new File(file.getParent(), file.getName() + "." + ymd + ".bak");
		while(newFile.exists()) {
			ymd = SDF_YYYYMMDD.format(new Date());
			newFile = new File(file.getParent(), file.getName() + "." + ymd + ".bak");
		}
		
		try {
			log("[Info]备份文件 -> " + patchEntry.getRelativePath() + " 到 " + newFile.getName());
			FileUtils.moveFile(file, newFile);
		} catch (IOException e) {
			log("[Error]备份文件 -> 备份失败: " + patchEntry.getRelativePath() + ", Exception: " + e.getMessage());
		}
		
		return 1;
	}
	
	private void removeBackFile(File targetFile) {
		//com/axin/got/123.txt
		//123.txt
		//获取备份文件
		File[] deleteable = filterDeleteable(targetFile);
		//对备份文件整理并排序
		List<Deleteable> formatFileList = formatFileList(deleteable);
		Integer cacheMaxDays = config.getCacheMaxDays();
		for (int i = cacheMaxDays; i < formatFileList.size(); i++) {
			Deleteable deleteable2 = formatFileList.get(i);
			for (File file : deleteable2.files) {
				if(file.exists() && file.isFile()) {
					log("[Info]删除过时的备份文件: " + file.getName());
					file.delete();
				}
			}
		}
	}
	
	/**
	 * 可删除的备份文件
	 * 
	 * @author haojiaxin <https://www.yuque.com/xinblog>
	 * @date 2024年9月1日 17:03:49
	 */
	private class Deleteable{
		String date;
		List<File> files = new ArrayList<File>();
	}
	
	
	private List<Deleteable> formatFileList(File[] list) {
		
		Map<String,Deleteable> dateSet = new HashMap<String,Deleteable>();
		List<Deleteable> resultList = new ArrayList<Deleteable>();
		for (int i = 0; i < list.length; i++) {
			File file = list[i];
			String name = file.getName();
			if(!name.endsWith(".bak")) {
				continue;
			}
			//名字去除.bak
			int lastIndex = name.lastIndexOf(".bak");
			name = name.substring(0,lastIndex);
			
			//123.txt.20240901_154237
			lastIndex = name.lastIndexOf(".");
		    if (lastIndex <= 0) {
		    	continue;
		    }
		    
		    String versionDate = name.substring(lastIndex + 1, lastIndex + 1 + 8);
		    Matcher matcher = VersionDatePattern.matcher(versionDate);
			if(!matcher.matches()) {
				continue;
			}
			
			if(dateSet.containsKey(versionDate)) {
				dateSet.get(versionDate).files.add(file);
			}else {
				Deleteable deleteable = new Deleteable();
				deleteable.date = versionDate;
				deleteable.files.add(file);
				
				resultList.add(deleteable);
				
				dateSet.put(versionDate, deleteable);
			}
		}
		
		Collections.sort(resultList,new Comparator<Deleteable>() {

			@Override
			public int compare(Deleteable o1, Deleteable o2) {
				//倒序
				return o2.date.compareTo(o1.date);
			}
		});
		
		return resultList;
		
	}
	
	private File[] filterDeleteable(File targetFile) {
		File dir = targetFile.getParentFile();
		File[] listFiles = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				//123.txt.20240901_154237.bak
				if(!name.endsWith(".bak")) {
					return false;
				}
				//名字去除.bak
				int lastIndex = name.lastIndexOf(".bak");
				name = name.substring(0,lastIndex);
				
				//123.txt.20240901_154237
				lastIndex = name.lastIndexOf(".");
			    if (lastIndex <= 0) {
			      return false;
			    }
			    
			    //去除备份文件名后的文件名，如果不是当前部署的文件名称，则忽略
			    String subFileName = name.substring(0, lastIndex);
			    if (!targetFile.getName().equals(subFileName)) {
			       return false;
			    }
			    String versionDate = name.substring(lastIndex + 1, lastIndex + 1 + 8);
			    Matcher matcher = VersionDatePattern.matcher(versionDate);
				if(!matcher.matches()) {
					return false;
				}
				return true;
			}
		});
		
		return listFiles;
	}
	
	
	
	private List<String> fillEntry() {
		List<String> msg = new ArrayList<>();
		List<PatchEntry> entryList = manifest.getEntry();
		String os = System.getProperty("os.name");
		for (int i = 0; i < entryList.size(); i++) {
			PatchEntry patchEntry = entryList.get(i);
			if(os != null && os.toLowerCase().startsWith("linux")) {
				patchEntry.setRelativePath(patchEntry.getRelativePath().replace("\\", "/"));
			}
			File file = new File(patchFolder.getPath(),ENTRY_FOLDER + File.separator + patchEntry.getDigest());
			try {
				String md5 = FileUtil.calculateMD5(file);
				if(md5.equals(patchEntry.getDigest())) {
					patchEntry.setFile(file);
				}else {
					msg.add("[Error]校验文件完整性 -> 文件不完整: " + patchEntry.getRelativePath());
				}
				
			}catch (Exception e) {
				msg.add("[Error]校验文件完整性 -> 校验失败: " + patchEntry.getRelativePath() + ", Exception:" + e.getMessage());
				e.printStackTrace();
			}
		}
		
		return msg;
	}
	
	
	private void parseManifest() {
		File file = new File(patchFolder,MANIFEST_FILE);
		if(!file.exists()) {
			throw new RuntimeException("[Error]manifest file parse error:  manifest file is not exists!");
		}
		
		
        try {
            String content = FileUtils.readFileToString(file, "UTF-8");
            manifest = JSON.parseObject(content, Manifest.class);
            if(manifest == null) {
            	throw new RuntimeException("[Error]manifest file parse error:  parse result is null!");
            }
            
            if(manifest.getProjectName() == null) {
            	throw new RuntimeException("[Error]manifest file error:  the project_name is null!");
            }
        } catch (IOException e) {
        	e.printStackTrace();
        	throw new RuntimeException("[Error]manifest file parse error: " + e.getMessage());
        }
	}
	
	
	private void parseConfig() {
        try {
            String content = FileUtils.readFileToString(confFile, "UTF-8");
            config = JSON.parseObject(content, Config.class);
            if(config == null) {
            	throw new RuntimeException("[Error]config file parse error:  parse result is null!");
            }
            
            if(config.getProjectName() == null) {
            	throw new RuntimeException("[Error]config file error:  the project_name is null!");
            }
            
            if(config.getPublishRoot() == null) {
            	throw new RuntimeException("[Error]config file error:  the publish_root is null!");
            }
            
            if(config.getCacheMaxDays() == null) {
    			config.setCacheMaxDays(DEFAULT_CACHE_MAX_DAYS);
    		}
        } catch (IOException e) {
        	e.printStackTrace();
        	throw new RuntimeException("[Error]config file parse error: " + e.getMessage());
        }
		
	}



	private void initDefaultConfFile() {
		if(confFile == null) {
			String os = System.getProperty("os.name");
			if(os != null && os.toLowerCase().startsWith("windows")) {
				String userDir = System.getProperty("user.home");
				File file = new File(userDir,DEFAULT_WIN_CONF_FILE);
				if(!file.exists()) {
					throw new RuntimeException("[Error]config file " + file.getPath() + " is not exists!");
				}
				
			}else {
				File file = new File(DEFAULT_UNIX_CONF_FILE);
				if(!file.exists()) {
					throw new RuntimeException("[Error]config file " + DEFAULT_UNIX_CONF_FILE + " is not exists!");
				}
			}
		}
	}
	
	
	private void log(List<String> msgList) {
		for (int i = 0; i < msgList.size(); i++) {
			String msg = msgList.get(i);
			log(msg);
		}
		
	}
	
	private void log(String str) {
		System.out.println(str);
		if(logWriter == null) {
			if(zipFile == null) {
				return;
			}
			try {
				String name = zipFile.getName();
				int lastIndexOf = name.lastIndexOf(".");
				if(lastIndexOf > 0) {
					name = name.substring(0,lastIndexOf);
				}
				logFile = new File(zipFile.getParent(),name + "_" + time + ".log");
				FileWriter fileWriter = new FileWriter(logFile);
				logWriter = new BufferedWriter(fileWriter);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(logWriter != null) {
			try {
				logWriter.write(str);
				logWriter.newLine();
				logWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void report(String str) {
		if(reportWriter == null) {
			if(zipFile == null) {
				return;
			}
			try {
				String name = zipFile.getName();
				int lastIndexOf = name.lastIndexOf(".");
				if(lastIndexOf > 0) {
					name = name.substring(0,lastIndexOf);
				}
				reportFile = new File(zipFile.getParent(),name + "_" + time + ".report.txt");
				FileWriter fileWriter = new FileWriter(reportFile);
				reportWriter = new BufferedWriter(fileWriter);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(reportWriter != null) {
			try {
				reportWriter.write(str);
				reportWriter.newLine();
				reportWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	private static void lastFinally() {
		if(logFile != null) {
			System.out.println("日志文件: " + logFile.getAbsolutePath());
		}
		
		if(reportFile != null) {
			System.out.println("报告文件: " + reportFile.getAbsolutePath());
		}
		
		if(logWriter != null) {
			try {
				logWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(reportWriter != null) {
			try {
				reportWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
	}
	
	
	
    public static void main(String... args) {
    	App app = new App();
    	try {
    		 int exitCode = new CommandLine(app).execute(args);
    		 lastFinally();
    		 System.exit(exitCode);
		} catch (Exception e) {
			 e.printStackTrace();
			 StackTraceElement[] stackTrace = e.getStackTrace();
	         for (StackTraceElement element : stackTrace) {
	        	 app.log(element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
	         }
			 
			 lastFinally();
			 System.exit(1);
		}
    	
       
        
    }

}
