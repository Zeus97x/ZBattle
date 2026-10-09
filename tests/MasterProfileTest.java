import com.zeus97x.zbattle.MasterProfile;
public final class MasterProfileTest {
    private static void invalid(String name, int style, int gender, int starter) {
        try { new MasterProfile(name, style, gender, starter); throw new AssertionError("Invalid profile accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        for (int s=0;s<5;s++) for(int g=0;g<2;g++) for(int p=0;p<3;p++) {
            MasterProfile profile = new MasterProfile(" Zeus97x ", s, g, p);
            if (!profile.name.equals("Zeus97x")) throw new AssertionError("Name normalization failed");
        }
        invalid(null,0,0,0); invalid(" ",0,0,0); invalid("a".repeat(25),0,0,0);
        invalid("Zeus",-1,0,0); invalid("Zeus",5,0,0); invalid("Zeus",0,-1,0);
        invalid("Zeus",0,2,0); invalid("Zeus",0,0,-1); invalid("Zeus",0,0,3);
        System.out.println("PASS: 30 appearance/companion combinations and 9 invalid profiles.");
    }
}
