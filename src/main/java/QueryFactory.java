import Annotations.mySqlColumn;
import com.google.gson.Gson;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QueryFactory {
    static String createTableMySQLStatement(Class<?> clz) {
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

    private static String createColumnMySqlDeclaration(Field field) {
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

    private static String primaryKeyString(Field[] fields) {
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

    //Add a single row:
    //INSERT INTO table_name (column1, column2, column3,etc)
    //VALUES (value1, value2, value3, etc);
    public static <T> String createInsertOneQuery(T instance) {

        Class<?> clz = instance.getClass();
        String tableName = clz.getSimpleName().toLowerCase();

        StringBuilder queryString = new StringBuilder(String.format("INSERT INTO %s ", tableName));
        String columnsString = columnsFormattedString(instance);
        StringBuilder valuesString = new StringBuilder("VALUES ").append(valuesFormattedString(instance));

        return queryString.append(columnsString).append(valuesString).toString();
    }
    public static <T> String createInsertMultipleQuery(List<T> itemList, Class<?> clz) {

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
    private static String handleValue(Object val) {
        if (ClassUtils.isPrimitiveOrWrapper(val.getClass())) {
            return val.toString();
        } else if (val instanceof String) {
            return String.format("\"%s\"", val.toString());
        } else {
            return new Gson().toJson(val);
        }
    }

    //TODO: change parameter to Class<?> clz, we don't actually need an instance here.
    private static <T> String columnsFormattedString(T instance) {

        StringBuilder columnsString = new StringBuilder("(");

        Field[] declaredFields = instance.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            columnsString.append(fieldName);
            columnsString.append(",");
        }
        columnsString.delete(columnsString.length() - 1, columnsString.length());
        columnsString.append(")");

        return columnsString.toString();
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

        return valuesString.toString();
    }
}
