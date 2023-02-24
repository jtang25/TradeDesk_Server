package org.example;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import static spark.Spark.*;
import java.sql.*;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

public class Main {
    public static Connection connection = null;
    public static Statement statement = null;
    public static String password = null;
    public static void main(String[] args) {
        setConnection();
        get("/connect", (req, res) -> {
            return setConnection();
        });
        get("/register/:newUsername/:newPassword", (req, res) -> {
            return registerUser(req.params(":newUsername"), req.params(":newPassword"));
        });
        get("/login/:username/:password", (req, res) -> {
            return loginUser(req.params(":username"), req.params(":password"));
        });
    }

    private static String setConnection(){
        try{
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .clientId("bf037dee-bdef-4860-9cd3-05a0f0e9e2e7")
                    .clientSecret("JS_8Q~eFn_Te-Uxu4YZFSRoUXiqGtSLTWcKFkcDO")
                    .tenantId("9bce7f0b-99b6-43fd-8820-9afb17c1c5fd")
                    .build();
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl("https://TradeDesk-KV.vault.azure.net")
                    .credential(credential)
                    .buildClient();
            KeyVaultSecret secret = secretClient.getSecret("sql-pwd");
            String secretValue = secret.getValue();
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager
                    .getConnection("jdbc:sqlserver://tradedeskserver.database.windows.net:1433;database=TradeDesk;" +
                            "user=jasontang@tradedeskserver;password="+secretValue+";encrypt=true;trustServerCertificate=false;" +
                            "hostNameInCertificate=*.database.windows.net;loginTimeout=30;");

            // Statements allow to issue SQL queries to the database
            statement = connection.createStatement();
            String strShowDatabases = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE'";
            ResultSet rs = statement.executeQuery(strShowDatabases);
            int tbCount = 0;
            while(rs.next()){
                String dbName = rs.getString(1);
                System.out.println(dbName);
                tbCount++;
            }
            return "There are "+tbCount+" tables on this database.";
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String registerUser(String newUsername, String newPassword){
        try{
            String addUser = "INSERT INTO USERS (username, password) VALUES ('"+newUsername+"', '"+hashPassword(newPassword)+"')";
            int rowsInserted = statement.executeUpdate(addUser);
            if(rowsInserted>0){
                return "A user has been registered successfully!";
            }
            else{
                return "User registration failed.";
            }
        } catch (SQLException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String loginUser(String username, String password){
        try{
            String getUsersPwds = "SELECT username, password FROM users;";
            ResultSet upSet = statement.executeQuery(getUsersPwds);
            boolean verified = false;
            while(upSet.next()){
                String user = upSet.getString(1);
                String pwd = upSet.getString(2);
                if(user.equals(username) && pwd.equals(hashPassword(password))){
                    verified = true;
                }
            }
            if(verified){
                return "Login Successful.";
            }
            else{
                return "Login Unsuccessful.";
            }
        }
            catch (SQLException | UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hashPassword(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = password.getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] theSHA256digest = md.digest(bytesOfMessage);
        StringBuilder sb = new StringBuilder();
        for (byte b : theSHA256digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}