package com.winlator.cmo.xenvironment.components;

import com.winlator.cmo.alsaserver.ALSAClientConnectionHandler;
import com.winlator.cmo.alsaserver.ALSARequestHandler;
import com.winlator.cmo.xconnector.UnixSocketConfig;
import com.winlator.cmo.xconnector.XConnectorEpoll;
import com.winlator.cmo.xenvironment.EnvironmentComponent;

public class ALSAServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final UnixSocketConfig socketConfig;

    public ALSAServerComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new ALSAClientConnectionHandler(), new ALSARequestHandler());
        connector.setMultithreadedClients(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
    }
}
