package app.demo;

public interface LoginDetailsDAO {
    public void CreateAccount(Account account);

    public boolean ConfrimLogin(String username, String password);

    public Account getAccount(String username, String password);

}
