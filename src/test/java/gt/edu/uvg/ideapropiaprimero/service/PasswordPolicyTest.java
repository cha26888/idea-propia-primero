package gt.edu.uvg.ideapropiaprimero.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PasswordPolicyTest {
    @Test void configurableLengthWithoutCompositionRules() {
        assertThat(new PasswordPolicy(4).error("1234")).isNull();
        assertThat(new PasswordPolicy(4).error("abcd")).isNull();
        assertThat(new PasswordPolicy(8).error("abcd")).isNotNull();
        assertThat(new PasswordPolicy(4).error("   ")).isNotNull();
        assertThat(new PasswordPolicy(4).error("a".repeat(73))).isNotNull();
        assertThat(new PasswordPolicy(4).error("é".repeat(37))).isNotNull();
        assertThatThrownBy(() -> new PasswordPolicy(3)).isInstanceOf(IllegalArgumentException.class);
    }
}
