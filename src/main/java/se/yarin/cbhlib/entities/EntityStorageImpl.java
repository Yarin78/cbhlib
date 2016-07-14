package se.yarin.cbhlib.entities;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EntityStorageImpl<T extends Entity & Comparable<T>>
        extends EntityStorageBase<T> implements EntityStorage<T> {
    private static final Logger log = LoggerFactory.getLogger(EntityStorageImpl.class);

    private static final int ENTITY_DELETED = -999;

    private final EntityNodeStorageBase<T> nodeStorage;
    private final EntityNodeStorageMetadata metadata;

    private EntityStorageImpl(@NonNull File file, @NonNull EntitySerializer<T> serializer)
            throws IOException {
        nodeStorage = new PersistentEntityNodeStorage<>(file, serializer);
        metadata = ((PersistentEntityNodeStorage) nodeStorage).getMetadata();
    }

    private EntityStorageImpl() {
        nodeStorage = new InMemoryEntityNodeStorage<>();
        metadata = new EntityNodeStorageMetadata(0, 0);
    }

    public static <T extends Entity & Comparable<T>> EntityStorageImpl open(
            File file, @NonNull EntitySerializer<T> serializer) throws IOException {
        return new EntityStorageImpl<>(file, serializer);
    }

    public static <T extends Entity & Comparable<T>> EntityStorageImpl create(
            File file, @NonNull EntitySerializer<T> serializer) throws IOException {
        PersistentEntityNodeStorage.createEmptyStorage(file, serializer);
        return open(file, serializer);
    }

    public static <T extends Entity & Comparable<T>> EntityStorageImpl createInMemory(String name) {
        return new EntityStorageImpl<T>();
    }

    // TODO: Add public static method for creating new in-memory version
    // Maybe also in-memory version that loads initial data from disk

    @Override
    public int getNumEntities() {
        return metadata.getNumEntities();
    }

    @Override
    public T getEntity(int entityId) throws IOException {
        if (entityId < 0 || entityId >= metadata.getCapacity()) {
            return null;
        }
        return nodeStorage.getEntityNode(entityId).getEntity();
    }

    @Override
    public int addEntity(@NonNull T entity) throws IOException, EntityStorageException {
        int entityId;

        TreePath result = treeSearch(entity);
        if (result != null && result.compare == 0) {
            throw new EntityStorageException("An entity with the same key already exists");
        }

        if (metadata.getFirstDeletedEntityId() >= 0) {
            // Replace a deleted entity
            entityId = metadata.getFirstDeletedEntityId();
            metadata.setFirstDeletedEntityId(nodeStorage.getEntityNode(entityId).getRightEntityId());
        } else {
            // Appended new entity to the end
            entityId = metadata.getCapacity();
            metadata.setCapacity(entityId + 1);
        }
        metadata.setNumEntities(metadata.getNumEntities() + 1);

        if (result == null) {
            metadata.setRootEntityId(entityId);
        } else {
            // TODO: The height should be updated here
            if (result.compare < 0) {
                result.node = result.node.update(entityId, result.node.getRightEntityId(), result.node.getHeightDif());
            } else {
                result.node = result.node.update(result.node.getLeftEntityId(), entityId, result.node.getHeightDif());
            }
            nodeStorage.putEntityNode(result.node);
        }
        // TODO: Balance tree

        nodeStorage.putEntityNode(nodeStorage.createNode(entityId, entity));
        nodeStorage.putMetadata(metadata);

        return entityId;
    }

    @AllArgsConstructor
    private class TreePath {
        private int compare;
        private EntityNode<T> node;
        private TreePath parent;
    }

    /**
     * Searches the tree for a specific entity. Returns a path from the root
     * to the searched entity.
     * If the entity doesn't exist in the tree, the path ends at the node in the
     * tree where the entity can be inserted.
     * @param entity the entity to search for
     * @return the most recent node in the path
     * @throws IOException if an IO error occurred when searching in the tree
     */
    private TreePath treeSearch(@NonNull T entity) throws IOException {
        return treeSearch(metadata.getRootEntityId(), null, entity);
    }

    /**
     * Searches the tree for a specific entity. Returns a path from the root
     * to the searched entity.
     * If the entity doesn't exist in the tree, the path ends at the node in the
     * tree where the entity can be inserted.
     * @param currentId the start node to search from
     * @param path the path searched for so far
     * @param entity the entity to search for
     * @return the most recent node in the path
     * @throws IOException if an IO error occurred when searching in the tree
     */
    private TreePath treeSearch(int currentId, TreePath path, @NonNull T entity) throws IOException {
        if (currentId < 0) {
            return path;
        }

        T current = getEntity(currentId);
        EntityNode<T> node = nodeStorage.getEntityNode(currentId);
        int comp = entity.compareTo(current);

        path = new TreePath(comp, node, path);
        if (comp == 0) {
            return path;
        } else if (comp < 0) {
            return treeSearch(node.getLeftEntityId(), path, entity);
        } else {
            return treeSearch(node.getRightEntityId(), path, entity);
        }
    }

    @Override
    public void putEntity(int entityId, @NonNull T entity) throws IOException, EntityStorageException {
        if (entityId < 0 || entityId >= metadata.getCapacity()) {
            throw new IllegalArgumentException(String.format("Can't put an entity with id %d when capacity is %d",
                    entityId, metadata.getCapacity()));
        }
        EntityNode<T> oldNode = nodeStorage.getEntityNode(entityId);
        if (oldNode.isDeleted()) {
            throw new IllegalArgumentException("Can't replace a deleted entity");
        }
        if (oldNode.getEntity().compareTo(entity) == 0) {
            // The key is the same, so we don't have to update the tree
            EntityNode<T> newNode = nodeStorage.createNode(entityId, entity);
            newNode = newNode.update(oldNode.getLeftEntityId(), oldNode.getRightEntityId(), oldNode.getHeightDif());
            nodeStorage.putEntityNode(newNode);
        } else {
            // TODO: Do this in a transaction?
            deleteEntity(entityId);
            int newEntityId = addEntity(entity);
            assert entityId == newEntityId; // Important!
        }
    }

    private void replaceChild(TreePath path, int newChildId) throws IOException {
        if (path == null) {
            // The root node has no parent
            metadata.setRootEntityId(newChildId);
        } else {
            EntityNode<T> node = nodeStorage.getEntityNode(path.node.getEntityId());
            if (path.compare < 0) {
                node = node.update(newChildId, node.getRightEntityId(), node.getHeightDif());
            } else {
                node = node.update(node.getLeftEntityId(), newChildId, node.getHeightDif());
            }
            nodeStorage.putEntityNode(node);
        }
    }

    @Override
    public boolean deleteEntity(int entityId) throws IOException, EntityStorageException {
        EntityNode<T> node = nodeStorage.getEntityNode(entityId);
        if (node.isDeleted()) {
            log.debug("Deleted entity with id " + entityId + " that was already deleted");
            return false;
        }

        // Find the node we want to delete in the tree
        TreePath nodePath = treeSearch(node.getEntity());
        if (nodePath == null || nodePath.compare != 0) {
            throw new EntityStorageException("Broken database structure; couldn't find the node to delete.");
        }
        nodePath = nodePath.parent;

        // Switch the node we want to delete with a successor node until it has at most one child
        // This will take at most one iteration, so we could simplify this
        while (node.getLeftEntityId() >= 0 && node.getRightEntityId() >= 0) {
            // Invariant: node is the node we want to delete, and it has two children
            // nodePath.node = the parent node
            // nodePath.compare < 0 if the deleted node is a left child, > 0 if a right child

            // Find successor node and replace it with this one
            TreePath successorPath = treeSearch(node.getRightEntityId(), null, node.getEntity());
            assert successorPath.compare < 0; // Should always be a left child
            EntityNode<T> successorNode = successorPath.node;
            // successorPath.node = the node we want to move up and replace node
            successorPath = successorPath.parent; // successorPath.node may now equal node!!

            EntityNode<T> newNode = node.update(successorNode.getLeftEntityId(), successorNode.getRightEntityId(), successorNode.getHeightDif());
            int rid = node.getRightEntityId();
            if (rid == successorNode.getEntityId()) {
                rid = node.getEntityId();
            }
            EntityNode<T> newSuccessorNode = successorNode.update(node.getLeftEntityId(), rid, node.getHeightDif());
            replaceChild(nodePath, successorNode.getEntityId());
            if (successorPath != null) {
                replaceChild(successorPath, node.getEntityId());
            }
            nodeStorage.putEntityNode(newNode);
            nodeStorage.putEntityNode(newSuccessorNode);

            node = newNode;
            nodePath = successorPath; // Won't work probably if parent to successor was node
            if (nodePath == null) {
                nodePath = new TreePath(1, newSuccessorNode, null);
            }
        }

        // Now node has at most one child!
        // nodePath.node = the parent node
        // nodePath.compare < 0 if the deleted node is a left child, > 0 if a right child
        int onlyChild = node.getLeftEntityId() >= 0 ? node.getLeftEntityId() : node.getRightEntityId();
        replaceChild(nodePath, onlyChild);

        // Nothing should now point to the node we want to delete
        EntityNode<T> deletedNode = nodeStorage.createNode(entityId, null)
                .update(ENTITY_DELETED, metadata.getFirstDeletedEntityId(), 0);

        nodeStorage.putEntityNode(deletedNode);
        metadata.setFirstDeletedEntityId(entityId);
        metadata.setNumEntities(metadata.getNumEntities() - 1);

        nodeStorage.putMetadata(metadata);
        return true;
    }



    @Override
    public void close() throws IOException {
        nodeStorage.close();
    }

    /**
     * Validates that the entity headers correctly reflects the order of the entities
     */
    public void validateStructure() throws EntityStorageException, IOException {
        if (metadata.getRootEntityId() == -1) {
            if (getNumEntities() == 0) {
                return;
            }
            throw new EntityStorageException(String.format(
                    "Header says there are %d entities in the storage but the root points to no entity.", getNumEntities()));
        }

        int sum = validate(metadata.getRootEntityId(), null, null);
        if (sum != getNumEntities()) {
            throw new EntityStorageException(String.format(
                    "Found %d entities when traversing the base but the header says there should be %d entities.", sum, getNumEntities()));
        }
    }

    private int validate(int entityId, T min, T max) throws IOException, EntityStorageException {
        // TODO: Validate height difference of left and right tree
        EntityNode<T> node = nodeStorage.getEntityNode(entityId);
        T entity = node.getEntity();
        if (node.isDeleted() || entity == null) {
            throw new EntityStorageException(String.format(
                    "Reached deleted element %d when validating the storage structure.", entityId));
        }
        if ((min != null && min.compareTo(entity) >= 0) || (max != null && max.compareTo(entity) <= 0)) {
            throw new EntityStorageException(String.format(
                    "Entity %d out of order when validating the storage structure", entityId));
        }

        // Since the range is strictly decreasing every time, we should not have to worry
        // about ending up in an infinite recursion.
        int cnt = 1;
        if (node.getLeftEntityId() != -1) {
            cnt += validate(node.getLeftEntityId(), min, entity);
        }
        if (node.getRightEntityId() != -1) {
            cnt += validate(node.getRightEntityId(), entity, max);
        }
        return cnt;
    }


    @Override
    public Stream<T> getEntityStream()  {
        int bufferSize = 1000;

        return IntStream.range(0, (metadata.getCapacity() + bufferSize - 1) / bufferSize)
                .mapToObj(rangeId -> {
                    int rangeStart = rangeId * bufferSize;
                    int rangeEnd = Math.min(metadata.getCapacity(), (rangeId + 1) * bufferSize);
                    try {
                        return nodeStorage.getEntityNodes(rangeStart, rangeEnd);
                    } catch (IOException e) {
                        throw new UncheckedEntityException("Error reading entities", e);
                    }
                })
                .flatMap(List::stream)
                .map(EntityNode::getEntity);
    }

    private class EntityStorageIterator implements Iterator<T> {

        // Invariant: treePath.node is the next entity to be returned
        // If treePath == null, there are no more entities to be returned
        private TreePath treePath;
        private final boolean ascending;

        public EntityStorageIterator(TreePath treePath, boolean ascending) throws IOException {
            this.treePath = treePath;
            this.ascending = ascending;
        }

        @Override
        public boolean hasNext() {
            return treePath != null;
        }

        @Override
        public T next() {
            try {
                // TODO: Check if any writes have happened since iterator was created
                T entity = treePath.node.getEntity();
                if (ascending && treePath.node.getRightEntityId() >= 0) {
                    treePath.compare = 1;
                    treePath = treeSearch(treePath.node.getRightEntityId(), treePath, entity);
                } else if (!ascending && treePath.node.getLeftEntityId() >= 0) {
                    treePath.compare = -1;
                    treePath = treeSearch(treePath.node.getLeftEntityId(), treePath, entity);
                } else {
                    treePath = treePath.parent;
                    while (treePath != null && treePath.compare * (ascending ? 1 : -1) > 0) {
                        treePath = treePath.parent;
                    }
                }
                return entity;
            } catch (IOException e) {
                throw new UncheckedEntityException("Error iterating entities", e);
            }
        }
    }

    private Iterator<T> getOrderedIterator(T startEntity, boolean ascending) throws IOException {
        TreePath treePath = null;
        if (startEntity == null) {
            int currentId = metadata.getRootEntityId();
            while (currentId >= 0) {
                EntityNode<T> node = nodeStorage.getEntityNode(currentId);
                treePath = new TreePath(ascending ? -1 : 1, node, treePath);
                currentId = ascending ? node.getLeftEntityId() : node.getRightEntityId();
            }
        } else {
            treePath = treeSearch(startEntity);
            while (treePath != null && treePath.compare * (ascending ? 1 : -1) > 0) {
                treePath = treePath.parent;
            }
        }
        return new EntityStorageIterator(treePath, ascending);
    }

    public Iterator<T> getOrderedAscendingIterator(T startEntity) throws IOException {
        return getOrderedIterator(startEntity, true);
    }

    public Iterator<T> getOrderedDescendingIterator(T startEntity) throws IOException {
        return getOrderedIterator(startEntity, false);
    }
}
