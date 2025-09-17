package app.demo;

public class Account {
    private String username;
    private String passwordHash;

    public Account(String username, String password){
        this.username = username;
        this.passwordHash = passwordEncryption.hashPassword(password);
    }
    public Account(String username, String passwordHash, boolean alreadyHashed) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername(){
        return username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }
}
