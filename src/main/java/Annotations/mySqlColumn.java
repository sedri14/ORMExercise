package Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface mySqlColumn {

    int length() default 0;
    String columnName() default "";

    boolean notNull() default false;
    boolean unique() default false;
    boolean primaryKey() default false;
    boolean autoIncrement() default false;
}
