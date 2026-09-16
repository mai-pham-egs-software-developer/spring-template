package com.my.craft.security.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.my.craft.security.service.OrganizationMemberNotFoundException;
import com.my.craft.security.service.OrganizationNotFoundException;
import com.my.craft.security.service.RoleNotFoundException;
import com.my.craft.security.service.UserNotFoundException;

/** Covers every controller under {@code com.my.craft.security.web} (including {@code
 * .operator}) -- mirrors file-storage's {@code FileStorageExceptionHandler}. */
@RestControllerAdvice(basePackages = "com.my.craft.security.web")
public class SecurityWebExceptionHandler {

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ProblemDetail handleOrganizationNotFound(OrganizationNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ProblemDetail handleRoleNotFound(RoleNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OrganizationMemberNotFoundException.class)
    public ProblemDetail handleOrganizationMemberNotFound(OrganizationMemberNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
