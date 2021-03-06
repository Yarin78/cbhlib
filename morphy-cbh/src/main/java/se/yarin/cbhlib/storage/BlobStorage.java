package se.yarin.cbhlib.storage;

import lombok.NonNull;
import se.yarin.cbhlib.exceptions.ChessBaseIOException;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>
 * Interface for a simple blob storage. Blobs can be read and written
 * to given their starting offset. The length of the blob must be
 * encoded in the blob.
 * </p>
 * <p>
 * An external index is required that keep track of
 * where each blob starts, and to prevent that blobs overwrite each other.
 * </p>
 */
public interface BlobStorage {
    /**
     * Reads a blob from the storage.
     *
     * @param offset to offset to where the blob begins
     * @return a buffer containing the blob
     */
    ByteBuffer readBlob(long offset);

    /**
     * Writes a new blob to the end of the storage
     * @param blob the blob to append
     * @return the offset in the storage to the blob
     */
    long writeBlob(@NonNull ByteBuffer blob);

    /**
     * Writes a blob to the storage at the specific offset.
     * It's up to the caller to ensure that the blob doesn't overwrite anything.
     * @param offset the offset to write the blob
     * @param blob the blob to write
     */
    void writeBlob(long offset, @NonNull ByteBuffer blob);

    /**
     * Gets the current size of the storage.
     * @return the size
     */
    long getSize();

    /**
     * Inserts the specified number of bytes at the given start position.
     * All data after will be adjusted. It's up to the caller to ensure
     * that indices to any blob stored after the specified offset is updated.
     * @param offset the offset in the storage at which to insert empty bytes
     * @param noBytes the number of empty bytes to insert
     * @throws ChessBaseIOException if an IO error occurred during the insert
     */
    void insert(long offset, long noBytes);

    /**
     * Closes the storage. Any further operations on the storage will cause IO errors.
     * @throws IOException if an IO error occurs
     */
    void close() throws IOException;
}
