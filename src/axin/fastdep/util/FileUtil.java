package axin.fastdep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

public class FileUtil {
	
	/**
	 * 解压压缩包，返回目录对象
	 * @param zipFile
	 * @return
	 * @throws Exception
	 * @author haojiaxin <https://www.yuque.com/xinblog>
	 * @date 2024年8月18日 17:13:34
	 */
    public static File unzip(File zipFile) throws Exception {
    	int lastIndexOf = zipFile.getName().lastIndexOf(".");
    	if(!zipFile.getName().toLowerCase().endsWith(".zip")) {
    		throw new RuntimeException(zipFile.getPath() + "is not a file with zip suffix!");
    	}
    	String path = zipFile.getPath();
    	if(lastIndexOf > 0) {
    		lastIndexOf = path.lastIndexOf(".");
    		path = path.substring(0,lastIndexOf);
    	}
    	File targetFolder = new File(path);
    	if(targetFolder.exists()) {
    		System.out.println("清除旧的目录 => " + targetFolder.getPath());
    		FileUtils.deleteDirectory(targetFolder);
    	}
    	
    	
    	FileInputStream fileInputStream = new FileInputStream(zipFile);
    	
    	@SuppressWarnings("resource")
		ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        byte[] buffer = new byte[4096];
        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        	if(zipEntry.isDirectory()) {
        		continue;
        	}
            String fileName = zipEntry.getName();
            File newFile = new File(path + File.separator + fileName);

            // Create directories if needed
            new File(newFile.getParent()).mkdirs();

            // Write the extracted file to disk
        	@SuppressWarnings("resource")
			FileOutputStream fos = new FileOutputStream(newFile);
            int length;
            while ((length = zipInputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            zipInputStream.closeEntry();
        }
        targetFolder = new File(path);
        return targetFolder;
    }

}
