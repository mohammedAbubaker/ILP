package uk.ac.ed.inf;
public class App
{
    public static void main( String[] args )
    {
        AppService appService = new AppService(args[1], args[0]);
        appService.runAppService();
    }
}
