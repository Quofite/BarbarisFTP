package org.barbaris.ftpclient.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.barbaris.ftpclient.models.FTPServerData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelpingMethodsController {

    // -------------- STATIC FINAL FIELDS AND CONSTANTS

    private static final String[] imagesResolutions = {"png", "jpg", "jpeg", "bmp", "svg", "ico", "tif", "gif", "psd", "xcf"};
    private static final String[] executableResolutions = {"exe", "com", "sh", "drv", "sys", "dll", "deb"};
    private static final String[] videosResolutions = {"mp4", "avi", "mpeg", "mov", "mkv"};
    private static final String[] audioResolutions = {"mp3", "wav", "ogg", "flac"};
    private static final String[] codeResolutions = {"py", "java", "c", "cpp", "go", "php", "cs", "pas", "r", "html", "js", "css", "sql"};
    private static final String[] textResolutions = {"txt", "doc", "docx", "pdf", "rtf"};
    private static final String[] archivesResolutions = {"zip", "rar", "7z", "tar", "gz", "jar", "war"};


    // ----------- METHOD THAT CHOOSES PROPER ICON FOR FILES WITH DIFFERENT RESOLUTIONS
    public static String getFileImage(String res) {
        for (String resolution: imagesResolutions) {
            if(res.equals(resolution)) {
                return "img.png";
            }
        }

        for (String resolution: executableResolutions) {
            if(res.equals(resolution)) {
                return "exec.png";
            }
        }

        for (String resolution: videosResolutions) {
            if(res.equals(resolution)) {
                return "vid.png";
            }
        }

        for (String resolution: audioResolutions) {
            if(res.equals(resolution)) {
                return "audio.png";
            }
        }

        for (String resolution: codeResolutions) {
            if(res.equals(resolution)) {
                return "code.png";
            }
        }

        for (String resolution: textResolutions) {
            if(res.equals(resolution)) {
                return "text.png";
            }
        }

        for (String resolution: archivesResolutions) {
            if(res.equals(resolution)) {
                return "arch.png";
            }
        }

        return "default.png";
    }

    // DOWNLOAD FROM FTP SERVER METHOD
    public static boolean download(FTPClient client, String files, String name) {
        try {
            String dir = "/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/";
            Path path = Paths.get(dir);

            if(!Files.exists(path)) {
                Files.createDirectories(path);
            } else {
                FileUtils.cleanDirectory(new File(dir));
            }

            String[] fileNames = files.split(";");
            String[] remoteFileNames = client.listNames();

            for (String fileName : fileNames) {
                boolean isExists = false;
                for (String remoteFileName : remoteFileNames) {
                    if(fileName.equals(remoteFileName)) {
                        isExists = true;
                        break;
                    }
                }

                if(!isExists) {
                    return false;
                }
            }

            for (String remoteFile : fileNames) {
                File downloaded = new File("/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/" + remoteFile);

                OutputStream download = new BufferedOutputStream(new FileOutputStream(downloaded));
                client.retrieveFile(remoteFile, download);

                download.flush();
                download.close();
            }
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    // UPLOAD TO FTP SERVER METHOD
    public static boolean upload(String name, FTPClient client, String fileName) {

        try {
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);


            // saving file into ftp server
            File localFile = new File("/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/" + fileName);
            InputStream stream = new FileInputStream(localFile);
            boolean success = client.storeFile(fileName, stream);
            stream.close();

            if(success) {
                return true;
            }

        } catch (Exception ex) {
            System.out.println(ex.getCause().toString());
            return false;
        }

        return false;
    }


    public static void userNameFileDebuger(String name) {
        String dir = "/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name;
        Path path = Paths.get(dir);

        try {
            if(Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception ignored) {}
    }
}



















