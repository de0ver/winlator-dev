package com.winlator.cmo.xserver.extensions;

import com.winlator.cmo.xconnector.XInputStream;
import com.winlator.cmo.xconnector.XOutputStream;
import com.winlator.cmo.xserver.XClient;
import com.winlator.cmo.xserver.errors.XRequestError;

import java.io.IOException;

public interface Extension {
    String getName();

    byte getMajorOpcode();

    byte getFirstErrorId();

    byte getFirstEventId();

    void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
