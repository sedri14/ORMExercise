import Annotations.mySqlColumn;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

class MyInt{
    int id;

    public MyInt() {
        this.id = ThreadLocalRandom.current().nextInt();
    }

    public MyInt(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((MyInt)obj).id;
    }
}

class WithObject {
    int id;

    List<Float> grades;

    public WithObject() {
        id = ThreadLocalRandom.current().nextInt();
    }

    public WithObject(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WithObject that = (WithObject) o;
        return id == that.id && Objects.equals(grades, that.grades);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, grades);
    }
}

class WithAnnotations {
    @mySqlColumn(columnName = "id", primaryKey = true)
    String thisId;

    @mySqlColumn(columnName = "scienceGrade", notNull = true)
    Double grade;

    @mySqlColumn(columnName = "ageAtStart")
    int age;

    public WithAnnotations() {
        this.thisId = String.valueOf(ThreadLocalRandom.current().nextInt());
        this.grade = ThreadLocalRandom.current().nextDouble();
    }

    public WithAnnotations(int id) {
        this.thisId = String.valueOf(id);
        this.grade = ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WithAnnotations that = (WithAnnotations) o;
        return age == that.age && thisId.equals(that.thisId) && grade.equals(that.grade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(thisId, grade, age);
    }
}

class NoId {
    String myString;
    double grade;

    public NoId() {
        this.myString = "Hello";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoId noId = (NoId) o;
        return Double.compare(noId.grade, grade) == 0 && Objects.equals(myString, noId.myString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myString, grade);
    }
}