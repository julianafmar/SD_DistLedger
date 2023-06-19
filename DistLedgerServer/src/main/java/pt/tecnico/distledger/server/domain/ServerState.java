package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.grpc.DistLedgerCrossServerService;
import pt.tecnico.distledger.server.domain.client.User;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ServerState {

    private Map<Integer, List<Operation>> ledger = new HashMap<>();
    private Map<String, User> users = new HashMap<>();
    private List<Integer> valueTS = new ArrayList<>();
    private List<Integer> replicaTS = new ArrayList<>();
    private boolean active = true;
    private Integer id;

    private final DistLedgerCrossServerService crossService;
    

    public ServerState(String address, String qualifier) {
        User user = new User("broker");
        user.setBalance(1000);
        users.put("broker", user);
        crossService = new DistLedgerCrossServerService(qualifier);
        id = crossService.register(address, qualifier);
        if(id == -1) System.exit(-1);
        valueTS.add(0);
        valueTS.add(0);
        valueTS.add(0);
        replicaTS.add(0);
        replicaTS.add(0);
        replicaTS.add(0);
        ledger.put(0, new ArrayList<>());
        ledger.put(1, new ArrayList<>());
        ledger.put(2, new ArrayList<>());
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public Map<Integer, List<Operation>> getLedger() {
        return ledger;
    }

    public int getId() { 
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public List<Integer> getValueTS() {
        return valueTS;
    }

    public List<Integer> getReplicaTS() {
        return replicaTS;
    }

    public void updateValueTS(int index) {
        int x = valueTS.get(index) + 1;
        valueTS.set(index, x);
    }

    public void updateReplicaTS(int index) {
        int x = replicaTS.get(index) + 1;
        replicaTS.set(index, x);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public DistLedgerCrossServerService getCrossService() {
        return crossService;
    }

    public void addOperation(Operation op, int index) {
        ledger.get(index).add(op);
        updateReplicaTS(index);
        executePendingOp();
        if (!op.getStable()) {
            op.setTS(instableOrCanceled(index, -2));
        }
    }

    public synchronized void newAccount(String userId, List<Integer> prevTS, int index) {
        
        if (!this.isActive())
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Server is not active"));
            
        if(isUpdated(prevTS)) {
            CreateOp op = new CreateOp(userId, true, prevTS);
            Operation aux = updatedOperation(op, index);
            addOperation(aux, index);
        }

        else {
            addOperation(new CreateOp(userId, false, prevTS), index);
        }
    }

    public synchronized Integer balance(String userId, List<Integer> prevTS) {
        if (!this.isActive())
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Server is not active"));
        else if (!isUpdated(replicaTS)) {
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Server is not updated"));
        }
        else {
            return users.get(userId).getBalance();
        }
    }

    public synchronized void transferTo(String idOrigin, String idDest, int amount, List<Integer> prevTS, int index) throws StatusRuntimeException {
        if (!this.isActive())
            throw new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Server is not active"));

        if(isUpdated(prevTS)) {
            TransferOp op = new TransferOp(idOrigin, idDest, amount, true, prevTS);
            Operation aux = updatedOperation(op, index);
            addOperation(aux, index);
        }
        else {
            addOperation(new TransferOp(idOrigin, idDest, amount, false, prevTS), index);
        }
    }

    private Operation updatedOperation(Operation op, int index) {
        updateValueTS(index);

        if (op instanceof TransferOp) {
            TransferOp t = (TransferOp) op;
            if (t.getAmount() <= 0 || t.getAccount().equals(t.getDestAccount()) || !users.containsKey(t.getDestAccount()) || !users.containsKey(t.getAccount()) || users.get(t.getAccount()).getBalance() < t.getAmount()) {
                t.setTS(instableOrCanceled(index, -1));
            }
            else {
                users.get(t.getAccount()).updateBalance(-t.getAmount());
                users.get(t.getDestAccount()).updateBalance(t.getAmount());
                t.setTS(getValueTS());
            }
        }

        else if (op instanceof CreateOp) {
            if(getUsers().containsKey((op.getAccount()))) {
                op.setTS(instableOrCanceled(index, -1));
            }

            else {
                users.put(op.getAccount(), new User((op.getAccount())));
                op.setTS(getValueTS());
            }
        }
        return op;
    } 

    public synchronized List<DistLedgerCommonDefinitions.Operation> getState() {
        List<DistLedgerCommonDefinitions.Operation> ls = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            for (Operation o : getLedger().get(i)) {

                if (o instanceof TransferOp) {
                    TransferOp t = (TransferOp) o;
                    ls.add(DistLedgerCommonDefinitions.Operation.newBuilder().setTypeValue(1).setUserId(t.getAccount())
                                                                .setDestUserId(t.getDestAccount()).setAmount(t.getAmount())
                                                                .addAllPrevTS(t.getPrevTS()).addAllTS(t.getTS()).build());
                }

                else {
                    ls.add(DistLedgerCommonDefinitions.Operation.newBuilder().setTypeValue(2).setUserId(o.getAccount())
                                                                .addAllPrevTS(o.getPrevTS()).addAllTS(o.getTS()).build());
                }
            }
        }
        return ls;
    }

    public void executePendingOp() {
        List<Operation> operations = Stream.of(getLedger().get(0), getLedger().get(1), getLedger().get(2))
                                    .flatMap(Collection::stream)
                                    .filter(op -> !op.getStable())
                                    .sorted(new CompareOperation())
                                    .collect(Collectors.toList());

        if (operations.size() == 0) return;

        for (Operation op : operations) {
            if(isUpdated(op.getPrevTS())) {
                op.setStable(true);
                Operation aux = updatedOperation(op, op.getTS().indexOf(-2));
                op.setTS(aux.getTS());
            }
        }
    }

    public boolean isUpdated(List<Integer> prevTS) {
        for (int i = 0; i < prevTS.size(); i++) {
            if (prevTS.get(i) > valueTS.get(i)) {
                return false;
            }
        }
        return true;
    }

    public List<Integer> instableOrCanceled(int index, int value) {
        List<Integer> aux = new ArrayList<>();
        aux.add(0);
        aux.add(0);
        aux.add(0);
        aux.set(index, value);
        return aux;
    }

    
    public void receiveOperations(List<DistLedgerCommonDefinitions.Operation> ls, List<Integer> replicaTS) throws StatusRuntimeException {
        if (!isActive()) {
            throw new StatusRuntimeException(Status.UNAVAILABLE.withDescription("Server is not active"));
        }
        
        else {
            int j = 0;
            for (int i = 0; i < replicaTS.size(); i++) {
                if (getReplicaTS().get(i) >= replicaTS.get(i)) {
                    j += replicaTS.get(i);
                    continue;
                }

                else 
                    j += getReplicaTS().get(i);
                int index = accumulate(i, replicaTS);
                for (; j < index; j++) {

                    DistLedgerCommonDefinitions.Operation distOp = ls.get(j);
                    addGossipOperation(distOp, i);
                }
            }
        }
    }

    private void addGossipOperation(DistLedgerCommonDefinitions.Operation distOp, int index) {
        Operation op = distOp.getTypeValue() == 1 ? 
            new TransferOp(distOp.getUserId(), distOp.getDestUserId(), distOp.getAmount(), distOp.getPrevTSList(), distOp.getTSList()) 
            : new CreateOp(distOp.getUserId(), distOp.getPrevTSList(), distOp.getTSList());

        if (op.getTS().contains(-1)) {
            updateValueTS(index);
            op.setStable(true);
            addOperation(op, index);
        }
        
        else if (op.getTS().contains(-2)) {
            if(isUpdated(op.getPrevTS())) {
                op.setStable(true);
                updateValueTS(index);
                if(op instanceof TransferOp) {
                    TransferOp t = (TransferOp) op;
                    users.get(t.getAccount()).updateBalance(-t.getAmount());
                    users.get(t.getDestAccount()).updateBalance(t.getAmount());
                    t.setTS(getValueTS());
                }
                
                else if (op instanceof CreateOp) {
                    users.put(op.getAccount(), new User((op.getAccount())));
                    op.setTS(getValueTS());
                }
    
                addOperation(op, index);
            } 
            else {
                op.setStable(false);
                addOperation(op, index);
            }
        }

        else{
            op.setStable(true);

            if(op instanceof TransferOp) {
                TransferOp t = (TransferOp) op;
                users.get(t.getAccount()).updateBalance(-t.getAmount());
                users.get(t.getDestAccount()).updateBalance(t.getAmount());
            }
            
            else if (op instanceof CreateOp) {
                users.put(op.getAccount(), new User((op.getAccount())));
            }

            updateValueTS(index);
            addOperation(op, index);
        }
    }

    public int accumulate(int index, List<Integer> replicaTS) {
        int value = 0;
        for (int i = 0; i <= index; i++) value += replicaTS.get(i);
        return value;
    }

    public class CompareOperation implements Comparator <Operation> {
        @Override
        public int compare(Operation op1, Operation op2) {
            int i = 0;
            for (int j = 0; j < 3; j++) {
                i += op1.getTS().get(j) - op2.getTS().get(j);
            }
            if (i == 0) {
                return 0;
            }
            else if (i < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }

}
