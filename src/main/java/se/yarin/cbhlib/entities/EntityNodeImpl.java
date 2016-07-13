package se.yarin.cbhlib.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class EntityNodeImpl<T extends Entity & Comparable<T>> implements EntityNode<T> {
    @Getter
    private int entityId;
    @Getter
    private T entity;
    @Getter
    private int leftEntityId;
    @Getter
    private int rightEntityId;
    @Getter
    private int heightDif;

    public boolean isDeleted() {
        return this.leftEntityId == -999;
    }

    public EntityNode<T> update(int newLeftEntityId, int newRightEntityId, int newHeightDif) {
        return new EntityNodeImpl<>(entityId, entity, newLeftEntityId, newRightEntityId, newHeightDif);
    }

    @Override
    public String toString() {
        return "EntityNode{" +
                "entityId=" + entityId +
                ", leftEntityId=" + leftEntityId +
                ", rightEntityId=" + rightEntityId +
                ", heightDif=" + heightDif +
                '}';
    }
}
