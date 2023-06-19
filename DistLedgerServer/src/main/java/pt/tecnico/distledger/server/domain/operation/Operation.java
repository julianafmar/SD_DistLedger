package pt.tecnico.distledger.server.domain.operation;


import java.util.List;
import java.util.ArrayList;

public class Operation {
    
    private String account;
    private boolean stable;
    private List<Integer> prevTS = new ArrayList<>();
    private List<Integer> TS = new ArrayList<>();

    public Operation(String fromAccount, boolean stable, List<Integer> prevTS) { 
        this.account = fromAccount;
        this.stable = stable;
        this.prevTS = prevTS;
        this.TS.add(0);
        this.TS.add(0);
        this.TS.add(0);
    }

    public Operation(String fromAccount, List<Integer> prevTS, List<Integer> TS) { 
        this.account = fromAccount;
        this.prevTS = prevTS;
        this.TS = TS;
        
        if(TS.contains(-2)){
            this.stable = false;
        }
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public boolean getStable(){
        return stable;
    }

    public void setStable(boolean stable) {
        this.stable = stable;
    }

    public List<Integer> getPrevTS() {
        return prevTS;
    }

    public void setPrevTS(List<Integer> prevTS) {
        this.prevTS.clear();
        this.prevTS.addAll(prevTS);
    }

    public List<Integer> getTS() {
        return TS;
    }

    public void setTS(List<Integer> tS) {
        this.TS = new ArrayList<>();
        this.TS = tS;
    }

}
