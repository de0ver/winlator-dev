package com.winlator.cmo.sysvshm;

import com.winlator.cmo.xconnector.Client;
import com.winlator.cmo.xconnector.RequestHandler;
import com.winlator.cmo.xconnector.XInputStream;
import com.winlator.cmo.xconnector.XOutputStream;
import com.winlator.cmo.xconnector.XStreamLock;

import java.io.IOException;

public class SysVSHMRequestHandler implements RequestHandler {
    @Override
    public boolean handleRequest(Client client) throws IOException {
        SysVSharedMemory sysVSharedMemory = (SysVSharedMemory)client.getTag();
        XInputStream inputStream = client.getInputStream();
        XOutputStream outputStream = client.getOutputStream();

        if (inputStream.available() < 5) return false;
        byte requestCode = inputStream.readByte();

        switch (requestCode) {
            case RequestCodes.SHMGET: {
                long size = inputStream.readUnsignedInt();
                int shmid = sysVSharedMemory.get(size);

                try (XStreamLock lock = outputStream.lock()) {
                    outputStream.writeInt(shmid);
                }
                break;
            }
            case RequestCodes.GET_FD: {
                int shmid = inputStream.readInt();

                try (XStreamLock lock = outputStream.lock()) {
                    outputStream.writeByte((byte)0);
                    outputStream.setAncillaryFd(sysVSharedMemory.getFd(shmid));
                }
                break;
            }
            case RequestCodes.DELETE: {
                int shmid = inputStream.readInt();
                sysVSharedMemory.delete(shmid);
                break;
            }
        }
        return true;
    }
}
