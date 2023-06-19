package pt.tecnico.distledger.server.domain.operation;

import java.util.List;

public class CreateOp extends Operation {

    public CreateOp(String account, boolean stable, List<Integer> prevTS) {
        super(account, stable, prevTS);
    }

    public CreateOp(String account, List<Integer> prevTS, List<Integer> TS) {
        super(account, prevTS, TS);
    }

}
