import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Annotations.mySqlColumn;

class MysqlCon<T> {

    Connection con;

    private final Class<T> clz;

    public MysqlCon(Class<T> clz) {
        this.clz = clz;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/myschema",
                    "root", null);

            DatabaseMetaData meta = con.getMetaData();
            if (!meta.getTables(null, null, clz.getSimpleName().toLowerCase(), new String[] {"TABLE"}).next()) {
                initTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
                results.add(makeSingleInstance(rs));
            }

            return results;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }


    public T makeSingleInstance(ResultSet rs) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
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
        try (Statement statement = con.createStatement()){
            return statement.execute(createTableMySQLStatement());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String createTableMySQLStatement() {
        StringBuilder queryString = new StringBuilder("CREATE TABLE " + clz.getSimpleName().toLowerCase() + "(");
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            queryString.append(" ").append(createColumnMySqlDeclaration(field)).append(", ");
        }
        queryString.delete(queryString.length()-2, queryString.length()-1);
        queryString.append(");");
        return queryString.toString();
    }

    private String createColumnMySqlDeclaration(Field field) {
        mySqlColumn mySqlAnnotation = field.getAnnotation(mySqlColumn.class);
        String type;
        String name;
        if (mySqlAnnotation != null) {
            type = mySqlAnnotation.type().toString();
            name = mySqlAnnotation.columnName();
        } else {
            type = mySqlType(field.getType());
            name = field.getName();
        }
        return name + " " + type;
    }

    public void close() {
        try {
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String mySqlType(Class<?> clz) {
        switch(clz.getSimpleName()) {
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