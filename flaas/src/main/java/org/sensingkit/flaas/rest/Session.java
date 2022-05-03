package org.sensingkit.flaas.rest;

public interface Session {

    boolean isLoggedIn();

    void saveToken(String token);

    String getToken();

    void saveRefreshToken(String token);

    String getRefreshToken();

    void saveUsername(String username);

    String getUsername();

    void savePassword(String password);

    String getPassword();

    void invalidate();
}
