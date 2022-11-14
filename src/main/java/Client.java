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
        User user = repo.findOne(1);
        System.out.println(user);

        repo.updateSingleProperty(1,"age","21");
        System.out.println(repo.findOne(1));

        repo.updateRow(1,user);
        repo.close();
    }
}