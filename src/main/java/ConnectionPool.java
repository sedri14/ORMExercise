import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {
    private static BasicDataSource ds = new BasicDataSource();

    static {
        ConfigManager configs = new ConfigManager();
        ds.setUrl("jdbc:mysql://localhost:3306/" + configs.schemaName);
        ds.setUsername(configs.username);
        ds.setPassword(configs.password);
        ds.setMinIdle(1);
        ds.setMaxIdle(10);
        ds.setMaxOpenPreparedStatements(100);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    private ConnectionPool() {}
}
