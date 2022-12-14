package Entities;

import Annotations.mySqlColumn;

public class User {
    @mySqlColumn(autoIncrement = true, primaryKey = true)
    public int id;
    public String name;
    public int age;

    public Animal animal;

    public User(){

    }

    public User(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
        animal = new Animal(6, "sdfsd", "sefsdf", "sssdfsdgs");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
