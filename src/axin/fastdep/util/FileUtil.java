package axin.fastdep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    		System.out.println("[Info]清除过时的解压目录 " + targetFolder.getPath());
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
    
    public static String calculateMD5(String filePath) throws IOException, NoSuchAlgorithmException {
		File file = new File(filePath);
        return calculateMD5(file);
    }
	
	
	public static String calculateMD5(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = fis.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }

        fis.close();
        byte[] digestBytes = md.digest();
        BigInteger bigInt = new BigInteger(1, digestBytes);
        String string = bigInt.toString(16);
        while (string.length() < 32) {
        	string = "0" + string;
        }
        return string;
        
        
        
    }

}
