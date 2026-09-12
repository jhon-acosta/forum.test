package com.forum.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.forum.api.config.ForumProperties;
import com.forum.api.model.User;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

class JsonFileRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonMapper mapper;
    private UserRepository repository;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        repository = new UserRepository(mapper, new ForumProperties(tempDir, 3, List.of("http://localhost:4200")));
    }

    private User user(String username) {
        return new User(UUID.randomUUID(), username, "hash", 3, LocalDateTime.now());
    }

    @Test
    void findAllReturnsEmptyWhenFileDoesNotExist() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void savePersistsEntityAndFindByIdReturnsIt() {
        User user = user("jhon");

        repository.save(user);

        assertThat(repository.findById(user.id())).contains(user);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void saveReplacesEntityWithSameId() {
        User user = user("jhon");
        repository.save(user);

        repository.save(new User(user.id(), "jhon-updated", "new-hash", 5, user.createdAt()));

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(user.id()).orElseThrow().username()).isEqualTo("jhon-updated");
    }

    @Test
    void deleteByIdRemovesEntity() {
        User user = user("jhon");
        repository.save(user);

        repository.deleteById(user.id());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findByUsernameAndExistsByUsername() {
        repository.save(user("jhon"));

        assertThat(repository.findByUsername("jhon")).isPresent();
        assertThat(repository.findByUsername("other")).isEmpty();
        assertThat(repository.existsByUsername("jhon")).isTrue();
        assertThat(repository.existsByUsername("other")).isFalse();
    }

    @Test
    void dataIsVisibleToNewRepositoryInstance() {
        repository.save(user("jhon"));

        UserRepository reopened = new UserRepository(mapper, new ForumProperties(tempDir, 3, List.of()));

        assertThat(reopened.findByUsername("jhon")).isPresent();
    }

    @Test
    void emptyFileIsTreatedAsEmptyCollection() throws Exception {
        Files.writeString(tempDir.resolve("users.json"), "");

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void datesAreStoredAsIsoStrings() throws Exception {
        repository.save(user("jhon"));

        String content = Files.readString(tempDir.resolve("users.json"));

        assertThat(content).contains("\"createdAt\":\"");
    }

    @Test
    void concurrentSavesKeepFileConsistent() throws Exception {
        int threads = 8;
        int perThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int thread = 0; thread < threads; thread++) {
            final int currentThread = thread;
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        repository.save(user("user-" + currentThread + "-" + i));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(repository.findAll()).hasSize(threads * perThread);
        List<User> parsed = mapper.readValue(
                tempDir.resolve("users.json"),
                mapper.getTypeFactory().constructCollectionType(List.class, User.class));
        assertThat(parsed).hasSize(threads * perThread);
    }
}
