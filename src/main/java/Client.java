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

        //getByProperty
        List<User> allMoshes = repo.getByProperty("name", "moshe");
        System.out.println(allMoshes);

        repo.close();
    }
}