package pt.tecnico.distledger.namingserver.domain;

import java.util.HashMap;
import java.util.Map;

public class NamingServer {

    private Map<String, ServiceEntry> services = new HashMap<>();

    public NamingServer() {
    }

    public NamingServer(Map<String, ServiceEntry> services) {
        this.services = services;
    }

    public Map<String, ServiceEntry> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceEntry> services) {
        this.services = services;
    }

    public void addService(String serviceName, ServiceEntry serviceEntry) {
        services.put(serviceName, serviceEntry);
    }

    public void removeService(String serviceName) {
        services.remove(serviceName);
    }

    public String toString() {
        return "NamingServer{" + services + "}";
    }

}