import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class RepositoryTests {

    private Repository<?> repo;

    private static Stream<Arguments> classesToCheck() {
        return Stream.of(
                Arguments.of(MyInt.class, new MyInt()),
                Arguments.of(WithObject.class, new WithObject()),
                Arguments.of(WithAnnotations.class, new WithAnnotations())
        );
    }

    private static Stream<Arguments> classesToCheckWithObjectsOfID1() {
        return Stream.of(
                Arguments.of(MyInt.class, new MyInt(1)),
                Arguments.of(WithObject.class, new WithObject(1)),
                Arguments.of(WithAnnotations.class, new WithAnnotations(1))
        );
    }

    @AfterEach
    void cleanUp() {
        if (repo != null) {
            repo.truncateTable();
            repo = null;
        }
    }

    @ParameterizedTest
    @MethodSource("classesToCheck")
    void init_defaultClass_DoesNotThrow(Class<?> clz) {
        Assertions.assertDoesNotThrow(() -> new Repository<>(clz));
    }

    @ParameterizedTest
    @MethodSource("classesToCheck")
    <T> void insertOne_defaultObject_Returns1(Class<T> clz, T o) {
        repo = new Repository<>(clz);

        Assertions.assertEquals(1, repo.insertOne(o), "failed to insert into the database");
    }

    @ParameterizedTest
    @MethodSource("classesToCheck")
    <T> void insertMultipleToTable_defaultObject_Returns1(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        ArrayList<T> objects = new ArrayList<>();
        objects.add(o);

        Assertions.assertEquals(1, repo.insertMultiple(objects), "Failed to insert multiple into database");
    }

    @ParameterizedTest
    @MethodSource("classesToCheckWithObjectsOfID1")
    <T> void getByProperty_defaultObject_ReturnsObject(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        Assertions.assertTrue(repo.getByProperty("id", "1").contains(o),"Get By Property doesn't return correct variables");
    }

    @ParameterizedTest
    @MethodSource("classesToCheckWithObjectsOfID1")
    <T> void findOne_defaultObject_ReturnsObject(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        Assertions.assertEquals(repo.findOne(1), o, "find one doesn't return correct object");
    }

    @ParameterizedTest
    @MethodSource("classesToCheck")
    <T> void findAll_defaultObject_ReturnsObjectInList(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        List<?> objects = repo.findAll();
        Assertions.assertTrue(objects.stream().anyMatch(object -> Objects.equals(o, object)), "find all doesn't return objects");
    }

    @ParameterizedTest
    @MethodSource("classesToCheck")
    <T> void truncateTable_defaultObject_RemovesAllObjects(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);
        repo.truncateTable();

        Assertions.assertEquals(0, repo.findAll().size(), "Truncate didn't erase rows");
    }

    @ParameterizedTest
    @MethodSource("classesToCheckWithObjectsOfID1")
    <T> void updateRow_defaultObject_returns1(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        Assertions.assertEquals(1, ((Repository<T>)repo).updateRow(1, o), "update row returns unexpected number");
    }

    @ParameterizedTest
    @MethodSource("classesToCheckWithObjectsOfID1")
    <T> void updateSingleProperty_defaultObject_Returns1(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        Assertions.assertEquals(1, repo.updateSingleProperty(1, "id", "2"), "update by property returns unexpected value");
    }

    @ParameterizedTest
    @MethodSource("classesToCheckWithObjectsOfID1")
    <T> void itemDeletionByProperty_defaultObject_Returns1(Class<T> clz, T o) {
        repo = new Repository<>(clz);
        repo.insertOne(o);

        Assertions.assertEquals(1, repo.singleAndMultipleItemDeletionByProperty("id", 1));
    }
}
