package com.example.tdspring.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DBException extends Exception {
    public DBException(String message) {
        super(message);
    }
}
