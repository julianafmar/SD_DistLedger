package pt.tecnico.distledger.server.domain.client;

public class User {

    private String id_account;

    private Integer balance;

    public User(String id) {
        this.id_account = id;
        this.balance = 0;
    }

    public String getIdAccount() {
        return this.id_account;
    }

    public Integer getBalance() {
        return this.balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public void updateBalance(Integer balance) {
        this.balance += balance;
    }
    
    @Override
    public String toString() {
        return "Id Account: " + id_account + "\nBalance: " + balance;
    }
}
