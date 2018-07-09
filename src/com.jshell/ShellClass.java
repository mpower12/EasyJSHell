package com.jshell;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShellClass {

    /**
     * Designates the prompt used in the shell.
     *  ">" is the default value.
     * @return the shellPrompt.
     */
    String shellPrompt() default ">";

    /**
     * Enables timestamping on the shell interface.
     * @return true if enabled.
     */
    boolean timeStampEnable() default false;

    /**
     * Requires login to be authenticated before commands can be executed.
     * @return true if login required.
     */
    boolean requireLogin() default  false;

    /**
     * Sets the argument delimiter.
     * @return the delimiter.
     */
    String argumentDelimiter() default " ";


}
