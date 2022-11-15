import Entities.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class QueryFactoryTest {

    @Test
    public void createTableMySQLStatement_tableUser_createQuery(){
        Class<?> clz = User.class;
        String query = QueryFactory.createTableMySQLStatement(clz);
        assertEquals("create table user(id int,name varchar(50),age int);",query);
    }
    @Test
    public void createFindOneQuery_userTable_getRows(){
        Class<?> clz = User.class;
        int id = 1;
        String query = QueryFactory.createFindOneQuery( clz,id);
        assertEquals("SELECT * FROM user WHERE id=1",query);
    }
    @Test
    public void createFindAllQuery_userTable_allRows(){
        Class<?> clz = User.class;
        String query = QueryFactory.createFindAllQuery( clz);
        assertEquals("SELECT * FROM user",query);
    }
    @Test
    public void createUpdateSinglePropertyQuery_updatePropertyUserTable_updateQuery(){
        Class<?> clz = User.class;
        String item = "name";
        String newValue = "safaa";
        int id = 1;
        String query = QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id);
        assertEquals("UPDATE user SET name = \"safaa\" WHERE id = 1;",query);
    }
    @Test
    public void createUpdateSinglePropertyQuery_updatePropertyNotExistUserTable_updateQuery(){
        Class<?> clz = User.class;
        String item = "height";
        int newValue = 20;
        int id = 1;
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> QueryFactory.createUpdateSinglePropertyQuery(clz,item,newValue,id));
        assertEquals("There is no field with name height", exception.getMessage());
    }
}