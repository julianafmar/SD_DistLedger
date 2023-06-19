package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.tecnico.distledger.server.domain.ServerState;

import io.grpc.StatusRuntimeException;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private ServerState st;

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public UserServiceImpl(ServerState st) {
        debug("Create UserServiceImpl");
        this.st = st;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        debug("Execute createAccount");

        try {
            st.newAccount(request.getUserId(), request.getPrevTSList(), st.getId());
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                                        .addAllTS(request.getPrevTSList())
                                        .setTS(st.getId(), st.getReplicaTS()
                                        .get(st.getId())).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException());
        }
    }

    @Override 
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        debug("Execute balance");

        try {
            BalanceResponse response = BalanceResponse.newBuilder()
                                            .setValue(st.balance(request.getUserId(), request.getPrevTSList()))
                                            .addAllValueTS(request.getPrevTSList())
                                            .setValueTS(st.getId(), st.getReplicaTS()
                                            .get(st.getId())).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException()); 
        }
    }

    @Override 
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        debug("Execute transferTo");
        
        try {
            st.transferTo(request.getAccountFrom(), request.getAccountTo(), request.getAmount(), request.getPrevTSList(), st.getId());
            TransferToResponse response = TransferToResponse.newBuilder().addAllTS(request.getPrevTSList())
                                                .setTS(st.getId(), st.getReplicaTS().get(st.getId())).build();    

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e.getStatus().asRuntimeException());
        }
    }
}
