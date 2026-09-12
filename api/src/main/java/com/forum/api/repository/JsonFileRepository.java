package com.forum.api.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

public abstract class JsonFileRepository<T, ID> {

    private final Path file;
    private final Function<T, ID> idExtractor;
    private final JsonMapper mapper;
    private final JavaType listType;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected JsonFileRepository(Path file, Class<T> type, Function<T, ID> idExtractor, JsonMapper mapper) {
        this.file = file;
        this.idExtractor = idExtractor;
        this.mapper = mapper;
        this.listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        createParentDirectory();
    }

    public List<T> findAll() {
        lock.readLock().lock();
        try {
            return readAll();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<T> findById(ID id) {
        return findAll().stream().filter(entity -> idExtractor.apply(entity).equals(id)).findFirst();
    }

    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    public T save(T entity) {
        lock.writeLock().lock();
        try {
            List<T> entities = new ArrayList<>(readAll());
            ID id = idExtractor.apply(entity);
            int index = indexOf(entities, id);
            if (index >= 0) {
                entities.set(index, entity);
            } else {
                entities.add(entity);
            }
            writeAll(entities);
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteById(ID id) {
        lock.writeLock().lock();
        try {
            List<T> entities = new ArrayList<>(readAll());
            entities.removeIf(entity -> idExtractor.apply(entity).equals(id));
            writeAll(entities);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private int indexOf(List<T> entities, ID id) {
        for (int i = 0; i < entities.size(); i++) {
            if (idExtractor.apply(entities.get(i)).equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private List<T> readAll() {
        try {
            if (!Files.exists(file) || Files.size(file) == 0) {
                return new ArrayList<>();
            }
            return new ArrayList<>(mapper.readValue(file, listType));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read data file " + file, exception);
        }
    }

    private void writeAll(List<T> entities) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            mapper.writeValue(tempFile, entities);
            moveAtomically(tempFile, file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write data file " + file, exception);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    private void createParentDirectory() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create data directory for " + file, exception);
        }
    }
}
