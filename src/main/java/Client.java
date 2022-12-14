import Entities.User;

import java.util.ArrayList;
import java.util.List;

public class Client {
    public static void main(String[] args) {

        Repository<User> repo = new Repository<>(User.class);
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

        System.out.println("--------------------------");

        //insetOne
        User user1 = new User(7546,"TestUser",35);
        repo.insertOne(user1);

        System.out.println("--------------------------");

        //insertMultiple
        List<User> usersToInsert = new ArrayList<>();
        usersToInsert.add(new User(222, "Gogo", 28));
        usersToInsert.add(new User(333, "Yoyo", 35));
        usersToInsert.add(new User(444, "Dodo", 15));

        repo.insertMultiple(usersToInsert);

        //repo.insertOne(new User(2,"safaa",20));
        repo.updateRow(2,new User(2,"saf",33));
    }
}