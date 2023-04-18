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
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class MainController {

    // INITIALIZING INSTANCES
    private FTPClient client;
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

        if(name == null) {
            return "redirect:/login";
        }

        HelpingMethodsController.userNameFileDebuger(name);
        model.addAttribute("name", name);
        ArrayList<FileModel> files = new ArrayList<>();

        try {
            FTPFile[] ftpFiles = client.listFiles();  // getting all user's files

            for(FTPFile file : ftpFiles) {      // and putting it all to the html file
                FileModel fileModel = new FileModel();

                String fName = file.getName();
                String[] splited = fName.split("\\.");
                String resolution = splited[splited.length - 1];
                fileModel.setFileName(fName);
                fileModel.setResolution(resolution);

                fileModel.setImageType(HelpingMethodsController.getFileImage(resolution));

                files.add(fileModel);
            }

        } catch (IOException ex) {
            model.addAttribute("is_hidden", false);
            model.addAttribute("error_message", "Не удалось подключиться к файловому серверу");
            System.out.println(ex.getMessage());
            return "login";
        }


        model.addAttribute("files", files);
        model.addAttribute("is_hidden", true);
        model.addAttribute("error_message", "-");
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
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/");
            return new ResponseEntity<String>(headers, HttpStatus.FOUND);
        }

        // block that response to user with zip file
        try {
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(outputZip.toPath()));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputZip.getName());
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            for (String fileName : fileNames) {
                try {
                    FileUtils.delete(new File(path + fileName));
                } catch (Exception ex) {
                    HttpHeaders errHeaders = new HttpHeaders();
                    headers.add("Location", "/");
                    return new ResponseEntity<String>(errHeaders, HttpStatus.FOUND);
                }
            }

            return ResponseEntity.ok().headers(headers).contentLength(outputZip.length()).contentType(MediaType.MULTIPART_MIXED).body(resource);

        } catch (Exception ex) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/");
            return new ResponseEntity<String>(headers, HttpStatus.FOUND);
        }
    }

    @GetMapping("/downloadFtp")     // method that downloads files to the CLIENT server
    public ResponseEntity downloadFtp(@RequestParam String files, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");

        HelpingMethodsController.userNameFileDebuger(name);

        // connecting to server
        try {
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            client.setControlEncoding("UTF-8");
        } catch (Exception ex) {
            return new ResponseEntity<String>(HttpStatus.GATEWAY_TIMEOUT);
        }

        // custom downloading method
        if(HelpingMethodsController.download(client, files, name)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/");
            return new ResponseEntity<String>(headers, HttpStatus.FOUND);
        } else {
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------- FILES UPLOADING METHODS

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, HttpServletRequest request, Model model) {
        // Session check
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute("name");

        if(name == null) {
            return "redirect:/login";
        }

        HelpingMethodsController.userNameFileDebuger(name);

        // getting and setting file info
        String fileName = file.getOriginalFilename();
        String dir = "/home/gleb/Coding/FTP/FTPClient/src/main/resources/static/files/" + name + "/";
        Path path = Paths.get(dir + "/");
        try {
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            } else {
                FileUtils.cleanDirectory(new File(dir));
            }
        } catch(Exception ex) {
            model.addAttribute("error_message", "Не получилось создать Вашу директорию на сервере");
            return "error";
        }

        try {
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // saving uploaded files into client directory
            try(InputStream stream = file.getInputStream()) {
                Path filePath;

                if(fileName != null) {
                    filePath = path.resolve(fileName);
                } else {
                    return "error";
                }

                FileUtils.cleanDirectory(new File(dir));
                Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                model.addAttribute("error_message", "Ошибка копирования файла на сервер");
                return "error";
            }
        } catch (Exception ex) {
            model.addAttribute("error_message", "Ошибка копирования файла на сервер");
            return "error";
        }

        // streaming that file into ftp server via custom method
        if(HelpingMethodsController.upload(name, client, fileName)) {
            try {
                FileUtils.cleanDirectory(new File(dir));
            } catch (Exception ignored) {}
            return "redirect:/";
        } else {
            try {
                FileUtils.cleanDirectory(new File(dir));
            } catch (Exception ignored) {}
            model.addAttribute("error_message", "Ошибка копирования файла на файловый сервер");
            return "error";
        }
    }


    // --------------------------------------------- LOGIN AND LOGOUT METHODS

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, Model model) {
        /*
        *   basically all it does is removing
        *   session attributes with ftp client's
        *   name and password and then redirecting
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
        // getting session data and request parameters from login form
        HttpSession session = request.getSession();
        UserModel user = new UserModel();
        user.setName(name);
        user.setPassword(password);

        // login attempt
        String response = service.login(user);

        if(response.equals("OK")) {
            // if OK than setting session attributes
            session.setAttribute("name", name);
            session.setAttribute("password", password);

            // and creating FTP connection
            client = new FTPClient();
            FTPServerData data = new FTPServerData();

            try {
                client.connect(data.getUrl(), data.getPort());
                client.setControlEncoding("UTF-8");
                int ftpResponse = client.getReplyCode();

                if(!FTPReply.isPositiveCompletion(ftpResponse)) {
                    model.addAttribute("is_hidden", false);
                    model.addAttribute("error_message", "Внутренняя ошибка сервера");
                    return "login";
                }

                try {
                    if(client.login(name, password)) {
                        return "redirect:/";
                    }
                } catch (Exception ex) {
                    model.addAttribute("is_hidden", false);
                    model.addAttribute("error_message", "Не удалось подключиться к серверному аккаунту");
                    return "login";
                }
            } catch (Exception e) {
                model.addAttribute("is_hidden", false);
                model.addAttribute("error_message", "Не удалось подключиться к файловому серверу");
                return "login";
            }

            model.addAttribute("is_hidden", false);
            model.addAttribute("error_message", "Не удалось подключиться к файловому серверу");
            return "login";

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

























