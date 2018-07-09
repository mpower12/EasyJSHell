package com.jshell;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ShellCommand {

    /**
     * Default will be the name of the method.
     * Do not use spaces.
     * @return returns the commandName string.
     */
    String commandName() default "";

}
