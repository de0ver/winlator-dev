package com.winlator.cmo.xserver.errors;

public class BadIdChoice extends XRequestError {
    public BadIdChoice(int id) {
        super(14, id);
    }
}
