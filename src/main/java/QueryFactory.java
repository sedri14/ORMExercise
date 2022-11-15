import Annotations.mySqlColumn;
import com.google.gson.Gson;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class QueryFactory {
    public static String createTableMySQLStatement(Class<?> clz) {
        StringBuilder queryString = new StringBuilder("CREATE TABLE " + clz.getSimpleName().toLowerCase() + "(");
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            queryString.append(createColumnMySqlDeclaration(field)).append(", ");
        }

        queryString.delete(queryString.length() - 2, queryString.length());
        queryString.append(createPrimaryKeyString(declaredFields));
        queryString.append(");");
        return String.valueOf(queryString);
    }

    public static String createFindOneQuery(Class<?> clz, int id) {
        return String.format("SELECT * FROM %s WHERE id=%d", clz.getSimpleName().toLowerCase(), id);
    }

    public static String createFindAllQuery(Class<?> clz) {
        return String.format("SELECT * FROM %s", clz.getSimpleName().toLowerCase());
    }
    public static String createUpdateSinglePropertyQuery(Class<?> clz,String item,Object newValue,int id){

        Field[] fields = clz.getDeclaredFields();
        for (Field field:
                fields) {
            if(field.getName().equals(item)){
                Class<?> fieldType = field.getType();
                if(newValue.getClass().equals(fieldType) || ClassUtils.isAssignable(newValue.getClass(), fieldType)){
                    newValue = handleValue(newValue);
                    return String.format("UPDATE %s SET %s = %s WHERE id = %d;", clz.getSimpleName().toLowerCase(),item,newValue, id);
                } else{
                    throw new IllegalArgumentException("The value and the required field type are different");
                }
            }

        }
        throw  new IllegalArgumentException("There is no field with name "+ item);
    }
    public static String createDeleteQuery(Class<?> clz,String property,Object value){
        Field[] fields = clz.getDeclaredFields();
        for (Field field:
                fields) {
            if(field.getName().equals(property)){
                Class<?> fieldType = field.getType();
                if(property.getClass().equals(fieldType)){
                    value = handleValue(value);
                    return String.format("DELETE FROM %s WHERE %s=%s;", clz.getSimpleName().toLowerCase(),property, value);
                } else{
                    throw new IllegalArgumentException("The value and the required field's type are different");
                }
            }

        }
        throw  new IllegalArgumentException("There is no field with name "+ property);
    }
    public static <T> String createUpdateRowQuery(Class<?> clz,T object,int id) {
        StringBuilder query= new StringBuilder(String.format("UPDATE %s SET ", clz.getSimpleName().toLowerCase()));
        Field[] fields = clz.getDeclaredFields();
        for (Field field:
                fields) {
            query.append(field.getName());
            query.append(" = ");
            try {
                query.append(QueryFactory.handleValue(field.get(object)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Field %s is inaccessible", field.getName()));
            }            query.append(" , ");
        }
        query.delete(query.length()-3, query.length()-1);
        query.append(String.format("WHERE id = %d;",id));
        return String.valueOf(query);
    }
    public static <T> String createInsertOneQuery(T instance) {

        Class<?> clz = instance.getClass();
        String tableName = clz.getSimpleName().toLowerCase();

        StringBuilder queryString = new StringBuilder(String.format("INSERT INTO %s ", tableName));
        String columnsString = columnsFormattedString(instance.getClass());
        String valuesString = "VALUES " + valuesFormattedString(instance);

        return String.valueOf(queryString.append(columnsString).append(valuesString));
    }

    public static <T> String createInsertMultipleQuery(List<T> itemList, Class<?> clz) {

        String tableName = clz.getSimpleName().toLowerCase();
        StringBuilder queryString = new StringBuilder(String.format("INSERT INTO %s ", tableName));
        String columnsString = columnsFormattedString(clz);
        StringBuilder listValuesString = new StringBuilder("VALUES ");

        for (T item : itemList) {
            listValuesString.append(valuesFormattedString(item));
            listValuesString.append(",");
        }
        listValuesString.delete(listValuesString.length() - 1, listValuesString.length());

        return String.valueOf(queryString.append(columnsString).append(listValuesString));
    }


    // -----------------------HELPERS-----------------------//
    public static String handleValue(Object val) {

        if (val == null) {
            return "NULL";
        }
        if (ClassUtils.isPrimitiveOrWrapper(val.getClass())) {
            return (val instanceof Character) ? String.format("\'%c\'", val) : String.valueOf(val);
        } else if (val instanceof String) {
            return String.format("\"%s\"", val);
        } else {
            return String.format("\"%s\"", new Gson().toJson(val));
        }
    }

    private static String columnsFormattedString(Class<?> clz) {

        StringBuilder columnsString = new StringBuilder("(");

        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            String colName = getFieldName(field);
            columnsString.append(colName);
            columnsString.append(",");
        }
        columnsString.delete(columnsString.length() - 1, columnsString.length());
        columnsString.append(")");

        return String.valueOf(columnsString);
    }

    public static String getFieldName(Field field) {

        if (field.isAnnotationPresent(mySqlColumn.class)) {
            if (!field.getAnnotation(mySqlColumn.class).columnName().isEmpty()) {
                return field.getAnnotation(mySqlColumn.class).columnName();
            }
        }
        return field.getName();
    }

    private static <T> String valuesFormattedString(T instance) {

        StringBuilder valuesString = new StringBuilder("(");

        Field[] declaredFields = instance.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            Object val = null;
            try {
                val = field.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Object valToInsert = handleValue(val);
            valuesString.append(valToInsert);
            valuesString.append(",");
        }
        valuesString.delete(valuesString.length() - 1, valuesString.length());
        valuesString.append(")");

        return String.valueOf(valuesString);
    }

    private static String createPrimaryKeyString(Field[] fields) {
        List<Field> listFields = List.of(fields);
        List<Field> primaryKeys = listFields.stream().filter(field -> field.isAnnotationPresent(mySqlColumn.class))
                .filter(field -> field.getAnnotation(mySqlColumn.class).primaryKey()).collect(Collectors.toList());
        if (primaryKeys.isEmpty()) {
            return "";
        }

        StringBuilder primaryKeyConstraint = new StringBuilder(", CONSTRAINT PK_test PRIMARY KEY (");
        for (Field field : primaryKeys) {
            primaryKeyConstraint.append(getFieldName(field)).append(", ");
        }

        primaryKeyConstraint.delete(primaryKeyConstraint.length() - 2, primaryKeyConstraint.length());
        primaryKeyConstraint.append(")");
        return String.valueOf(primaryKeyConstraint);
    }

    private static String createColumnMySqlDeclaration(Field field) {
        String type = mySqlType(field.getType());
        String name = getFieldName(field);
        String extras = createExtrasString(field);
        return name + " " + type + extras;
    }

    private static String createExtrasString(Field field) {
        mySqlColumn mySqlAnnotation = field.getAnnotation(mySqlColumn.class);
        if (mySqlAnnotation == null){
            return "";
        }

        String extras = "";
        if (mySqlAnnotation.notNull()) {
            extras += " NOT NULL";
        }
        if (mySqlAnnotation.unique()) {
            extras += " UNIQUE";
        }
        if (mySqlAnnotation.autoIncrement()) {
            extras += " AUTO_INCREMENT";
        }
        return extras;
    }

    private static String mySqlType(Class<?> clz) {
        switch (clz.getSimpleName()) {
            case "int":
            case "Integer":
                return "INTEGER";
            case "boolean":
            case "Boolean":
                return "BOOLEAN";
            case "byte":
            case "Byte":
                return "TINYINT";
            case "short":
            case "Short":
                return "SMALLINT";
            case "long":
            case "Long":
                return "BIGINT";
            case "float":
            case "Float":
                return "FLOAT";
            case "double":
            case "Double":
                return "DOUBLE";
            case "char":
            case "Character":
                return "CHAR";
            case "String":
                return "VARCHAR(45)";
            default:
                return "TEXT";
        }
    }

    public static <T> String createGetByPropertyQuery(Class<T> clz, String propName, String propVal) {
        return String.format("SELECT * FROM %s WHERE %s = '%s'", clz.getSimpleName().toLowerCase(), propName.toLowerCase(), propVal.toLowerCase());
    }
}
