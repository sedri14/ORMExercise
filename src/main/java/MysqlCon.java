import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import Annotations.mySqlColumn;
import com.google.gson.Gson;
import org.apache.commons.lang3.ClassUtils;

class MysqlCon<T> {

    Connection con;

    private final Class<T> clz;

    public MysqlCon(Class<T> clz) {
        this.clz = clz;
        try {
            ConfigManager configs = new ConfigManager();
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/" + configs.schemaName,
                    configs.username, configs.password);

            DatabaseMetaData meta = con.getMetaData();
            if (!meta.getTables(null, null, clz.getSimpleName().toLowerCase(), new String[]{"TABLE"}).next()) {
                initTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public List<T> getByProperty(String propName, String propVal) {

        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s'", clz.getSimpleName().toLowerCase(), propName.toLowerCase(), propVal.toLowerCase()));
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(createSingleInstance(rs));
            }

            return results;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public T findOne(int id) {
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s WHERE id=%d", clz.getSimpleName().toLowerCase(), id));

            Constructor<T> constructor = clz.getConstructor(null);
            T clzInstance = constructor.newInstance();
            if (rs.next()) {
                fieldsAssignment(clzInstance, rs);
            } else {

            }

            return clzInstance;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public List<T> findAll() {

        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s", clz.getSimpleName().toLowerCase()));
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(createSingleInstance(rs));
            }

            return results;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    //Add a single row:
    //INSERT INTO table_name (column1, column2, column3,etc)
    //VALUES (value1, value2, value3, etc);
    public <T> void insertOne(T instance) {

        int rowsAffected = 0;
        try {
            Statement stmt = con.createStatement();
            rowsAffected = stmt.executeUpdate(buildInsertOneQueryString(instance));
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);
    }

    //Add multiple rows:
//    INSERT INTO table_name (column1, column2, column3,etc)
//    VALUES
//            (value1, value2, value3, etc),
//            (value1, value2, value3, etc),
//            (value1, value2, value3, etc);
    public <T> void insertMultiple(List<T> itemList) {
        int rowsAffected = 0;
        try {
            Statement stmt = con.createStatement();
            rowsAffected = stmt.executeUpdate(buildInsertMultipleQueryString(itemList));
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);

    }

    private <T> String buildInsertMultipleQueryString(List<T> itemList) {
        StringBuilder queryString = new StringBuilder("INSERT INTO " + clz.getSimpleName().toLowerCase() + "(");
        Field[] declaredFields = clz.getDeclaredFields();

        //string list of columns (column1, column2, column3,etc)
        StringBuilder columnsString = new StringBuilder();

        for (Field field : declaredFields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            columnsString.append(fieldName);
            columnsString.append(", ");
        }

        columnsString.delete(columnsString.length() - 1, columnsString.length());
        columnsString.append(")");

        //string lists of values (value1, value2, value3, etc),(value1, value2, value3, etc),(value1, value2, value3, etc)
        StringBuilder valuesString = new StringBuilder("VALUES ");
        for (T item : itemList) {
            valuesString.append("(");
            //make a new iterator for each iteration???
            for (Field field : declaredFields) {
                //Field field = iterator.next();
                field.setAccessible(true);
                //String fieldName = field.getName();
                Object val = null;
                try {
                    val = field.get(item);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                Object valToInsert = handleValue(val);
                valuesString.append(valToInsert);
                valuesString.append(", ");
            }
                valuesString.delete(valuesString.length() - 1, valuesString.length());
                valuesString.append(")");
                valuesString.append(",");
        }
        valuesString.delete(valuesString.length() - 1, valuesString.length());

        String res = queryString.append(columnsString).append(valuesString).toString();
        return res;
    }

    private  <T> String buildInsertOneQueryString(T instance) {
        StringBuilder queryString = new StringBuilder("INSERT INTO " + clz.getSimpleName().toLowerCase() + "(");
        StringBuilder columnsString = new StringBuilder();
        StringBuilder valuesString = new StringBuilder("VALUES (");

        Field[] declaredFields = clz.getDeclaredFields();
        Iterator<Field> iterator = Arrays.stream(declaredFields).iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            field.setAccessible(true);
            String fieldName = field.getName();
            Object val = null;
            try {
                val = field.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            columnsString.append(fieldName);
            Object valToInsert = handleValue(val);
            valuesString.append(valToInsert);

            if (iterator.hasNext()) {
                columnsString.append(", ");
                valuesString.append(", ");
            }
        }
        columnsString.append(")");
        valuesString.append(")");

        return queryString.append(columnsString).append(valuesString).toString();
    }

    private String handleValue(Object val) {
        if (ClassUtils.isPrimitiveOrWrapper(val.getClass())) {
            return val.toString();
        } else if (val instanceof String) {
            return String.format("\"%s\"", val.toString());
        } else {
            return new Gson().toJson(val);
        }
    }

    //Add multiple rows:
//    INSERT INTO table_name (column1, column2, column3,etc)
//    VALUES
//            (value1, value2, value3, etc),
//    (value1, value2, value3, etc),
//            (value1, value2, value3, etc);


    public T createSingleInstance(ResultSet rs) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        Constructor<T> constructor = clz.getConstructor(null);
        T clzInstance = constructor.newInstance();
        fieldsAssignment(clzInstance, rs);

        return clzInstance;
    }

    public void fieldsAssignment(T clzInstance, ResultSet rs) throws SQLException, IllegalAccessException {
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            field.set(clzInstance, rs.getObject(field.getName()));
        }
    }

    public boolean initTable() {
        try (Statement statement = con.createStatement()) {
            return statement.execute(createTableMySQLStatement());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean truncateTable() {
        String queryString = "TRUNCATE TABLE " + clz.getSimpleName().toLowerCase() + ";";
        try (Statement statement = con.createStatement()){
            return statement.execute(queryString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private SQLProps getConfigs() {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("MySql.config");
        if (resourceAsStream == null) {
            throw new IllegalArgumentException("config file not found!");
        }

        Properties props = new Properties();

        try {
            props.load(resourceAsStream);
            return new SQLProps(props.getProperty("SCHEMA_NAME"), props.getProperty("DB_USER"), props.getProperty("DB_PASSWORD"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class SQLProps {
        String schemaName;
        String username;
        String password;

        public SQLProps(String schemaName, String username, String password) {
            this.schemaName = schemaName;
            this.username = username;
            this.password = password;
        }
    }

    private static String mySqlType(Class<?> clz) {
        switch (clz.getSimpleName()) {
            case "int":
                return "INTEGER";
            case "String":
                return "VARCHAR(45)";
            case "boolean":
                return "BOOLEAN";
            default:
                return "VARCHAR(255)";
        }
    }
}  