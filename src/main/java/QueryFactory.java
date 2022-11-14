import Annotations.mySqlColumn;

import java.lang.reflect.Field;
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
}
