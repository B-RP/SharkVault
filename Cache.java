import java.util.ArrayList;
import java.util.List;

//When out of space, removes the element that was least recently used

public class Cache {
    List<cacheElement> cache;
    int size;
    int count;

    Cache(int size){
        this.size = size;
        cache = new ArrayList<>();
    }

    public void add(Object value, String ID){
        cacheElement ce = new cacheElement(value, ID);
        if(count == size){ //max sized is reached
            cache.remove(0); //remove last element to be used
            cache.add(ce);
        }
        else{
            cache.add(ce);
            count++;
        }
    }

    public boolean remove(String ID){
        cacheElement target = new cacheElement(null, ID);
        int index = cache.indexOf(target);
        if(index > -1){
            cache.remove(index);
            count --;
            return true;
        }
        return false;
    }

    public Object search(String ID){
        cacheElement target = new cacheElement(null, ID);
        int index = cache.indexOf(target);
        if(index > -1){
            cacheElement found = cache.get(index);
            //remove and add the found element to reset the position to the top
            cache.remove(index);
            cache.add(found);

            return found.value;

        }
        return null;
    }
    
    public String toString(){
        String s = "";
        for(int i = 0; i < count; i++){
            s += cache.get(i);
            s += '\n';
        }
        return s;
    }
}

class cacheElement {
    public String id;
    public Object value;

    cacheElement(Object o, String ID){
        this.id = ID;
        this.value = o;
    }

    @Override
    public boolean equals(Object p){

        cacheElement ce = (cacheElement) p;

        //we find cache elements based on id only
        if(p instanceof cacheElement){
            return (ce.id == this.id);

        }
        else{
            return false;
        }
        
    }
    
    public String toString(){

        String s = id+" "+value;
        return s;
    }

}


