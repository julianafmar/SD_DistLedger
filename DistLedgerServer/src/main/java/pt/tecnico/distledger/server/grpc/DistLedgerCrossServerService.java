package pt.tecnico.distledger.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.FAILED_PRECONDITION;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.RegisterResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossNamingServer;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.ArrayList;

public class DistLedgerCrossServerService {

    private String qualifier;

    public final ManagedChannel namingChannel;
    public NamingServerServiceGrpc.NamingServerServiceBlockingStub namingStub;

    public ManagedChannel channel1;
    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub1;

    public ManagedChannel channel2;
    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub2;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}
    
    public DistLedgerCrossServerService(String qualifier){
        this.qualifier = qualifier;
        namingChannel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        debug("Created naming channel");
        namingStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);
        debug("Created naming stub\n");
        channel1 = null;
        stub1 = null;
        channel2 = null;
        stub2 = null;
    }

    public boolean propagateState(List<DistLedgerCommonDefinitions.Operation> list, List<Integer> replicaTS) throws StatusRuntimeException {
        int num = 0;
        if (stub1 == null || stub2 == null) {
            LookupResponse response = namingStub.lookup(LookupRequest.newBuilder().setServiceName("DistLedger").build());
            if (response.getServerCount() <= 1)
                throw new StatusRuntimeException(FAILED_PRECONDITION.withDescription("Operation canceled"));
            else {
                List<CrossNamingServer.ServerEntry> servers = new ArrayList<>();
                servers.addAll(response.getServerList());
                servers.removeIf(server -> server.getQualifier().equals(qualifier));
                if(servers.size() > 1) { 
                    num++;
                    channel2 = ManagedChannelBuilder.forTarget(servers.get(1).getAddress()).usePlaintext().build();
                    stub2 = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel2);
                }
                num++;
                channel1 = ManagedChannelBuilder.forTarget(servers.get(0).getAddress()).usePlaintext().build();
                stub1 = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel1);
            }
        }
        boolean exit = true;
        DistLedgerCommonDefinitions.LedgerState ledgerState = DistLedgerCommonDefinitions.LedgerState.newBuilder().addAllLedger(list).build();
        for(int i = 0; i < num; i++) {
            if (i == 0) {
                try {
                    stub1.propagateState(PropagateStateRequest.newBuilder().setState(ledgerState).addAllReplicaTS(replicaTS).build());
                    debug("propagateState executed successfuly\n");
                } catch (StatusRuntimeException sre) {
                    exit = false;
                } 
                continue;
            }
            
            else if(i == 1) {
                try {
                    stub2.propagateState(PropagateStateRequest.newBuilder().setState(ledgerState).addAllReplicaTS(replicaTS).build());
                    debug("propagateState executed successfuly\n");
                } catch (StatusRuntimeException sre) {
                    exit = false;
                } 
            }
            
        }
        return exit;
    }

    public Integer register(String address, String qualifier) {
        debug("Call register");
        try {
            RegisterResponse response = namingStub.register(RegisterRequest.newBuilder().setAddress(address)
                                .setQualifier(qualifier).setServiceName("DistLedger").build());
            debug("register executed successfuly\n");
            return response.getId();
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
            System.exit(-1);
        }
        return -1;
    }

    public void delete(String address) {
        debug("Call delete");
        try{
            namingStub.delete(DeleteRequest.newBuilder().setServiceName("DistLedger").setAddress(address).build());
            debug("delete executed successfuly\n");
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

}
