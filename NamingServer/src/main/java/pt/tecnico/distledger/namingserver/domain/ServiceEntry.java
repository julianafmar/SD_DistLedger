package pt.tecnico.distledger.namingserver.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.*;

public class ServiceEntry {
    
    private String serviceName;

    private List<ServerEntry> servers = new ArrayList<>();

    private Integer id = 0;

    public ServiceEntry(String serviceName, List<ServerEntry> servers) {
        this.serviceName = serviceName;
        this.servers = servers;
    }

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<ServerEntry> getServers() {
        return servers;
    }

    public void setServers(List<ServerEntry> servers) {
        this.servers = servers;
    }

     public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void addServerEntry(ServerEntry serverEntry) {
        servers.add(serverEntry);
        id++;
    }

    public void removeServerEntry(String address) {
        servers = servers.stream().filter(server -> !(server.getAddress().equals(address))).collect(Collectors.toList());
    }

    public boolean existsServerEntry(String address, String qualifier) {

        if (qualifier == null)
            return servers.stream()
                    .filter(server -> server.getAddress().equals(address))
                    .collect(Collectors.toList()).size() > 0;
        else if (address == null)
            return servers.stream()
                .filter(server -> server.getQualifier().equals(qualifier))
                .collect(Collectors.toList()).size() > 0;
        else
            return servers.stream()
                    .filter(server -> server.getAddress().equals(address) && server.getQualifier().equals(qualifier))
                    .collect(Collectors.toList()).size() > 0;
    }

    @Override
    public String toString() {
        return "ServiceEntry{serviceName=" + getServiceName() 
                    + ", servers=" + getServers() + "}";
    }
}