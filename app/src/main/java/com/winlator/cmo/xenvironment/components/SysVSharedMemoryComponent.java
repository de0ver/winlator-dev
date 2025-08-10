package com.winlator.cmo.xenvironment.components;

import com.winlator.cmo.sysvshm.SysVSHMConnectionHandler;
import com.winlator.cmo.sysvshm.SysVSHMRequestHandler;
import com.winlator.cmo.sysvshm.SysVSharedMemory;
import com.winlator.cmo.xconnector.UnixSocketConfig;
import com.winlator.cmo.xconnector.XConnectorEpoll;
import com.winlator.cmo.xenvironment.EnvironmentComponent;
import com.winlator.cmo.xserver.SHMSegmentManager;
import com.winlator.cmo.xserver.XServer;

public class SysVSharedMemoryComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    public final UnixSocketConfig socketConfig;
    private SysVSharedMemory sysVSharedMemory;
    private final XServer xServer;

    public SysVSharedMemoryComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        sysVSharedMemory = new SysVSharedMemory();
        connector = new XConnectorEpoll(socketConfig, new SysVSHMConnectionHandler(sysVSharedMemory), new SysVSHMRequestHandler());
        connector.start();

        xServer.setSHMSegmentManager(new SHMSegmentManager(sysVSharedMemory));
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }

        sysVSharedMemory.deleteAll();
    }
}
