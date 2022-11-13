import Entities.User;

import java.util.List;

public class Client {
    public static void main(String[] args) {

        //findAll
        List<User> users = MysqlCon.findAll(User.class);
        users.forEach(user -> System.out.println(user));

        System.out.println("--------------------------");

        //findOne
        User user = MysqlCon.findOne(2, User.class);
        System.out.println(user);
    }
}