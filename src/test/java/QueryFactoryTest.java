import Entities.Animal;
import Entities.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class QueryFactoryTest {
    static Class<?> clz;
    @BeforeAll
    public static void classInstance(){
        clz = User.class;
    }
    @Test
    public void createTableMySQLStatement_tableUser_createQuery(){
        String query = QueryFactory.createTableMySQLStatement(clz);
        assertEquals("CREATE TABLE user(id INTEGER AUTO_INCREMENT, name VARCHAR(45), age INTEGER, CONSTRAINT PK_test PRIMARY KEY (id));",query);
    }
    @Test
    public void createTableMySQLStatement_tableAnimal_createQuery(){
        String query = QueryFactory.createTableMySQLStatement(Animal.class);
        assertEquals("CREATE TABLE animal(id INTEGER, name VARCHAR(45), color VARCHAR(45), gender VARCHAR(45));",query);
    }
    @Test
    public void createFindOneQuery_userTable_getRows(){
        int id = 1;
        String query = QueryFactory.createFindOneQuery( clz,id);
        assertEquals("SELECT * FROM user WHERE id=1",query);
    }
    @Test
    public void createFindAllQuery_userTable_allRows(){
        String query = QueryFactory.createFindAllQuery( clz);
        assertEquals("SELECT * FROM user",query);
    }
    @Test
    public void createUpdateSinglePropertyQuery_updatePropertyUserTable_updateQuery(){
        String item = "name";
        String newValue = "safaa";
        int id = 1;
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id);
        assertEquals("UPDATE user SET name = \"safaa\" WHERE id = 1;",query);
    }
    @Test
    public void createUpdateSinglePropertyQuery_updatePropertyNotExistInUserTable_updateQuery(){
        String item = "height";
        int newValue = 20;
        int id = 1;
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id));
        assertEquals("There is no field with name height", exception.getMessage());
    }
    @Test
    public void createDeleteQuery_deleteFromUserTable_deleteQuery(){
        String property = "name";
        String value = "saf";
        String query = QueryFactory.createDeleteQuery(clz,property,value);
        assertEquals("DELETE FROM user WHERE name=\"saf\";",query);
    }
    @Test
    public void createDeleteQuery_deleteFromUserTable_valueTypeNotFit(){
        String property = "name";
        int value = 33;
        Throwable exception = assertThrows(IllegalArgumentException.class,() -> QueryFactory.createDeleteQuery(clz,property,value));
        assertEquals("The value and the required field's type are different",exception.getMessage());
    }
    @Test
    public void createUpdateRowQuery_updateRowInUserTable_updateQueryWithManyProperties() throws IllegalAccessException {
        User user = new User(2,"safaa",23);
        String query = QueryFactory.createUpdateRowQuery(clz,user,2);
        assertEquals("UPDATE user SET id=2,name=\"safaa\",age=23 WHERE id=2;",query);
    }
    @Test
    public void createInsertOneQuery_insertRowInUserTable_insertQuery() {
        User user = new User(3,"soso",30);
        String query = QueryFactory.createInsertOneQuery(user);
        assertEquals("INSERT INTO user (id,name,age) VALUES (3,\"soso\",30)",query);
    }
    @Test
    public void createInsertMultipleQuery_insertRowsInUserTable_insertQuery() {
        User user1 = new User(3,"soso",30);
        User user2 = new User(4,"fofo",40);

        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        String query = QueryFactory.createInsertMultipleQuery(users,clz);
        assertEquals("INSERT INTO user (id,name,age) VALUES (3,\"soso\",30),(4,\"fofo\",40)",query);
    }
    @Test
    public void handleValue_string_stringWithApostrophes(){
        String string = "hello";
        String result = QueryFactory.handleValue(string);
        assertEquals("\"hello\"",result);
    }
    @Test
    public void handleValue_int_stringWithoutApostrophes(){
        int someInt = 4;
        String result = QueryFactory.handleValue(someInt);
        assertEquals("4",result);
    }
    @Test
    public void handleValue_userAsObject_stringJison(){
        User user = new User(3,"soso",30);
        String result = QueryFactory.handleValue(user);
        assertEquals("\"{\"id\":3,\"name\":\"soso\",\"age\":30}\"",result);
    }
    @Test
    public void getFieldName_firstFieldInUserClass_id(){
        String result = QueryFactory.getFieldName(clz.getDeclaredFields()[0]);
        assertEquals("id",result);
    }
    @Test
    public void createGetByPropertyQuery_getRowByPropertyNameFromUserTable_getRowsWithProperty() {
        String propName ="name";
        String propVal = "safaa";
        String query = QueryFactory.createGetByPropertyQuery(clz, propName,propVal);
        assertEquals("SELECT * FROM user WHERE name = \"safaa\"",query);
    }

}