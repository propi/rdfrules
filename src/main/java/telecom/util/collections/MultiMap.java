package telecom.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MultiMap<K, V> {
    private Map<K, List<V>> mLocalMap = new HashMap<K, List<V>>();

    public void add(K key, V val) {
        if (mLocalMap.containsKey(key)) {
            List<V> list = mLocalMap.get(key);
            if (list != null)
                list.add(val);
        } else {
            List<V> list = new ArrayList<V>();
            list.add(val);
            mLocalMap.put(key, list);
        }
    }
    public List<V> get(K key) {
        return mLocalMap.get(key);
    }
    public V get(K key, int index) {
        return mLocalMap.get(key).get(index);
    }
    public void put(K key, V val) {
        remove(key);
        add(key, val);
    }
    public void remove(K key) {
        mLocalMap.remove(key);
    }
    public void removeAll(Collection<K> keys) {
    	for (K key : keys)
    		remove(key);
    }
    public void clear() {
        mLocalMap.clear();
    }
    public String toString() {
        StringBuffer str = new StringBuffer();
        for (K key : mLocalMap.keySet()) {
            str.append("[ ");
            str.append(key);
            str.append(" : ");
            str.append(java.util.Arrays.toString(mLocalMap.get(key).toArray()));
            str.append(" ]");
        }
        return str.toString();
    }
    public Set<K> keySet() {
        return mLocalMap.keySet();
    }
    
    public Set<Entry<K,List<V>>> entrySet() {
        return mLocalMap.entrySet();
    }
    public List<V> values() {
    	List<V> result = new ArrayList<>();
    	for (List<V> bucket : mLocalMap.values()) {
    		result.addAll(bucket);
    	}
    	return result;
    }
    public int keySize() {
    	return mLocalMap.size();
    }    
    public int valueSize() {
    	int valueSize = 0;
    	for (K key : mLocalMap.keySet()) {
    		valueSize += mLocalMap.get(key).size();
    	}
    	return valueSize;
    }
   
}