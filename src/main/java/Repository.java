import Annotations.mySqlColumn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Repository<T> {

    private final Class<T> clz;
    private static Logger logger = LogManager.getLogger(Repository.class.getName());

    public Repository(Class<T> clz) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.clz = clz;
        try (Connection con = ConnectionPool.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            if (!meta.getTables(null, null, clz.getSimpleName().toLowerCase(), new String[]{"TABLE"}).next()) {
                checkClassForCompatibility(clz);
                initTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkClassForCompatibility(Class<?> clz) {
        Field[] declaredFields = clz.getDeclaredFields();
        boolean isAutoIncrementPresent = false;
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(mySqlColumn.class)) {
                mySqlColumn annotation = field.getAnnotation(mySqlColumn.class);
                if (annotation.autoIncrement()) {
                    if (!annotation.primaryKey()) {
                        throw new IllegalArgumentException("Class fields cant have Auto_Increment without Primary_Key");
                    } else if (isAutoIncrementPresent) {
                        throw new IllegalArgumentException("Class cant have two Auto_Increment fields");
                    }
                    isAutoIncrementPresent = true;
                }
            }
        }
    }


    public List<T> getByProperty(String propName, String propVal) {
        if(propName == null || propVal == null) throw new IllegalArgumentException();
        String query = QueryFactory.createGetByPropertyQuery(clz, propName, propVal);
        List<T> results = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            results = listResults(rs);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e);
        }

        return results;
    }

    public T findOne(int id) {
        String query = QueryFactory.createFindOneQuery(clz, id);
        T result = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            result = (rs.next() ? createSingleInstance(rs) : null);
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            throw new RuntimeException("DB error", e);
        }
        return result;
    }

    public List<T> findAll() {
        String query = QueryFactory.createFindAllQuery(clz);
        List<T> results = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            results = listResults(rs);
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            throw new RuntimeException("DB error", e);
        }
        return results;
    }

    public <T> int insertOne(T instance) {
        if(instance==null) throw new IllegalArgumentException("cannot insert a null instance to database!");
        String query = QueryFactory.createInsertOneQuery(instance);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
        System.out.println("Rows affected:" + rowsAffected);

        return rowsAffected;
    }

    public <T> int insertMultiple(List<T> itemList) {
        String query = QueryFactory.createInsertMultipleQuery(itemList, clz);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement()) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            //System.out.println(e.getMessage());
            throw new RuntimeException("DB error", e);
        }
        System.out.println("Rows affected:" + rowsAffected);
        return rowsAffected;
    }

    private List<T> listResults(ResultSet rs) throws SQLException {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
            results.add(createSingleInstance(rs));
        }

        return results;
    }

    private T createSingleInstance(ResultSet rs) {
        T clzInstance;
        try {
            Constructor<T> constructor = clz.getConstructor(null);
            clzInstance = constructor.newInstance();
            fieldsAssignment(clzInstance, rs);

        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new UnsupportedOperationException("Could not instantiate result class");
        }

        return clzInstance;
    }

    public void fieldsAssignment(T clzInstance, ResultSet rs) {
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            String colName = QueryFactory.getFieldName(field);
            field.setAccessible(true);
            try {
                field.set(clzInstance, rs.getObject(colName));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Field %s is inaccessible", field.getName()));
            } catch(SQLException e) {
                throw new IllegalArgumentException("Column label is not valid");
            }
        }
    }

    public boolean initTable() {
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement()) {
            return stmt.execute(QueryFactory.createTableMySQLStatement(clz));
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
    }

    public boolean truncateTable() {
        String queryString = "TRUNCATE TABLE " + clz.getSimpleName().toLowerCase() + ";";
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            return stmt.execute(queryString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int updateSingleProperty(int id,String item,Object newValue) {
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection();Statement stmt = con.createStatement()) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
            throw new RuntimeException("DB error", e);
        }
        System.out.println("1 property has been updated successfully");

        return rowsAffected;
    }

    public int updateRow(int id, T object) {
        String query = QueryFactory.createUpdateRowQuery(clz, object, id);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(String.valueOf(query));

        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
        System.out.println("The row updated successfully");

        return rowsAffected;
    }
    public int singleAndMultipleItemDeletionByProperty(String property,Object value) {
        String query = QueryFactory.createDeleteQuery(clz,property,value);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            throw new RuntimeException("DB error", e);
        }

        return rowsAffected;
    }
}