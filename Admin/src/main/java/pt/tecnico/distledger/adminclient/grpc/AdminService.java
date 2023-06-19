package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;

public class AdminService {

    public ManagedChannel channel;
    public final ManagedChannel namingChannel;
    public NamingServerServiceGrpc.NamingServerServiceBlockingStub namingStub;
    public AdminServiceGrpc.AdminServiceBlockingStub stub;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}
    
    public AdminService() {

        debug("Create channel");
        
        namingChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        namingStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);
        
    }

    private AdminServiceGrpc.AdminServiceBlockingStub lookupServer(String server) {

        LookupResponse lookupResponse = namingStub.lookup(LookupRequest.newBuilder()
                                                        .setServiceName("DistLedger")
                                                        .setQualifier(server)
                                                        .build());
        
        if (lookupResponse.getServerCount() > 0) {

            channel = ManagedChannelBuilder
                                    .forTarget(lookupResponse.getServer(0).getAddress())
                                    .usePlaintext()
                                    .build();
                                    
            stub = AdminServiceGrpc.newBlockingStub(channel);

            return stub;
            
        }
        return null;
    }

    public String activate(String server) {

        int trials = 0;
        AdminServiceGrpc.AdminServiceBlockingStub stub;

        while(trials < 3) {

            try{
                debug("Call activate");
                stub = lookupServer(server);
                if(stub == null) 
                    return "No server available";
                    
                stub.activate(ActivateRequest.getDefaultInstance());
                return "OK\n";
            }
            catch (StatusRuntimeException sre) {
                return "Caught exception with description: " 
                        + sre.getStatus().getDescription() 
                        + "\n";
            }
            catch (IllegalArgumentException iae) {
                return iae.getMessage();
            }
            
        }

        return "The operation was not successful, try again.\n";
    }

    public String deactivate(String server) {
        int trials = 0;
        AdminServiceGrpc.AdminServiceBlockingStub stub;

        while(trials < 3) {

            try{

                debug("Call deactivate");

                stub = lookupServer(server);
                if(stub == null)
                    return "No server available";

                stub.deactivate(DeactivateRequest.getDefaultInstance());

                return "OK\n";

            }
            
            catch (StatusRuntimeException sre) {

                if (sre.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    trials ++;
                    System.out.println("The server is unavailable");
                }
                else
                    return "Caught exception with description: " 
                            + sre.getStatus().getDescription() 
                            + "\n";
            }

            catch (IllegalArgumentException iae) {
                return iae.getMessage();
            }

        }

        return "The operation was not successful, try again.\n";
    }

    public String getLedgerState(String server) {
        
        int trials = 0;

        AdminServiceGrpc.AdminServiceBlockingStub stub;

        while(trials < 3) {

            try {

                debug("Call getLedgerState");

                stub = lookupServer(server);
                if (stub == null)
                    return "No server available";

                getLedgerStateResponse ledgerState = stub.getLedgerState(getLedgerStateRequest.getDefaultInstance());

                return "OK\n" + ledgerState;

            } 

            catch (StatusRuntimeException sre) {
                return "Caught exception with description: " 
                        + sre.getStatus().getDescription() 
                        + "\n";
            }

            catch (IllegalArgumentException iae) {
                return iae.getMessage();
            }

        }

        return "The operation was not successful, try again.\n";
    }

    public String gossip(String server) {
            
            int trials = 0;
    
            AdminServiceGrpc.AdminServiceBlockingStub stub;
    
            while(trials < 3) {
    
                try {
    
                    debug("Call gossip");
    
                    stub = lookupServer(server);
                    if (stub == null)
                        return "No server available";
    
                    stub.gossip(GossipRequest.getDefaultInstance());

                    return "OK\n";
    
                } 
    
                catch (StatusRuntimeException sre) {
                    return "Caught exception with description: " 
                        + sre.getStatus().getDescription() 
                        + "\n";
                }
    
                catch (IllegalArgumentException iae) {
                    return "Caught exception with message: " 
                        + iae.getMessage()
                        + "\n";
                }
    
            }
    
            return "The operation was not successful, try again.\n";
            
    }

    public void exit() {
        
        debug("Call exit");

        if(channel != null)
            channel.shutdownNow();
        namingChannel.shutdownNow();

        debug("The exit was successful");
        
    }

}
