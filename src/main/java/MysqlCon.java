import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import org.apache.commons.lang3.ClassUtils;

class MysqlCon<T> {

    private final Class<T> clz;

    public MysqlCon(Class<T> clz) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.clz = clz;
        try (Connection con = ConnectionPool.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            if (!meta.getTables(null, null, clz.getSimpleName().toLowerCase(), new String[]{"TABLE"}).next()) {
                initTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<T> getByProperty(String propName, String propVal) {
        if(propName==null || propVal==null) throw new IllegalArgumentException();
        String query = QueryFactory.createGetByPropertyQuery(clz, propName,propVal);
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
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
        String query = QueryFactory.createFindOneQuery(clz, id);
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            T clzInstance;
            if (rs.next()) {
                clzInstance = createSingleInstance(rs);
            } else {
                return null;
            }

            return clzInstance;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public List<T> findAll() {
        String query = QueryFactory.createFindAllQuery(clz);
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);

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

    public <T> void insertOne(T instance) {
        if(instance==null) throw new IllegalArgumentException("cannot insert a null instance to database!");
        String query = QueryFactory.createInsertOneQuery(instance);
        int rowsAffected = 0;
        try {
            Connection con = ConnectionPool.getConnection();
            Statement stmt = con.createStatement();
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);
    }

    public <T> void insertMultiple(List<T> itemList) {
        if(itemList==null) throw new IllegalArgumentException("cannot insert a null instances to database!");

        String query = QueryFactory.createInsertMultipleQuery(itemList, clz);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);

    }

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
        try (Connection con = ConnectionPool.getConnection(); Statement statement = con.createStatement()) {
            return statement.execute(QueryFactory.createTableMySQLStatement(clz));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean truncateTable() {
        String queryString = "TRUNCATE TABLE " + clz.getSimpleName().toLowerCase() + ";";
        try (Connection con = ConnectionPool.getConnection();
             Statement statement = con.createStatement()){
            return statement.execute(queryString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSingleProperty(int id,String item,Object newValue) {
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id);
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("1 property has been updated successfully");
    }
    public void updateRow(int id,T object) {
        try (Connection con = ConnectionPool.getConnection()){
            Statement stmt = con.createStatement();
            String query = QueryFactory.createUpdateRowQuery(clz,object,id);
            stmt.executeUpdate(String.valueOf(query));
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("The row updated successfully");
    }
    public void singleAndMultipleItemDeletionByProperty(String property,String value) {
        try (Connection con = ConnectionPool.getConnection()) {
            Statement stmt = con.createStatement();
            String query = QueryFactory.createDeleteQuery(clz,property,value);
            stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}