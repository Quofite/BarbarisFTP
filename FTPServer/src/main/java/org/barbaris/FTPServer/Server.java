package org.barbaris.FTPServer;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Server {
    static FtpServerFactory server;
    static ListenerFactory factory;
    static PropertiesUserManagerFactory userManagerFactory;
    static DatabaseInfo data;


    // USER INITIALIZING METHOD
    public static void initUser(String name, String password) {
        UserManager userManager = userManagerFactory.createUserManager();

        BaseUser user = new BaseUser();
        user.setName(name);
        user.setPassword(password);
        user.setHomeDirectory("/home/gleb/Coding/FTP/FTPServer/src/main/resources/" + name + "/");

        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);

        try {
            userManager.save(user);
        } catch (Exception ignored) {}

        server.setUserManager(userManager);
    }

    public static void main(String[] args) {
        // INITIALIZING FACTORIES AND FIELDS

        server = new FtpServerFactory();
        factory = new ListenerFactory();

        factory.setPort(50000);
        server.addListener("default", factory.createListener());

        data = new DatabaseInfo();

        // BUILDING DATASOURCE

        BasicDataSource source = new BasicDataSource();
        source.setDriverClassName("org.postgresql.Driver");
        source.setUsername(data.getUser());
        source.setPassword(data.getPassword());
        source.setUrl(data.getUrl());

        // INITIALIZING USER MANAGER

        userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(new File("/home/gleb/Coding/FTP/FTPServer/src/main/resources/users.txt"));
        userManagerFactory.setPasswordEncryptor(new PasswordEncryptor() {
            @Override
            public String encrypt(String password) {
                return password;
            }

            @Override
            public boolean matches(String password, String storedPassword) {
                return password.equals(storedPassword);
            }
        });

        // INITIALIZING USERS HERE

        String sql = "SELECT * FROM ftpusers;";
        JdbcTemplate template = new JdbcTemplate(source);
        List<Map<String , Object>> rows = template.queryForList(sql);

        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            String pass = (String) row.get("password");

            System.out.println(name);
            System.out.println(pass);

            initUser(name, pass);
        }

        // -----------------------

        // CREATING FTPLET

        Map<String, Ftplet> map = new HashMap<>();
        map.put("miaFtplet", new Ftplet() {
            @Override
            public void init(FtpletContext ftpletContext) {
                System.out.println("Initialized in " + Thread.currentThread().getName());
            }

            @Override
            public void destroy() {
                System.out.println("Destroyed in " + Thread.currentThread().getName());
            }

            @Override
            public FtpletResult beforeCommand(FtpSession ftpSession, FtpRequest ftpRequest) {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult afterCommand(FtpSession ftpSession, FtpRequest ftpRequest, FtpReply ftpReply) {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onConnect(FtpSession ftpSession) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onDisconnect(FtpSession ftpSession) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }
        });

        // SETTING FTPLET AND STARTING SERVER

        server.setFtplets(map);
        FtpServer ftpServer = server.createServer();

        try {
            ftpServer.start();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}

























