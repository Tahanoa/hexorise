package org.example.hexorise.bot;
import org.example.hexorise.room.RoomSettings;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
class SpamGuardTest {
    final AtomicLong clock = new AtomicLong(TimeUnit.SECONDS.toNanos(1));
    final SpamGuard guard = new SpamGuard(clock::get);
    @Test void blocksRepeatedMessagesOnlyOnceThenRecovers() {
        var room = RoomSettings.defaults();
        for (int i = 0; i < 3; i++) assertThat(guard.check("user", "!ping", room)).isEqualTo(SpamGuard.Verdict.ALLOW);
        assertThat(guard.check("user", "  !PING  ", room)).isEqualTo(SpamGuard.Verdict.NEW_BLOCK);
        assertThat(guard.check("user", "different", room)).isEqualTo(SpamGuard.Verdict.BLOCKED);
        assertThat(guard.check("another", "!ping", room)).isEqualTo(SpamGuard.Verdict.ALLOW);
        clock.addAndGet(TimeUnit.SECONDS.toNanos(20));
        assertThat(guard.check("user", "!ping", room)).isEqualTo(SpamGuard.Verdict.ALLOW);
    }
    @Test void blocksMessageBurstAndExpiresWindow() {
        var room = RoomSettings.defaults();
        for (int i = 0; i < 6; i++) assertThat(guard.check("user", "message" + i, room)).isEqualTo(SpamGuard.Verdict.ALLOW);
        clock.addAndGet(TimeUnit.SECONDS.toNanos(11));
        assertThat(guard.check("user", "after-window", room)).isEqualTo(SpamGuard.Verdict.ALLOW);
        for (int i = 0; i < 5; i++) guard.check("user", "next" + i, room);
        assertThat(guard.check("user", "seventh", room)).isEqualTo(SpamGuard.Verdict.NEW_BLOCK);
    }
    @Test void disabledGuardAllowsMessagesWithoutAccumulation() {
        var disabled = new RoomSettings(true, "Hi", "!", 3, false, null, null, null, null, null, null, null, null);
        for (int i = 0; i < 100; i++) assertThat(guard.check("user", "repeat", disabled)).isEqualTo(SpamGuard.Verdict.ALLOW);
        assertThat(guard.check("user", "repeat", RoomSettings.defaults())).isEqualTo(SpamGuard.Verdict.ALLOW);
    }
}
