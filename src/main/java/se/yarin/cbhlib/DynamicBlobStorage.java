package se.yarin.cbhlib;

import lombok.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface for a storage of dynamically sized blobs.
 * Requires an external index that keeps track of where the blobs start.
 * The length of each blob is encoded in the blob.
 */
public interface DynamicBlobStorage {
    /**
     * Gets a blog from the storage.
     *
     * @param offset an offset where the blob starts
     * @return a buffer containing the blob
     */
    ByteBuffer getBlob(int offset) throws IOException;

    /**
     * Adds a new blob to the storage
     * @param blob the blob to add
     * @return the offset in the storage that the blob received
     */
    int addBlob(@NonNull ByteBuffer blob) throws IOException;

    /**
     * Updates an existing blob in the storage. If possible, the blob
     * will be stored at the same offset as it was previously.
     * @param oldOffset the old offset of the blob
     * @param blob the blob to put
     * @return the offset in the storage that the blob received
     */
    int putBlob(int oldOffset, @NonNull ByteBuffer blob) throws IOException;

    /**
     * Gets the current size of the storage.
     * @return the size
     */
    int getSize();

    /**
     * Inserts the specified number of bytes at the given start position.
     * All data after will be adjusted. It's up to the caller to ensure
     * that any pointers to positions after start is updated.
     * @param offset the offset in the file at which to insert empty bytes
     * @param noBytes the number of empty bytes to insert
     * @throws IOException if an IO error occurred during the insert
     */
    void insert(int offset, int noBytes) throws IOException;

    /**
     * Closes the storage. Any further operations on the storage will cause IO errors.
     * @throws IOException if an IO error occurs
     */
    void close() throws IOException;
}
