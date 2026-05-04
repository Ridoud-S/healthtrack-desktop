package com.itc.healthtrack.session;

import com.itc.healthtrack.model.User;

public class UserSession {

    private static volatile UserSession instance;
    private User loggedUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public void setLoggedUser(User user) {
        this.loggedUser = user;
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    public void cleanSession() {
        this.loggedUser = null;
    }
}