import Entities.User;

import java.util.List;

public class Client {
    public static void main(String[] args) {
        List<User> users = MysqlCon.executeQuery("select * from user");
        users.forEach(user -> System.out.println(user));
    }
}