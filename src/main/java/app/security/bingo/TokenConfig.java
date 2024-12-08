package app.security.bingo;

import app.utils.Utils;

public class TokenConfig {

    public static final String SECRET_KEY = System.getenv("SECRET_KEY") != null 
        ? System.getenv("SECRET_KEY") 
        : Utils.getPropertyValue("SECRET_KEY", "config.properties");

    public static final String ISSUER = System.getenv("ISSUER") != null 
        ? System.getenv("ISSUER") 
        : Utils.getPropertyValue("ISSUER", "config.properties");

    public static final long TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME") != null 
        ? Long.parseLong(System.getenv("TOKEN_EXPIRE_TIME")) 
        : Long.parseLong(Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties"));
}
