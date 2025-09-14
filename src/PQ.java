import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class PQ<t> implements Iterable<t>{
    public class PQNode<t> {
        t obj;
        int priority;

        public PQNode(t o, int p) {
            obj = o;
            priority = p;
        }
    }

    ArrayList<PQNode<t>> queue = new ArrayList<PQNode<t>>();

    public void add(t o) {
        if (!queue.contains(o)) {
            queue.add(insertIndex(o, 0), new PQNode<t>(o, 0));
        }
    }

    public void add(t o, int p) {
        if (!contains(o)) {
            queue.add(insertIndex(o, p), new PQNode<t>(o, p));
        }
    }

    public int insertIndex(t o, int p) {
      /*int mid = queue.size() / 2;
      int bot = -1;
      int top = queue.size();
      while (top != bot) {
        if ()
      }*/
      for (int x = 0; x < size(); x++) {
        if (queue.get(x).priority <= p) return x;
      }
      return size();
    }

    public t get(int x) {
        return queue.get(x).obj;
    }

    public boolean contains(t o) {
        for (PQNode n : queue) {
            if (n.obj == o) return true;
        }
        return false;
    }

    public int indexOf(t o) {
        for (int x = 0; x < size(); x++) {
            if (get(x) == o) return x;
        }
        return -1;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
      queue.clear();
    }

    public void remove(t o) {
        for (int x = 0; x < size(); x++) {
            if (get(x) == o) queue.remove(x);
        }
    }

    public Iterator<t> iterator() {
        return new PQIterator<t>();
    }
    
    public class PQIterator<t2> implements Iterator<t> {
        int i = -1;
        public boolean hasNext() {
            return i+1 < queue.size();
        }
        public t next() {
            i++;
            return queue.get(i).obj;
        }
    }
}