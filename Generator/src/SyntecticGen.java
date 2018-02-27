import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SyntecticGen {
    static int exp1Hits, exp1Miss, exp2Hits, exp2Miss, exp1, exp2, cacheeSize, reqRange, neededSeq, noise;
    static boolean start_Exp1;
    static int[] options;
    static Cache exp2_Cache, exp1_Cache;
    static BufferedWriter writer;
    public static void main(String[] args) throws IOException {        
        Scanner in = new Scanner(System.in);
        
        System.out.println("Name of the file to read the Configuration of the Experiment");
        String filenameProp  = in.nextLine();
        System.out.println("Name of the file to write the traces");
        String filenameOut  = in.nextLine();        
        writer = new BufferedWriter(new FileWriter("..\\Output\\"+filenameOut, true));     
        new InputGen(filenameProp);
        LoadPropertieFile();
        if (exp1 == exp2){
            System.out.println("Experts can't be the same algorithm");
            return;
        }
        if (exp1 > 3 || exp1 < 1 || exp2 > 3 || exp2 < 1 ){
            System.out.println("You must select a possible option");
            return;
        }      
        options = new int[reqRange];  
        for (int i = 0; i < reqRange; i++){
            options[i] = i+1;
        }
        shuffleArray(options);   
        
        if (exp1 + exp2 == 3)
            LRULFU();
        else{
            if (exp1 + exp2 == 4){
                LRUARC();
            }
            else{
                LFUARC();
            }
        }
        writer.close();        
        insertFileAttr(Paths.get("..\\Output\\"+filenameOut));
    }
    static void shuffleArray(int[] ar)
    {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
          int index = rnd.nextInt(i + 1);
          int a = ar[index];
          ar[index] = ar[i];
          ar[i] = a;
        }
    }
    
    static int seqInt (int prob, boolean startLRU){
        if (prob < 1)
            return 1;
        if (prob < 30)
            return 2;
        return 3;            
    }
    
    static boolean coinFlip(){
        int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
        return randomNum < 100;
    }
    static boolean addNoise(){
        int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
        return randomNum < noise;
    }
    static void LRULFU() throws IOException{
        { 
            exp1_Cache = new LRUCache(cacheeSize);
            exp2_Cache = new LFUCache(cacheeSize);   

            int rep;
            for (int i = 0; i < cacheeSize; i++){            
                int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
                rep = seqInt(randomNum, start_Exp1);
                for (int j = 0; j < rep; j++){
                    writer.append(options[i] + "\n");
                    exp2_Cache.set(options[i]);
                    exp1_Cache.set(options[i]);
                }           
            }

            int bothmiss, optLru, optLfu;
            boolean flip = false;
            for (int i = 0; i < neededSeq; i++){
                if (addNoise()){
                    int randomReq = ThreadLocalRandom.current().nextInt(0, reqRange);
                    writer.append(randomReq + "\n");                    
                    if (exp2_Cache.contains(randomReq))
                        exp2Hits ++;
                    else
                        exp2Miss ++;
                    if (exp1_Cache.contains(randomReq))
                        exp1Hits ++;
                    else
                        exp1Miss ++;
                    exp1_Cache.set(randomReq);
                    exp2_Cache.set(randomReq);
                }
                else{
                    bothmiss = -1;
                    optLfu = -1;
                    optLru = -1;
                    if (start_Exp1){                
                        for (int j = 0; j < options.length; j++){
                            if (exp1_Cache.contains(options[j]) && !exp2_Cache.contains(options[j]) && optLru == -1 && coinFlip()){
                                optLru = options[j];
                            }
                            if (!exp1_Cache.contains(options[j]) && !exp2_Cache.contains(options[j]) && (bothmiss == -1 || coinFlip()))
                                bothmiss = options[j];
                        }
                        if (optLru != -1){
                            writer.append(optLru + "\n");
                            exp1_Cache.set(optLru);
                            exp2_Cache.set(optLru);
                            exp1Hits ++;
                            exp2Miss ++;
                        }
                        else{
                            writer.append(bothmiss + "\n");
                            exp1_Cache.set(bothmiss);
                            exp2_Cache.set(bothmiss);
                            exp1Miss ++;
                            exp2Miss ++;
                        }
                    }
                    else{
                        for (int j = 0; j < options.length; j++){
                            if (!exp1_Cache.contains(options[j]) && exp2_Cache.contains(options[j]) && (optLfu == -1 || coinFlip())){
                                optLfu = options[j];
                            }
                            if (!exp1_Cache.contains(options[j]) && !exp2_Cache.contains(options[j]) && (bothmiss == -1 || coinFlip()))
                                bothmiss = options[j];                    
                        }
                        if (optLfu != -1){
                            writer.append(optLfu + "\n");     
                            exp1_Cache.set(optLfu);
                            exp2_Cache.set(optLfu);
                            exp2Hits ++;
                            exp1Miss ++;
                        }
                        else{
                            writer.append(bothmiss + "\n"); 
                            exp1_Cache.set(bothmiss);
                            exp2_Cache.set(bothmiss);
                            exp1Miss ++;
                            exp2Miss ++;
                        }
                    }
                }
                if (!flip && i > neededSeq/2 ){
                    start_Exp1 = !start_Exp1;
                    flip = true;
                }
            }
            System.out.println("");
            System.out.println("***************************************************");
            System.out.println("*   LRU hits: "+ exp1Hits +"   LRU Misses: "+ exp1Miss);
            System.out.println("*   LFU hits: "+ exp2Hits +"   LFU Misses: "+ exp2Miss);
            System.out.println("***************************************************");
        }        
    }        
    static void LRUARC()throws IOException { 
        exp1_Cache = new LRUCache(cacheeSize);
        exp2_Cache = new ARCCache(cacheeSize);   

        int rep;
        for (int i = 0; i < cacheeSize; i++){            
            int randomNum = ThreadLocalRandom.current().nextInt(0, 100);
            rep = seqInt(randomNum, start_Exp1);
            for (int j = 0; j < rep; j++){
                writer.append(options[i] + "\n");
                exp1_Cache.set(options[i]);
                exp2_Cache.set(options[i]);
            }           
        }

        int bothmiss, optArc_t1, optArc_t2, optLru, hitHistory_b1, hitHistory_b2;
        boolean flip = false;
        for (int i = 0; i < neededSeq; i++){
            if (addNoise()){
                int randomReq = ThreadLocalRandom.current().nextInt(0, reqRange);
                writer.append(randomReq + "\n");
                if (exp2_Cache.contains(randomReq))
                    exp2Hits ++;
                else
                    exp2Miss ++;
                if (exp1_Cache.contains(randomReq))
                    exp1Hits ++;
                else
                    exp1Miss ++;
                exp1_Cache.set(randomReq);
                exp2_Cache.set(randomReq);
            }
            else{
                hitHistory_b1 = -1;
                hitHistory_b2 = -1;
                bothmiss = -1;
                optArc_t1 = -1;
                optArc_t2 = -1;
                optLru = -1;
                if (start_Exp1){                
                    for (int j = 0; j < options.length; j++){
                        if (exp1_Cache.contains(options[j])){
                            if (exp2_Cache.containsArc(options[j]) == 0){                                
                                if (exp2_Cache.containsHistory(options[j]) == 2 && (hitHistory_b2 == -1)){
                                    hitHistory_b2 = options[j];
                                }
                                else{
                                    if (exp2_Cache.containsHistory(options[j]) == 1 && hitHistory_b1 == -1)
                                        hitHistory_b1 = options[j];
                                    else {
                                        if (exp2_Cache.containsHistory(options[j]) == 0 && (optLru == -1))
                                            optLru = options[j];
                                    }
                                }
                            }
                        }
                        else{
                            if (exp2_Cache.containsArc(options[j]) == 0 && exp2_Cache.containsHistory(options[j]) == 0 && (bothmiss == -1))
                                bothmiss = options[j];
                        }                    
                    }
                    if (hitHistory_b2 != -1){
                            writer.append(hitHistory_b2 + "\n");
                            exp1_Cache.set(hitHistory_b2);
                            exp2_Cache.set(hitHistory_b2);
                            exp1Hits ++;
                            exp2Miss ++;            
                    }
                    else{
                        if (hitHistory_b1 != -1){
                            writer.append(hitHistory_b1 + "\n");
                            exp1_Cache.set(hitHistory_b1);
                            exp2_Cache.set(hitHistory_b1);
                            exp1Hits ++;
                            exp2Miss ++;            
                        }
                        else{
                            if (optLru != -1){
                                writer.append(optLru + "\n");
                                exp1_Cache.set(optLru);
                                exp2_Cache.set(optLru);
                                exp1Hits ++;
                                exp2Miss ++;            
                            }
                            else{
                                writer.append(bothmiss + "\n");
                                exp1_Cache.set(bothmiss);
                                exp2_Cache.set(bothmiss);
                                exp1Miss ++;
                                exp2Miss ++; 
                            }
                        }                    
                    }
                }
                else{
                    for (int j = 0; j < options.length; j++){
                        if (!exp1_Cache.contains(options[j])){
                            if (exp2_Cache.containsArc(options[j]) == 2 && (optArc_t2 == -1 || coinFlip())){                                
                                optArc_t2 = options[j];
                                break;
                            }
                            else{
                                if ( exp2_Cache.containsArc(options[j]) == 1 && (optArc_t1 == -1 || coinFlip()))
                                    optArc_t1 = options[j];
                                else{
                                    if (exp2_Cache.containsHistory(options[j]) == 2 && (hitHistory_b2 == -1 || coinFlip()))
                                        hitHistory_b2 = options[j];
                                    else{
                                        if (exp2_Cache.containsHistory(options[j]) == 1 && (hitHistory_b1 == -1 || coinFlip()))
                                            hitHistory_b1 = options[j];
                                        else{
                                            if (!exp2_Cache.contains(options[j]) && (bothmiss == -1 || coinFlip()))
                                                bothmiss = options[j]; 
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (optArc_t2 != -1){
                        writer.append(optArc_t2 + "\n");     
                        exp1_Cache.set(optArc_t2);
                        exp2_Cache.set(optArc_t2);
                        exp2Hits ++;
                        exp1Miss ++;            
                    }                    
                    else{
                        if (optArc_t1 != -1){
                            writer.append(optArc_t1 + "\n");     
                            exp1_Cache.set(optArc_t1);
                            exp2_Cache.set(optArc_t1);
                            exp2Hits ++;
                            exp1Miss ++;            
                        }
                        else{
                            if (hitHistory_b2 != -1){
                                writer.append(hitHistory_b2 + "\n");
                                exp1_Cache.set(hitHistory_b2);
                                exp2_Cache.set(hitHistory_b2);
                                exp1Miss ++;
                                exp2Miss ++;            
                            }
                            else{
                                if(hitHistory_b1 != -1){
                                    writer.append(hitHistory_b1 + "\n");
                                    exp1_Cache.set(hitHistory_b1);
                                    exp2_Cache.set(hitHistory_b1);
                                    exp1Miss ++;
                                    exp2Miss ++;            
                                }
                                else{
                                    writer.append(bothmiss + "\n"); 
                                    exp1_Cache.set(bothmiss);
                                    exp2_Cache.set(bothmiss);
                                    exp1Miss ++;
                                    exp2Miss ++;            
                                }
                            }
                        }
                    }
                }
            }
            if (!flip && i > neededSeq/2 ){
                start_Exp1 = !start_Exp1;
                flip = true;
                System.out.println("");
                System.out.println("***************************************************");
                System.out.println("*   LRU hits: "+ exp1Hits +"   LRU Misses: "+ exp1Miss);
                System.out.println("*   ARC hits: "+ exp2Hits +"   ARC Misses: "+ exp2Miss);
                System.out.println("***************************************************");
            }
        }
        System.out.println("");
        System.out.println("***************************************************");
        System.out.println("*   LRU hits: "+ exp1Hits +"   LRU Misses: "+ exp1Miss);
        System.out.println("*   ARC hits: "+ exp2Hits +"   ARC Misses: "+ exp2Miss);
        System.out.println("***************************************************");
    }   
        
    static void LFUARC(){
            
    }
    
    static void LoadPropertieFile() throws FileNotFoundException, IOException{
        Properties prop = new Properties();
	InputStream input = null;
        input = new FileInputStream("config.properties");

        // load a properties file
        prop.load(input);

        // get the property value and print it out
        exp1 = Integer.parseInt(prop.getProperty("firstExpert"));
        exp2 = Integer.parseInt(prop.getProperty("secondExpert"));
        cacheeSize = Integer.parseInt(prop.getProperty("cacheSize"));
        reqRange = Integer.parseInt(prop.getProperty("maxRequest"));
        start_Exp1 = prop.getProperty("startFirst").equals("Y");
        noise = Integer.parseInt(prop.getProperty("noiseLevel"));
        neededSeq = Integer.parseInt(prop.getProperty("sequencesNeeded"));
        input.close();
    }
    
    static void insertFileAttr(Path filePath) throws FileNotFoundException, IOException{
        UserDefinedFileAttributeView view = Files.getFileAttributeView(filePath, UserDefinedFileAttributeView.class);
        // The file attribute
        String name = "Configuration";
        String value = "firstExpert: "+ exp1 + "   secondExpert: "+ exp2 +"   cacheSize: "+ cacheeSize +"   maxRequest: "+ reqRange +"   startFirst: "+ !start_Exp1 +"   noiseLevel: "+ noise +"   sequencesNeeded: "+ neededSeq;
        // Write the properties
        byte[] bytes = value.getBytes("UTF-8");
        ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        writeBuffer.put(bytes);
        writeBuffer.flip();
        view.write(name, writeBuffer);
        ByteBuffer readBuffer = ByteBuffer.allocate(view.size(name));
        view.read(name, readBuffer);
        readBuffer.flip();
        String valueFromAttributes = new String(readBuffer.array(), "UTF-8");
        System.out.println("File Attribute: " + valueFromAttributes);
    }    
}



