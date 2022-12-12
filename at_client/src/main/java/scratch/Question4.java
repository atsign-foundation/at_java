package scratch;

public class Question4 {
    public static void main(String[] args) {
        System.out.println(go(1, 10, 0));
    }

    public static int go(int le, int ri, int indent) {
        printTabs(indent);
        System.out.println("go(" + le + ", " + ri + ")");
        if (ri-le <=1) {
            return le;
        }
        int cp = le + (ri - le) / 2;
        int m1 = go(le, cp, indent+1);
        int m2 = go(cp, ri, indent+1);
        return Math.max(m1, m2);
    }

    public static void printTabs(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("\t");
        }
    }
}

