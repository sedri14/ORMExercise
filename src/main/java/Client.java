import Entities.User;

import java.util.List;

public class Client {
    public static void main(String[] args) {

        //findAll
        List<User> users = MysqlCon.executeQuery("select * from user");
        users.forEach(user -> System.out.println(user));

        //findOne
        User user = MysqlCon.findOne(2);
        System.out.println(user);
    }
}