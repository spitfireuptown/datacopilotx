import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("datacopilotx");
        System.out.println(hash);
        System.out.println("Length: " + hash.length());
        System.out.println("Matches: " + encoder.matches("datacopilotx", hash));
    }
}
