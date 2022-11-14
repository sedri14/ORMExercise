import Entities.User;

import java.util.List;

public class Client {
    public static void main(String[] args) {

        MysqlCon<User> repo = new MysqlCon<>(User.class);
        //findAll
        List<User> users = repo.findAll();
        users.forEach(System.out::println);

        System.out.println("--------------------------");

        //findOne
        User user = repo.findOne(2);
        System.out.println(user);

        System.out.println("--------------------------");

        //insetOne
        User user1 = new User(105,"TestUser",35);
        repo.insertOne(user1);

        repo.close();
    }
}