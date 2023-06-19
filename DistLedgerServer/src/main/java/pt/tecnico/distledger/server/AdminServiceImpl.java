package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.UNAVAILABLE;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private ServerState st;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public AdminServiceImpl(ServerState st) {
        debug("Create AdminServiceImpl");
        this.st = st;
    }

    @Override
    public synchronized void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        debug("Execute activate");
        st.setActive(true);
        responseObserver.onNext(null);
        responseObserver.onCompleted();
    }
    
    @Override
    public synchronized void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        debug("Execute deactivate");
        if (!st.isActive()) {
            responseObserver.onError(UNAVAILABLE.withDescription("The server is not available").asRuntimeException());
        }
        st.setActive(false);
        responseObserver.onNext(null);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        debug("Execute getLedgeState");
        DistLedgerCommonDefinitions.LedgerState ledgerState = DistLedgerCommonDefinitions.LedgerState.newBuilder().addAllLedger(st.getState()).build();
        getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        debug("Execute gossip");
        if (!st.isActive()) {
            responseObserver.onError(UNAVAILABLE.withDescription("The server is not available").asRuntimeException());
        }

        else {
          
            boolean aux = st.getCrossService().propagateState(st.getState(), st.getReplicaTS());
            if(aux) {
                responseObserver.onNext(null);
                responseObserver.onCompleted();
            }
            else {
                responseObserver.onError(UNAVAILABLE.withDescription("One or both servers are not active").asRuntimeException());
            }  
        }   
    }
}
