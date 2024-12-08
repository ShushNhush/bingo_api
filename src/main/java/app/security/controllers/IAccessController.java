package app.security.controllers;

import app.security.exceptions.NotAuthorizedException;
import io.javalin.http.Context;

public interface IAccessController {
    void accessHandler(Context ctx) throws NotAuthorizedException;
}
