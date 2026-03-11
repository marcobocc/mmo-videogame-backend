package io.mmo.authentication.business.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("User already exists");
    }
}