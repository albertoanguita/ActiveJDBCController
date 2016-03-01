package jacz.storage;

import org.javalite.activejdbc.Base;

import java.util.ArrayDeque;

/**
 * This controller provides unified connection and disconnection methods so several projects
 * relying on active jdbc can work simultaneously
 */
public class ActiveJDBCController {

    private static final ThreadLocal<ArrayDeque<String>> connectionsStack = new ThreadLocal();

    private static ArrayDeque<String> getConnectionsStack() {
        if (connectionsStack.get() == null) {
            connectionsStack.set(new ArrayDeque<>());
        }
        return connectionsStack.get();
    }

    public static void connect(String dbPath) {
        String currentConnection = getConnectionsStack().peek();
        if (currentConnection == null || !currentConnection.equals(dbPath)) {
            // we must perform a connection to dbPath
            if (Base.hasConnection()) {
                // first disconnect
                Base.close();
            }
            Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + dbPath, "", "");
        }
        getConnectionsStack().push(dbPath);
    }

    public static void disconnect(String dbPath) {
        dbPath = getConnectionsStack().pop();
        if (!getConnectionsStack().isEmpty() && !getConnectionsStack().peek().equals(dbPath)) {
            // we must disconnect and reconnect to the new top of the connection stack
            Base.close();
            Base.open("org.sqlite.JDBC", "jdbc:sqlite:" + getConnectionsStack().peek(), "", "");
        } else if (getConnectionsStack().isEmpty()) {
            // todo: maybe in the future we can maintain this last connection. Must investigate if it would
            // get closed after thread is GC
            Base.close();
        }
    }


}
