package com.jshell;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shell{

    /**
     * Object that has the command shell implementation
     */
    Object shellCommandInterface;

    /**
     * Thread used for the shell loop
     */
    Thread loopThread;

    /**
     * Shell Loop, input handler for the shell
     */
    ShellLoop shellLoop;

    /**
     * Determines if the shell is running or not
     */
    boolean running=false;

    /**
     * Determines if the shell has been initialized
     */
    boolean initialized = false;

    /**
     * All the methods annotated with the @ShellCommand annotation
     */
    HashMap<String, Method> shellMethods;

    /**
     * Specifies the shellprompt
     */
    String shellPrompt;

    /**
     * specifies the argument delimiter
     */
    String argumentDelimiter;

    /**
     * Specifies if the shell output should be timestamped
     */
    boolean timeStampEnabled;

    /**
     * Specifies if the shell requires a login
     */
    boolean requireLogin;

    /**
     * Regex pattern for the command.
     */
    Pattern commandPattern;

    public Shell(Object shellCommandInterface){
        this.shellCommandInterface = shellCommandInterface;
        shellMethods = new HashMap<>();
        processAnnotations(shellCommandInterface);
    }

    /**
     * Initializes the shell
     * @param obj object with ShellClass annotation
     */
    void initializeShell(Class obj){
        Annotation annotation = obj.getAnnotation(ShellClass.class);
        ShellClass shellClass = (ShellClass)annotation;
        shellPrompt = shellClass.shellPrompt();
        argumentDelimiter = shellClass.argumentDelimiter();
        timeStampEnabled = shellClass.timeStampEnable();
        requireLogin = shellClass.requireLogin();

        commandPattern =  Pattern.compile("(^\\S*){1}|("+argumentDelimiter+"[\\w]+)");

        initialized = true;
    }

    /**
     * Initializes annotations
     * @param shellCommandInterface the object with ShellClass annotation
     */
    void processAnnotations(Object shellCommandInterface){
        Class<Object> obj = (Class<Object>) shellCommandInterface.getClass();
        if(obj.isAnnotationPresent(ShellClass.class)){
            initializeShell(obj);
        }else{
            System.out.println("Object does not have ShellClass annotation shell will not start.");
            return;
        }

        processMethodAnnotations(obj);


    }

    /**
     * Processes all method annotations.
     * @param obj the class containing @ShellCommand annotations.
     */
    void processMethodAnnotations(Class obj){
        for(Method method : obj.getDeclaredMethods()){

            Annotation annotation = method.getAnnotation(ShellCommand.class);
            if(annotation == null){
               continue;
            }else{
                ShellCommand shellCommand = (ShellCommand)annotation;

                if(shellCommand.commandName().isEmpty() || shellCommand.commandName().equals("")){
                    shellMethods.put(method.getName(),method);
                }else{
                    shellMethods.put(shellCommand.commandName().replaceAll("\\s",""),method);
                }
            }
        }


    }

    /**
     * Process input from the shell loop.
     * @param input
     */
    void processInput(String input){
        Matcher matcher = commandPattern.matcher(input);

        ArrayList<String> matches = new ArrayList<>();

        ArrayList<String> commands = new ArrayList<>();

        ArrayList<String> arguments = new ArrayList<>();

        String command= "";

        while(matcher.find()){
            commands.add(matcher.group(1));
            matches.add(matcher.group(2));
        }

        command = commands.get(0);

        //System.out.println(command);

        for(String s : matches){
            if(s == null || s.isEmpty()){
                continue;
            }else{
                String arg = s.replace("\\s","");
                arg = arg.replace(argumentDelimiter,"");
                arguments.add(arg);
            }
        }
        //System.out.println("Checking Commands, command count: "+shellMethods.size());
        for(Entry entry : shellMethods.entrySet()){
            //Check if a valid command was input.
            if(command.equals(entry.getKey())){
                //System.out.println("Found command: "+entry.getKey());
                Method m = (Method) entry.getValue();
                //Check if the correct number of arguments were provided.
                if(m.getParameterCount() == arguments.size()){
                    //System.out.println("Correct number of arguments passed: "+m.getParameterCount());
                    Class[] types = m.getParameterTypes();
                    ArrayList<Object> convertedArguments = new ArrayList<>();
                    for(int i = 0; i < types.length ;i++){

                        convertedArguments.add(convertArgumentType(types[i],arguments.get(i)));
                    }
                    try {
                        //System.out.println("Invoking command: "+entry.getKey());
                        m.invoke(shellCommandInterface,convertedArguments.toArray(new Object[convertedArguments.size()]));
                        return;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("Incorrect number of arguments.");
                }
            }
        }
        System.out.println("Unsupported Command.");
    }

    /**
     * Converts the arguments to the appropriate type.
     * @param c the class to convert to
     * @param argument the string value of the argument.
     * @return the converted object
     */
    Object convertArgumentType(Class c, String argument){
        //System.out.println("Class type: "+c.toString() + " argument: "+argument);
        if(c == boolean.class){
            return Boolean.parseBoolean(argument);
        }else if(byte.class.equals(c.getClass())){
            return Byte.parseByte(argument);
        }else if(short.class.equals(c.getClass())){
            return Short.parseShort(argument);
        }else if(int.class.equals(c)){
            return Integer.parseInt(argument);
        }else if(long.class.equals(c.getClass())){
            return Long.parseLong(argument);
        }else if(float.class.equals(c.getClass())){
            return Float.parseFloat(argument);
        }else if(double.class.equals(c.getClass())){
            return Double.parseDouble(argument);
        }else if(c == String.class){
            return argument;
        }

        System.out.println("Unsupported argument type.");

        return null;
    }

//    /**
//     * Adds methods to the shell.  This can be used if additional methods outside of the shell class are to be added.
//     * @param shellCommandInterface the object to check for ShellCommand annotations.
//     */
//    public void addMethods(Object shellCommandInterface){
//        Class<Object> obj = (Class<Object>) shellCommandInterface.getClass();
//        processMethodAnnotations(obj);
//    }

    /**
     * Stops the shell from running.
     */
    public void stop(){
        if(running){
            shellLoop.running = false;
            loopThread.interrupt();
            loopThread = null;
            shellLoop = null;
            running = false;
        }
    }

    /**
     * Starts the shell.
     */
    public void start(){

        if(!initialized)
            return;

        if(!running){
            shellLoop = new ShellLoop(this);
            loopThread = new Thread(shellLoop);
            loopThread.setDaemon(true);
            loopThread.start();
            running = true;
        }
    }


    /**
     * Handles the input for the shell.
     */
    private class ShellLoop implements Runnable{

        /**
         * Reference to the shell.
         */
        Shell shell;

        /**
         * Specifies if the shell loop is running.
         */
        boolean running = true;

        /**
         * Reads in the console input.
         */
        Scanner reader;

        public ShellLoop(Shell shell){
            this.shell = shell;
            reader = new Scanner(System.in);
        }

        @Override
        public void run() {
            System.out.print(shell.shellPrompt+" ");
            while(running){
                try {
                    shell.processInput(reader.nextLine());
                }catch(Exception e){
                    e.printStackTrace();
                }
                if(shell.timeStampEnabled){
                    System.out.print(LocalDateTime.now()+" "+shell.shellPrompt+" ");
                }else{
                    System.out.print(shell.shellPrompt+" ");
                }

            }
        }
    }
}
