package org.example.user.utils;


import jakarta.persistence.criteria.Predicate;
import org.example.user.dto.UserFilterDto;
import org.example.user.entity.AccountStatus;
import org.example.user.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserSpecification {

    public static Specification<UserEntity> filter(UserFilterDto userFilterDto, Long currentUserId) {
        return filter(userFilterDto, currentUserId, List.of());
    }

    public static Specification<UserEntity> filter(
            UserFilterDto userFilterDto,
            Long currentUserId,
            Collection<Long> hiddenUserIds
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.isNull(root.get("accountStatus")),
                    cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE)
            ));

            if (currentUserId != null) {
                predicates.add(cb.notEqual(root.get("id"), currentUserId));
            }

            if (hiddenUserIds != null && !hiddenUserIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(hiddenUserIds)));
            }

            if (userFilterDto.getFirstName() != null && !userFilterDto.getFirstName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")),
                        "%" + userFilterDto.getFirstName().toLowerCase().trim() + "%"));
            }

            if (userFilterDto.getLastName() != null && !userFilterDto.getLastName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")),
                        "%" + userFilterDto.getLastName().toLowerCase().trim() + "%"));
            }

            if (userFilterDto.getNumberPhone() != null) {
                predicates.add(cb.equal(root.get("numberPhone"), userFilterDto.getNumberPhone()));
            }

            if (userFilterDto.getBirthdayFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("birthday"), userFilterDto.getBirthdayFrom()));
            }
            if (userFilterDto.getBirthdayTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("birthday"), userFilterDto.getBirthdayTo()));
            }

            if (userFilterDto.getTimeStamp() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("timeStamp"), userFilterDto.getTimeStamp()));
            }
            if (userFilterDto.getCity() != null && !userFilterDto.getCity().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("city")),
                        "%" + userFilterDto.getCity().toLowerCase().trim() + "%"));
            }

            if (userFilterDto.getDistrictName() != null && !userFilterDto.getDistrictName().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("districtName")),
                        "%" + userFilterDto.getDistrictName().toLowerCase().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}