package utils;

import java.util.HashMap;

/**
 * Utility methods used across EchoServer and ChatClient
 */
public class SCUtilities {

    /**
     * Checks if a String instance is not null and not empty
     * @param str the string object to check
     * @return false if string is null or empty, else true
     */
    public static boolean isValidString(String str) {
        return (str != null && !str.isEmpty());
    }

    /**
     * Checks if an array object is not null and not empty
     * @param objArray array object to be checked
     * @return false if array is null or empty, else true
     */
    public static boolean isValidArray(Object[] objArray) {
        return (objArray != null && objArray.length > 0);
    }

    /**
     * Checks is a provided command value is one of the accepted commands
     * @param command string value of the command to be validated
     * @return true if valid command, else false
     */
    public static boolean isValidCommand(String command, HashMap<String, Boolean> acceptedCommands) {
        return command != null && !command.isEmpty() && acceptedCommands.containsKey(command);
    }

    /**
     * Method to extract command and arguments separately from a string based on a separator
     * @param userInput string containing command and args
     * @param command_args_separator separator for separating command and argument in the userInput
     * @return String array, command value at index 0, and arguments (if any) at index 1
     */
    public static String[] extractCommandAndArgs(String userInput, String command_args_separator) {
        // guard-clause, if not a valid userInput throw null pointer exception
        if (!isValidString(userInput)) {
            return null;
        }
        // process user input, split on space
        String[] result = userInput.split(command_args_separator);
        // extract the command (without COMMAND_PREFIX)
        return new String[]{ result[0], (result.length >= 2) ? result[1] : null };
    }
}
