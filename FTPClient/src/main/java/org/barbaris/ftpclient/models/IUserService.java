package org.barbaris.ftpclient.models;

public interface IUserService {
    String login(UserModel user);
    String register(UserModel user);

}
