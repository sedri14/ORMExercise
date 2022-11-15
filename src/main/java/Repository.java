import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class Repository<T> {

    private final Class<T> clz;

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
                initTable();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<T> getByProperty(String propName, String propVal) {
        String query = QueryFactory.createGetByPropertyQuery(clz, propName, propVal);
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);

            return listResults(rs);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public T findOne(int id) {
        String query = QueryFactory.createFindOneQuery(clz, id);
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);

            return (rs.next() ? createSingleInstance(rs) : null);

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public List<T> findAll() {
        String query = QueryFactory.createFindAllQuery(clz);
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);

            return listResults(rs);

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public <T> int insertOne(T instance) {
        String query = QueryFactory.createInsertOneQuery(instance);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();){
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("Rows affected:" + rowsAffected);
        return rowsAffected;
    }

    public <T> int insertMultiple(List<T> itemList) {
        String query = QueryFactory.createInsertMultipleQuery(itemList, clz);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Rows affected:" + rowsAffected);
        return rowsAffected;
    }

    private List<T> listResults(ResultSet rs) throws SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
            results.add(createSingleInstance(rs));
        }

        return results;
    }

    private T createSingleInstance(ResultSet rs) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        Constructor<T> constructor = clz.getConstructor(null);
        T clzInstance = constructor.newInstance();
        fieldsAssignment(clzInstance, rs);

        return clzInstance;
    }

    public void fieldsAssignment(T clzInstance, ResultSet rs) throws SQLException, IllegalAccessException {
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            String colName = QueryFactory.getFieldName(field);
            field.setAccessible(true);
            field.set(clzInstance, rs.getObject(colName));
        }
    }

    public boolean initTable() {
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement()) {
            return stmt.execute(QueryFactory.createTableMySQLStatement(clz));
        } catch (SQLException e) {
            throw new RuntimeException(e);
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

    public int updateSingleProperty(int id, String item, String newValue) {
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz, item, newValue, id);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }
        return rowsAffected;
    }

    public int updateRow(int id, T object) {
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            String query = QueryFactory.createUpdateRowQuery(clz, object, id);
            rowsAffected = stmt.executeUpdate(String.valueOf(query));

        } catch (Exception e) {
            System.out.println(e);
        }
        return rowsAffected;
    }

    public int singleItemDeletionByProperty(String property, String value) {
        String query = QueryFactory.createDeleteQuery(clz, property, value);
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
        }

        return rowsAffected;
    }
}