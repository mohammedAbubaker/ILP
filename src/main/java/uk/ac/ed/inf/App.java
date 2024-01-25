package uk.ac.ed.inf;

/**
 * Instantiates a context with the provided arguments.
 * Argument can be either [-date, -url], or [-url]
 * */
public class App
{
    public static void main( String[] args )
    {
        // Reject if too many arguments passed in
        if (args.length > 2) {
            System.err.println("Error: invalid number of arguments provided. Please try again with the format [-date -url] or  [-url].");
        }
        // Case where only url is provided
        if (args.length == 1) {
            new Context(args[0], "");
        }
        // Case where date and url are provided
        else {
            String date = args[0];
            String url = args[1];
            new Context(url, date);
            System.out.println("Program terminated successfully. Exiting...");
        }
    }
}