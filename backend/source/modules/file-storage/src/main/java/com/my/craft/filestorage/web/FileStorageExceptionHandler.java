package com.my.craft.filestorage.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.my.craft.filestorage.file.FileNotFoundException;

@RestControllerAdvice(basePackageClasses = FileStorageController.class)
public class FileStorageExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public ProblemDetail handleFileNotFound(FileNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
