import Entities.User;

import java.util.ArrayList;
import java.util.List;

public class Client {
    public static void main(String[] args) {

        Repository<User> repo = new Repository<>(User.class);
        //READ
        //findAll
        List<User> users = repo.findAll();
        users.forEach(System.out::println);
        System.out.println("--------------------------");
        //findOne
        User user = repo.findOne(2);
        System.out.println(user);
        System.out.println("--------------------------");
        //getByProperty
        List<User> all35 = repo.getByProperty("age", 35);
        System.out.println(all35);

        //ADD
        //insetOne
        User user1 = new User(7878,"Lulu",25);

        repo.insertOne(user1);
        repo.insertOne(user1);
        System.out.println("--------------------------");
        //insertMultiple
        List<User> usersToInsert = new ArrayList<>();
        usersToInsert.add(new User(232323, "Gogo", 28));
        usersToInsert.add(new User(343434, "Yoyo", 35));

        repo.insertMultiple(usersToInsert);
        System.out.println("--------------------------");
        //update a single property of a single item
        repo.updateSingleProperty(4841,"age", 100);
        //repo.updateSingleProperty(4841,"job", 100);
        //repo.updateSingleProperty(4841,"age", "ThisIsNotAge");
        System.out.println("--------------------------");
        //update an entire item
        repo.updateRow(2,new User(2,"saf",33));

        //DELETE
        //delete items
        repo.deleteByProperty("name", "Gogo");
        System.out.println("--------------------------");
        //delete table
        //repo.truncateTable();
        System.out.println("--------------------------");
    }
}