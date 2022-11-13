import Entities.User;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class MysqlCon {

    public static <T> T findOne(int id, Class<T> clz) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/orm", "root", "Sharoni123");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s WHERE id=%d", clz.getSimpleName().toLowerCase(), id));

            Constructor<T> constructor = clz.getConstructor(null);
            T clzInstance = constructor.newInstance();
            if (rs.next()) {
                fieldsAssignment(clz, clzInstance, rs);
            } else {

            }
            con.close();

            return clzInstance;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public static <T> List<T> findAll(Class<T> clz) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/orm", "root", "Sharoni123");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s", clz.getSimpleName().toLowerCase()));
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(makeSingleInstance(clz,rs));
            }
            con.close();

            return results;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }


    public static <T> T makeSingleInstance(Class<T> clz, ResultSet rs) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, SQLException {
        Constructor<T> constructor = clz.getConstructor(null);
        T clzInstance = constructor.newInstance();
        fieldsAssignment(clz, clzInstance, rs);

        return clzInstance;
    }

    public static <T> void fieldsAssignment(Class<T> clz, T clzInstance, ResultSet rs) throws SQLException, IllegalAccessException {
        Field[] declaredFields = clz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            field.set(clzInstance, rs.getObject(field.getName()));
        }
    }
}  