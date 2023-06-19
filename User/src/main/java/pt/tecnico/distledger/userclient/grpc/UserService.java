package pt.tecnico.distledger.userclient.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;

public class UserService {

    public ManagedChannel channel;
    public UserServiceGrpc.UserServiceBlockingStub stub;
    public final ManagedChannel namingChannel;
    public NamingServerServiceGrpc.NamingServerServiceBlockingStub namingStub;
    private List<Integer> timestamp = new ArrayList<>();
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public UserService() {
        debug("Create channel to communicate with naming server");
        namingChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        namingStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);

        timestamp.add(0);
        timestamp.add(0);
        timestamp.add(0);
    }

    private UserServiceGrpc.UserServiceBlockingStub lookupServer(String server) {
        debug("Lookup server (" + server +")");            
        LookupResponse lookupResponse = namingStub.lookup(LookupRequest.newBuilder()
                                                        .setServiceName("DistLedger")
                                                        .setQualifier(server)
                                                        .build());
        
        if (lookupResponse.getServerCount() > 0) {
            
            channel = ManagedChannelBuilder
                        .forTarget(lookupResponse.getServer(0).getAddress())
                        .usePlaintext()
                        .build();

            stub = UserServiceGrpc.newBlockingStub(channel);
            
            return stub;

        }
        
        return null;
        
    }

    public String balance(String server, String userId) {

        int trials = 0;

        UserServiceGrpc.UserServiceBlockingStub stub;

        while(trials < 3) {

            try {

                debug("Call balance");

                stub = lookupServer(server);
                if (stub == null)
                    return "No server available";

                BalanceResponse response = stub.balance(BalanceRequest.newBuilder().setUserId(userId).addAllPrevTS(timestamp).build());
                timestamp.clear();
                timestamp.addAll(response.getValueTSList());
                
                return "OK\n" + response.getValue();

            }
            
            catch (StatusRuntimeException sre) {

                if (sre.getStatus().getCode() == Status.Code.UNAVAILABLE){
                    trials++;
                    System.out.println("Caught exception with description: " 
                            + sre.getStatus().getDescription() 
                            + "\n");
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

    public String createAccount(String server, String userId) {

        int trials = 0;
        
        UserServiceGrpc.UserServiceBlockingStub stub;

        while(trials < 3) {
            
            try {
                
                debug("Call createAccount");
                
                stub = lookupServer(server);
                if (stub == null)
                    return "No server available";

                CreateAccountResponse response = stub.createAccount(CreateAccountRequest.newBuilder().setUserId(userId).addAllPrevTS(timestamp).build());
                timestamp.clear();
                timestamp.addAll(response.getTSList());
                
                return "OK\n";
                
            }

            catch (StatusRuntimeException sre) {

                if (sre.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    trials++;
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

    public String transferTo(String server, String accountFrom, String accountTo, Integer amount) {

        int trials = 0;
        
        UserServiceGrpc.UserServiceBlockingStub stub;

        while(trials < 3) {
            
            try {

                debug("Call transferTo");
                
                stub = lookupServer(server);
                if (stub == null)
                    return "No server available";
                    
                TransferToResponse response = stub.transferTo(TransferToRequest.newBuilder()
                                .setAccountFrom(accountFrom).setAccountTo(accountTo)
                                .setAmount(amount).addAllPrevTS(timestamp).build());

                timestamp.clear();
                timestamp.addAll(response.getTSList());

                return "OK\n";
                
            }

            catch (StatusRuntimeException sre) {

                if(sre.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                    trials++;
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

    public void exit() {
        
        debug("Call exit");
        
        if(channel != null)
            channel.shutdownNow();
        namingChannel.shutdownNow();

        debug("The exit was successful");
        
    }
}
