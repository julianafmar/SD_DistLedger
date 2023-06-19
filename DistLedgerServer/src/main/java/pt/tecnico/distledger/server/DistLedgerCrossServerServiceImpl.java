package pt.tecnico.distledger.server;

import static io.grpc.Status.UNAVAILABLE;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateResponse;

import io.grpc.StatusRuntimeException;

public class DistLedgerCrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private ServerState st;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public DistLedgerCrossServerServiceImpl(ServerState st) {
        debug("Create DistLedgerCrossServerServiceImpl");
        this.st = st;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        debug("Execute propagateState");
        try {
            st.receiveOperations(request.getState().getLedgerList(), request.getReplicaTSList());
            responseObserver.onNext(null);
            responseObserver.onCompleted();
            debug("propagateState executed successfully\n");
        } catch (StatusRuntimeException e) {

            responseObserver.onError(UNAVAILABLE.withDescription("The server with id: " + st.getId() + " is not available").asRuntimeException());
        }
    }
    
}
