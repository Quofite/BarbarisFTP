package org.barbaris.ftpclient.services;

import org.barbaris.ftpclient.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService implements IUserService {
    @Autowired
    private JdbcTemplate template;

    @Override
    public String login(UserModel user) {
        String sql = String.format("SELECT * FROM ftpusers WHERE name='%s' AND password='%s';", user.getName(), user.getPassword());

        try {
            List<Map<String, Object>> rows = template.queryForList(sql);
            if(rows.size() == 1) {
                return "OK";
            } else {
                return "NO USER";
            }
        } catch (Exception ex) {
            return "ERR";
        }
    }

    @Override
    public String register(UserModel user) {
        String sql = String.format("SELECT * FROM ftpusers WHERE name='%s';", user.getName());
        List<Map<String, Object>> rows = template.queryForList(sql);

        if(rows.size() > 0) {
            return "EXISTS";
        }

        sql = String.format("INSERT INTO ftpusers(name, password) VALUES ('%s', '%s');", user.getName(), user.getPassword());

        try {
            template.execute(sql);
        } catch (Exception ex) {
            return "COULD NOT CREATE USER";
        }

        return "OK";
    }
}
