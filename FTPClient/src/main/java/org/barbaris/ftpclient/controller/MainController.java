package org.barbaris.ftpclient.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.barbaris.ftpclient.models.FTPServerData;
import org.barbaris.ftpclient.models.FileModel;
import org.barbaris.ftpclient.models.UserModel;
import org.barbaris.ftpclient.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class MainController {
    private final UserService service;

    @Autowired
    public MainController(UserService service) {
        this.service = service;
    }


    // -------------------------------------------- GET MAIN PAGE

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {

        // Getting session data
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");
        String pass = (String) session.getAttribute("password");

        if(name == null) {
            return "redirect:/login";
        }

        model.addAttribute("name", name);

        // creating ftp server instances
        FTPClient client = new FTPClient();
        FTPServerData data = new FTPServerData();
        ArrayList<FileModel> files = new ArrayList<>();

        try {
            // connecting to ftp server
            client.connect(data.getUrl(), data.getPort());
            client.setControlEncoding("UTF-8");
            int response = client.getReplyCode();

            if(!FTPReply.isPositiveCompletion(response)) {
                model.addAttribute("is_hidden", false);
                model.addAttribute("error_message", "Внутренняя ошибка сервера");
                return "login";
            }

            if(client.login(name, pass)) {
                // getting the list of all files that user has...
                FTPFile[] ftpFiles = client.listFiles();

                // ... and putting it into the list that will be places in html file
                for(FTPFile file : ftpFiles) {
                    FileModel fileModel = new FileModel();

                    String fName = file.getName();
                    String[] splited = fName.split("\\.");
                    String resolution = splited[splited.length - 1];
                    fileModel.setFileName(fName);
                    fileModel.setResolution(resolution);

                    fileModel.setImageType(HelpingMethodsController.getFileImage(resolution));

                    files.add(fileModel);
                }
            } else {
                model.addAttribute("is_hidden", false);
                model.addAttribute("error_message", "Внутренняя ошибка сервера");
                return "login";
            }

        } catch (IOException ex) {
            model.addAttribute("is_hidden", false);
            model.addAttribute("error_message", "Внутренняя ошибка сервера");
            return "login";
        }


        model.addAttribute("files", files);

        return "index";
    }


    // --------------------------------------- FILES DOWNLOADING METHODS

    @GetMapping("/download")    // method that downloads files to USER'S device
    public ResponseEntity download(@RequestParam String files, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");

        String[] fileNames = files.split(";");

        // creating zip archive that would be returned as downloaded file
        File outputZip = new File("/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/download.zip");

        try {
            outputZip.createNewFile();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // path to the user's folder
        String path = "/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/";

        // block that stream files to zip archive
        try {
            byte[] buffer = new byte[1024];
            FileOutputStream output = new FileOutputStream(outputZip);
            ZipOutputStream zipStream = new ZipOutputStream(output);

            for (String fileName : fileNames) {
                File file = new File(path + fileName);
                FileInputStream inputStream = new FileInputStream(file);
                zipStream.putNextEntry(new ZipEntry(fileName));

                int length;

                while ((length = inputStream.read(buffer)) > 0) {
                    zipStream.write(buffer, 0, length);
                }

                zipStream.closeEntry();
                inputStream.close();
            }

            zipStream.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        // block that response to user with zip file
        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(outputZip));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputZip.getName());
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            for (String fileName : fileNames) {
                try {
                    FileUtils.delete(new File(path + fileName));
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

            return ResponseEntity.ok().headers(headers).contentLength(outputZip.length()).contentType(MediaType.MULTIPART_MIXED).body(resource);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/downloadFtp")     // method that downloads files to the CLIENT server
    public String downloadFtp(@RequestParam String files, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");
        String pass = (String) session.getAttribute("password");

        // ftp connection instances creation
        FTPClient client = new FTPClient();
        FTPServerData data = new FTPServerData();

        // connecting to server
        try {
            client.connect(data.getUrl(), data.getPort());
            client.login(name, pass);
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.setControlEncoding("UTF-8");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        // custom downloading method
        HelpingMethodsController.download(client, files, name);

        return "redirect:/";
    }


    // --------------------------------------------- LOGIN AND LOGOUT METHODS

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, Model model) {
        /*
        *   basically all it does is removing
        *   session attributes with ftp client's
        *   mane and password and then redirecting
        *   to login page
        *  */

        HttpSession session = request.getSession();
        session.removeAttribute("name");
        session.removeAttribute("password");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");

        if(name != null) {
            session.removeAttribute("name");
            session.removeAttribute("password");
        }

        model.addAttribute("is_hidden", true);
        model.addAttribute("error_message", "-");
        return "login";
    }

    @PostMapping("login")
    public String loginPost(@RequestParam String name, @RequestParam String password, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        UserModel user = new UserModel();
        user.setName(name);
        user.setPassword(password);

        String response = service.login(user);

        if(response.equals("OK")) {
            session.setAttribute("name", name);
            session.setAttribute("password", password);
            return "redirect:/";
        } else if(response.equals("NO USER")) {
            model.addAttribute("is_hidden", false);
            model.addAttribute("error_message", "Неправильный логин или пароль");
            return "login";
        } else {
            model.addAttribute("is_hidden", false);
            model.addAttribute("error_message", "Внутренняя ошибка сервера");
            return "login";
        }
    }


    // ---------------------------------------- REGISTER METHODS

    @GetMapping("/register")
    public String register(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();

        if(session.getAttribute("name") != null) {
            session.removeAttribute("name");
            session.removeAttribute("pass");
        }

        model.addAttribute("is_hidden", true);
        model.addAttribute("error_message", "-");
        return "register";
    }

}

























