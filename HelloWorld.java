import java.util.*;


public class HelloWorld{
    
    
    public static Map<Long,Integer> countRecords =  new HashMap<>();
    public static void insertRecord(long n){
        boolean keyExists = countRecords.containsKey(new Long(n));
        if(keyExists){
            Integer value =countRecords.get(new Long(n));
            countRecords.put(n,++value);
        }
        
        else {
            countRecords.put(new Long(n),new Integer(1));
        }
    }
    
    
    public static void printMap() {
        Iterator it = countRecords.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }



    public static void main(String[] args) { 

        // command-line argument
        //long n = Long.parseLong(args[0]);
        
        long n= 216;

        System.out.print("The prime factorization of " + n + " is: ");

        // for each potential factor i
        for (long i = 2; i*i <= n; i++) {
            // if i is a factor of N, repeatedly divide it out
            while (n % i == 0) {
                HelloWorld.insertRecord(i);
                System.out.print(i + " "); 
                n = n / i;
            }
        }

        // if biggest factor occurs only once, n > 1
        if (n > 1){
            HelloWorld.insertRecord(n);
            System.out.println(n);
        }
        else{       
            printMap();
            System.out.println();
        }
    }
}


