package org.example.friend.util;

import lombok.experimental.UtilityClass;
import org.example.friend.entity.FriendRequestEntity;
import jakarta.persistence.criteria.Predicate;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class FriendRequestSpecification {

    public static Specification<FriendRequestEntity> requestsByStatus(
            Long requesterId,
            FriendRequestStatus status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("requesterId"), requesterId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<FriendRequestEntity> incomingPendingRequests(Long addresseeId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Только входящие заявки (addresseeId = текущий пользователь)
            predicates.add(criteriaBuilder.equal(root.get("addresseeId"), addresseeId));

            // Только статус PENDING
            predicates.add(criteriaBuilder.equal(root.get("status"), FriendRequestStatus.PENDING));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
