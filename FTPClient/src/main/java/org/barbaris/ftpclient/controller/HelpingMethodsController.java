package org.barbaris.ftpclient.controller;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
    public static void download(FTPClient client, String files, String name) {
        try {
            Files.createDirectories(Paths.get("/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/"));

            String[] fileNames = files.split(";");

            for (String remoteFile : fileNames) {
                File downloaded = new File("/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/" + remoteFile);

                OutputStream download = new BufferedOutputStream(new FileOutputStream(downloaded));
                boolean success = client.retrieveFile(remoteFile, download);

                if (success) {
                    System.out.println("Downloaded " + remoteFile);
                }

                download.flush();
                download.close();

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            // TODO: нормальный вывод об ошибке
        }
    }
}
