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
            logger.fatal(String.format("Could not get or instantiate table: ",clz.getSimpleName().toLowerCase()));
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


    public List<T> getByProperty(String property, Object value) {
        if(property == null || value == null) throw new IllegalArgumentException();
        String query = QueryFactory.createGetByPropertyQuery(clz, property, value);
        List<T> results = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            results = listResults(rs);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
        }

        return results;
    }

    public T findOne(int id) {
        String query = QueryFactory.createFindOneQuery(clz, id);
        logger.debug(String.format("findOne: Execute query: %s", query));
        T result = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            result = (rs.next() ? createSingleInstance(rs) : null);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info("findOne execute query finished");

        return result;
    }

    public List<T> findAll() {
        String query = QueryFactory.createFindAllQuery(clz);
        logger.debug(String.format("findAll: Execute query: %s", query));
        List<T> results = null;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            ResultSet rs = stmt.executeQuery(query);
            results = listResults(rs);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info("findAll execute query finished");


        return results;
    }

    public <T> int insertOne(T instance) {
        if (instance == null) throw new IllegalArgumentException("Can not insert a null instance to database!");
        String query = QueryFactory.createInsertOneQuery(instance);
        logger.debug(String.format("insertOne: Execute query: %s", query));
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info(String.format("Item inserted. Rows affected: %d", rowsAffected));

        return rowsAffected;
    }

    public <T> int insertMultiple(List<T> itemList) {
        String query = QueryFactory.createInsertMultipleQuery(itemList, clz);
        logger.debug(String.format("insertMultiple: Execute query: %s", query));
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement()) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info(String.format("Items inserted. Rows affected: %d", rowsAffected));

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
            logger.error("Throwing exception: Could not instantiate result class");
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
                logger.error(String.format("Throwing exception: Field %s is inaccessible", field.getName()));
                throw new RuntimeException(String.format("Field %s is inaccessible", field.getName()));
            } catch(SQLException e) {
                logger.error("Throwing exception: Column label is not valid");
                throw new IllegalArgumentException("Column label is not valid");
            }
        }
    }

    private Object objectAsType(Class<?> fieldClass, Object object) {
        if (!ClassUtils.isPrimitiveOrWrapper(fieldClass) && !fieldClass.equals(String.class)) {
            Gson gson = new Gson();
            return gson.fromJson(String.valueOf(object), fieldClass);
        }
        return object;
    }

    public boolean initTable() {
        String query = QueryFactory.createTableMySQLStatement(clz);
        logger.debug(String.format("initTable: Execute query: %s", query));
        boolean isInit = false;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement()) {
            isInit = stmt.execute(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info("Table initialized");

        return isInit;
    }

    public boolean truncateTable() {
        String query = "TRUNCATE TABLE " + clz.getSimpleName().toLowerCase() + ";";
        logger.debug(String.format("truncateTable: Execute query: %s", query));
        boolean isTruncated = false;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            isTruncated = stmt.execute(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException(e);
        }
        logger.info("Table truncated");

        return isTruncated;
    }

    public int updateSingleProperty(int id, String property, Object newValue) {
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz, property, newValue,id);
        logger.debug(String.format("updateSingleProperty: Execute query: %s", query));
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection();Statement stmt = con.createStatement()) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info(String.format("Property %s has been updated to %s, Item id: %d", property, String.valueOf(newValue),id));

        return rowsAffected;
    }

    public int updateRow(int id, T object) {
        String query = QueryFactory.createUpdateRowQuery(clz, object, id);
        logger.debug(String.format("updateRow: Execute query: %s", query));
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(String.valueOf(query));

        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info(String.format("Row id %d has been updated", id));

        return rowsAffected;
    }
    public int singleAndMultipleItemDeletionByProperty(String property, Object value) {
        String query = QueryFactory.createDeleteQuery(clz,property,value);
        logger.debug(String.format("singleAndMultipleItemDeletionByProperty: Execute query: %s", query));
        int rowsAffected = 0;
        try (Connection con = ConnectionPool.getConnection(); Statement stmt = con.createStatement();) {
            rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            logger.error(e.getMessage() + e.getErrorCode());
            throw new RuntimeException("DB error", e);
        }
        logger.info(String.format("%d rows have been deleted", rowsAffected));

        return rowsAffected;
    }
}