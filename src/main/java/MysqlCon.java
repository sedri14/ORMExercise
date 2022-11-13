import Entities.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

class MysqlCon {

    public static List<User> executeQuery(String query) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/orm", "root", "Sharoni123");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt(1));
                user.setName(rs.getString(2));
                user.setAge(rs.getInt(3));
                users.add(user);
            }
            con.close();
            return users;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}  