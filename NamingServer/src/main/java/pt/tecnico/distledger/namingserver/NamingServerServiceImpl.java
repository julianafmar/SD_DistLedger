package pt.tecnico.distledger.namingserver;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.ALREADY_EXISTS;
import static io.grpc.Status.NOT_FOUND;

import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.tecnico.distledger.namingserver.domain.ServiceEntry;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.RegisterResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.DeleteResponse;

import java.util.stream.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase{
    private NamingServer namingServer;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public NamingServerServiceImpl(NamingServer namingServer) {
        this.namingServer = namingServer;
    }

    public synchronized void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        debug("Execute register");

        Map<String, ServiceEntry> services = namingServer.getServices();
        if (!services.containsKey(request.getServiceName())) {
            ServiceEntry service = new ServiceEntry(request.getServiceName());
            service.addServerEntry(new ServerEntry(request.getAddress(), request.getQualifier()));
            namingServer.addService(request.getServiceName(), service);

            RegisterResponse response = RegisterResponse.newBuilder().setId(service.getId() - 1).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            debug("register executed successfuly\n");
        }

        else if (services.containsKey(request.getServiceName())) {
            if(services.get(request.getServiceName()).existsServerEntry(request.getAddress(), request.getQualifier())) {
                responseObserver.onError(ALREADY_EXISTS
                                        .withDescription("Not possible to register the server")
                                        .asRuntimeException());
            }
            else{
                ServerEntry entry = new ServerEntry(request.getAddress(), request.getQualifier());
                services.get(request.getServiceName()).addServerEntry(entry);
                RegisterResponse response = RegisterResponse.newBuilder().setId(services.get(request.getServiceName()).getId() - 1).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                debug("register executed successfuly\n");
            }
        }
    }
    
    public synchronized void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        debug("Execute lookup");
        LookupResponse response;
        Map<String, ServiceEntry> services = namingServer.getServices();
        List<CrossNamingServer.ServerEntry> servers = new ArrayList<>();

        if (services.containsKey(request.getServiceName())) {
            // if the service exists
            ServiceEntry service = services.get(request.getServiceName());
            if (request.getQualifier().equals("")){
                // if the qualifier is not given
                servers = service.getServers().stream()
                        .map(server -> CrossNamingServer.ServerEntry.newBuilder()
                                        .setAddress(server.getAddress())
                                        .setQualifier(server.getQualifier())
                                        .build())
                        .collect(Collectors.toList());
            }
            
            else if (service.existsServerEntry(null, request.getQualifier())){
                // if the qualifier exists
                servers = service.getServers().stream()
                        .filter(server -> server.getQualifier().equals(request.getQualifier()))
                        .map(server -> CrossNamingServer.ServerEntry.newBuilder()
                                        .setAddress(server.getAddress())
                                        .setQualifier(server.getQualifier())
                                        .build())
                        .collect(Collectors.toList());
            }

        }
        
        response = LookupResponse.newBuilder().addAllServer(servers).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        debug("lookup executed successfuly\n");
    }

    public synchronized void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        debug("Execute delete");
        Map<String, ServiceEntry> services = namingServer.getServices();

        if (services.containsKey(request.getServiceName())) {
            // if the service exists
            ServiceEntry service= services.get(request.getServiceName());
            if (service.existsServerEntry(request.getAddress(), null)){
                service.removeServerEntry(request.getAddress());
                responseObserver.onNext(null);
                responseObserver.onCompleted();
                debug("delete executed successfuly\n");
            }
            else
                responseObserver.onError(NOT_FOUND
                                        .withDescription("Not possible to remove the server")
                                        .asRuntimeException());
        }
        else
            responseObserver.onError(NOT_FOUND
                                    .withDescription("Not possible to remove the server")
                                    .asRuntimeException());
    }

}
