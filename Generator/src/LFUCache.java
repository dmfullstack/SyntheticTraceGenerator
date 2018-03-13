//Modified LeetCode implementation of an LFU Cache
//By Wendy Aleman Martinez
//Intentional left out value since for the purpose of creating request the value will be equal to the key

import java.util.*;

public class LFUCache implements Cache{
    long stamp;
    int capacity;
    int num;
    PriorityQueue<Pair> minHeap;
    HashMap<Integer, Pair> hashMap;

    // @param capacity, an integer
    public LFUCache(int capacity) {
        // Write your code here
        this.capacity = capacity;
        num = 0;
        minHeap = new PriorityQueue<>();
        hashMap = new HashMap<>();
        stamp = 0;
    }

    // @param key, an integer
    // @param value, an integer
    // @return nothing
    public void set(int key) {
        if (capacity == 0) {
            return;
        }
        // Write your code here
        if (hashMap.containsKey(key)) {
            Pair old = hashMap.get(key);
            minHeap.remove(old);

            Pair newNode = new Pair(key, old.times + 1, stamp++);

            hashMap.put(key, newNode);
            minHeap.offer(newNode);
        } else if (num == capacity) {
            Pair old = minHeap.poll();
            hashMap.remove(old.key);

            Pair newNode = new Pair(key, 1, stamp++);

            hashMap.put(key, newNode);
            minHeap.offer(newNode);
        } else {
            num++;
            Pair pair = new Pair(key, 1, stamp++);
            hashMap.put(key, pair);
            minHeap.offer(pair);
        }
    }

    public int get(int key) {
        if (capacity == 0) {
            return -1;
        }
        // Write your code here
        if (hashMap.containsKey(key)) {
            Pair old = hashMap.get(key);
            minHeap.remove(old);

            Pair newNode = new Pair(key, old.times + 1, stamp++);

            hashMap.put(key, newNode);
            minHeap.offer(newNode);
            return key;
        }
        return -1;
    }
    
    public boolean contains(int key){
        if (capacity == 0) {
            return false;
        }
        return hashMap.containsKey(key);
    }     
    
    public boolean containsOnce (int key){
        if (capacity == 0) {
            return false;
        }
        Pair holder = hashMap.get(key);        
        return holder != null && holder.times == 1;
    }

    public int containsHistory(int key) {
       return -1;
    }
    
    public int containsArc(int key) {
       return -1;
    }
    
    public void printCache(){
        System.out.println("");
    }       
}