import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    final String schemaName;
    final String username;
    final String password;

    public ConfigManager() {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("MySql.config");
        if (resourceAsStream == null) {
            throw new IllegalArgumentException("config file not found!");
        }

        Properties props = new Properties();

        try {
            props.load(resourceAsStream);
            this.schemaName = props.getProperty("SCHEMA_NAME");
            this.username = props.getProperty("DB_USER");
            this.password = props.getProperty("DB_PASSWORD");
            resourceAsStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
