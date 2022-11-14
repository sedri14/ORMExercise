import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s WHERE %s = '%s'", clz.getSimpleName().toLowerCase(),propName.toLowerCase(),propVal.toLowerCase()));
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(createSingleInstance(rs));
            }

            return results;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        catch (Exception e) {
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
            rowsAffected = stmt.executeUpdate(buildInsertQueryString(instance));
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);
    }

    public <T> String buildInsertQueryString(T instance){
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

    private String createTableMySQLStatement() {
        StringBuilder queryString = new StringBuilder("CREATE TABLE " + clz.getSimpleName().toLowerCase() + "(");
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            queryString.append(createColumnMySqlDeclaration(field)).append(", ");
        }

        queryString.delete(queryString.length()-2, queryString.length());
        queryString.append(primaryKeyString(declaredFields));
        queryString.append(");");
        return queryString.toString();
    }

    private String createColumnMySqlDeclaration(Field field) {
        mySqlColumn mySqlAnnotation = field.getAnnotation(mySqlColumn.class);
        String type;
        String name;
        String extras = "";
        if (mySqlAnnotation != null) {
            if (mySqlAnnotation.type() != mySqlColumn.MySqlType.DEFAULT) {
                type = mySqlAnnotation.type().toString();
                if (mySqlAnnotation.length() != 0) {
                    type += "(" + mySqlAnnotation.length() + ")";
                }
            } else {
                type = mySqlType(field.getType());
            }

            if (!Objects.equals(mySqlAnnotation.columnName(), "")) {
                name = mySqlAnnotation.columnName();
            } else {
                name = field.getName();
            }

            if (mySqlAnnotation.notNull()) {
                extras += " NOT NULL";
            }
            if (mySqlAnnotation.unique()) {
                extras += " UNIQUE";
            }
            if (mySqlAnnotation.autoIncrement()) {
                extras += " AUTO_INCREMENT";
            }
        } else {
            type = mySqlType(field.getType());
            name = field.getName();
        }
        return name + " " + type + extras;
    }

    private String primaryKeyString(Field[] fields) {
        List<Field> listFields = List.of(fields);
        List<Field> primaryKeys = listFields.stream().filter(field -> field.isAnnotationPresent(mySqlColumn.class)).filter(field -> field.getAnnotation(mySqlColumn.class).primaryKey()).collect(Collectors.toList());
        if (primaryKeys.isEmpty()) {
            return "";
        }

        StringBuilder primaryKeyConstraint = new StringBuilder(", CONSTRAINT PK_test PRIMARY KEY (");
        for (Field field : primaryKeys) {
            if (field.getAnnotation(mySqlColumn.class).columnName().equals("")) {
                primaryKeyConstraint.append(field.getName());
            } else {
                primaryKeyConstraint.append(field.getAnnotation(mySqlColumn.class).columnName());
            }
            primaryKeyConstraint.append(", ");
        }
        primaryKeyConstraint.delete(primaryKeyConstraint.length()-2, primaryKeyConstraint.length());
        primaryKeyConstraint.append(")");
        return primaryKeyConstraint.toString();
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